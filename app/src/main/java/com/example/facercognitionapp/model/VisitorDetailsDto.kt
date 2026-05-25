package com.example.facercognitionapp.model

import com.google.gson.annotations.SerializedName

data class VisitorDetailsDto(
    @SerializedName(value = "CompanyName", alternate = ["companyName"]) val companyName: String? = null,
    @SerializedName(value = "VisitorName", alternate = ["visitorName"]) val visitorName: String? = null,
    @SerializedName(value = "CompanyAddress", alternate = ["companyAddress"]) val companyAddress: String? = null,
    @SerializedName(value = "ContactPersonName", alternate = ["contactPersonName"]) val contactPersonName: String? = null,
    @SerializedName(value = "ContactPersonNo", alternate = ["contactPersonNo"]) val contactPersonNo: String? = null,
    @SerializedName(value = "Email", alternate = ["email"]) val email: String? = null,
    @SerializedName(value = "Remarks", alternate = ["remarks"]) val remarks: String? = null,
    @SerializedName(value = "EntryDate", alternate = ["entryDate"]) val entryDate: String? = null
)
