package com.example.facercognitionapp.network

import com.example.facercognitionapp.model.ApiResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @Multipart
    @Headers("Accept: application/json")
    @POST("api/Face/recognize")
    suspend fun scanFace(
        @Part file: MultipartBody.Part
    ): Response<ApiResponse>
}