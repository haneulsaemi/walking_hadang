package com.example.walking_hadang.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.example.walking_hadang.R
import com.example.walking_hadang.data.AssetCourseData
import com.example.walking_hadang.databinding.ItemCourseCardHomeBinding
import com.example.walking_hadang.ui.MapFragment

class CourseCardAdapter(
    private var items: List<AssetCourseData>,
    private val fragmentManager: FragmentManager?
) :
    RecyclerView.Adapter<CourseCardAdapter.CourseViewHolder>() {

    inner class CourseViewHolder(val binding: ItemCourseCardHomeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        CourseViewHolder(ItemCourseCardHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val item = items[position]
        holder.binding.title.text = item.name
        holder.binding.lengthInfo.text = "${item.length}km"
        holder.binding.timeInfo.text = "${item.time}시간"
        holder.binding.btnGoto.setOnClickListener {
            fragmentManager?.let {
                val fragment = MapFragment.newInstance(item)
                it.beginTransaction()
                    .replace(R.id.main_fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            } ?: run {
                Log.w("CourseCardAdapter", "FragmentManager is null. Cannot navigate to MapFragment.")
            }


        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<AssetCourseData>) {
        items = newItems
        notifyDataSetChanged()
    }
}