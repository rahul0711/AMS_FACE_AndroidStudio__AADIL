package com.example.facercognitionapp.ui

import android.os.Bundle
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.example.facercognitionapp.R
import com.example.facercognitionapp.ui.core.BaseDrawerContentActivity

abstract class PlaceholderDrawerActivity : BaseDrawerContentActivity() {

    @StringRes
    protected abstract fun pageTitleRes(): Int

    @IdRes
    protected abstract fun menuItemId(): Int

    override fun contentLayoutRes() = R.layout.content_screen_placeholder

    override fun screenTitleRes() = pageTitleRes()

    override fun drawerMenuItemId() = menuItemId()

    override fun onContentInflated(savedInstanceState: Bundle?) {
        val root = shellBinding.contentContainer.getChildAt(0)
        root.findViewById<TextView>(R.id.placeholderTitle).setText(pageTitleRes())
    }
}
