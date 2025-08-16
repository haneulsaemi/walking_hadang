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
import com.example.walking_hadang.util.chart.RoundedBarRenderer
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LimitLine
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
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.renderer.CombinedChartRenderer
import com.google.firebase.Timestamp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecodeBloodSugarFragment : Fragment() {
    private val COLOR_AXIS_BLUE = 0xFF2E8BEF.toInt()
    private val COLOR_LABEL = 0xFF5A7DA0.toInt()
    private val COLOR_GRID_SOFT = 0xFFE6EEF7.toInt()
    private val COLOR_LEGEND = 0xFF666666.toInt()

    private val COLOR_BAR = 0xFFFFE8C6.toInt()   // 베이지 톤 막대
    private val COLOR_LINE = 0xFFFF6E6E.toInt()  // 연한 레드
    private val COLOR_HIGHLIGHT = 0xFF2E8BEF.toInt()
    private val COLOR_TARGET = 0xFFFFA64D.toInt() // 목표선(예: 125)

    private var _binding: FragmentRecodeBloodSugarBinding? = null
    private val binding get() = _binding!!
    private var loadJob: Job? = null
    private lateinit var chart: CombinedChart

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRecodeBloodSugarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val swRange = binding.swRange

        setupChart()
        binding.tvSwitchLabel.text = if (swRange.isChecked) "주간" else "오늘"
        triggerLoad(swRange.isChecked)

        swRange.setOnCheckedChangeListener { _, isChecked ->
            binding.tvSwitchLabel.text = if (isChecked) "주간" else "오늘"
            triggerLoad(isChecked)
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

    private fun setupChart() {
        chart = CombinedChart(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            description.isEnabled = false
            setDrawGridBackground(false)
            setBackgroundColor(Color.TRANSPARENT)
            setExtraOffsets(8f, 8f, 8f, 8f)
            isHighlightPerTapEnabled = true

            // X 축
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawAxisLine(true)
                setDrawGridLines(false)
                axisLineColor = COLOR_AXIS_BLUE
                textColor = COLOR_LABEL
                textSize = 10f
                granularity = 1f
            }

            // Y 축
            axisRight.isEnabled = false
            axisLeft.apply {
                setDrawAxisLine(false)
                setDrawGridLines(true)
                gridColor = COLOR_GRID_SOFT
                gridLineWidth = 1f
                textColor = COLOR_LABEL
                textSize = 10f
                setLabelCount(5, false)
            }

            // 범례
            legend.apply {
                isEnabled = true
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                textSize = 10f
                textColor = COLOR_LEGEND
            }

            drawOrder = arrayOf(
                CombinedChart.DrawOrder.BAR,
                CombinedChart.DrawOrder.LINE
            )
        }

        // ★ CombinedChart에 둥근 막대 렌더러 장착
        val combinedRenderer = object : CombinedChartRenderer(chart, chart.animator, chart.viewPortHandler) {
            init {
                // 기존 BarChartRenderer 교체
                val rounded = RoundedBarRenderer(chart, chart.animator, chart.viewPortHandler, 18f)
                mRenderers.removeAll { it is BarChartRenderer }
                mRenderers.add(rounded)
            }
        }
        chart.renderer = combinedRenderer
        chart.setDrawBarShadow(false) // 필요하면 true

        binding.chartContainer.removeAllViews()
        binding.chartContainer.addView(chart)
    }
    private fun triggerLoad(isWeekly: Boolean) {
        // 이전 로드 취소
        Log.d("glucose","triggerLoad(isWeekly=$isWeekly) called. cancel prev=${loadJob != null}")
        loadJob?.cancel()

        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (isWeekly) {
                    // 주간 데이터
                    val fasting = withContext(Dispatchers.IO) {
                        GlucoseRepository.getWeeklyByType(GlucoseType.FASTING)
                    }
                    Log.d("glucose", fasting.toString())
                    // UI 업데이트는 메인 스레드
                    Log.d("glucose" , "load weekly: done, count=${fasting.size}")
                    val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
                    val dayFmt = SimpleDateFormat("E", Locale.KOREA)

                    renderWeeklyCombined(
                        data = fasting,
                        title = "주간 공복 평균",
                        labelOf = { gd ->
                            val d = keyFmt.parse(gd.dateKey)
                            if (d != null) dayFmt.format(d) else ""
                        },
                        valueOf = { gd -> gd.value.toFloat() },
                        showAvgLine = true
                    )
                } else {
                    // 오늘 데이터
                    val today = withContext(Dispatchers.IO) {
                        GlucoseRepository.getDaily()
                    }
                    Log.d("glucose", "load daily: done, count=${today.size}")
                    renderDailyCombined(today)
                }
            } catch (e: CancellationException) {
                // 취소는 무시 (연타 시 정상)
            } catch (e: Exception) {
                // 에러 처리
                Log.e("glucose", "load error", e)
                // showErrorToast() 등
            }
        }
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
    private fun scaleYAxis(entriesY: List<Float>) {
        if (entriesY.isEmpty()) return
        val minVal = entriesY.minOrNull() ?: 0f
        val maxVal = entriesY.maxOrNull() ?: 0f
        chart.axisLeft.apply {
            axisMinimum = (minVal - 15f).coerceAtLeast(60f)
            axisMaximum = (maxVal + 15f).coerceAtMost(250f)
        }
    }

    private fun renderDailyCombined(data: List<GlucoseData>) {
        if (data.isEmpty()) { chart.clear(); chart.invalidate(); return }

        val sorted = data.sortedBy { it.recordedAt.seconds }

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

        val barSet = BarDataSet(barEntries, "오늘(막대)").apply {
            color = COLOR_BAR
            setDrawValues(false)
            highLightAlpha = 30
            highLightColor = COLOR_HIGHLIGHT
        }
        val barData = BarData(barSet).apply { barWidth = 0.45f }

        val lineSet = LineDataSet(lineEntries, "추세(라인)").apply {
            color = COLOR_LINE
            lineWidth = 2.2f
            setDrawCircles(true)
            circleRadius = 3f
            setDrawCircleHole(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            highLightColor = COLOR_HIGHLIGHT
        }
        val lineData = LineData(lineSet)

        chart.data = CombinedData().apply {
            setData(barData)
            setData(lineData)
        }

        chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            setLabelCount(labels.size.coerceAtMost(6), false)
            yOffset = 6f
        }

        // 목표선(원하면 값 바꿔 쓰기)
        chart.axisLeft.apply {
            removeAllLimitLines()
            val target = LimitLine(125f, "").apply {
                lineColor = COLOR_TARGET
                lineWidth = 1f
                enableDashedLine(6f, 6f, 0f)
            }
            addLimitLine(target)
            setDrawLimitLinesBehindData(true)
        }

        scaleYAxis(lineEntries.map { it.y })
        chart.animateY(600)
        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    private fun <T> renderWeeklyCombined(
        data: List<T>,
        title: String,
        labelOf: (T) -> String,
        valueOf: (T) -> Float,
        showAvgLine: Boolean = false
    ) {
        if (data.isEmpty()) { chart.clear(); chart.invalidate(); return }

        // X 라벨 & 막대 엔트리 구성
        val labels = ArrayList<String>()
        val barEntries = ArrayList<BarEntry>()

        data.forEachIndexed { idx, item ->
            labels += labelOf(item)
            barEntries += BarEntry(idx.toFloat(), valueOf(item))
        }

        val barSet = BarDataSet(barEntries, title).apply {
            color = COLOR_BAR
            setDrawValues(false)
            highLightAlpha = 30
            highLightColor = COLOR_HIGHLIGHT
        }
        val barData = BarData(barSet).apply { barWidth = 0.55f }

        // (옵션) 주간 평균 라인
        val combined = CombinedData().apply { setData(barData) }

        if (showAvgLine) {
            val avg = barEntries.map { it.y }.average().toFloat()
            val lineEntries = labels.indices.map { idx -> Entry(idx.toFloat(), avg) }
            val lineSet = LineDataSet(lineEntries, "주간 평균").apply {
                color = COLOR_LINE
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.HORIZONTAL_BEZIER
                highLightColor = COLOR_HIGHLIGHT
            }
            combined.setData(LineData(lineSet))
        }

        chart.data = combined

        chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            setLabelCount(labels.size, true)
            yOffset = 6f
            // 라벨이 '월 화 수 ...' 같이 짧다면 회전 불필요, 길면 아래처럼:
            // labelRotationAngle = -15f
        }

        // 목표선(공복/식후에 맞춰 조정)
        chart.axisLeft.apply {
            removeAllLimitLines()
            val target = LimitLine(125f, "").apply {
                lineColor = COLOR_TARGET
                lineWidth = 1f
                enableDashedLine(6f, 6f, 0f)
            }
            addLimitLine(target)
            setDrawLimitLinesBehindData(true)
        }

        // 스케일
        scaleYAxis(barEntries.map { it.y })

        chart.animateY(600)
        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    override fun onDestroyView() {
        loadJob?.cancel()
        loadJob = null
        _binding = null
        super.onDestroyView()
    }
}