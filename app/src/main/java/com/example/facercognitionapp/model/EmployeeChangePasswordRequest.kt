package com.example.facercognitionapp.model

import com.google.gson.annotations.SerializedName

data class EmployeeChangePasswordRequest(
    @SerializedName(value = "EmployeeId", alternate = ["employeeId"]) val employeeId: Int,
    @SerializedName(value = "EmployeeCardNo", alternate = ["employeeCardNo"]) val employeeCardNo: String,
    @SerializedName(value = "EmployeeName", alternate = ["employeeName"]) val employeeName: String,
    @SerializedName(value = "MobileNo", alternate = ["mobileNo"]) val mobileNo: String,
    @SerializedName(value = "CompanyId", alternate = ["companyId"]) val companyId: Int,
    @SerializedName(value = "UnitId", alternate = ["unitId"]) val unitId: Int,
    @SerializedName(value = "Password", alternate = ["password"]) val password: String,
    @SerializedName(value = "OldPassword", alternate = ["oldPassword"]) val oldPassword: String,
    @SerializedName(value = "NewPassword", alternate = ["newPassword"]) val newPassword: String,
    @SerializedName(value = "ConfirmNewPassword", alternate = ["confirmNewPassword"]) val confirmNewPassword: String
)
