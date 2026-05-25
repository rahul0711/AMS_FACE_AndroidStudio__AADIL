package com.example.facercognitionapp.util

import android.content.Context
import android.util.Log
import com.google.gson.Gson

/**
 * Logcat helper for Client Visit / My Visit debugging.
 * Filter Logcat by tag: ClientVisit | MyVisit | VisitSession
 */
object VisitDebugLog {

    const val TAG_CLIENT_VISIT = "ClientVisit"
    const val TAG_MY_VISIT = "MyVisit"
    const val TAG_SESSION = "VisitSession"

    private val gson = Gson()
    private const val MAX_CHUNK = 3500

    fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    fun logSession(context: Context, tag: String = TAG_SESSION) {
        val prefs = context.getSharedPreferences(SessionHelper.PREFS_AUTH, Context.MODE_PRIVATE)
        d(
            tag,
            """
            |--- auth session ---
            |logged_in=${prefs.getBoolean("logged_in", false)}
            |visitor_id=${prefs.getInt("visitor_id", 0)}
            |employee_id=${prefs.getInt("employee_id", 0)}
            |user_id=${prefs.getInt("user_id", 0)}
            |resolved_visitorId=${SessionHelper.visitorId(context)}
            |company_id=${prefs.getInt("company_id", 0)}
            |unit_id=${prefs.getInt("unit_id", 0)}
            |employee_card_no=${prefs.getString("employee_card_no", null)}
            |employee_name=${prefs.getString("employee_name", null)}
            |--- end session ---
            """.trimMargin()
        )
    }

    fun logJson(tag: String, label: String, raw: String?) {
        if (raw.isNullOrBlank()) {
            d(tag, "$label: (empty body)")
            return
        }
        d(tag, "$label (${raw.length} chars):")
        chunkLog(tag, raw)
    }

    fun logObject(tag: String, label: String, obj: Any?) {
        val json = runCatching { gson.toJson(obj) }.getOrElse { obj.toString() }
        logJson(tag, label, json)
    }

    fun logApiResult(
        tag: String,
        endpoint: String,
        httpCode: Int,
        success: Boolean,
        rawBody: String?,
        errorBody: String? = null
    ) {
        d(tag, "API $endpoint → HTTP $httpCode success=$success")
        logJson(tag, "response body", rawBody)
        if (!errorBody.isNullOrBlank()) {
            logJson(tag, "error body", errorBody)
        }
    }

    private fun chunkLog(tag: String, text: String) {
        if (text.length <= MAX_CHUNK) {
            Log.d(tag, text)
            return
        }
        var offset = 0
        var part = 1
        while (offset < text.length) {
            val end = (offset + MAX_CHUNK).coerceAtMost(text.length)
            Log.d(tag, "part $part: ${text.substring(offset, end)}")
            offset = end
            part++
        }
    }
}
