package com.example.facercognitionapp.ui.core

import android.os.Bundle
import androidx.annotation.LayoutRes

/**
 * Drawer shell with a single layout inflated below the universal header.
 * Use for SelectCompany, Punch, Camera (MainActivity), etc.
 */
abstract class BaseDrawerContentActivity : AbstractDrawerActivity() {

    @LayoutRes
    protected abstract fun contentLayoutRes(): Int

    override fun setupDrawerContent(savedInstanceState: Bundle?) {
        layoutInflater.inflate(contentLayoutRes(), shellBinding.contentContainer, true)
        onContentInflated(savedInstanceState)
    }

    /** Called after [contentLayoutRes] is inflated into the content container. */
    protected open fun onContentInflated(savedInstanceState: Bundle?) {}
}
