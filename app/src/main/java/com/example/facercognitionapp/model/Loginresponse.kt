package com.example.facercognitionapp.model

import com.google.gson.annotations.SerializedName

data class LoginResponse(

    @SerializedName(value = "employeeId", alternate = ["EmployeeId"])
    val employeeId: Int? = null,

    @SerializedName("parentCompanyId")
    val parentCompanyId: Int? = null,

    @SerializedName("branchId")
    val branchId: Int? = null,

    @SerializedName(value = "companyId", alternate = ["CompanyId"])
    val companyId: Int? = null,

    @SerializedName(value = "userId", alternate = ["UserId"])
    val userId: Int? = null,

    @SerializedName(value = "unitId", alternate = ["UnitId"])
    val unitId: Int? = null,

    @SerializedName("unitName")
    val unitName: String? = null,

    @SerializedName("parentCompanyName")
    val parentCompanyName: String? = null,

    @SerializedName("branchName")
    val branchName: String? = null,

    @SerializedName("companyName")
    val companyName: String? = null,

    @SerializedName(value = "employeeCardNo", alternate = ["EmployeeCardNo"])
    val employeeCardNo: String? = null,

    @SerializedName("employeeDeviceCode")
    val employeeDeviceCode: String? = null,

    @SerializedName(value = "employeeName", alternate = ["EmployeeName"])
    val employeeName: String? = null,

    @SerializedName("employeeGuardian")
    val employeeGuardian: String? = null,

    @SerializedName("relation")
    val relation: String? = null,

    @SerializedName("dateOfBirth")
    val dateOfBirth: String? = null,

    @SerializedName("currentAddress")
    val currentAddress: String? = null,

    @SerializedName("permanentAddress")
    val permanentAddress: String? = null,

    @SerializedName("mobileNo")
    val mobileNo: String? = null,

    @SerializedName("phoneNoHome")
    val phoneNoHome: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("bloodGroup")
    val bloodGroup: String? = null,

    @SerializedName("gender")
    val gender: String? = null,

    @SerializedName("nationality")
    val nationality: String? = null,

    @SerializedName("religion")
    val religion: String? = null,

    @SerializedName("maritalStatus")
    val maritalStatus: String? = null,

    @SerializedName("qualification")
    val qualification: String? = null,

    @SerializedName("dateOfJoining")
    val dateOfJoining: String? = null,

    @SerializedName("adharCardNo")
    val adharCardNo: String? = null,

    @SerializedName("panCardNo")
    val panCardNo: String? = null,

    @SerializedName("uanNo")
    val uanNo: String? = null,

    @SerializedName("pfNo")
    val pfNo: String? = null,

    @SerializedName("pfAccountNo")
    val pfAccountNo: String? = null,

    @SerializedName("pfJoinDate")
    val pfJoinDate: String? = null,

    @SerializedName("restrictedPF")
    val restrictedPF: String? = null,

    @SerializedName("zeroPension")
    val zeroPension: String? = null,

    @SerializedName("esicNo")
    val esicNo: String? = null,

    @SerializedName("esicApplicable")
    val esicApplicable: String? = null,

    @SerializedName("ptApplicable")
    val ptApplicable: String? = null,

    @SerializedName("bankName")
    val bankName: String? = null,

    @SerializedName("bankBranch")
    val bankBranch: String? = null,

    @SerializedName("bankAccountNo")
    val bankAccountNo: String? = null,

    @SerializedName("ifscCode")
    val ifscCode: String? = null,

    @SerializedName("dateOfResign")
    val dateOfResign: String? = null,

    @SerializedName("resignReason")
    val resignReason: String? = null,

    @SerializedName("password")
    val password: String? = null,

    @SerializedName("departmentId")
    val departmentId: Int? = null,

    @SerializedName("designationId")
    val designationId: Int? = null,

    @SerializedName("shiftGroupId")
    val shiftGroupId: Int? = null,

    @SerializedName("shiftId")
    val shiftId: Int? = null,

    @SerializedName("departmentName")
    val departmentName: String? = null,

    @SerializedName("designationName")
    val designationName: String? = null,

    @SerializedName("shiftGroupName")
    val shiftGroupName: String? = null,

    @SerializedName("shiftName")
    val shiftName: String? = null,

    @SerializedName("weekOffDay")
    val weekOffDay: String? = null,

    @SerializedName("employeeType")
    val employeeType: String? = null,

    @SerializedName("payType")
    val payType: String? = null,

    @SerializedName("remarks")
    val remarks: String? = null,

    @SerializedName("entryDate")
    val entryDate: String? = null,

    @SerializedName("entryBy")
    val entryBy: String? = null,

    @SerializedName("updateDate")
    val updateDate: String? = null,

    @SerializedName("updateBy")
    val updateBy: String? = null,

    @SerializedName("isActive")
    val isActive: String? = null,

    @SerializedName("userType")
    val userType: String? = null,

    @SerializedName("isApproved")
    val isApproved: String? = null,

    @SerializedName("qrCode")
    val qrCode: String? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("msg")
    val msg: String? = null,

    @SerializedName("canteenUserId")
    val canteenUserId: Int? = null,

    @SerializedName("grade")
    val grade: String? = null,

    @SerializedName("approvedBy")
    val approvedBy: String? = null,

    @SerializedName("attendanceId")
    val attendanceId: Int? = null,

    @SerializedName("day")
    val day: Int? = null,

    @SerializedName("month")
    val month: Int? = null,

    @SerializedName("year")
    val year: Int? = null,

    @SerializedName("rowNum")
    val rowNum: Int? = null,

    @SerializedName("salaryMasterId")
    val salaryMasterId: Int? = null,

    @SerializedName("basicSalary")
    val basicSalary: String? = null,

    @SerializedName("grossSalary")
    val grossSalary: String? = null,

    @SerializedName("loan")
    val loan: String? = null,

    @SerializedName("advance")
    val advance: String? = null,

    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("company_id")
    val company_id: Int? = null,

    @SerializedName("unit_id")
    val unit_id: Int? = null,

    @SerializedName("confidence_score")
    val confidenceScore: Int? = null,

    @SerializedName("punch_time")
    val punchTime: String? = null
)

data class CompanyRight(
    @SerializedName("userCompanyRightsId") val userCompanyRightsId: Int,
    @SerializedName("userId") val userId: Int,
    @SerializedName("companyId") val companyId: Int,
    @SerializedName("companyName") val companyName: String,
    @SerializedName("plantId") val plantId: Int,
    @SerializedName("plantName") val plantName: String
)