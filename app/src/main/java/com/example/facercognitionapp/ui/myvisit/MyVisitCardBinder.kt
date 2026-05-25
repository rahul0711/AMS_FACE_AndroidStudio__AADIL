package com.example.facercognitionapp.ui.myvisit

import com.example.facercognitionapp.R
import com.example.facercognitionapp.databinding.IncludeVisitDetailRowBinding
import com.example.facercognitionapp.databinding.ItemMyVisitCardBinding
import com.example.facercognitionapp.model.VisitorDetailsDto
import com.example.facercognitionapp.util.VisitorDetailsJsonParser

object MyVisitCardBinder {

    fun bind(card: ItemMyVisitCardBinding, visit: VisitorDetailsDto) {
        bindRow(card.rowCompanyName, R.string.my_visit_label_company, visit.companyName)
        bindRow(card.rowVisitorName, R.string.my_visit_label_visitor, visit.visitorName)
        bindRow(card.rowCompanyAddress, R.string.my_visit_label_address, visit.companyAddress)
        bindRow(card.rowContactPersonName, R.string.my_visit_label_contact, visit.contactPersonName)
        bindRow(card.rowContactPersonNo, R.string.my_visit_label_phone, visit.contactPersonNo)
        bindRow(card.rowEmail, R.string.my_visit_label_email, visit.email)
        bindRow(card.rowRemarks, R.string.my_visit_label_remarks, visit.remarks)
        bindRow(
            card.rowEntryDate,
            R.string.my_visit_label_entry_date,
            VisitorDetailsJsonParser.formatEntryDate(visit.entryDate)
        )
    }

    private fun bindRow(
        row: IncludeVisitDetailRowBinding,
        labelRes: Int,
        value: String?
    ) {
        row.tvLabel.text = row.root.context.getString(labelRes)
        row.tvValue.text = value?.trim()?.takeIf { it.isNotEmpty() } ?: "—"
    }
}
