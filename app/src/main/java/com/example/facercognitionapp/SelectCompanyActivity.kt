package com.example.facercognitionapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.facercognitionapp.databinding.ContentSelectCompanyBinding
import com.example.facercognitionapp.model.CompanyRight
import com.example.facercognitionapp.ui.core.BaseDrawerContentActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SelectCompanyActivity : BaseDrawerContentActivity() {

    private lateinit var contentBinding: ContentSelectCompanyBinding
    private val gson = Gson()

    override fun contentLayoutRes() = R.layout.content_select_company

    override fun screenTitleRes() = R.string.title_select_company

    override fun drawerMenuItemId() = com.example.facercognitionapp.ui.core.AbstractDrawerActivity.NO_DRAWER_SELECTION

    override fun onContentInflated(savedInstanceState: Bundle?) {
        contentBinding = ContentSelectCompanyBinding.bind(
            shellBinding.contentContainer.getChildAt(0)
        )

        val rightsJson = intent.getStringExtra(EXTRA_COMPANY_RIGHTS_JSON)
            ?: getSharedPreferences("auth", MODE_PRIVATE).getString(PREF_COMPANY_RIGHTS_JSON, null)
        val rights = parseCompanyRights(rightsJson)

        if (rights.isNullOrEmpty()) {
            Toast.makeText(this, "No company rights found.", Toast.LENGTH_SHORT).show()
            return
        }

        contentBinding.companyRightsList.layoutManager = LinearLayoutManager(this)
        contentBinding.companyRightsList.adapter = CompanyRightsAdapter(
            items = rights,
            onClick = { right ->
                getSharedPreferences("auth", MODE_PRIVATE).edit().apply {
                    putInt("company_id", right.companyId)
                    putString("company_name", right.companyName)
                    putInt("plant_id", right.plantId)
                    putString("plant_name", right.plantName)
                    apply()
                }

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        )
    }

    private fun parseCompanyRights(json: String?): List<CompanyRight>? {
        if (json.isNullOrBlank()) return null
        val type = object : TypeToken<List<CompanyRight>>() {}.type
        return runCatching { gson.fromJson<List<CompanyRight>>(json, type) }.getOrNull()
    }

    companion object {
        const val EXTRA_COMPANY_RIGHTS_JSON = "extra_company_rights_json"
        const val PREF_COMPANY_RIGHTS_JSON = "company_rights_json"
    }
}
