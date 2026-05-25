package com.example.facercognitionapp.ui.core

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.facercognitionapp.PunchActivity
import com.example.facercognitionapp.R
import com.example.facercognitionapp.databinding.ActivityDrawerShellBinding
import com.example.facercognitionapp.ui.navigation.DrawerNavigation
import com.google.android.material.navigation.NavigationView

/**
 * Base for any screen with sidebar + universal header.
 * Subclasses provide title, active menu item, and body content below the header.
 */
abstract class AbstractDrawerActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    protected lateinit var shellBinding: ActivityDrawerShellBinding
    protected lateinit var headerController: AppHeaderController
    private lateinit var drawerToggle: ActionBarDrawerToggle

    @StringRes
    protected abstract fun screenTitleRes(): Int

    @IdRes
    protected abstract fun drawerMenuItemId(): Int

    /** Inflate page UI into [ActivityDrawerShellBinding.contentContainer]. */
    protected abstract fun setupDrawerContent(savedInstanceState: Bundle?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        shellBinding = ActivityDrawerShellBinding.inflate(layoutInflater)
        setContentView(shellBinding.root)

        headerController = AppHeaderController(shellBinding.appHeader)
        headerController.setTitle(screenTitleRes())

        setupDrawer()
        setupBackPressed()
        bindDrawerHeader()
        setupLogoutButton()

        shellBinding.navigationView.setNavigationItemSelectedListener(this)
        val menuId = drawerMenuItemId()
        if (menuId != NO_DRAWER_SELECTION) {
            shellBinding.navigationView.setCheckedItem(menuId)
        }

        setupDrawerContent(savedInstanceState)
        AppFooterHelper.bind(shellBinding.appFooter.root)
    }

    override fun onResume() {
        super.onResume()
        bindDrawerHeader()
    }

    protected fun closeDrawer() {
        shellBinding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun setupDrawer() {
        drawerToggle = ActionBarDrawerToggle(
            this,
            shellBinding.drawerLayout,
            headerController.toolbar,
            R.string.nav_open_drawer,
            R.string.nav_close_drawer
        )
        shellBinding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
    }

    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (shellBinding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    closeDrawer()
                    return
                }
                if (isDashboardHome()) {
                    DrawerNavigation.navigateToLogin(this@AbstractDrawerActivity)
                } else {
                    DrawerNavigation.navigateToDashboard(this@AbstractDrawerActivity)
                }
            }
        })
    }

    /** True only on the main dashboard ([PunchActivity]); other screens go back here first. */
    protected open fun isDashboardHome(): Boolean = this is PunchActivity

    private fun bindDrawerHeader() {
        val prefs = getSharedPreferences("auth", MODE_PRIVATE)
        val name = prefs.getString("employee_name", null)?.takeIf { it.isNotBlank() }
            ?: getString(R.string.nav_header_guest)
        val company = prefs.getString("company_name", null)?.takeIf { it.isNotBlank() }
            ?: getString(R.string.nav_header_subtitle)
        val role = prefs.getString("designation_name", null)?.takeIf { it.isNotBlank() }
            ?: prefs.getString("department_name", null)?.takeIf { it.isNotBlank() }
            ?: ""

        val panel = shellBinding.drawerPanel
        panel.findViewById<TextView>(R.id.navHeaderName).text = name
        panel.findViewById<TextView>(R.id.navHeaderCompany).text = company
        panel.findViewById<TextView>(R.id.navHeaderRole).apply {
            text = role
            visibility = if (role.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
        }
        panel.findViewById<TextView>(R.id.navHeaderInitials).text = initialsFrom(name)
    }

    private fun setupLogoutButton() {
        shellBinding.btnDrawerLogout.setOnClickListener {
            closeDrawer()
            performLogout()
        }
    }

    protected fun performLogout() {
        getSharedPreferences("auth", MODE_PRIVATE).edit().clear().apply()
        Toast.makeText(this, R.string.msg_logout_success, Toast.LENGTH_SHORT).show()
        DrawerNavigation.navigateToLogin(this)
    }

    private fun initialsFrom(name: String): String {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "${parts.first().first()}${parts.last().first()}".uppercase()
        }
    }

    override fun onNavigationItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == drawerMenuItemId()) {
            closeDrawer()
            return true
        }
        if (!DrawerNavigation.navigate(this, item.itemId)) {
            closeDrawer()
        }
        return true
    }

    companion object {
        /** Use when this screen is not listed in the drawer (e.g. company picker). */
        const val NO_DRAWER_SELECTION = 0
    }
}
