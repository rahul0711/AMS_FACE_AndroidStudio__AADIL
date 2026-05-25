package com.example.facercognitionapp.util

import android.content.Context
import android.content.SharedPreferences
import com.example.facercognitionapp.model.EmployeeChangePasswordRequest

object SessionHelper {

    const val PREFS_AUTH = "auth"

    fun visitorId(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_AUTH, Context.MODE_PRIVATE)
        return sequenceOf(
            prefs.getInt("visitor_id", 0),
            prefs.getInt("employee_id", 0),
            prefs.getInt("user_id", 0)
        ).firstOrNull { it > 0 } ?: 0
    }

    fun isSessionValid(context: Context): Boolean = visitorId(context) > 0

    fun persistParsedSession(
        editor: SharedPreferences.Editor,
        session: LoginSessionParser.ParsedLoginSession
    ) {
        val id = session.resolvedId()
        editor.putBoolean("logged_in", true)
        editor.putInt("employee_id", id)
        editor.putInt("visitor_id", session.visitorId.takeIf { it > 0 } ?: id)
        editor.putInt("user_id", session.userId)
        editor.putInt("company_id", session.companyId)
        editor.putInt("unit_id", session.unitId)
        editor.putString("employee_name", session.employeeName)
        editor.putString("employee_card_no", session.employeeCardNo)
        editor.putString("mobile_no", session.mobileNo)
        editor.putString("company_name", session.companyName)
        editor.putString("department_name", session.departmentName)
        editor.putString("designation_name", session.designationName)
        editor.putString("user_type", session.userType)
    }

    /** Store login password for APIs that require Password field (e.g. change password). */
    fun saveLoginPassword(editor: SharedPreferences.Editor, password: String) {
        editor.putString("login_password", password)
    }

    fun buildChangePasswordRequest(
        context: Context,
        oldPassword: String,
        newPassword: String,
        confirmPassword: String
    ): EmployeeChangePasswordRequest {
        val prefs = context.getSharedPreferences(PREFS_AUTH, Context.MODE_PRIVATE)
        val employeeId = visitorId(context).takeIf { it > 0 } ?: 6063
        val loginPassword = prefs.getString("login_password", null).orEmpty()
        return EmployeeChangePasswordRequest(
            employeeId = employeeId,
            employeeCardNo = prefs.getString("employee_card_no", null).orEmpty().ifBlank { "3126" },
            employeeName = prefs.getString("employee_name", null).orEmpty().ifBlank { "Employee" },
            mobileNo = prefs.getString("mobile_no", null).orEmpty(),
            companyId = prefs.getInt("company_id", 0).takeIf { it > 0 } ?: 26,
            unitId = prefs.getInt("unit_id", 0).takeIf { it > 0 } ?: 51,
            password = loginPassword.ifBlank { newPassword },
            oldPassword = oldPassword,
            newPassword = newPassword,
            confirmNewPassword = confirmPassword
        )
    }
}
