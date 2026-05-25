package com.example.facercognitionapp.model

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName

/**
 * Parsed body from POST /Recognize. The app shows the server [displayMessage] as-is
 * and only records a punch when [isPunchSuccess] is true.
 *
 * Success: `{"message":"IN Punch Done Successfully"}`
 * Failure: `{"match":false,"score":0,"threshold":0,"message":"Face not recognised. Please try again."}`
 */
data class RecognizeResponse(
    @SerializedName("match") val match: Boolean? = null,
    @SerializedName("score") val score: Double? = null,
    @SerializedName("threshold") val threshold: Double? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("msg") val msg: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("punch_time") val punchTime: String? = null,
    @SerializedName("punchTime") val punchTimeAlt: String? = null,
    @SerializedName("employeeName") val employeeName: String? = null,
    @SerializedName("attendanceId") val attendanceId: Int? = null,
    @SerializedName("employeeId") val responseEmployeeId: Int? = null
) {
    val displayMessage: String?
        get() = message?.takeIf { it.isNotBlank() } ?: msg?.takeIf { it.isNotBlank() }

    val resolvedPunchTime: String?
        get() = punchTime?.takeIf { it.isNotBlank() } ?: punchTimeAlt?.takeIf { it.isNotBlank() }

    /** Text to show the user: prefer `message` / `msg`, else nothing (caller may fall back to raw JSON). */
    fun primaryDisplayText(): String? = displayMessage

    /**
     * Backend failure: `{ "match": false, "message": "Face not recognised..." }`
     * or a recognition-failure message without `match`.
     */
    fun isPunchFailure(): Boolean {
        if (match == false) return true
        val msg = displayMessage?.lowercase() ?: return false
        return msg.contains("not recogn") ||
            msg.contains("not match") ||
            msg.contains("try again")
    }

    /**
     * Backend success: `{ "message": "IN Punch Done Successfully" }` (no `match` field).
     * Only treat as punch success when the server message confirms it — never on unknown bodies.
     */
    fun isPunchSuccess(): Boolean {
        if (isPunchFailure()) return false
        val msg = displayMessage?.lowercase() ?: return false
        return msg.contains("successfully") || msg.contains("punch done")
    }

    companion object {

        fun fromJson(json: String?): RecognizeResponse? {
            if (json.isNullOrBlank()) return null
            return try {
                val o = JsonParser.parseString(json).asJsonObject
                RecognizeResponse(
                    match = readBoolean(o, "match") ?: readBoolean(o, "Match"),
                    score = readDouble(o, "score"),
                    threshold = readDouble(o, "threshold"),
                    message = readString(o, "message"),
                    msg = readString(o, "msg"),
                    status = readString(o, "status"),
                    punchTime = readString(o, "punch_time"),
                    punchTimeAlt = readString(o, "punchTime"),
                    employeeName = readString(o, "employeeName"),
                    attendanceId = readInt(o, "attendanceId"),
                    responseEmployeeId = readInt(o, "employeeId")
                )
            } catch (_: Exception) {
                null
            }
        }

        private fun readString(o: JsonObject, key: String): String? {
            if (!o.has(key) || o.get(key).isJsonNull) return null
            return o.get(key).asString
        }

        private fun readInt(o: JsonObject, key: String): Int? {
            if (!o.has(key) || o.get(key).isJsonNull) return null
            val e = o.get(key)
            return when {
                e.isJsonPrimitive && e.asJsonPrimitive.isNumber -> e.asInt
                e.isJsonPrimitive && e.asJsonPrimitive.isString ->
                    e.asString.toIntOrNull()
                else -> null
            }
        }

        private fun readDouble(o: JsonObject, key: String): Double? {
            if (!o.has(key) || o.get(key).isJsonNull) return null
            val e = o.get(key)
            return when {
                e.isJsonPrimitive && e.asJsonPrimitive.isNumber -> e.asDouble
                e.isJsonPrimitive && e.asJsonPrimitive.isString ->
                    e.asString.toDoubleOrNull()
                else -> null
            }
        }

        private fun readBoolean(o: JsonObject, key: String): Boolean? {
            if (!o.has(key) || o.get(key).isJsonNull) return null
            val e = o.get(key)
            return when {
                e.isJsonPrimitive && e.asJsonPrimitive.isBoolean -> e.asBoolean
                e.isJsonPrimitive && e.asJsonPrimitive.isNumber -> e.asInt != 0
                e.isJsonPrimitive && e.asJsonPrimitive.isString -> {
                    val s = e.asString.lowercase()
                    when (s) {
                        "true", "1", "yes" -> true
                        "false", "0", "no" -> false
                        else -> null
                    }
                }
                else -> null
            }
        }
    }
}
