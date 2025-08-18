package com.example.walking_hadang.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.walking_hadang.R
import com.example.walking_hadang.data.MealDayDoc

class MealDayAdapter(
    private val onClick: (MealDayDoc) -> Unit
) : ListAdapter<MealDayDoc, MealDayAdapter.VH>(diff) {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val ivCover: ImageView = v.findViewById(R.id.ivCover)
        val tvDate: TextView = v.findViewById(R.id.tvDate)
        val tvSummary: TextView = v.findViewById(R.id.tvSummary)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meal_day, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.tvDate.text = item.dateKey
        val kcal = item.totalKcal?.let { " / ${it}kcal" } ?: ""
        holder.tvSummary.text = "음식 ${item.itemCount}개$kcal"

        if (!item.coverImageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView).load(item.coverImageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .centerCrop().into(holder.ivCover)
        } else {
            holder.ivCover.setImageResource(R.drawable.ic_image_placeholder)
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }

    companion object {
        private val diff = object : DiffUtil.ItemCallback<MealDayDoc>() {
            override fun areItemsTheSame(o: MealDayDoc, n: MealDayDoc) = o.dateKey == n.dateKey
            override fun areContentsTheSame(o: MealDayDoc, n: MealDayDoc) = o == n
        }
    }
}