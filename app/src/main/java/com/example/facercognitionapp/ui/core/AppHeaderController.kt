package com.example.facercognitionapp.ui.core

import androidx.annotation.StringRes
import com.example.facercognitionapp.databinding.IncludeAppHeaderBinding
import com.google.android.material.appbar.MaterialToolbar

/**
 * Controls the universal app header title and toolbar actions.
 * Reuse on any screen by including [R.layout.include_app_header].
 */
class AppHeaderController(
  private val binding: IncludeAppHeaderBinding
) {
    val toolbar: MaterialToolbar
        get() = binding.appHeaderToolbar

    fun setTitle(@StringRes titleRes: Int) {
        toolbar.setTitle(titleRes)
    }

    fun setTitle(title: CharSequence) {
        toolbar.title = title
    }

    fun setSubtitle(subtitle: CharSequence?) {
        toolbar.subtitle = subtitle
    }

    fun setNavigationOnClickListener(listener: () -> Unit) {
        toolbar.setNavigationOnClickListener { listener() }
    }
}
