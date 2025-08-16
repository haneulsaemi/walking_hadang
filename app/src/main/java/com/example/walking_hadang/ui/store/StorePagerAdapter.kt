package com.example.walking_hadang.ui.store

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class StorePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment = when (position) {   // 반환 타입 반드시 Fragment
        0 -> LowSugarFragment()   // 저당식품
        1 -> SportswearFragment() // 운동복
        else -> SneakersFragment()// 운동화
    } as Fragment
}
