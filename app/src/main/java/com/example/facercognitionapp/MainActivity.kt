package com.example.facercognitionapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ExperimentalGetImage
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.facercognitionapp.camera.CameraHelper
import com.example.facercognitionapp.databinding.ContentMainCameraBinding
import com.example.facercognitionapp.model.RecognizeResponse
import com.example.facercognitionapp.network.ApiClient
import com.example.facercognitionapp.ui.core.BaseDrawerContentActivity
import com.example.facercognitionapp.util.LocationHelper
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.Locale

@ExperimentalGetImage
class MainActivity : BaseDrawerContentActivity() {

    private lateinit var contentBinding: ContentMainCameraBinding
    private lateinit var cameraHelper: CameraHelper
    private lateinit var tts: TextToSpeech

    private var inOutFlag: Int = 1
    private var punchType: String = "IN"
    private var apiInFlight = false

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) checkLocationAndStartCamera()
            else showStatus("Camera permission denied")
        }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            val ok = grants.values.any { it }
            if (ok) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    startCamera()
                }
            } else {
                Toast.makeText(this, "Location permission required for punch", Toast.LENGTH_SHORT).show()
            }
        }

    override fun contentLayoutRes() = R.layout.content_main_camera

    override fun screenTitleRes() = R.string.title_face_scan

    override fun drawerMenuItemId() = R.id.nav_dashboard

    override fun onContentInflated(savedInstanceState: Bundle?) {
        contentBinding = ContentMainCameraBinding.bind(shellBinding.contentContainer.getChildAt(0))

        inOutFlag = intent.getIntExtra(EXTRA_IN_OUT_FLAG, 1)
        punchType = intent.getStringExtra(EXTRA_PUNCH_TYPE) ?: if (inOutFlag == 2) "OUT" else "IN"

        headerController.setTitle(
            if (inOutFlag == 2) getString(R.string.title_punch_out) else getString(R.string.title_punch_in)
        )

        tts = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
            }
        }

        cameraHelper = CameraHelper(
            context = this,
            lifecycleOwner = this,
            onFaceDetected = { imageFile ->
                showStatus("Recognizing...")
                sendToBackend(imageFile)
            },
            onNoFace = {
                showStatus("No face detected")
            },
            onLowLightChanged = { isDark ->
                if (isDark) {
                    contentBinding.screenFlashOverlay.visibility = View.VISIBLE
                    val lp = window.attributes
                    lp.screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                    window.attributes = lp
                } else {
                    contentBinding.screenFlashOverlay.visibility = View.GONE
                    val lp = window.attributes
                    lp.screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                    window.attributes = lp
                }
            }
        )

        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            checkLocationAndStartCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun checkLocationAndStartCamera() {
        val hasLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (hasLocation) {
            startCamera()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun startCamera() {
        cameraHelper.startCamera(contentBinding.previewView)
        showStatus("Align your face — ${if (inOutFlag == 1) "Punch IN" else "Punch OUT"}")
    }

    private fun sendToBackend(imageFile: File) {
        if (apiInFlight) return

        val prefs = getSharedPreferences("auth", MODE_PRIVATE)
        val companyId = prefs.getInt("company_id", 0)
        val employeeId = prefs.getInt("employee_id", 0)
        val employeeCardNo = prefs.getString("employee_card_no", "") ?: ""
        val isLocationBypass = prefs.getInt("is_location_bypass", 0)

        if (companyId == 0 || employeeId == 0 || employeeCardNo.isBlank()) {
            showStatus("Login data missing. Please login again.")
            cameraHelper.unlockAfterApi()
            return
        }

        apiInFlight = true
        cameraHelper.lockForApi()

        lifecycleScope.launch {
            var isSuccess = false
            try {
                val location = LocationHelper.getCurrentLocation(this@MainActivity)
                if (location == null) {
                    showStatus("Unable to get location. Enable GPS.")
                    return@launch
                }

                val latitude = location.latitude.toString()
                val longitude = location.longitude.toString()

                if (!imageFile.exists() || imageFile.length() < 512L) {
                    showStatus("Photo file is empty or too small. Try again.")
                    return@launch
                }

                Log.i(
                    TAG,
                    """
                    |>>> POST /Recognize REQUEST
                    |    InOutFlag=$inOutFlag punchType=$punchType
                    |    CompanyId=$companyId EmployeeId=$employeeId EmployeeCardNo=$employeeCardNo
                    |    Latitude=$latitude Longitude=$longitude
                    |    file.name=${imageFile.name} file.bytes=${imageFile.length()} path=${imageFile.absolutePath}
                    """.trimMargin()
                )

                val fileBody = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", imageFile.name, fileBody)

                val textType = "text/plain".toMediaTypeOrNull()

                val response = ApiClient.api.recognize(
                    file = filePart,
                    inOutFlag = inOutFlag.toString().toRequestBody(textType),
                    latitude = latitude.toRequestBody(textType),
                    longitude = longitude.toRequestBody(textType),
                    companyId = companyId.toString().toRequestBody(textType),
                    employeeId = employeeId.toString().toRequestBody(textType),
                    employeeCardNo = employeeCardNo.toRequestBody(textType),
                    isLocationBypass = isLocationBypass.toString().toRequestBody(textType)
                )

                val rawJson = response.body()?.string()
                    ?: response.errorBody()?.string()

                val parsed = RecognizeResponse.fromJson(rawJson)
                val displayText = parsed?.primaryDisplayText()?.takeIf { it.isNotBlank() }
                    ?: rawJson?.trim()?.takeIf { it.isNotBlank() }
                    ?: "(empty body)"
                val punchSuccess = parsed?.isPunchSuccess() == true
                val punchFailure = parsed?.isPunchFailure() == true

                Log.i(
                    TAG,
                    """
                    |>>> POST /Recognize RESPONSE
                    |    HTTP ${response.code()} isSuccessful=${response.isSuccessful}
                    |    rawBody=$rawJson
                    |    parsed match=${parsed?.match} score=${parsed?.score} threshold=${parsed?.threshold}
                    |    displayMessage=$displayText
                    |    punchSuccess=$punchSuccess punchFailure=$punchFailure
                    """.trimMargin()
                )

                if (!response.isSuccessful) {
                    showServerResponse(displayText, success = false)
                    resetCameraStatus()
                    return@launch
                }

                val isRedStyle = parsed?.successRaw == "1"
                isSuccess = punchSuccess
                showServerResponse(displayText, success = punchSuccess, isRedStyle = isRedStyle)

                if (!punchSuccess) {
                    resetCameraStatus()
                    return@launch
                }

                val punchTime = parsed?.resolvedPunchTime
                    ?: java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                        .format(java.util.Date())
                savePunchTime(punchType, punchTime)
                runOnUiThread {
                    Handler(Looper.getMainLooper()).postDelayed({
                        setResult(
                            RESULT_OK,
                            Intent().apply {
                                putExtra(EXTRA_PUNCH_TYPE, punchType)
                                putExtra(EXTRA_PUNCH_TIME, punchTime)
                            }
                        )
                        finish()
                    }, 2500)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Recognize request failed", e)
                showStatus("Network error: ${e.localizedMessage}")
            } finally {
                if (!isSuccess) {
                    apiInFlight = false
                    cameraHelper.unlockAfterApi()
                }
                disableScreenFlash()
            }
        }
    }

    private fun disableScreenFlash() {
        runOnUiThread {
            contentBinding.screenFlashOverlay.visibility = View.GONE
            val lp = window.attributes
            lp.screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = lp
        }
    }

    private fun savePunchTime(type: String, time: String) {
        val prefs = getSharedPreferences("auth", MODE_PRIVATE).edit()
        when (type) {
            "IN" -> prefs.putString(PunchActivity.PREF_LAST_IN_TIME, time)
            "OUT" -> prefs.putString(PunchActivity.PREF_LAST_OUT_TIME, time)
        }
        prefs.apply()
    }

    private fun resetCameraStatus() {
        showStatus("Align your face — ${if (inOutFlag == 1) "Punch IN" else "Punch OUT"}")
    }

    private fun showServerResponse(message: String, success: Boolean, isRedStyle: Boolean = false) {
        runOnUiThread {
            contentBinding.welcomeText.text = message
            if (isRedStyle) {
                contentBinding.welcomeSubtext.visibility = View.GONE
                contentBinding.btnOk.visibility = View.VISIBLE
                contentBinding.welcomeCard.visibility = View.VISIBLE
                contentBinding.welcomeCard.setBackgroundColor(0xFFFFCDD2.toInt())
                contentBinding.welcomeIcon.text = "✕"
                contentBinding.welcomeIconBg.backgroundTintList = ColorStateList.valueOf(0xFFEF5350.toInt())
                tts.speak(message.lines().firstOrNull()?.take(120) ?: message, TextToSpeech.QUEUE_FLUSH, null, null)

                contentBinding.btnOk.setOnClickListener {
                    setResult(RESULT_OK)
                    finish()
                }
            } else if (!success) {
                contentBinding.welcomeSubtext.visibility = View.GONE
                contentBinding.btnOk.visibility = View.GONE
                contentBinding.welcomeCard.visibility = View.VISIBLE
                contentBinding.welcomeCard.setBackgroundColor(0xFFFFCDD2.toInt())
                contentBinding.welcomeIcon.text = "✕"
                contentBinding.welcomeIconBg.backgroundTintList = ColorStateList.valueOf(0xFFEF5350.toInt())
                tts.speak(message.lines().firstOrNull()?.take(120) ?: message, TextToSpeech.QUEUE_FLUSH, null, null)

                Handler(Looper.getMainLooper()).postDelayed({
                    contentBinding.welcomeCard.visibility = View.GONE
                }, 3500)
            } else {
                contentBinding.welcomeSubtext.visibility = View.VISIBLE
                contentBinding.btnOk.visibility = View.GONE
                contentBinding.welcomeCard.visibility = View.VISIBLE
                contentBinding.welcomeCard.setBackgroundColor(0xFFC8E6C9.toInt())
                contentBinding.welcomeIcon.text = "✓"
                contentBinding.welcomeIconBg.backgroundTintList = ColorStateList.valueOf(0xFF22C55E.toInt())
                tts.speak(message.lines().firstOrNull()?.take(120) ?: message, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    private fun showStatus(text: String) {
        runOnUiThread {
            contentBinding.statusText.text = text
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::tts.isInitialized) {
            tts.shutdown()
        }
        if (::cameraHelper.isInitialized) {
            cameraHelper.stopCamera()
        }
        try {
            val lp = window.attributes
            lp.screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = lp
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore brightness in onDestroy", e)
        }
    }

    companion object {
        private const val TAG = "RecognizeAPI"
        const val EXTRA_IN_OUT_FLAG = "IN_OUT_FLAG"
        const val EXTRA_PUNCH_TYPE = "PUNCH_TYPE"
        const val EXTRA_PUNCH_TIME = "PUNCH_TIME"
    }
}
