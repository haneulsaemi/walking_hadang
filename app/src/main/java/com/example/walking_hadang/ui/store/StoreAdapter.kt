package com.example.walking_hadang.ui.store

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.walking_hadang.R

class StoreAdapter(private val items: List<StoreItem>) :
    RecyclerView.Adapter<StoreAdapter.StoreViewHolder>() {

    inner class StoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImage: ImageView = itemView.findViewById(R.id.itemImage)
        val itemName: TextView = itemView.findViewById(R.id.itemName)
        val itemPrice: TextView = itemView.findViewById(R.id.itemPrice)
        val itemDiscountPrice: TextView = itemView.findViewById(R.id.itemDiscountPrice)
        val itemDiscountRate: TextView = itemView.findViewById(R.id.itemDiscountRate)
        val buyButton: Button = itemView.findViewById(R.id.buyButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoreViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_store, parent, false)
        return StoreViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoreViewHolder, position: Int) {
        val item = items[position]

        holder.itemName.text = item.상품명
        holder.itemPrice.text = "원가: ${item.원가}"
        holder.itemDiscountPrice.text = "할인가: ${item.할인가}"
        holder.itemDiscountRate.text = "할인율: ${item.할인율}"

        // drawable 리소스로 이미지 설정
        val resId = holder.itemView.context.resources.getIdentifier(
            item.이미지리소스, // StoreItem에 "이미지리소스" 필드 필요
            "drawable",
            holder.itemView.context.packageName
        )
        holder.itemImage.setImageResource(resId)

        // 버튼 클릭 → 링크 열기
        holder.buyButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.링크))
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size
}
