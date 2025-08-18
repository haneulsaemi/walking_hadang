package com.example.walking_hadang.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.walking_hadang.data.AssetCourseData
import com.example.walking_hadang.data.CourseData
import com.example.walking_hadang.databinding.ItemCourseCardBinding
import com.example.walking_hadang.databinding.ItemCourseCardHomeBinding

class AssetCourseCardAdapter(private var items: List<AssetCourseData>) :
    RecyclerView.Adapter<AssetCourseCardAdapter.CourseViewHolder>() {

    inner class CourseViewHolder(val binding: ItemCourseCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        CourseViewHolder(ItemCourseCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val item = items[position]
        holder.binding.title.text = item.name
        holder.binding.lengthInfo.text = "${item.length}km"
        holder.binding.timeInfo.text = "${item.time}시간"
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<AssetCourseData>) {
        items = newItems
        notifyDataSetChanged()
    }
}