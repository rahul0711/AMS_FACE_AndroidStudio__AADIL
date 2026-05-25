package com.example.facercognitionapp.util

import java.text.SimpleDateFormat
import java.util.Locale

object AttendanceDateTimeFormat {

    private val serverDateTime = SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.ENGLISH)
    private val timeOut = SimpleDateFormat("hh:mm a", Locale.US)
    private val dateOut = SimpleDateFormat("dd MMM yyyy", Locale.US)
    private val punchButtonOut = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    /** API `inTime` / `outTime` like `20-May-2026 10:01` → ("10:01 AM", "20 May 2026") or empty if blank. */
    fun formatPunchLine(raw: String?): Pair<String, String> {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return "" to ""
        return try {
            val d = serverDateTime.parse(s) ?: return s to ""
            timeOut.format(d) to dateOut.format(d)
        } catch (_: Exception) {
            s to ""
        }
    }

    fun displayOnCard(raw: String?, emptyLabel: String): String {
        val (time, date) = formatPunchLine(raw)
        if (time.isEmpty()) return emptyLabel
        return if (date.isNotEmpty()) "$time  ·  $date" else time
    }

    /** Dashboard punch buttons: `2026-05-23 10:22` */
    fun formatForPunchButton(raw: String?): String {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return ""
        return try {
            val d = serverDateTime.parse(s) ?: return s
            punchButtonOut.format(d)
        } catch (_: Exception) {
            s
        }
    }
}
