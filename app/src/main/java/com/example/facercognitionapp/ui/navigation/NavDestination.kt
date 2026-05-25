package com.example.facercognitionapp.ui.navigation

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.example.facercognitionapp.R
import com.example.facercognitionapp.ui.dashboard.DashboardFragment
import com.example.facercognitionapp.ui.profile.ProfileFragment

/**
 * Maps drawer menu items to screen title + fragment (used by [HomeActivity] only).
 * Main app flow uses dedicated activities — [PunchActivity] is the Dashboard.
 */
sealed class NavDestination(
    @IdRes val menuItemId: Int,
    @StringRes val titleRes: Int
) {
    abstract fun createFragment(): Fragment

    data object Dashboard : NavDestination(
        menuItemId = R.id.nav_dashboard,
        titleRes = R.string.title_dashboard
    ) {
        override fun createFragment() = DashboardFragment()
    }

    data object AttendanceDetails : NavDestination(
        menuItemId = R.id.nav_attendance_details,
        titleRes = R.string.title_attendance_details
    ) {
        override fun createFragment() = ProfileFragment()
    }

    data object ClientVisit : NavDestination(
        menuItemId = R.id.nav_client_visit,
        titleRes = R.string.title_client_visit
    ) {
        override fun createFragment() = ProfileFragment()
    }

    data object MyVisit : NavDestination(
        menuItemId = R.id.nav_my_visit,
        titleRes = R.string.title_my_visit
    ) {
        override fun createFragment() = ProfileFragment()
    }

    data object ChangePassword : NavDestination(
        menuItemId = R.id.nav_change_password,
        titleRes = R.string.title_change_password
    ) {
        override fun createFragment() = ProfileFragment()
    }

    companion object {
        val drawerDestinations: List<NavDestination> = listOf(
            Dashboard,
            AttendanceDetails,
            ClientVisit,
            MyVisit,
            ChangePassword
        )

        fun fromMenuItemId(@IdRes menuItemId: Int): NavDestination? =
            drawerDestinations.find { it.menuItemId == menuItemId }
    }
}
