package com.example.facercognitionapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ExperimentalGetImage
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.facercognitionapp.camera.CameraHelper
import com.example.facercognitionapp.databinding.ActivityMainBinding
import com.example.facercognitionapp.network.ApiClient
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import android.speech.tts.TextToSpeech
import java.io.File
import java.util.Locale
import android.os.Handler
import android.os.Looper
import android.content.Intent

@ExperimentalGetImage
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraHelper: CameraHelper
    private lateinit var tts: TextToSpeech

    // ✅ STEP 3: receive punch type
    private var punchType: String = ""

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else showStatus("Camera permission denied")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ GET DATA FROM PunchActivity
        punchType = intent.getStringExtra("PUNCH_TYPE") ?: ""

        // (Optional debug)
        showStatus("Mode: $punchType")

        // ✅ TTS setup
        tts = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
            }
        }

        // ✅ Camera Helper
        cameraHelper = CameraHelper(
            context = this,
            lifecycleOwner = this,

            onFaceDetected = { imageFile ->
                showStatus("Recognizing... ($punchType)")
                sendToBackend(imageFile)
            },

            onNoFace = {
                showStatus("No face detected")
            }
        )

        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        cameraHelper.startCamera(binding.previewView)
        showStatus("No face detected ($punchType)")
    }

    // ================= API =================

    private fun sendToBackend(imageFile: File) {

        lifecycleScope.launch {

            try {

                val requestBody =
                    imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())

                val part = MultipartBody.Part.createFormData(
                    "file",
                    imageFile.name,
                    requestBody
                )

                // 🔥 NEXT STEP: we will add punchType here later
                val response = ApiClient.api.scanFace(part)

                if (response.isSuccessful) {

                    val data = response.body()

                    if (data != null) {

                        val score = data.score ?: 0.0
                        val threshold = data.threshold ?: 0.6

                        if (data.match == true && score >= threshold){

                            val name = data.name ?: "User"
                            val status = data.attendance?.status ?: ""
                            val punch = data.attendance?.punch_type ?: ""

                            val timeRaw = data.attendance?.punch_time
                            val time = timeRaw?.substring(11, 16) ?: ""

                            val message = "$name\n$punch - $status\n$time"

                            showWelcome(message, name, status)

                            // ✅ SEND RESULT BACK TO PunchActivity
                            val resultIntent = Intent()
                            resultIntent.putExtra("PUNCH_TYPE", punchType)
                            resultIntent.putExtra("PUNCH_TIME", timeRaw ?: "")

                            setResult(RESULT_OK, resultIntent)

                            // 🔥 CLOSE CAMERA SCREEN AFTER SUCCESS
                            Handler(Looper.getMainLooper()).postDelayed({
                                finish()
                            }, 1200)
                        } else {
                            handleUnknownFace(data.message)
                        }

                    } else {
                        showStatus("Empty response")
                    }

                } else {
                    showStatus("Server error ${response.code()}")
                }

            } catch (e: Exception) {
                showStatus("Network error")
            }

            cameraHelper.resetCapture()
        }
    }

    // ================= UI =================

    private fun showWelcome(message: String, name: String, status: String) {
        runOnUiThread {

            binding.welcomeText.text = message
            binding.welcomeCard.visibility = View.VISIBLE

            if (status.contains("Late", true)) {
                binding.welcomeCard.setBackgroundColor(0xFFFFCDD2.toInt())
            } else {
                binding.welcomeCard.setBackgroundColor(0xFFC8E6C9.toInt())
            }

            tts.speak("$name $status", TextToSpeech.QUEUE_FLUSH, null, null)

            Handler(Looper.getMainLooper()).postDelayed({
                binding.welcomeCard.visibility = View.GONE
            }, 1200)
        }
    }

    private fun handleUnknownFace(apiMessage: String?) {
        runOnUiThread {

            val message = apiMessage ?: "Face not recognized"

            binding.welcomeText.text = message
            binding.welcomeCard.visibility = View.VISIBLE
            binding.welcomeCard.setBackgroundColor(0xFFFFF9C4.toInt())

            tts.speak("Face not recognized", TextToSpeech.QUEUE_FLUSH, null, null)

            Handler(Looper.getMainLooper()).postDelayed({
                binding.welcomeCard.visibility = View.GONE
            }, 1000)
        }
    }

    private fun showStatus(text: String) {
        runOnUiThread {
            binding.statusText.text = text
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
        cameraHelper.stopCamera()
    }
}