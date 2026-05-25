package com.example.facercognitionapp.ui.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.example.facercognitionapp.AttendanceDetailsActivity
import com.example.facercognitionapp.ChangePasswordActivity
import com.example.facercognitionapp.ClientVisitActivity
import com.example.facercognitionapp.MyVisitActivity
import com.example.facercognitionapp.LoginActivity
import com.example.facercognitionapp.PunchActivity
import com.example.facercognitionapp.R

/**
 * Shared sidebar navigation — used by every screen that hosts the drawer shell.
 */
object DrawerNavigation {

    /** Opens the dashboard ([PunchActivity]) and closes the current screen. */
    fun navigateToDashboard(context: Context) {
        val intent = Intent(context, PunchActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        context.startActivity(intent)
        if (context is Activity) {
            context.finish()
        }
    }

    /** Opens the login screen and clears the activity stack (does not clear saved credentials). */
    fun navigateToLogin(context: Context) {
        val intent = Intent(context, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
        if (context is Activity) {
            context.finish()
        }
    }

    fun navigate(context: Context, @androidx.annotation.IdRes menuItemId: Int): Boolean {
        if (context is Activity && isCurrentScreen(context, menuItemId)) {
            return false
        }

        val intent = when (menuItemId) {
            R.id.nav_dashboard -> Intent(context, PunchActivity::class.java)
            R.id.nav_attendance_details -> Intent(context, AttendanceDetailsActivity::class.java)
            R.id.nav_client_visit -> Intent(context, ClientVisitActivity::class.java)
            R.id.nav_my_visit -> Intent(context, MyVisitActivity::class.java)
            R.id.nav_change_password -> Intent(context, ChangePasswordActivity::class.java)
            else -> return false
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        context.startActivity(intent)
        if (context is Activity) {
            context.finish()
        }
        return true
    }

    private fun isCurrentScreen(activity: Activity, menuItemId: Int): Boolean {
        return when (menuItemId) {
            R.id.nav_dashboard -> activity is PunchActivity
            R.id.nav_attendance_details -> activity is AttendanceDetailsActivity
            R.id.nav_client_visit -> activity is ClientVisitActivity
            R.id.nav_my_visit -> activity is MyVisitActivity
            R.id.nav_change_password -> activity is ChangePasswordActivity
            else -> false
        }
    }
}
