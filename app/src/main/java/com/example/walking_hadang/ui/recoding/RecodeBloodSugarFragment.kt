package com.example.walking_hadang.ui.recoding

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.walking_hadang.MyApplication
import com.example.walking_hadang.R
import com.example.walking_hadang.data.GlucoseData
import com.example.walking_hadang.data.GlucoseType
import com.example.walking_hadang.data.MealType
import com.example.walking_hadang.databinding.FragmentRecodeBloodSugarBinding
import com.example.walking_hadang.util.GlucoseRepository
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecodeBloodSugarFragment : Fragment() {

    private var _binding: FragmentRecodeBloodSugarBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRecodeBloodSugarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
//            val today = GlucoseRepository.getDaily(); //오늘 데이터
//            renderDailyChart(today)

            // 예) 공복 주간
            val fasting = GlucoseRepository.getWeeklyByType(GlucoseType.FASTING)
            Log.d("glucose", fasting.toString())
            renderWeeklyCombined(fasting, "주간 공복 평균")
        }

        binding.btnAddGlucose.setOnClickListener {
            Log.d("GlucoseDebug", "버튼 클릭됨")
            val valueStr = binding.etGlucose.text.toString()
            Log.d("GlucoseDebug", "입력된 혈당 문자열: '$valueStr'")

            if (valueStr.isBlank()) {
                Toast.makeText(requireContext(), "혈당 값을 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val value = valueStr.toInt()
            Log.d("GlucoseDebug", "혈당 값(Int): $value")
            // 측정 종류
            val type = when (binding.chipTimeGroup.checkedChipId) {
                R.id.chipFasting ->  GlucoseType.FASTING
                R.id.chipPost2h ->  GlucoseType.POSTPRANDIAL
                R.id.chipBeforeBed ->  GlucoseType.BEDTIME
                else -> {
                    Log.w("GlucoseDebug", "측정 종류 미선택")
                    Toast.makeText(requireContext(), "측정 종류를 선택하세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            val note = binding.etNote.text.toString().ifBlank { null }
            Log.d("GlucoseDebug", "메모: $note")
            // 끼니 (식후일 경우만)
            var mealType: MealType? = null
            var minutesAfter: Int? = null
            if (type == GlucoseType.POSTPRANDIAL) {
                mealType = when (binding.chipMealGroup.checkedChipId) {
                    R.id.chipBreakfast ->  MealType.BREAKFAST

                    R.id.chipLunch ->  MealType.LUNCH

                    R.id.chipDinner ->  MealType.DINNER
                    else -> {
                        Log.w("GlucoseDebug", "식후인데 끼니 미선택")
                        Toast.makeText(requireContext(), "식후 측정 시 끼니를 선택하세요.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
                minutesAfter = 120 // 기본 2시간, 필요하면 입력 필드로 변경 가능
                Log.d("GlucoseDebug", "식후 분: $minutesAfter")
            }

            Log.d("GlucoseDebug", "Firestore 저장 시작")
            val data = GlucoseData(
                value = value,
                type = type,
                meal = mealType,
                postprandialMinutes = minutesAfter,
                memo = note ?: "" // note가 null이면 빈 문자열로
            )

            GlucoseRepository.addGlucoseEntry(
                raw = data,
                onSuccess = { id ->
                    Log.d("GlucoseDebug", "저장 성공: $id")
                },
                onError = { e ->
                    Log.e("GlucoseDebug", "저장 실패", e)
                }
            )
        }
    }

    private fun tsToMillis(ts: Timestamp): Long =
        ts.seconds * 1000L + ts.nanoseconds / 1_000_000L

    private class DateValueFormatter(
        private val pattern: String
    ) : ValueFormatter() {
        private val sdf = SimpleDateFormat(pattern, Locale.KOREA)
        override fun getFormattedValue(value: Float): String {
            return sdf.format(Date(value.toLong()))
        }
    }

    /** chartContainer에 LineChart를 새로 넣고 반환 */
    private fun Fragment.makeLineChart(): LineChart {
        val chart = LineChart(requireContext())

        chart.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        binding.chartContainer.removeAllViews()
        binding.chartContainer.addView(chart)
        return chart
    }
    private fun Fragment.makeCombinedChart(): CombinedChart {
        val chart = CombinedChart(requireContext())
        chart.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        binding.chartContainer.removeAllViews()
        binding.chartContainer.addView(chart)

        // 공통 옵션
        chart.description.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = true
        chart.isHighlightPerTapEnabled = true
        chart.drawOrder = arrayOf(
            CombinedChart.DrawOrder.BAR,        // 막대를 뒤에 깔고
            CombinedChart.DrawOrder.LINE        // 라인을 위에
        )
        return chart
    }

    /** Y축 스케일을 혈당 범위에 맞게 조정 */
    private fun scaleYAxis(chart: CombinedChart, entriesY: List<Float>) {
        if (entriesY.isEmpty()) return
        val minVal = entriesY.minOrNull() ?: 0f
        val maxVal = entriesY.maxOrNull() ?: 0f
        chart.axisLeft.apply {
            axisMinimum = (minVal - 10f).coerceAtLeast(60f)
            axisMaximum = (maxVal + 10f).coerceAtMost(250f)
        }
    }

    private fun renderDailyCombined(data: List<GlucoseData>) {
        val chart = makeCombinedChart()
        if (data.isEmpty()) { chart.clear(); return }

        // 시간순 정렬
        val sorted = data.sortedBy { it.recordedAt.seconds }

        // X축 라벨(HH:mm) + entries(index 기반)
        val labels = ArrayList<String>()
        val barEntries = ArrayList<BarEntry>()
        val lineEntries = ArrayList<Entry>()

        val timeFmt = SimpleDateFormat("HH:mm", Locale.KOREA)
        sorted.forEachIndexed { idx, gd ->
            val ms = gd.recordedAt.seconds * 1000L + gd.recordedAt.nanoseconds / 1_000_000L
            labels += timeFmt.format(Date(ms))
            val v = gd.value.toFloat()
            barEntries += BarEntry(idx.toFloat(), v)
            lineEntries += Entry(idx.toFloat(), v)
        }

        val barSet = BarDataSet(barEntries, "혈당(막대)").apply {
            color = Color.parseColor("#6EC6FF")
            setDrawValues(false)
            axisDependency = chart.axisLeft.axisDependency
        }
        val lineSet = LineDataSet(lineEntries, "혈당(라인)").apply {
            color = Color.parseColor("#FF5252")
            lineWidth = 2f
            setDrawCircles(true)
            circleRadius = 3f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            axisDependency = chart.axisLeft.axisDependency
        }

        val combined = CombinedData().apply {
            setData(BarData(barSet).apply { barWidth = 0.45f })
            setData(LineData(lineSet))
        }
        chart.data = combined

        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            valueFormatter = IndexAxisValueFormatter(labels)
            setLabelCount(labels.size.coerceAtMost(6), false) // 너무 많으면 자동 간격
        }

        scaleYAxis(chart, lineEntries.map { it.y })
        chart.invalidate()
    }

    private fun renderWeeklyCombined(weekly: List<GlucoseData>, label: String) {
        val chart = makeCombinedChart()
        if (weekly.isEmpty()) { chart.clear(); return }

        // yyyy-MM-dd로 그룹 → 평균, 라벨은 MM/dd
        val sdfKey = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
        val sdfShow = SimpleDateFormat("MM/dd", Locale.KOREA)

        val grouped = weekly.groupBy {
            val ms = it.recordedAt.seconds * 1000L + it.recordedAt.nanoseconds / 1_000_000L
            sdfKey.format(Date(ms))
        }.toSortedMap()

        val labels = ArrayList<String>()
        val barEntries = ArrayList<BarEntry>()
        val lineEntries = ArrayList<Entry>()

        grouped.entries.forEachIndexed { idx, (dayKey, items) ->
            val avg = items.map { it.value }.average().toFloat()
            labels += sdfShow.format(sdfKey.parse(dayKey)!!)
            barEntries += BarEntry(idx.toFloat(), avg)
            lineEntries += Entry(idx.toFloat(), avg)
        }

        val barSet = BarDataSet(barEntries, "$label(막대)").apply {
            color = Color.parseColor("#6EC6FF")
            setDrawValues(false)
            axisDependency = chart.axisLeft.axisDependency
        }
        val lineSet = LineDataSet(lineEntries, "$label(라인)").apply {
            color = Color.parseColor("#FF5252")
            lineWidth = 1f
            setDrawCircles(true)
            circleRadius = 3f
            setDrawValues(false)
            mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            axisDependency = chart.axisLeft.axisDependency
        }

        val combined = CombinedData().apply {
            setData(BarData(barSet).apply { barWidth = 0.45f })
            setData(LineData(lineSet))
        }
        chart.data = combined

        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            valueFormatter = IndexAxisValueFormatter(labels)
            setLabelCount(labels.size.coerceAtMost(7), false)
        }

        scaleYAxis(chart, lineEntries.map { it.y })
        chart.invalidate()
    }
}