package com.example.walking_hadang.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.walking_hadang.R
import com.example.walking_hadang.data.MealEntryDoc
import com.example.walking_hadang.data.MealItemLine
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

class MealAdapter(
    private val onClick: (MealEntryDoc) -> Unit,
    private val onDelete: (MealEntryDoc) -> Unit
) : ListAdapter<MealEntryDoc, MealAdapter.VH>(diff) {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        val tvTimeBadge: TextView = itemView.findViewById(R.id.tvTimeBadge)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvBullets: TextView = itemView.findViewById(R.id.tvBullets)
        val tvFooter: TextView = itemView.findViewById(R.id.tvFooter)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meal_entry, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)

        // 이미지
        if (!item.photoUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView).load(item.photoUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .centerCrop()
                .into(holder.ivPhoto)
        } else {
            holder.ivPhoto.setImageResource(R.drawable.ic_image_placeholder)
        }

        // 시간 배지
        holder.tvTimeBadge.text = item.eatenAt.toBadge()

        // 제목
        holder.tvTitle.text = "[${item.mealType}]"

        // 불릿
        holder.tvBullets.text = toBullets(item.items)

        // 하단 (다 먹음 or 메모)
        holder.tvFooter.visibility = View.GONE
        when {
            item.eatenAll == true -> {
                holder.tvFooter.visibility = View.VISIBLE
                holder.tvFooter.text = "다 먹음!"
            }
            !item.note.isNullOrBlank() -> {
                holder.tvFooter.visibility = View.VISIBLE
                holder.tvFooter.text = item.note
            }
        }

        // 클릭/삭제
        holder.itemView.setOnClickListener { onClick(item) }
        holder.btnDelete.setOnClickListener { onDelete(item) }
    }

    companion object {
        private val diff = object : DiffUtil.ItemCallback<MealEntryDoc>() {
            override fun areItemsTheSame(o: MealEntryDoc, n: MealEntryDoc) = o.entryId == n.entryId
            override fun areContentsTheSame(o: MealEntryDoc, n: MealEntryDoc) = o == n
        }

        private fun toBullets(items: List<MealItemLine>): String {
            if (items.isEmpty()) return ""
            return items.joinToString(separator = "\n") { "• ${it.name}${amountSuffix(it)}" }
        }

        private fun amountSuffix(i: MealItemLine): String {
            val meta = listOfNotNull(i.amount, i.kcal?.let { "${it}kcal" })
            return if (meta.isEmpty()) "" else " (${meta.joinToString(", ")})"
        }

        private val timeFmt = SimpleDateFormat("a h:mm", Locale.getDefault()).apply {
            // "오전/오후" 그대로 나오게 로케일에 맡김
        }
        private fun Timestamp?.toBadge(): String =
            this?.toDate()?.let { timeFmt.format(it) } ?: "-"
    }
}