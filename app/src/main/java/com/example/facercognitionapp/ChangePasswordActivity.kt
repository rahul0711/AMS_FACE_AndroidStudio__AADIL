package com.example.facercognitionapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.facercognitionapp.databinding.ContentChangePasswordBinding
import com.example.facercognitionapp.network.ApiClient
import com.example.facercognitionapp.ui.core.BaseDrawerContentActivity
import com.example.facercognitionapp.util.SessionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangePasswordActivity : BaseDrawerContentActivity() {

    private lateinit var contentBinding: ContentChangePasswordBinding

    override fun contentLayoutRes() = R.layout.content_change_password

    override fun screenTitleRes() = R.string.title_change_password

    override fun drawerMenuItemId() = R.id.nav_change_password

    override fun onContentInflated(savedInstanceState: Bundle?) {
        contentBinding = ContentChangePasswordBinding.bind(
            shellBinding.contentContainer.getChildAt(0)
        )

        contentBinding.btnUpdatePassword.setOnClickListener {
            updatePassword()
        }
    }

    private fun updatePassword() {
        val oldPass = contentBinding.inputCurrentPassword.text?.toString()?.trim().orEmpty()
        val newPass = contentBinding.inputNewPassword.text?.toString()?.trim().orEmpty()
        val confirm = contentBinding.inputConfirmPassword.text?.toString()?.trim().orEmpty()

        when {
            oldPass.isBlank() || newPass.isBlank() || confirm.isBlank() ->
                Toast.makeText(this, R.string.change_password_fill_all, Toast.LENGTH_SHORT).show()
            newPass != confirm ->
                Toast.makeText(this, R.string.change_password_mismatch, Toast.LENGTH_SHORT).show()
            SessionHelper.visitorId(this) == 0 ->
                Toast.makeText(this, R.string.change_password_no_session, Toast.LENGTH_LONG).show()
            else -> submitChangePassword(oldPass, newPass, confirm)
        }
    }

    private fun submitChangePassword(
        oldPassword: String,
        newPassword: String,
        confirmPassword: String
    ) {
        val request = SessionHelper.buildChangePasswordRequest(
            this,
            oldPassword = oldPassword,
            newPassword = newPassword,
            confirmPassword = confirmPassword
        )

        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.api.employeeChangePassword(request)
                }

                withContext(Dispatchers.Main) {
                    setLoading(false)

                    if (response.isSuccessful) {
                        val msg = response.body()?.displayMessage()
                            ?: getString(R.string.change_password_success)
                        Toast.makeText(this@ChangePasswordActivity, msg, Toast.LENGTH_LONG).show()
                        getSharedPreferences(SessionHelper.PREFS_AUTH, MODE_PRIVATE)
                            .edit()
                            .apply {
                                SessionHelper.saveLoginPassword(this, newPassword)
                                apply()
                            }
                        contentBinding.inputCurrentPassword.text?.clear()
                        contentBinding.inputNewPassword.text?.clear()
                        contentBinding.inputConfirmPassword.text?.clear()
                    } else {
                        val err = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                        Toast.makeText(
                            this@ChangePasswordActivity,
                            getString(R.string.change_password_error_http, response.code()) +
                                if (err != null) "\n$err" else "",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setLoading(false)
                    Toast.makeText(
                        this@ChangePasswordActivity,
                        e.localizedMessage ?: getString(R.string.change_password_error_generic),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        contentBinding.btnUpdatePassword.isEnabled = !loading
        contentBinding.progressChangePassword.visibility =
            if (loading) View.VISIBLE else View.GONE
        contentBinding.btnUpdatePassword.text = if (loading) {
            getString(R.string.change_password_updating)
        } else {
            getString(R.string.action_update_password)
        }
    }
}
