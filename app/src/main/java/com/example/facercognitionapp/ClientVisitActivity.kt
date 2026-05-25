package com.example.facercognitionapp

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.facercognitionapp.databinding.ContentClientVisitBinding
import com.example.facercognitionapp.network.ApiClient
import com.example.facercognitionapp.ui.core.BaseDrawerContentActivity
import com.example.facercognitionapp.util.LocationHelper
import com.example.facercognitionapp.util.SessionHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

class ClientVisitActivity : BaseDrawerContentActivity() {

    private lateinit var binding: ContentClientVisitBinding

    private var latitude: String? = null
    private var longitude: String? = null
    private var saving = false
    private var locationFetchInProgress = false
    private var visitorPhotoFile: File? = null
    private var cameraPhotoFile: File? = null
    private var cameraPhotoUri: Uri? = null

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            if (grants.values.any { it }) {
                fetchLocation()
            } else {
                updateLocationUi()
            }
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchCameraCapture()
            } else {
                Toast.makeText(this, R.string.client_visit_camera_permission, Toast.LENGTH_LONG).show()
            }
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                loadVisitorPhotoFromUri(uri)
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                cameraPhotoFile?.let { applyVisitorPhotoFile(it) }
            } else {
                cameraPhotoFile?.delete()
                cameraPhotoFile = null
                cameraPhotoUri = null
            }
        }

    private val formWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {}
    }

    override fun contentLayoutRes() = R.layout.content_client_visit

    override fun screenTitleRes() = R.string.title_client_visit

    override fun drawerMenuItemId() = R.id.nav_client_visit

    override fun onContentInflated(savedInstanceState: Bundle?) {
        binding = ContentClientVisitBinding.bind(shellBinding.contentContainer.getChildAt(0))

        val prefs = getSharedPreferences(SessionHelper.PREFS_AUTH, MODE_PRIVATE)
        val visitorName = prefs.getString("employee_name", null).orEmpty()
        val cardNo = prefs.getString("employee_card_no", null).orEmpty()
        val visitorId = SessionHelper.visitorId(this)

        binding.tvVisitorName.text = visitorName.ifBlank { getString(R.string.nav_header_guest) }
        binding.tvVisitorMeta.text = getString(
            R.string.client_visit_visitor_meta,
            cardNo.ifBlank { "—" },
            visitorId
        )

        listOf(
            binding.inputCompanyName,
            binding.inputCompanyAddress,
            binding.inputContactPersonName,
            binding.inputContactPersonNo,
            binding.inputEmail,
            binding.inputRemarks
        ).forEach { it.addTextChangedListener(formWatcher) }

        binding.btnPickVisitorPhoto.setOnClickListener { showPhotoSourceDialog() }
        binding.btnSaveVisit.setOnClickListener { saveVisit() }

        updateSaveButtonState()
        updateLocationUi()
        updateVisitorPhotoUi()
        ensureLocationPermissionAndFetch()
    }

    override fun onResume() {
        super.onResume()
        if (hasLocation()) {
            updateLocationUi()
        } else if (!locationFetchInProgress) {
            ensureLocationPermissionAndFetch()
        }
    }

    private fun hasLocation(): Boolean =
        !latitude.isNullOrBlank() && !longitude.isNullOrBlank()

    private fun hasVisitorPhoto(): Boolean =
        visitorPhotoFile?.let { it.exists() && it.length() > 0L } == true

    private fun ensureLocationPermissionAndFetch() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            fetchLocation()
        } else {
            updateLocationUi()
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun fetchLocation() {
        locationFetchInProgress = true
        binding.tvLocationStatus.text = getString(R.string.client_visit_location_pending)
        updateLocationUi()
        lifecycleScope.launch {
            val location = LocationHelper.getCurrentLocation(this@ClientVisitActivity)
            latitude = location?.latitude?.toString()
            longitude = location?.longitude?.toString()
            locationFetchInProgress = false
            updateLocationUi()
        }
    }

    private fun updateLocationUi() {
        val located = hasLocation()
        binding.cardLocationAlert.visibility = if (located) View.GONE else View.VISIBLE
        binding.tvLocationStatus.text = when {
            locationFetchInProgress ->
                getString(R.string.client_visit_location_pending)
            located ->
                getString(R.string.client_visit_location_ok, latitude.orEmpty(), longitude.orEmpty())
            else ->
                getString(R.string.client_visit_location_unavailable)
        }
    }

    private fun showPhotoSourceDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.client_visit_photo_source_title)
            .setItems(
                arrayOf(
                    getString(R.string.client_visit_photo_camera),
                    getString(R.string.client_visit_photo_gallery)
                )
            ) { _, which ->
                when (which) {
                    0 -> requestCameraAndCapture()
                    1 -> pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }
            .show()
    }

    private fun requestCameraAndCapture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            launchCameraCapture()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCameraCapture() {
        val file = File(cacheDir, "visitor_photo_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
        cameraPhotoFile = file
        cameraPhotoUri = uri
        takePictureLauncher.launch(uri)
    }

    private fun loadVisitorPhotoFromUri(uri: Uri) {
        lifecycleScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    copyUriToCacheFile(uri)
                }
                applyVisitorPhotoFile(file)
            } catch (_: IOException) {
                Toast.makeText(
                    this@ClientVisitActivity,
                    R.string.client_visit_photo_error,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun copyUriToCacheFile(uri: Uri): File {
        val file = File(cacheDir, "visitor_photo_${System.currentTimeMillis()}.jpg")
        contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IOException("Cannot open image URI")
        if (!file.exists() || file.length() == 0L) {
            file.delete()
            throw IOException("Empty image file")
        }
        return file
    }

    private fun applyVisitorPhotoFile(file: File) {
        if (visitorPhotoFile != null && visitorPhotoFile != file && visitorPhotoFile != cameraPhotoFile) {
            visitorPhotoFile?.delete()
        }
        visitorPhotoFile = file
        binding.ivVisitorPhoto.setPadding(0, 0, 0, 0)
        binding.ivVisitorPhoto.imageTintList = null
        binding.ivVisitorPhoto.setImageURI(Uri.fromFile(file))
        updateVisitorPhotoUi()
    }

    private fun updateVisitorPhotoUi() {
        val hasPhoto = hasVisitorPhoto()
        binding.btnPickVisitorPhoto.text = getString(
            if (hasPhoto) R.string.client_visit_change_photo else R.string.client_visit_add_photo
        )
    }

    private fun clearVisitorPhoto() {
        if (visitorPhotoFile != cameraPhotoFile) {
            visitorPhotoFile?.delete()
        }
        visitorPhotoFile = null
        cameraPhotoFile?.delete()
        cameraPhotoFile = null
        cameraPhotoUri = null
        val placeholderPadding = (48 * resources.displayMetrics.density).toInt()
        binding.ivVisitorPhoto.setPadding(
            placeholderPadding,
            placeholderPadding,
            placeholderPadding,
            placeholderPadding
        )
        binding.ivVisitorPhoto.setImageResource(android.R.drawable.ic_menu_camera)
        binding.ivVisitorPhoto.imageTintList =
            ContextCompat.getColorStateList(this, android.R.color.darker_gray)
        updateVisitorPhotoUi()
    }

    private fun isFormFieldsComplete(): Boolean =
        binding.inputCompanyName.text?.toString()?.trim().orEmpty().isNotEmpty() &&
            binding.inputCompanyAddress.text?.toString()?.trim().orEmpty().isNotEmpty() &&
            binding.inputContactPersonName.text?.toString()?.trim().orEmpty().isNotEmpty() &&
            binding.inputContactPersonNo.text?.toString()?.trim().orEmpty().isNotEmpty() &&
            binding.inputEmail.text?.toString()?.trim().orEmpty().isNotEmpty() &&
            binding.inputRemarks.text?.toString()?.trim().orEmpty().isNotEmpty() &&
            hasVisitorPhoto()

    private fun updateSaveButtonState() {
        binding.btnSaveVisit.isEnabled = !saving
        binding.btnSaveVisit.alpha = if (saving) 0.6f else 1f
    }

    private fun saveVisit() {
        if (saving) return

        if (!hasLocation()) {
            binding.cardLocationAlert.visibility = View.VISIBLE
            binding.root.post {
                binding.root.smoothScrollTo(0, binding.cardLocationAlert.top)
            }
            Toast.makeText(
                this,
                getString(R.string.client_visit_location_not_found),
                Toast.LENGTH_LONG
            ).show()
            if (!locationFetchInProgress) {
                ensureLocationPermissionAndFetch()
            }
            return
        }

        if (!hasVisitorPhoto()) {
            Toast.makeText(this, R.string.client_visit_photo_required, Toast.LENGTH_LONG).show()
            return
        }

        if (!isFormFieldsComplete()) {
            Toast.makeText(this, R.string.client_visit_fill_all_fields, Toast.LENGTH_LONG).show()
            return
        }

        val prefs = getSharedPreferences(SessionHelper.PREFS_AUTH, MODE_PRIVATE)
        val visitorId = SessionHelper.visitorId(this)
        if (visitorId == 0) {
            Toast.makeText(this, R.string.client_visit_error_no_employee, Toast.LENGTH_LONG).show()
            return
        }

        val photoFile = visitorPhotoFile ?: return
        val textType = "text/plain".toMediaTypeOrNull()
        val imageType = "image/jpeg".toMediaTypeOrNull()
        val photoPart = MultipartBody.Part.createFormData(
            "VisitorPhoto",
            photoFile.name,
            photoFile.asRequestBody(imageType)
        )

        saving = true
        updateSaveButtonState()
        binding.progressSave.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.api.insertVisitorDetails(
                        clientVisitId = "1".toRequestBody(textType),
                        visitorId = visitorId.toString().toRequestBody(textType),
                        visitorCardNo = prefs.getString("employee_card_no", null).orEmpty()
                            .toRequestBody(textType),
                        visitorName = prefs.getString("employee_name", null).orEmpty()
                            .toRequestBody(textType),
                        companyName = binding.inputCompanyName.text.toString().trim()
                            .toRequestBody(textType),
                        companyAddress = binding.inputCompanyAddress.text.toString().trim()
                            .toRequestBody(textType),
                        contactPersonName = binding.inputContactPersonName.text.toString().trim()
                            .toRequestBody(textType),
                        contactPersonNo = binding.inputContactPersonNo.text.toString().trim()
                            .toRequestBody(textType),
                        email = binding.inputEmail.text.toString().trim().toRequestBody(textType),
                        latitude = latitude.orEmpty().toRequestBody(textType),
                        longitude = longitude.orEmpty().toRequestBody(textType),
                        status = "Visited".toRequestBody(textType),
                        companyId = prefs.getInt("company_id", 0).toString().toRequestBody(textType),
                        unitId = prefs.getInt("unit_id", 0).toString().toRequestBody(textType),
                        remarks = binding.inputRemarks.text.toString().trim().toRequestBody(textType),
                        visitorPhoto = photoPart
                    )
                }

                withContext(Dispatchers.Main) {
                    binding.progressSave.visibility = View.GONE
                    saving = false
                    updateSaveButtonState()

                    if (response.isSuccessful) {
                        val msg = response.body()?.message?.takeIf { it.isNotBlank() }
                            ?: getString(R.string.client_visit_saved_default)
                        Toast.makeText(this@ClientVisitActivity, msg, Toast.LENGTH_LONG).show()
                        clearForm()
                    } else {
                        Toast.makeText(
                            this@ClientVisitActivity,
                            getString(R.string.client_visit_error_http, response.code()),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressSave.visibility = View.GONE
                    saving = false
                    updateSaveButtonState()
                    Toast.makeText(
                        this@ClientVisitActivity,
                        e.localizedMessage ?: getString(R.string.client_visit_error_generic),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun clearForm() {
        binding.inputCompanyName.text?.clear()
        binding.inputCompanyAddress.text?.clear()
        binding.inputContactPersonName.text?.clear()
        binding.inputContactPersonNo.text?.clear()
        binding.inputEmail.text?.clear()
        binding.inputRemarks.text?.clear()
        clearVisitorPhoto()
    }
}
