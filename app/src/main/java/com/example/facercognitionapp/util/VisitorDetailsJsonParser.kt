package com.example.facercognitionapp.util

import com.example.facercognitionapp.model.VisitorDetailsDto
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Locale

object VisitorDetailsJsonParser {

    private val gson = Gson()
    private val listType = object : TypeToken<List<VisitorDetailsDto>>() {}.type

    private val entryDateFormats = listOf(
        SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH),
        SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.ENGLISH),
        SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.ENGLISH),
        SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    )

    fun parseList(raw: String?): List<VisitorDetailsDto> {
        if (raw.isNullOrBlank()) {
            VisitDebugLog.d(VisitDebugLog.TAG_MY_VISIT, "parseList: raw body empty")
            return emptyList()
        }
        return try {
            val root = unwrapToJsonElement(raw.trim(), maxDepth = 4)
            if (root == null) {
                VisitDebugLog.e(VisitDebugLog.TAG_MY_VISIT, "parseList: could not unwrap JSON root")
                return emptyList()
            }
            VisitDebugLog.d(
                VisitDebugLog.TAG_MY_VISIT,
                "parseList: root type=${if (root.isJsonArray) "array" else if (root.isJsonObject) "object" else "other"}"
            )
            val arrayRoot = extractArrayRoot(root)
            if (arrayRoot == null) {
                VisitDebugLog.e(VisitDebugLog.TAG_MY_VISIT, "parseList: no array/object to parse")
                return emptyList()
            }
            val list = when {
                arrayRoot.isJsonArray -> gson.fromJson(arrayRoot, listType) ?: emptyList()
                arrayRoot.isJsonObject -> listOf(
                    gson.fromJson(arrayRoot, VisitorDetailsDto::class.java)
                )
                else -> emptyList()
            }
            VisitDebugLog.d(VisitDebugLog.TAG_MY_VISIT, "parseList: parsed ${list.size} visit(s)")
            list.forEachIndexed { i, v ->
                VisitDebugLog.d(
                    VisitDebugLog.TAG_MY_VISIT,
                    "  [$i] company=${v.companyName} visitor=${v.visitorName} entryDate=${v.entryDate}"
                )
            }
            list
        } catch (e: Exception) {
            VisitDebugLog.e(VisitDebugLog.TAG_MY_VISIT, "parseList failed: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * APIs may return a bare array, one object, or a wrapper object containing an array
     * (e.g. `{ "Table": [ {...}, {...} ] }`).
     */
    private fun extractArrayRoot(root: JsonElement): JsonElement? {
        if (root.isJsonArray) return root
        if (!root.isJsonObject) return null
        val obj = root.asJsonObject
        val knownKeys = listOf(
            "Table", "table", "data", "Data", "result", "Result",
            "VisitorDetails", "visitorDetails", "Items", "items"
        )
        for (key in knownKeys) {
            if (obj.has(key) && obj.get(key).isJsonArray) return obj.get(key)
        }
        for (entry in obj.entrySet()) {
            if (entry.value.isJsonArray) return entry.value
        }
        return root
    }

    fun sortByEntryDateDescending(visits: List<VisitorDetailsDto>): List<VisitorDetailsDto> =
        visits.sortedByDescending { entryDateMillis(it.entryDate) }

    /** Most recent visit by [VisitorDetailsDto.entryDate] when parseable; else last list item. */
    fun pickMostRecent(visits: List<VisitorDetailsDto>): VisitorDetailsDto? {
        if (visits.isEmpty()) return null
        if (visits.size == 1) return visits.first()
        return visits.maxByOrNull { entryDateMillis(it.entryDate) }
    }

    fun entryDateMillis(raw: String?): Long {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return Long.MIN_VALUE
        for (fmt in entryDateFormats) {
            try {
                val d = fmt.parse(s) ?: continue
                return d.time
            } catch (_: Exception) {
                // try next
            }
        }
        return Long.MIN_VALUE
    }

    fun formatEntryDate(raw: String?): String {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return "—"
        val millis = entryDateMillis(s)
        if (millis == Long.MIN_VALUE) return s
        return try {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(millis)
        } catch (_: Exception) {
            s
        }
    }

    private fun unwrapToJsonElement(text: String, maxDepth: Int): JsonElement? {
        var current: JsonElement = try {
            JsonParser.parseString(text)
        } catch (_: Exception) {
            return null
        }
        var depth = maxDepth
        while (depth-- > 0 && current.isJsonPrimitive && current.asJsonPrimitive.isString) {
            val inner = current.asString.trim()
            if (inner.isEmpty()) break
            val looksLikeJson = inner.startsWith("[") || inner.startsWith("{")
            if (!looksLikeJson) break
            current = try {
                JsonParser.parseString(inner)
            } catch (_: Exception) {
                break
            }
        }
        return current
    }
}
