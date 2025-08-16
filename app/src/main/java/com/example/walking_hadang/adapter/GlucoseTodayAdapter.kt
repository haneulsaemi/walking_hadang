package com.example.walking_hadang.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.walking_hadang.R
import com.example.walking_hadang.data.GlucoseData
import com.example.walking_hadang.data.GlucoseType
import com.google.android.material.progressindicator.LinearProgressIndicator

class GlucoseTodayAdapter(
    private val onDelete: (GlucoseData) -> Unit
) : RecyclerView.Adapter<GlucoseTodayAdapter.VH>() {

    private val items = mutableListOf<GlucoseData>()
    fun submit(list: List<GlucoseData>) {
        items.clear()
        items.addAll(list.sortedBy { it.recordedAt.seconds }) // 시간 순
        notifyDataSetChanged()
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTime: TextView = v.findViewById(R.id.tvTime)
        val tvTitle: TextView = v.findViewById(R.id.tvTitle)
        val progress: LinearProgressIndicator = v.findViewById(R.id.progress)
        val tvBand1: TextView = v.findViewById(R.id.tvBand1)
        val tvBand2: TextView = v.findViewById(R.id.tvBand2)
        val tvBand3: TextView = v.findViewById(R.id.tvBand3)
        val tvRangeHint: TextView = v.findViewById(R.id.tvRangeHint)
        val btnDelete: ImageButton = v.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(p: ViewGroup, vt: Int): VH {
        val v = LayoutInflater.from(p.context).inflate(R.layout.item_glucose_entry, p, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, i: Int) {
        val d = items[i]

        // 시간 포맷
        val ms = d.recordedAt.seconds * 1000L + d.recordedAt.nanoseconds / 1_000_000L
        h.tvTime.text = android.text.format.DateFormat.format("a h:mm", java.util.Date(ms))

        // 제목
        val typeLabel = when (d.type) {
            GlucoseType.FASTING -> "공복 혈당"
            GlucoseType.POSTPRANDIAL -> "식후 혈당"
            GlucoseType.BEDTIME -> "취침 전 혈당"
        }
        h.tvTitle.text = "$typeLabel: ${d.value} mg/dL"

        // 구간 설정(공복/식후2h/취침전 별 기준)
        val (max, band1End, band2End, rangesText) = when (d.type) {
            GlucoseType.FASTING -> Quad(200, 99, 125, "정상 <100 | 전단계 100–125 | 당뇨 ≥126")
            GlucoseType.POSTPRANDIAL -> Quad(300, 139, 199, "정상 <140 | 전단계 140–199 | 당뇨 ≥200")
            GlucoseType.BEDTIME -> Quad(250, 129, 180, "권장 80–130 | 주의 131–180 | 고혈당 >180")
        }
        h.tvBand1.text = "정상"
        h.tvBand2.text = "전단계"
        h.tvBand3.text = "당뇨"
        // 취침전 텍스트를 좀 다르게 하려면 여기서 label 변경 가능
        if (d.type == GlucoseType.BEDTIME) {
            h.tvBand1.text = "권장"
            h.tvBand2.text = "주의"
            h.tvBand3.text = "고혈당"
        }
        h.tvRangeHint.text = rangesText

        // 막대 값/색
        h.progress.max = max
        h.progress.setProgressCompat(d.value.coerceAtMost(max), true)

        val indicator = when {
            d.value <= band1End -> 0xFF66BB6A.toInt() // green
            d.value <= band2End -> 0xFFFFB300.toInt() // amber
            else -> 0xFFE53935.toInt()                // red
        }
        h.progress.setIndicatorColor(indicator)

        h.btnDelete.setOnClickListener { onDelete(d) }
    }

    // 작은 튜플용
    data class Quad(val max: Int, val band1End: Int, val band2End: Int, val hint: String)
}
