package com.example.facercognitionapp.util

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

object LoginSessionParser {

    data class ParsedLoginSession(
        val employeeId: Int = 0,
        val userId: Int = 0,
        val visitorId: Int = 0,
        val companyId: Int = 0,
        val unitId: Int = 0,
        val employeeName: String? = null,
        val employeeCardNo: String? = null,
        val mobileNo: String? = null,
        val companyName: String? = null,
        val departmentName: String? = null,
        val designationName: String? = null,
        val userType: String? = null,
        val message: String? = null,
        val isLocationBypass: Int = 0
    ) {
        fun resolvedId(): Int =
            sequenceOf(employeeId, visitorId, userId).firstOrNull { it > 0 } ?: 0

        fun isValid(): Boolean = resolvedId() > 0
    }

    fun parse(raw: String?): ParsedLoginSession {
        if (raw.isNullOrBlank()) return ParsedLoginSession()
        return try {
            val root = unwrap(raw) ?: return ParsedLoginSession()
            val obj = when {
                root.isJsonObject -> root.asJsonObject
                root.isJsonArray && root.asJsonArray.size() > 0 && root.asJsonArray[0].isJsonObject ->
                    root.asJsonArray[0].asJsonObject
                else -> return ParsedLoginSession()
            }

            val targetObj = if (obj.has("data") && obj.get("data").isJsonObject) {
                obj.getAsJsonObject("data")
            } else {
                obj
            }

            val parsed = parseFromObject(targetObj)
            if (parsed != null && parsed.isValid()) {
                val rootMsg = readString(obj, "message", "Message", "msg", "Msg")
                parsed.copy(message = parsed.message ?: rootMsg)
            } else {
                ParsedLoginSession(
                    message = readString(obj, "message", "Message", "msg", "Msg")
                )
            }
        } catch (e: Exception) {
            VisitDebugLog.e(VisitDebugLog.TAG_SESSION, "LoginSessionParser error: ${e.message}", e)
            ParsedLoginSession()
        }
    }

    private fun parseFromObject(obj: JsonObject): ParsedLoginSession? {
        val fromList = parseFirstEmployeeInLists(obj)
        if (fromList != null && fromList.isValid()) return fromList

        val employeeId = readInt(obj, "employeeId", "EmployeeId")
        val visitorId = readInt(obj, "visitorId", "VisitorId")
        val userId = readInt(obj, "userId", "UserId")
        val deepId = if (employeeId > 0 || visitorId > 0 || userId > 0) 0 else deepFindId(obj)

        val resolvedEmployee = employeeId.takeIf { it > 0 } ?: visitorId.takeIf { it > 0 }
            ?: userId.takeIf { it > 0 } ?: deepId

        if (resolvedEmployee <= 0) return null

        return ParsedLoginSession(
            employeeId = employeeId.takeIf { it > 0 } ?: resolvedEmployee,
            userId = userId,
            visitorId = visitorId.takeIf { it > 0 } ?: resolvedEmployee,
            companyId = readInt(obj, "companyId", "CompanyId", "company_id"),
            unitId = readInt(obj, "unitId", "UnitId", "unit_id"),
            employeeName = readString(obj, "employeeName", "EmployeeName", "visitorName", "VisitorName"),
            employeeCardNo = readString(
                obj,
                "employeeCardNo",
                "EmployeeCardNo",
                "visitorCardNo",
                "VisitorCardNo"
            ),
            mobileNo = readString(obj, "mobileNo", "MobileNo"),
            companyName = readString(obj, "companyName", "CompanyName"),
            departmentName = readString(obj, "departmentName", "DepartmentName"),
            designationName = readString(obj, "designationName", "DesignationName"),
            userType = readString(obj, "userType", "UserType"),
            message = readString(obj, "message", "Message", "msg", "Msg"),
            isLocationBypass = readInt(obj, "isLocationBypass", "IsLocationBypass")
        )
    }

    private fun parseFirstEmployeeInLists(root: JsonObject): ParsedLoginSession? {
        val listKeys = listOf(
            "listEmployee", "ListEmployee",
            "employeeList", "EmployeeList",
            "listEmployeeSummary", "ListEmployeeSummary"
        )
        for (key in listKeys) {
            if (!root.has(key) || !root.get(key).isJsonArray) continue
            val arr = root.get(key).asJsonArray
            for (i in 0 until arr.size()) {
                val el = arr[i]
                if (!el.isJsonObject) continue
                val parsed = parseFromObject(el.asJsonObject)
                if (parsed != null && parsed.isValid()) return parsed
            }
        }
        return null
    }

    private fun deepFindId(obj: JsonObject, depth: Int = 0): Int {
        if (depth > 6) return 0
        val direct = readInt(obj, "employeeId", "EmployeeId", "visitorId", "VisitorId", "userId", "UserId")
        if (direct > 0) return direct
        for (entry in obj.entrySet()) {
            when {
                entry.value.isJsonObject -> {
                    val found = deepFindId(entry.value.asJsonObject, depth + 1)
                    if (found > 0) return found
                }
                entry.value.isJsonArray -> {
                    val arr = entry.value.asJsonArray
                    for (i in 0 until arr.size()) {
                        val el = arr[i]
                        if (el.isJsonObject) {
                            val found = deepFindId(el.asJsonObject, depth + 1)
                            if (found > 0) return found
                        }
                    }
                }
            }
        }
        return 0
    }

    private fun readString(obj: JsonObject, vararg keys: String): String? {
        for (key in keys) {
            if (!obj.has(key) || obj.get(key).isJsonNull) continue
            val el = obj.get(key)
            if (el.isJsonPrimitive) {
                val s = el.asString.trim()
                if (s.isNotEmpty()) return s
            }
        }
        return null
    }

    private fun readInt(obj: JsonObject, vararg keys: String): Int {
        for (key in keys) {
            if (!obj.has(key) || obj.get(key).isJsonNull) continue
            val el = obj.get(key)
            when {
                el.isJsonPrimitive && el.asJsonPrimitive.isNumber -> {
                    val v = el.asInt
                    if (v > 0) return v
                }
                el.isJsonPrimitive && el.asJsonPrimitive.isString ->
                    el.asString.trim().toIntOrNull()?.takeIf { it > 0 }?.let { return it }
            }
        }
        return 0
    }

    private fun unwrap(text: String): JsonElement? {
        var current: JsonElement = try {
            JsonParser.parseString(text.trim())
        } catch (_: Exception) {
            return null
        }
        var depth = 4
        while (depth-- > 0 && current.isJsonPrimitive && current.asJsonPrimitive.isString) {
            val inner = current.asString.trim()
            if (!inner.startsWith("{") && !inner.startsWith("[")) break
            current = try {
                JsonParser.parseString(inner)
            } catch (_: Exception) {
                break
            }
        }
        return current
    }
}
