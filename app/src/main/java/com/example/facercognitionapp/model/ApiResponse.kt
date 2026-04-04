package com.example.facercognitionapp.model

data class ApiResponse(
    val match: Boolean,
    val name: String?,
    val score: Double?,        // ✅ ADD
    val threshold: Double?,    // ✅ ADD
    val message: String?,      // ✅ ADD (top-level message)
    val attendance: Attendance?
) {
    data class Attendance(
        val status: String?,
        val message: String?,   // (this is different from top-level)
        val punch_time: String?,
        val punch_type: String?
    )
}


