package com.example.facercognitionapp.ui.home

import com.example.facercognitionapp.ui.core.BaseDrawerActivity
import com.example.facercognitionapp.ui.navigation.NavDestination

class HomeActivity : BaseDrawerActivity() {

    override fun initialDestination(): NavDestination = NavDestination.Dashboard

    companion object {
        const val EXTRA_OPEN_MENU_ID = "extra_open_menu_id"
    }
}
