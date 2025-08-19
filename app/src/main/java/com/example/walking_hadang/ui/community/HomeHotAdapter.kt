package com.example.walking_hadang.ui.community

import android.graphics.Color            // ✅ 추가
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.walking_hadang.R

class HomeHotAdapter(
    private val onClick: (CommunityPost) -> Unit
) : ListAdapter<CommunityPost, HomeHotAdapter.VH>(diff) {
    //테스트
    companion object {
        private const val NEW_WINDOW_MS = 24 * 60 * 60 * 1000L   // ✅ 추가 (24시간)
        val diff = object : DiffUtil.ItemCallback<CommunityPost>() {
            override fun areItemsTheSame(oldItem: CommunityPost, newItem: CommunityPost) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: CommunityPost, newItem: CommunityPost) = oldItem == newItem
        }
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val tvBadge = v.findViewById<TextView>(R.id.tvBadge)
        private val tvPostTitle = v.findViewById<TextView>(R.id.tvPostTitle)
        private val tvPostPreview = v.findViewById<TextView>(R.id.tvPostPreview)
        private val tvNew = v.findViewById<TextView>(R.id.tvNew)

        fun bind(item: CommunityPost) {
            tvBadge.text = item.badge ?: ""
            tvBadge.visibility = if (item.badge.isNullOrBlank()) View.GONE else View.VISIBLE

            tvPostTitle.text = item.title
            tvPostPreview.text = item.preview

            // NEW 배지 (#BF4040 배경, 흰색 텍스트)
            val isNew = (System.currentTimeMillis() - item.createdAt) <= NEW_WINDOW_MS
            if (isNew) {
                tvNew.visibility = View.VISIBLE
                tvNew.setBackgroundResource(R.drawable.bg_new_badge)
                tvNew.setTextColor(Color.WHITE)
            } else {
                tvNew.visibility = View.GONE
            }

            itemView.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_hot_post, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}
