package com.example.walking_hadang.ui.community

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.walking_hadang.R

class SugarPostAdapter(
    private val onClick: (CommunityPost) -> Unit
) : ListAdapter<CommunityPost, SugarPostAdapter.VH>(diff) {

    companion object {
        private const val NEW_WINDOW_MS = 24 * 60 * 60 * 1000L  // 24시간
        val diff = object : DiffUtil.ItemCallback<CommunityPost>() {
            override fun areItemsTheSame(oldItem: CommunityPost, newItem: CommunityPost) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: CommunityPost, newItem: CommunityPost) = oldItem == newItem
        }
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val tvBadge = v.findViewById<TextView>(R.id.tvBadge)
        private val tvTitle = v.findViewById<TextView>(R.id.tvPostTitle)
        private val tvPreview = v.findViewById<TextView>(R.id.tvPostPreview)
        private val tvLikes = v.findViewById<TextView>(R.id.tvLikes)
        private val tvComments = v.findViewById<TextView>(R.id.tvComments)
        private val tvTime = v.findViewById<TextView>(R.id.tvTime)
        private val tvNew = v.findViewById<TextView>(R.id.tvNew) // ⬅ 추가
        //테스트
        fun bind(item: CommunityPost) {
            // 배지
            tvBadge.text = item.badge ?: ""
            tvBadge.visibility = if (item.badge.isNullOrBlank()) View.GONE else View.VISIBLE

            // 텍스트
            tvTitle.text = item.title
            tvPreview.text = item.preview
            tvLikes.text = "👍 ${item.likes}"
            tvComments.text = "💬 ${item.comments}"
            tvTime.text = formatRelative(item.createdAt)

            // NEW 표시 (최근 24시간 이내)
            val isNew = (System.currentTimeMillis() - item.createdAt) <= NEW_WINDOW_MS
            tvNew.visibility = if (isNew) View.VISIBLE else View.GONE

            itemView.setOnClickListener { onClick(item) }
        }

        private fun formatRelative(epochMillis: Long): String {
            if (epochMillis <= 0) return ""
            val diff = System.currentTimeMillis() - epochMillis
            val min = diff / 60000
            val hour = min / 60
            val day = hour / 24
            return when {
                min < 60 -> "${min}분 전"
                hour < 24 -> "${hour}시간 전"
                else -> "${day}일 전"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_community_post, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}
