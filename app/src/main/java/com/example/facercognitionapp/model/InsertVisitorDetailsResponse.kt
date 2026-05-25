package com.example.facercognitionapp.model

import com.google.gson.annotations.SerializedName

data class InsertVisitorDetailsResponse(
    @SerializedName("message") val message: String? = null,
    @SerializedName("status") val status: String? = null
)
