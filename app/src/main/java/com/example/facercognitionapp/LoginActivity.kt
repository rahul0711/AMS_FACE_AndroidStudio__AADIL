package com.example.facercognitionapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.facercognitionapp.databinding.ActivityLoginBinding
import com.example.facercognitionapp.model.LoginRequest
import com.example.facercognitionapp.network.ApiClient
import com.example.facercognitionapp.ui.core.AppFooterHelper
import com.example.facercognitionapp.util.LoginSessionParser
import com.example.facercognitionapp.util.SessionHelper
import com.example.facercognitionapp.util.VisitDebugLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AppFooterHelper.bind(binding.appFooter.root)
        binding.loginBtn.setOnClickListener { login() }
    }

    private fun login() {
        val mobileNo = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()

        if (mobileNo.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.login_error_empty, Toast.LENGTH_SHORT).show()
            return
        }

        val request = LoginRequest(mobileNo = mobileNo, password = password)
        VisitDebugLog.logObject(VisitDebugLog.TAG_SESSION, "login request", request)

        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = ApiClient.api.login(request)

                val rawBody = withContext(Dispatchers.IO) {
                    response.body()?.string().orEmpty()
                }
                val errorBody = withContext(Dispatchers.IO) {
                    response.errorBody()?.string().orEmpty()
                }

                VisitDebugLog.logApiResult(
                    tag = VisitDebugLog.TAG_SESSION,
                    endpoint = "EmployeeAuthentication",
                    httpCode = response.code(),
                    success = response.isSuccessful,
                    rawBody = rawBody,
                    errorBody = errorBody.ifBlank { null }
                )

                if (!response.isSuccessful) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Login failed (HTTP ${response.code()})",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                val session = LoginSessionParser.parse(rawBody)

                if (!session.isValid()) {
                    val hint = session.message?.takeIf { it.isNotBlank() }
                        ?: "Server returned empty profile. Check mobile/password."
                    VisitDebugLog.e(
                        VisitDebugLog.TAG_SESSION,
                        "Login rejected: no employeeId/visitorId in response"
                    )
                    Toast.makeText(this@LoginActivity, hint, Toast.LENGTH_LONG).show()
                    return@launch
                }

                getSharedPreferences(SessionHelper.PREFS_AUTH, MODE_PRIVATE)
                    .edit()
                    .apply {
                        SessionHelper.persistParsedSession(this, session)
                        SessionHelper.saveLoginPassword(this, password)
                        apply()
                    }

                VisitDebugLog.logSession(this@LoginActivity)

                Toast.makeText(
                    this@LoginActivity,
                    "Login OK — ID ${session.resolvedId()}",
                    Toast.LENGTH_SHORT
                ).show()

                startActivity(Intent(this@LoginActivity, PunchActivity::class.java))
                finish()
            } catch (e: Exception) {
                VisitDebugLog.e(VisitDebugLog.TAG_SESSION, "Login exception", e)
                Toast.makeText(
                    this@LoginActivity,
                    "Network Error: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.loginBtn.isEnabled = !loading
        binding.loginBtn.text = getString(
            if (loading) R.string.login_please_wait else R.string.login_sign_in
        )
    }
}
