package com.example.facercognitionapp.network

import com.example.facercognitionapp.model.EmployeeChangePasswordRequest
import com.example.facercognitionapp.model.EmployeeChangePasswordResponse
import com.example.facercognitionapp.model.InsertVisitorDetailsResponse
import com.example.facercognitionapp.model.LoginRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiService {

    /**
     * POST https://demo.scriptindia.in:8037/Recognize
     * form-data: file, InOutFlag, Latitude, Longitude, CompanyId, EmployeeId, EmployeeCardNo
     *
     * Response body is shown to the user as returned (e.g. `{ "message": "IN Punch Done Successfully" }`).
     */
    @Multipart
    @Headers("Accept: application/json")
    @POST("Recognize")
    suspend fun recognize(
        @Part file: MultipartBody.Part,
        @Part("InOutFlag") inOutFlag: RequestBody,
        @Part("Latitude") latitude: RequestBody,
        @Part("Longitude") longitude: RequestBody,
        @Part("CompanyId") companyId: RequestBody,
        @Part("EmployeeId") employeeId: RequestBody,
        @Part("EmployeeCardNo") employeeCardNo: RequestBody
    ): Response<ResponseBody>

    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST("EmployeeAuthentication")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ResponseBody>

    /**
     * GET …/EmployeeWiseInOutReport?Month=5&Year=2026&EmployeeId=6063
     * Returns a JSON array (typically one employee summary + [FullMonthInOut]).
     */
    @Headers("Accept: application/json")
    @GET("EmployeeWiseInOutReport")
    suspend fun employeeWiseInOutReport(
        @Query("Month") month: Int,
        @Query("Year") year: Int,
        @Query("EmployeeId") employeeId: Int
    ): Response<ResponseBody>

    /**
     * GET …/EmployeeWiseCurrentDateInOutPunch?EmployeeId=6063
     * Returns a JSON array; first row is used for today’s IN/OUT on the punch screen.
     * Body may be double-encoded as a JSON string; parse with [AttendanceReportJsonParser].
     */
    @Headers("Accept: application/json")
    @GET("EmployeeWiseCurrentDateInOutPunch")
    suspend fun employeeWiseCurrentDateInOutPunch(
        @Query("EmployeeId") employeeId: Int
    ): Response<ResponseBody>

    /**
     * POST …/InsertVisitorDetails
     * form-data: ClientVisitId, VisitorId, VisitorCardNo, VisitorName, CompanyName,
     * CompanyAddress, ContactPersonName, ContactPersonNo, Email, Latitude, Longitude,
     * Status, CompanyId, UnitId, Remarks, VisitorPhoto (image file)
     */
    @Multipart
    @Headers("Accept: application/json")
    @POST("InsertVisitorDetails")
    suspend fun insertVisitorDetails(
        @Part("ClientVisitId") clientVisitId: RequestBody,
        @Part("VisitorId") visitorId: RequestBody,
        @Part("VisitorCardNo") visitorCardNo: RequestBody,
        @Part("VisitorName") visitorName: RequestBody,
        @Part("CompanyName") companyName: RequestBody,
        @Part("CompanyAddress") companyAddress: RequestBody,
        @Part("ContactPersonName") contactPersonName: RequestBody,
        @Part("ContactPersonNo") contactPersonNo: RequestBody,
        @Part("Email") email: RequestBody,
        @Part("Latitude") latitude: RequestBody,
        @Part("Longitude") longitude: RequestBody,
        @Part("Status") status: RequestBody,
        @Part("CompanyId") companyId: RequestBody,
        @Part("UnitId") unitId: RequestBody,
        @Part("Remarks") remarks: RequestBody,
        @Part visitorPhoto: MultipartBody.Part
    ): Response<InsertVisitorDetailsResponse>

    @Headers("Accept: application/json")
    @GET("VisitorIdAndMonthAndDateWiseDetails")
    suspend fun visitorIdAndMonthAndDateWiseDetails(
        @Query("Month") month: Int,
        @Query("Year") year: Int,
        @Query("Day") day: Int,
        @Query("VisitorId") visitorId: Int
    ): Response<ResponseBody>

    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST("EmployeeChangePassword")
    suspend fun employeeChangePassword(
        @Body request: EmployeeChangePasswordRequest
    ): Response<EmployeeChangePasswordResponse>
}
