package com.example.facercognitionapp.model

import com.google.gson.annotations.SerializedName

/** Element of GET EmployeeWiseCurrentDateInOutPunch JSON array. */
data class CurrentDatePunchDto(
    @SerializedName("attendanceId") val attendanceId: Int? = null,
    @SerializedName("employeeCardNo") val employeeCardNo: String? = null,
    @SerializedName("employeeName") val employeeName: String? = null,
    @SerializedName("employeeId") val employeeId: Int? = null,
    @SerializedName("attendanceDate") val attendanceDate: String? = null,
    @SerializedName("day") val day: Int? = null,
    @SerializedName("shiftInTime") val shiftInTime: String? = null,
    @SerializedName("shiftOutTime") val shiftOutTime: String? = null,
    @SerializedName("inTime") val inTime: String? = null,
    @SerializedName("outTime") val outTime: String? = null,
    @SerializedName("workHour") val workHour: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("companyId") val companyId: Int? = null,
    @SerializedName("unitId") val unitId: Int? = null,
    @SerializedName("isManualPunch") val isManualPunch: Boolean? = null
)
