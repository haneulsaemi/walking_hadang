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
import com.example.walking_hadang.data.WalkData
import com.example.walking_hadang.util.WalkRepository
import com.google.android.gms.maps.model.LatLng
import java.util.Locale

class WalkAdapter(
    private val buildStaticMapUrl: (String, LatLng?, LatLng?) -> String,
    private val onClick: (WalkData) -> Unit = {}
) : ListAdapter<WalkData, WalkAdapter.VH>(diff) {

    companion object {
        val diff = object : DiffUtil.ItemCallback<WalkData>() {
            override fun areItemsTheSame(a: WalkData, b: WalkData) = a.id == b.id
            override fun areContentsTheSame(a: WalkData, b: WalkData) = a == b
        }
    }

    inner class VH(val v: View) : RecyclerView.ViewHolder(v) {
        private val img = v.findViewById<ImageView>(R.id.imgThumb)
        private val txtDate = v.findViewById<TextView>(R.id.txtDate)
        private val txtDist = v.findViewById<TextView>(R.id.txtDistance)
        private val txtPace = v.findViewById<TextView>(R.id.txtPaceTime)

        fun bind(item: WalkData) {
            // 날짜
            txtDate.text = WalkRepository.dateKeyFrom(item.startedAt)

            // 거리/페이스(예: 분’초”/km) – 간단 계산
            val km = item.distanceM / 1000.0
            txtDist.text = String.format(Locale.getDefault(), "%.2f Km", km)

            val paceSecPerKm = if (km > 0) (item.durationSec / km).toInt() else 0
            val m = paceSecPerKm / 60
            val s = paceSecPerKm % 60
            txtPace.text = String.format(Locale.getDefault(), "%d'%02d\"  •  %02d:%02d",
                m, s, item.durationSec/60, item.durationSec%60)

            // 썸네일
            val poly = item.routePolyline
            if (!poly.isNullOrBlank()) {
                val start = item.startLat?.let { slat ->
                    item.startLng?.let { slng -> LatLng(slat, slng) }
                }
                val end = item.endLat?.let { elat ->
                    item.endLng?.let { elng -> LatLng(elat, elng) }
                }
                val url = buildStaticMapUrl(poly, start, end)
                Glide.with(img).load(url).into(img)
            } else {
                img.setImageDrawable(null)
                v.findViewById<TextView>(R.id.placeholderText).visibility = View.VISIBLE
            }

            v.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_walk, parent, false)
        return VH(view)
    }
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}
