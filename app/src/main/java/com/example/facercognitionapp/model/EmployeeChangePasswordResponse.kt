package com.example.facercognitionapp.model

import com.google.gson.annotations.SerializedName

data class EmployeeChangePasswordResponse(
    @SerializedName("message") val message: String? = null,
    @SerializedName("Message") val messagePascal: String? = null,
    @SerializedName("msg") val msg: String? = null,
    @SerializedName("status") val status: Boolean? = null
) {
    fun displayMessage(): String? =
        message?.takeIf { it.isNotBlank() }
            ?: messagePascal?.takeIf { it.isNotBlank() }
            ?: msg?.takeIf { it.isNotBlank() }
}
