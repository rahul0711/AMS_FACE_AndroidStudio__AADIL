package com.example.facercognitionapp

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.facercognitionapp.databinding.ContentAttendanceDetailsBinding
import com.example.facercognitionapp.model.FullMonthInOutDayDto
import com.example.facercognitionapp.network.ApiClient
import com.example.facercognitionapp.ui.attendance.AttendanceDayAdapter
import com.example.facercognitionapp.ui.core.BaseDrawerContentActivity
import com.example.facercognitionapp.util.AttendanceReportJsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AttendanceDetailsActivity : BaseDrawerContentActivity() {

    private lateinit var binding: ContentAttendanceDetailsBinding
    private val adapter = AttendanceDayAdapter()

    /** Spinner index 0 = January → API month 1 */
    private lateinit var yearOptions: List<Int>

    /** While true, spinner callbacks do not trigger API reload (programmatic setup). */
    private var suppressFilterReload = true

    private var debouncedFilterJob: Job? = null

    override fun contentLayoutRes() = R.layout.content_attendance_details

    override fun screenTitleRes() = R.string.title_attendance_details

    override fun drawerMenuItemId() = R.id.nav_attendance_details

    override fun onContentInflated(savedInstanceState: Bundle?) {
        binding = ContentAttendanceDetailsBinding.bind(
            shellBinding.contentContainer.getChildAt(0)
        )
        binding.recyclerAttendance.layoutManager = LinearLayoutManager(this)
        binding.recyclerAttendance.adapter = adapter

        setupMonthYearFilters()
    }

    override fun onDestroy() {
        debouncedFilterJob?.cancel()
        super.onDestroy()
    }

    private val filterSpinnerListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
            parent: AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long
        ) {
            if (suppressFilterReload) return
            scheduleLoadFromFilters()
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            // no-op
        }
    }

    private fun setupMonthYearFilters() {
        val cal = Calendar.getInstance()
        val nowMonth = cal.get(Calendar.MONTH)
        val nowYear = cal.get(Calendar.YEAR)

        val months = resources.getStringArray(R.array.attendance_month_names).toList()
        binding.spinnerMonth.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            months
        )

        // Long scrollable list (dropdown scrolls on tap); link: ?Month=&Year=&EmployeeId=
        val startYear = nowYear - YEAR_RANGE_PAST
        val endYear = nowYear + YEAR_RANGE_FUTURE
        yearOptions = (startYear..endYear).toList()
        binding.spinnerYear.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            yearOptions.map { it.toString() }
        )

        suppressFilterReload = true
        binding.spinnerMonth.onItemSelectedListener = filterSpinnerListener
        binding.spinnerYear.onItemSelectedListener = filterSpinnerListener

        binding.spinnerMonth.setSelection(nowMonth)
        binding.spinnerYear.setSelection(yearOptions.indexOf(nowYear).coerceIn(0, yearOptions.lastIndex))

        suppressFilterReload = false
        loadReportFromFilters()
    }

    /** Debounce so flicking month/year does not spam the API. */
    private fun scheduleLoadFromFilters() {
        debouncedFilterJob?.cancel()
        debouncedFilterJob = lifecycleScope.launch {
            delay(FILTER_DEBOUNCE_MS)
            loadReportFromFilters()
        }
    }

    private fun loadReportFromFilters() {
        val monthIndex = binding.spinnerMonth.selectedItemPosition
        val month = monthIndex + 1
        val year = yearOptions.getOrNull(binding.spinnerYear.selectedItemPosition)
            ?: Calendar.getInstance().get(Calendar.YEAR)
        loadReport(month, year)
    }

    private fun loadReport(month: Int, year: Int) {
        val prefs = getSharedPreferences("auth", MODE_PRIVATE)
        val employeeId = prefs.getInt("employee_id", 0)
        if (employeeId == 0) {
            showError(getString(R.string.attendance_error_no_employee))
            return
        }

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        binding.progressAttendance.visibility = View.VISIBLE
        binding.layoutTable.visibility = View.GONE
        binding.tvError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val (ok, httpCode, rawBody) = withContext(Dispatchers.IO) {
                    val response = ApiClient.api.employeeWiseInOutReport(month, year, employeeId)
                    val raw = response.body()?.string().orEmpty()
                    Triple(response.isSuccessful, response.code(), raw)
                }

                val reports = AttendanceReportJsonParser.parseInOutReport(rawBody)
                val report = reports.firstOrNull()
                val rows = report?.fullMonthInOut.orEmpty()

                withContext(Dispatchers.Main) {
                    binding.progressAttendance.visibility = View.GONE

                    if (!ok) {
                        showError(getString(R.string.attendance_error_http, httpCode))
                        return@withContext
                    }

                    val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
                    binding.tvReportPeriod.text = monthLabel

                    val name = report?.employeeName?.takeIf { it.isNotBlank() }
                        ?: prefs.getString("employee_name", null)?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.nav_header_guest)
                    binding.tvEmployeeName.text = name

                    binding.tvSummaryPresent.text = getString(
                        R.string.attendance_summary_present,
                        report?.totalPresent ?: 0
                    )
                    binding.tvSummaryAbsent.text = getString(
                        R.string.attendance_summary_absent,
                        report?.totalAbsent ?: 0
                    )

                    binding.layoutTable.visibility = View.VISIBLE
                    binding.tvError.visibility = View.GONE

                    if (rows.isEmpty()) {
                        val suspiciousBody =
                            rawBody.isNotBlank() &&
                                rawBody.trim() != "[]" &&
                                reports.isEmpty()

                        if (suspiciousBody) {
                            showError(getString(R.string.attendance_parse_error))
                            Toast.makeText(
                                this@AttendanceDetailsActivity,
                                getString(R.string.attendance_parse_error),
                                Toast.LENGTH_LONG
                            ).show()
                            return@withContext
                        }

                        adapter.submit(emptyList())
                        return@withContext
                    }

                    adapter.submit(sortByDay(rows))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressAttendance.visibility = View.GONE
                    val msg = e.localizedMessage ?: getString(R.string.attendance_error_generic)
                    showError(msg)
                    Toast.makeText(
                        this@AttendanceDetailsActivity,
                        msg,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun sortByDay(rows: List<FullMonthInOutDayDto>): List<FullMonthInOutDayDto> {
        return rows.sortedBy { it.day ?: 0 }
    }

    private fun showError(message: String) {
        binding.progressAttendance.visibility = View.GONE
        binding.layoutTable.visibility = View.GONE
        binding.tvError.visibility = View.VISIBLE
        binding.tvError.text = message
    }

    companion object {
        private const val YEAR_RANGE_PAST = 30
        private const val YEAR_RANGE_FUTURE = 2
        private const val FILTER_DEBOUNCE_MS = 320L
    }
}
