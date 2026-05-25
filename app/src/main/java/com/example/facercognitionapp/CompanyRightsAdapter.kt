package com.example.facercognitionapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.facercognitionapp.databinding.ItemCompanyRightBinding
import com.example.facercognitionapp.model.CompanyRight

class CompanyRightsAdapter(
    private val items: List<CompanyRight>,
    private val onClick: (CompanyRight) -> Unit
) : RecyclerView.Adapter<CompanyRightsAdapter.VH>() {

    class VH(val binding: ItemCompanyRightBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCompanyRightBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.binding.companyName.text = item.companyName
        holder.binding.plantName.text = item.plantName

        holder.binding.root.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size
}

