package com.example.facercognitionapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ExperimentalGetImage
import androidx.lifecycle.lifecycleScope
import com.example.facercognitionapp.databinding.ContentPunchBinding
import com.example.facercognitionapp.network.ApiClient
import com.example.facercognitionapp.ui.core.BaseDrawerContentActivity
import com.example.facercognitionapp.util.AttendanceDateTimeFormat
import com.example.facercognitionapp.util.AttendanceReportJsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@ExperimentalGetImage
class PunchActivity : BaseDrawerContentActivity() {

    private lateinit var contentBinding: ContentPunchBinding

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            refreshCurrentDatePunchFromServer()
        }
    }

    override fun contentLayoutRes() = R.layout.content_punch
    override fun screenTitleRes() = R.string.title_dashboard
    override fun drawerMenuItemId() = R.id.nav_dashboard

    override fun onContentInflated(savedInstanceState: Bundle?) {
        contentBinding = ContentPunchBinding.bind(shellBinding.contentContainer.getChildAt(0))
        bindEmployeeName()
        contentBinding.btnIn.setOnClickListener { openCamera(inOutFlag = 1, punchType = "IN") }
        contentBinding.btnOut.setOnClickListener { openCamera(inOutFlag = 2, punchType = "OUT") }
        refreshCurrentDatePunchFromServer()
    }

    override fun onResume() {
        super.onResume()
        bindEmployeeName()
        refreshCurrentDatePunchFromServer()
    }

    private fun bindEmployeeName() {
        val prefs = getSharedPreferences("auth", MODE_PRIVATE)
        val name = prefs.getString("employee_name", null)?.trim().orEmpty()
            .ifBlank { getString(R.string.nav_header_guest) }
        val cardNo = prefs.getString("employee_card_no", null)?.trim().orEmpty()
            .ifBlank { "—" }
        contentBinding.tvEmployeeName.text = getString(R.string.punch_employee_format, name, cardNo)
    }

    private fun refreshCurrentDatePunchFromServer() {
        val prefs = getSharedPreferences("auth", MODE_PRIVATE)
        val employeeId = prefs.getInt("employee_id", 0)
        if (employeeId == 0) {
            applyPunchLabelsFromPrefsFallback()
            return
        }

        lifecycleScope.launch {
            try {
                val (successful, row) = withContext(Dispatchers.IO) {
                    val response = ApiClient.api.employeeWiseCurrentDateInOutPunch(employeeId)
                    val raw = response.body()?.string().orEmpty()
                    val list = AttendanceReportJsonParser.parseCurrentDatePunch(raw)
                    Pair(response.isSuccessful, list.firstOrNull())
                }

                withContext(Dispatchers.Main) {
                    if (successful && row != null) {
                        applyInPunchText(row.inTime)
                        applyOutPunchText(row.outTime)
                        return@withContext
                    }
                    applyPunchLabelsFromPrefsFallback()
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    applyPunchLabelsFromPrefsFallback()
                }
            }
        }
    }

    private fun applyPunchLabelsFromPrefsFallback() {
        val prefs = getSharedPreferences("auth", MODE_PRIVATE)
        applyInPunchText(prefs.getString(PREF_LAST_IN_TIME, null))
        applyOutPunchText(prefs.getString(PREF_LAST_OUT_TIME, null))
    }

    private fun applyInPunchText(raw: String?) {
        contentBinding.tvInPunch.text = formatPunchButtonLabel(
            prefixWithTime = R.string.punch_in_with_time,
            prefixEmpty = R.string.punch_in_label,
            raw = raw
        )
    }

    private fun applyOutPunchText(raw: String?) {
        contentBinding.tvOutPunch.text = formatPunchButtonLabel(
            prefixWithTime = R.string.punch_out_with_time,
            prefixEmpty = R.string.punch_out_label,
            raw = raw
        )
    }

    private fun formatPunchButtonLabel(
        prefixWithTime: Int,
        prefixEmpty: Int,
        raw: String?
    ): String {
        val fromApi = AttendanceDateTimeFormat.formatForPunchButton(raw)
        val formatted = fromApi.ifBlank { formatPunchFromPrefs(raw) }
        return if (formatted.isBlank()) {
            getString(prefixEmpty)
        } else {
            getString(prefixWithTime, formatted)
        }
    }

    /** Local prefs store ISO from face punch success. */
    private fun formatPunchFromPrefs(time: String?): String {
        if (time.isNullOrBlank()) return ""
        return try {
            val dt = LocalDateTime.parse(time, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        } catch (_: Exception) {
            AttendanceDateTimeFormat.formatForPunchButton(time)
        }
    }

    private fun openCamera(inOutFlag: Int, punchType: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_IN_OUT_FLAG, inOutFlag)
            putExtra(MainActivity.EXTRA_PUNCH_TYPE, punchType)
        }
        launcher.launch(intent)
    }

    companion object {
        const val PREF_LAST_IN_TIME = "last_in_punch_time"
        const val PREF_LAST_OUT_TIME = "last_out_punch_time"
    }
}
