package com.example.walking_hadang.ui

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.walking_hadang.R
import com.example.walking_hadang.adapter.DayUi
import com.example.walking_hadang.databinding.FragmentRecodingBinding
import com.example.walking_hadang.ui.recoding.RecodeBloodSugarFragment
import com.example.walking_hadang.ui.recoding.RecodeMealFragment
import com.example.walking_hadang.ui.recoding.RecodeWalkingFragment
import com.example.walking_hadang.util.DayViewContainer
import com.example.walking_hadang.util.GlucoseRepository
import com.example.walking_hadang.util.RecodingSharedViewModel
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
    companion object {
        fun newInstance(startPosition: Int): RecodingFragment {
            val fragment = RecodingFragment()
            val args = Bundle()
            args.putInt("start_position", startPosition)
            fragment.arguments = args
            return fragment
        }
    }
    private var _binding: FragmentRecodingBinding? = null
    private val binding get() = _binding!!
    private val sharedVM: RecodingSharedViewModel by viewModels()
    private var selectedDate: LocalDate = LocalDate.now()
    private lateinit var viewPager: ViewPager2


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecodingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvSelectedDate.text = "%d. %d. %d (%s)".format(
            selectedDate.year, selectedDate.monthValue, selectedDate.dayOfMonth,
            selectedDate.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.KOREA)

        )
        setupWeekCalendar()
        viewPager = binding.sectionPager
        viewPager.adapter = SectionPagerAdapter(this) // 아래 어댑터

        val tabs = binding.sectionTabs
        TabLayoutMediator(tabs, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "혈당"
                1 -> "식사"
                2 -> "산책"
                else -> ""
            }
        }.attach()

        val startPosition = arguments?.getInt("start_position") ?: 0
        viewPager.setCurrentItem(startPosition, false)

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
                    if (isSelected)
                        ContextCompat.getDrawable(requireContext(), R.drawable.bg_day_selected)
                    else if(isToday)
                        ContextCompat.getDrawable(requireContext(), R.drawable.bg_today)
                    else null

                // 클릭: 선택 변경 & 해당 두 날짜만 갱신
                container.view.setOnClickListener {
                    val old = selectedDate
                    val new = date
                    if (old != new) {
                        selectedDate = new
                        calendar.notifyDateChanged(old)
                        calendar.notifyDateChanged(new)
                        // 필요 시 상단 날짜/그래프 갱신
                        // 선택 날짜 텍스트 갱신 등
                        binding.tvSelectedDate.text = "%d. %d. %d (%s)".format(
                            selectedDate.year, selectedDate.monthValue, selectedDate.dayOfMonth,
                            selectedDate.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.KOREA)
                        )
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

    private fun buildWindow(center: LocalDate): List<DayUi> {
        val start = center.minusDays(3)
        return (0..6).map { offset ->
            val d = start.plusDays(offset.toLong())
            DayUi(d, d == LocalDate.now(), d == center)
        }
    }

    private fun onDateSelected(date: LocalDate) {
        sharedVM.setSelectedDate(date)
    }

    override fun onPause() {
        super.onPause()
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        toolbar.findViewWithTag<View>("recodingTitleView")?.let {
            toolbar.removeView(it)
        }
    }

    override fun onResume() {
        super.onResume()
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        val titleView = LayoutInflater.from(context).inflate(R.layout.toolbar_custom, toolbar, false) as TextView
        titleView.text = "기록"
        titleView.apply {
            tag = "recodingTitleView" // 중복 방지용 태그
        }
        toolbar.addView(titleView)
    }
}
