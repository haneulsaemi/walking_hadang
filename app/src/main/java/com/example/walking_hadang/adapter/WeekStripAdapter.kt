package com.example.walking_hadang.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.walking_hadang.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class DayUi(val date: LocalDate, val isToday: Boolean, val isSelected: Boolean)

class WeekStripAdapter(
    private var items: List<DayUi>,
    private val onClick: (LocalDate) -> Unit
) : RecyclerView.Adapter<WeekStripAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvDow: TextView = v.findViewById(R.id.tvDow)
        val tvDay: TextView = v.findViewById(R.id.tvDay)
        init { v.setOnClickListener { onClick(items[bindingAdapterPosition].date) } }
    }

    override fun onCreateViewHolder(p: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_day, p, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, i: Int) {
        val d = items[i]
        h.tvDow.text = d.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.KOREA)
        h.tvDay.text = d.date.dayOfMonth.toString()
        // 주말 색
        h.tvDay.setTextColor(
            when (d.date.dayOfWeek) {
                DayOfWeek.SUNDAY -> Color.parseColor("#E53935")
                DayOfWeek.SATURDAY -> Color.parseColor("#1E88E5")
                else -> Color.BLACK
            }
        )
        // 오늘/선택 표시
        h.tvDay.alpha = if (d.isSelected) 1f else 0.85f
        h.itemView.isSelected = d.isSelected
    }

    fun submit(newItems: List<DayUi>) { items = newItems; notifyDataSetChanged() }
}
