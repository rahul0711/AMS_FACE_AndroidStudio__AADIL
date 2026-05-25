package com.example.facercognitionapp.model

import com.google.gson.annotations.SerializedName

/**
 * POST EmployeeAuthentication — server model uses camelCase in JSON response;
 * send both PascalCase and camelCase credential fields so binding succeeds.
 */
data class LoginRequest(
    @SerializedName("MobileNo") val mobileNo: String,
    @SerializedName("mobileNo") val mobileNoCamel: String = mobileNo,
    @SerializedName("UserName") val userName: String = mobileNo,
    @SerializedName("userName") val userNameCamel: String = mobileNo,
    @SerializedName("Password") val password: String,
    @SerializedName("password") val passwordCamel: String = password
)
