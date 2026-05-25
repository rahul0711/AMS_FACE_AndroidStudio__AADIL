package com.example.facercognitionapp.model

import com.google.gson.annotations.SerializedName

/** Root element of GET EmployeeWiseInOutReport JSON array. */
data class EmployeeWiseInOutReportDto(
    @SerializedName("EmployeeId") val employeeId: Int? = null,
    @SerializedName("EmployeeCardNo") val employeeCardNo: String? = null,
    @SerializedName("EmployeeName") val employeeName: String? = null,
    @SerializedName("MonthDay") val monthDay: String? = null,
    @SerializedName("TotalPresent") val totalPresent: Int? = null,
    @SerializedName("TotalAbsent") val totalAbsent: Int? = null,
    @SerializedName("TotalWeekOff") val totalWeekOff: Int? = null,
    @SerializedName("TotalWorkHour") val totalWorkHour: Double? = null,
    @SerializedName("FullMonthInOut") val fullMonthInOut: List<FullMonthInOutDayDto>? = null
)

data class FullMonthInOutDayDto(
    @SerializedName("AttendanceDate") val attendanceDate: String? = null,
    @SerializedName("Day") val day: Int? = null,
    @SerializedName("InTime") val inTime: String? = null,
    @SerializedName("OutTime") val outTime: String? = null,
    @SerializedName("ShiftInTime") val shiftInTime: String? = null,
    @SerializedName("ShiftOutTime") val shiftOutTime: String? = null,
    @SerializedName("WorkHour") val workHour: String? = null,
    @SerializedName("Status") val status: String? = null,
    @SerializedName("IsManualPunch") val isManualPunch: Boolean? = null
)
