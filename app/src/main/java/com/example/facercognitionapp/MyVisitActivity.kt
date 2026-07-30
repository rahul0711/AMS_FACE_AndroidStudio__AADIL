package com.example.facercognitionapp

import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.facercognitionapp.databinding.ContentMyVisitBinding
import com.example.facercognitionapp.databinding.ItemMyVisitCardBinding
import com.example.facercognitionapp.model.VisitorDetailsDto
import com.example.facercognitionapp.network.ApiClient
import com.example.facercognitionapp.ui.core.BaseDrawerContentActivity
import com.example.facercognitionapp.ui.myvisit.MyVisitCardBinder
import com.example.facercognitionapp.util.SessionHelper
import com.example.facercognitionapp.util.VisitorDetailsJsonParser
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class MyVisitActivity : BaseDrawerContentActivity() {

    private lateinit var binding: ContentMyVisitBinding

    private lateinit var yearOptions: List<Int>
    private var dayOptions: List<Int> = emptyList()
    private var suppressFilterReload = true
    private var debouncedLoadJob: Job? = null

    override fun contentLayoutRes() = R.layout.content_my_visit

    override fun screenTitleRes() = R.string.title_my_visit

    override fun drawerMenuItemId() = R.id.nav_my_visit

    override fun onContentInflated(savedInstanceState: android.os.Bundle?) {
        binding = ContentMyVisitBinding.bind(shellBinding.contentContainer.getChildAt(0))
        setupDateFilters()
        loadVisitsForSelectedDate()
    }

    override fun onDestroy() {
        debouncedLoadJob?.cancel()
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
            if (parent?.id == R.id.spinnerMonth || parent?.id == R.id.spinnerYear) {
                refreshDaySpinner()
            }
            scheduleLoadVisits()
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }

    private fun setupDateFilters() {
        val cal = Calendar.getInstance()
        val nowMonth = cal.get(Calendar.MONTH)
        val nowYear = cal.get(Calendar.YEAR)
        val nowDay = cal.get(Calendar.DAY_OF_MONTH)

        val months = resources.getStringArray(R.array.attendance_month_names).toList()
        binding.spinnerMonth.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            months
        )

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
        binding.spinnerDay.onItemSelectedListener = filterSpinnerListener

        binding.spinnerMonth.setSelection(nowMonth)
        binding.spinnerYear.setSelection(
            yearOptions.indexOf(nowYear).coerceIn(0, yearOptions.lastIndex)
        )
        refreshDaySpinner()
        binding.spinnerDay.setSelection(dayOptions.indexOf(nowDay).coerceAtLeast(0))

        suppressFilterReload = false
    }

    private fun refreshDaySpinner() {
        val month = binding.spinnerMonth.selectedItemPosition + 1
        val year = yearOptions.getOrNull(binding.spinnerYear.selectedItemPosition)
            ?: Calendar.getInstance().get(Calendar.YEAR)

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val previousDay = dayOptions.getOrNull(binding.spinnerDay.selectedItemPosition)
        dayOptions = (1..maxDay).toList()

        binding.spinnerDay.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            dayOptions.map { it.toString() }
        )

        val newIndex = dayOptions.indexOf(previousDay).let {
            if (it >= 0) it else dayOptions.lastIndex
        }
        binding.spinnerDay.setSelection(newIndex.coerceIn(0, dayOptions.lastIndex))
    }

    private fun scheduleLoadVisits() {
        debouncedLoadJob?.cancel()
        debouncedLoadJob = lifecycleScope.launch {
            delay(FILTER_DEBOUNCE_MS)
            loadVisitsForSelectedDate()
        }
    }

    private fun resolveVisitorId(): Int {
        val fromSession = SessionHelper.visitorId(this)
        if (fromSession > 0) return fromSession
        return DEFAULT_VISITOR_ID
    }

    private fun loadVisitsForSelectedDate() {
        val visitorId = resolveVisitorId()
        val month = binding.spinnerMonth.selectedItemPosition + 1
        val year = yearOptions.getOrNull(binding.spinnerYear.selectedItemPosition)
            ?: Calendar.getInstance().get(Calendar.YEAR)
        val day = dayOptions.getOrNull(binding.spinnerDay.selectedItemPosition)
            ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

        binding.progressRecent.visibility = View.VISIBLE
        binding.tvVisitError.visibility = View.GONE
        binding.layoutVisitList.visibility = View.GONE
        binding.tvVisitEmpty.visibility = View.GONE
        binding.tvVisitSectionTitle.visibility = View.GONE
        binding.tvVisitCount.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val response = ApiClient.api.visitorIdAndMonthAndDateWiseDetails(
                        month = month,
                        year = year,
                        day = day,
                        visitorId = visitorId
                    )
                    ApiResult(
                        ok = response.isSuccessful,
                        httpCode = response.code(),
                        body = response.body()?.string().orEmpty(),
                        errorBody = response.errorBody()?.string().orEmpty()
                    )
                }

                var isSuccess = true
                var backendMessage: String? = null
                try {
                    val rawJson = result.body.trim()
                    if (rawJson.startsWith("{")) {
                        val root = JsonParser.parseString(rawJson)
                        if (root.isJsonObject) {
                            val obj = root.asJsonObject
                            val successEl = obj.get("success") ?: obj.get("Success")
                            val statusEl = obj.get("status") ?: obj.get("Status")
                            val hasSuccess = successEl != null && !successEl.isJsonNull
                            val hasStatus = statusEl != null && !statusEl.isJsonNull

                            isSuccess = when {
                                hasSuccess -> when {
                                    successEl.isJsonPrimitive && successEl.asJsonPrimitive.isBoolean -> successEl.asBoolean
                                    successEl.isJsonPrimitive && successEl.asJsonPrimitive.isString -> {
                                        val s = successEl.asString.lowercase()
                                        s == "true" || s == "1" || s == "yes"
                                    }
                                    successEl.isJsonPrimitive && successEl.asJsonPrimitive.isNumber -> successEl.asInt != 0
                                    else -> true
                                }
                                hasStatus -> when {
                                    statusEl.isJsonPrimitive && statusEl.asJsonPrimitive.isBoolean -> statusEl.asBoolean
                                    statusEl.isJsonPrimitive && statusEl.asJsonPrimitive.isString -> {
                                        val s = statusEl.asString.lowercase()
                                        s == "true" || s == "1" || s == "yes"
                                    }
                                    statusEl.isJsonPrimitive && statusEl.asJsonPrimitive.isNumber -> statusEl.asInt != 0
                                    else -> true
                                }
                                else -> true
                            }
                            if (!isSuccess) {
                                backendMessage = obj.get("message")?.takeIf { !it.isJsonNull }?.asString
                                    ?: obj.get("Message")?.takeIf { !it.isJsonNull }?.asString
                                    ?: obj.get("msg")?.takeIf { !it.isJsonNull }?.asString
                                    ?: obj.get("Msg")?.takeIf { !it.isJsonNull }?.asString
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore parse errors or handle it as success = true
                }

                val visits = if (isSuccess) {
                    VisitorDetailsJsonParser.sortByEntryDateDescending(
                        VisitorDetailsJsonParser.parseList(result.body)
                    )
                } else {
                    emptyList()
                }

                val monthName = resources.getStringArray(R.array.attendance_month_names)
                    .getOrNull(month - 1) ?: month.toString()
                val dateLabel = "$day $monthName $year"

                withContext(Dispatchers.Main) {
                    binding.progressRecent.visibility = View.GONE

                    if (!isSuccess) {
                        showError(backendMessage ?: getString(R.string.my_visit_error_generic))
                        binding.layoutVisitList.removeAllViews()
                        return@withContext
                    }

                    if (!result.ok) {
                        showError(getString(R.string.my_visit_error_http, result.httpCode))
                        return@withContext
                    }

                    if (visits.isEmpty()) {
                        binding.layoutVisitList.removeAllViews()
                        binding.tvVisitEmpty.visibility = View.VISIBLE
                        binding.tvVisitEmpty.text = getString(R.string.my_visit_no_data)
                        return@withContext
                    }

                    binding.tvVisitSectionTitle.visibility = View.VISIBLE
                    binding.tvVisitSectionTitle.text =
                        getString(R.string.my_visit_list_title, dateLabel)
                    binding.tvVisitCount.visibility = View.VISIBLE
                    binding.tvVisitCount.text =
                        getString(R.string.my_visit_count, visits.size)
                    binding.layoutVisitList.visibility = View.VISIBLE
                    renderVisitCards(visits)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressRecent.visibility = View.GONE
                    showError(e.localizedMessage ?: getString(R.string.my_visit_error_generic))
                }
            }
        }
    }

    /** One card per API row — avoids RecyclerView inside ScrollView showing only one item. */
    private fun renderVisitCards(visits: List<VisitorDetailsDto>) {
        binding.layoutVisitList.removeAllViews()
        val inflater = LayoutInflater.from(this)
        for (visit in visits) {
            val cardBinding = ItemMyVisitCardBinding.inflate(
                inflater,
                binding.layoutVisitList,
                true
            )
            MyVisitCardBinder.bind(cardBinding, visit)
        }
    }

    private fun showError(message: String) {
        binding.tvVisitError.visibility = View.VISIBLE
        binding.tvVisitError.text = message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private data class ApiResult(
        val ok: Boolean,
        val httpCode: Int,
        val body: String,
        val errorBody: String
    )

    companion object {
        private const val DEFAULT_VISITOR_ID = 6063
        private const val YEAR_RANGE_PAST = 30
        private const val YEAR_RANGE_FUTURE = 2
        private const val FILTER_DEBOUNCE_MS = 320L
    }
}
