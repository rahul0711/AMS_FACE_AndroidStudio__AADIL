package com.example.facercognitionapp.model

import com.google.gson.annotations.SerializedName

data class InsertVisitorDetailsRequest(
    @SerializedName("ClientVisitId") val clientVisitId: String,
    @SerializedName("VisitorId") val visitorId: String,
    @SerializedName("VisitorCardNo") val visitorCardNo: String,
    @SerializedName("VisitorName") val visitorName: String,
    @SerializedName("CompanyName") val companyName: String,
    @SerializedName("CompanyAddress") val companyAddress: String,
    @SerializedName("ContactPersonName") val contactPersonName: String,
    @SerializedName("ContactPersonNo") val contactPersonNo: String,
    @SerializedName("Email") val email: String,
    @SerializedName("Latitude") val latitude: String,
    @SerializedName("Longitude") val longitude: String,
    @SerializedName("Status") val status: String,
    @SerializedName("CompanyId") val companyId: String,
    @SerializedName("UnitId") val unitId: String,
    @SerializedName("Remarks") val remarks: String
)
