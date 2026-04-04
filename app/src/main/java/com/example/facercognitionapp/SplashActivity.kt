package com.example.facercognitionapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ExperimentalGetImage
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    @OptIn(ExperimentalGetImage::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        val prefs = getSharedPreferences("auth", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("logged_in", false)

        Log.d("SPLASH_DEBUG", "Logged in value = $isLoggedIn")

        lifecycleScope.launch {

            delay(2000)

            Log.d("MY_DEBUG", "FORCING LOGIN SCREEN")

            startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            finish()
        }

    }
}
