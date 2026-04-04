package com.example.facercognitionapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.facercognitionapp.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    // 🔐 Hard-coded access (simple & safe for now)
    private val allowedEmail = "admin"
    private val allowedPassword = "admin"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginBtn.setOnClickListener {
            login()
        }
    }

    private fun login() {
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()

        if (email == allowedEmail && password == allowedPassword) {

            // ✅ Save login state
            getSharedPreferences("auth", MODE_PRIVATE)
                .edit()
                .putBoolean("logged_in", true)
                .apply()

            startActivity(Intent(this, PunchActivity::class.java))
            finish()

        } else {
            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
        }
    }
}
