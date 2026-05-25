package com.example.facercognitionapp.ui.attendance

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.facercognitionapp.databinding.ItemAttendanceDayRowBinding
import com.example.facercognitionapp.databinding.ItemAttendanceEmptyStateBinding
import com.example.facercognitionapp.model.FullMonthInOutDayDto

class AttendanceDayAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<FullMonthInOutDayDto>()

    fun submit(list: List<FullMonthInOutDayDto>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (items.isEmpty()) VIEW_TYPE_EMPTY else VIEW_TYPE_DAY
    }

    override fun getItemCount(): Int {
        return if (items.isEmpty()) 1 else items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_EMPTY -> EmptyVH(
                ItemAttendanceEmptyStateBinding.inflate(inflater, parent, false)
            )
            else -> DayVH(
                ItemAttendanceDayRowBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is DayVH -> holder.bind(items[position])
            is EmptyVH -> { /* Static empty message in layout */ }
        }
    }

    class EmptyVH(
        binding: ItemAttendanceEmptyStateBinding
    ) : RecyclerView.ViewHolder(binding.root)

    class DayVH(private val binding: ItemAttendanceDayRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: FullMonthInOutDayDto) {
            binding.tvDate.text = row.attendanceDate?.trim().orEmpty().ifEmpty { "—" }
            binding.tvIn.text = row.inTime?.trim().orEmpty().ifEmpty { "—" }
            binding.tvOut.text = row.outTime?.trim().orEmpty().ifEmpty { "—" }
            val st = row.status?.trim().orEmpty().ifEmpty { "—" }
            binding.tvStatus.text = st
            val color = when (st.uppercase()) {
                "P" -> Color.parseColor("#166534")
                "A" -> Color.parseColor("#991B1B")
                "WO", "W" -> Color.parseColor("#1D4ED8")
                else -> Color.parseColor("#6B7280")
            }
            binding.tvStatus.setTextColor(color)
        }
    }

    companion object {
        private const val VIEW_TYPE_EMPTY = 0
        private const val VIEW_TYPE_DAY = 1
    }
}
