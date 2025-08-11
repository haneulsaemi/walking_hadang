package com.example.walking_hadang.util

import android.view.View
import android.widget.TextView
import com.example.walking_hadang.R
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.view.ViewContainer

class DayViewContainer(view: View) : ViewContainer(view){
    // 날짜 클릭 처리를 위해 필드로 둡니다.
    lateinit var day: WeekDay

    // item_day.xml에 정의된 TextView들
    val tvDow: TextView = view.findViewById(R.id.tvDow)
    val tvDay: TextView = view.findViewById(R.id.tvDay)
}