package com.example.walking_hadang.ui

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.walking_hadang.R
import com.example.walking_hadang.databinding.FragmentRecodingBinding
import com.example.walking_hadang.ui.recoding.RecodeBloodSugarFragment
import com.example.walking_hadang.ui.recoding.RecodeMealFragment
import com.example.walking_hadang.ui.recoding.RecodeWalkingFragment
import com.example.walking_hadang.util.DayViewContainer
import com.example.walking_hadang.util.GlucoseRepository
import com.google.android.material.datepicker.DayViewDecorator
import com.google.android.material.tabs.TabLayoutMediator
import com.kizitonwose.calendar.core.Week
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.view.WeekDayBinder
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale


class RecodingFragment : Fragment() {
    private var _binding: FragmentRecodingBinding? = null
    private val binding get() = _binding!!
    private var selectedDate: LocalDate = LocalDate.now()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecodingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().findViewById<TextView>(R.id.toolbarTitle).text = "기록"
        setupWeekCalendar()
        val pager = binding.sectionPager
        pager.adapter = SectionPagerAdapter(this) // 아래 어댑터

        val tabs = binding.sectionTabs
        val titles = listOf("혈당", "식단", "산책")
        TabLayoutMediator(tabs, pager) { tab, pos -> tab.text = titles[pos] }.attach()

    }

    class SectionPagerAdapter(f: Fragment) : FragmentStateAdapter(f) {
        override fun getItemCount() = 3
        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> RecodeBloodSugarFragment()   // 그래프
            1 -> RecodeMealFragment()      // 카드들
            else -> RecodeWalkingFragment() // 산책 요약
        }
    }
    private fun setupWeekCalendar() {
        val calendar = binding.weekCalendar

        calendar.dayBinder = object : WeekDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)

            override fun bind(container: DayViewContainer, day: WeekDay) {
                container.day = day
                val date = day.date

                // 요일/날짜
                container.tvDow.text = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.KOREA) // 월화수…
                container.tvDay.text = date.dayOfMonth.toString()

                // 주말 색상
                val textColor = when (date.dayOfWeek) {
                    DayOfWeek.SUNDAY -> Color.parseColor("#E53935")
                    DayOfWeek.SATURDAY -> Color.parseColor("#1E88E5")
                    else -> Color.BLACK
                }
                container.tvDay.setTextColor(textColor)

                // 오늘/선택 하이라이트
                val isSelected = (date == selectedDate)
                val isToday = (date == LocalDate.now())
                container.tvDay.background =
                    if (isSelected || isToday)
                        ContextCompat.getDrawable(requireContext(), R.drawable.bg_day_selected)
                    else null

                // 클릭: 선택 변경 & 해당 두 날짜만 갱신
                container.view.setOnClickListener {
                    Log.d("CALENDAR", "캘린더 뷰는 잇어요")
                    val old = selectedDate
                    val new = date
                    if (old != new) {
                        selectedDate = new
                        calendar.notifyDateChanged(old)
                        calendar.notifyDateChanged(new)
                        // 필요 시 상단 날짜/그래프 갱신
                        onDateSelected(new)
                    }
                }
            }
        }

        val today = LocalDate.now()
        calendar.setup(
            startDate = today.minusWeeks(52),
            endDate = today.plusWeeks(52),
            firstDayOfWeek = DayOfWeek.MONDAY
        )
        calendar.post { calendar.scrollToDate(selectedDate) }
    }
    private fun onDateSelected(date: LocalDate) {
        // 예: 일별 혈당 데이터 조회 후 차트 갱신
        lifecycleScope.launch {
            //val dailyData = GlucoseRepository.getDaily(date)
            //renderDailyCombined(dailyData) // 기존 차트 그리는 함수
        }
    }
}
