package com.example.facercognitionapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.facercognitionapp.databinding.ActivityPunchBinding
import androidx.activity.result.contract.ActivityResultContracts
@androidx.camera.core.ExperimentalGetImage
class PunchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPunchBinding
    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == RESULT_OK) {

            val data = result.data

            val type = data?.getStringExtra("PUNCH_TYPE")
            val time = data?.getStringExtra("PUNCH_TIME")

            updateUI(type, time)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPunchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // IN button
        binding.btnIn.setOnClickListener {
            openCamera("IN")
        }

        // OUT button
        binding.btnOut.setOnClickListener {
            openCamera("OUT")
        }
    }

    private fun openCamera(type: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("PUNCH_TYPE", type)
        launcher.launch(intent)
    }
    private fun updateUI(type: String?, time: String?) {

        // Format time: 2026-04-01T09:56 → 2026-04-01 09:56
        val formattedTime = time?.substring(0, 16)?.replace("T", " ") ?: ""

        if (type == "IN") {
            binding.btnIn.text = "IN PUNCH: $formattedTime"
        } else if (type == "OUT") {
            binding.btnOut.text = "OUT PUNCH: $formattedTime"
        }
    }
}