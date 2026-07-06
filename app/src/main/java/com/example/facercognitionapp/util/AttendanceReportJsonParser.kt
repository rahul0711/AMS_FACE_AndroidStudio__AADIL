package com.example.facercognitionapp.util

import com.example.facercognitionapp.model.CurrentDatePunchDto
import com.example.facercognitionapp.model.EmployeeWiseInOutReportDto
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken

/**
 * Some endpoints return a JSON array/object, but the HTTP body is sometimes wrapped as a JSON **string**
 * (double-encoded). Gson then fails with: Expected BEGIN_ARRAY but was STRING.
 * This parser unwraps string layers and accepts either an array or a single object.
 */
object AttendanceReportJsonParser {

    private val gson = Gson()

    private val reportListType = object : TypeToken<List<EmployeeWiseInOutReportDto>>() {}.type
    private val punchListType = object : TypeToken<List<CurrentDatePunchDto>>() {}.type

    fun parseInOutReport(raw: String?): List<EmployeeWiseInOutReportDto> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val root = unwrapToJsonElement(raw.trim(), maxDepth = 4) ?: return emptyList()
            val dataRoot = extractData(root)
            when {
                dataRoot.isJsonArray -> gson.fromJson(dataRoot, reportListType) ?: emptyList()
                dataRoot.isJsonObject -> listOf(
                    gson.fromJson(dataRoot, EmployeeWiseInOutReportDto::class.java)
                )
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun parseCurrentDatePunch(raw: String?): List<CurrentDatePunchDto> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val root = unwrapToJsonElement(raw.trim(), maxDepth = 4) ?: return emptyList()
            val dataRoot = extractData(root)
            when {
                dataRoot.isJsonArray -> gson.fromJson(dataRoot, punchListType) ?: emptyList()
                dataRoot.isJsonObject -> listOf(
                    gson.fromJson(dataRoot, CurrentDatePunchDto::class.java)
                )
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun extractData(root: JsonElement): JsonElement {
        if (!root.isJsonObject) return root
        val obj = root.asJsonObject
        val knownKeys = listOf("data", "Data", "Table", "table", "result", "Result")
        for (key in knownKeys) {
            if (obj.has(key)) {
                val el = obj.get(key)
                if (el != null && !el.isJsonNull) {
                    if (el.isJsonPrimitive && el.asJsonPrimitive.isString) {
                        val unwrapped = unwrapToJsonElement(el.asString.trim(), maxDepth = 2)
                        if (unwrapped != null) return unwrapped
                    }
                    return el
                }
            }
        }
        return root
    }

    /**
     * Repeatedly parse if the root is a JSON string whose content looks like JSON.
     */
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
