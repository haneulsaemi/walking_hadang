package com.example.walking_hadang

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.walking_hadang.databinding.ItemCourseCardBinding

class CourseCardAdapter(private val items: List<CourseData>) :
    RecyclerView.Adapter<CourseCardAdapter.CourseViewHolder>() {

    inner class CourseViewHolder(val binding: ItemCourseCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        CourseViewHolder(ItemCourseCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val item = items[position]
        holder.binding.title.text = item.stretNm
        holder.binding.lengthInfo.text = "${item.stretLt}km"
        holder.binding.timeInfo.text = "${item.reqreTime}시간"
    }

    override fun getItemCount(): Int = items.size
}
