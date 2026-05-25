package com.example.facercognitionapp.ui.core

import android.os.Bundle
import androidx.fragment.app.commit
import com.example.facercognitionapp.R
import com.example.facercognitionapp.ui.navigation.NavDestination

/**
 * Drawer shell that swaps Fragments below the universal header (HomeActivity).
 */
abstract class BaseDrawerActivity : AbstractDrawerActivity() {

    private var currentDestination: NavDestination? = null

    protected abstract fun initialDestination(): NavDestination

    override fun screenTitleRes(): Int = initialDestination().titleRes

    override fun drawerMenuItemId(): Int = initialDestination().menuItemId

    override fun setupDrawerContent(savedInstanceState: Bundle?) {
        val menuId = savedInstanceState?.getInt(KEY_CURRENT_MENU_ID, -1) ?: -1
        val fromIntent = intent.getIntExtra(
            com.example.facercognitionapp.ui.home.HomeActivity.EXTRA_OPEN_MENU_ID,
            -1
        )
        val destination = when {
            fromIntent != -1 -> NavDestination.fromMenuItemId(fromIntent)
            menuId != -1 -> NavDestination.fromMenuItemId(menuId)
            else -> null
        } ?: initialDestination()

        navigateTo(destination)
    }

    protected fun navigateTo(destination: NavDestination) {
        currentDestination = destination
        headerController.setTitle(destination.titleRes)
        shellBinding.navigationView.setCheckedItem(destination.menuItemId)

        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(
                R.id.contentContainer,
                destination.createFragment(),
                destination::class.simpleName
            )
        }
        closeDrawer()
    }

    override fun onNavigationItemSelected(item: android.view.MenuItem): Boolean {
        val destination = NavDestination.fromMenuItemId(item.itemId)
        if (destination != null) {
            navigateTo(destination)
            return true
        }
        return super.onNavigationItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentDestination?.menuItemId?.let { outState.putInt(KEY_CURRENT_MENU_ID, it) }
    }

    companion object {
        private const val KEY_CURRENT_MENU_ID = "current_menu_id"
    }
}
