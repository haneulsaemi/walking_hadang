package com.example.walking_hadang.ui

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.walking_hadang.MyApplication
import com.example.walking_hadang.R
import com.example.walking_hadang.databinding.FragmentHomeBinding
import com.example.walking_hadang.util.GlucoseRepository
import com.example.walking_hadang.util.UserProfileRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var customToolbarView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        childFragmentManager.beginTransaction()
            .replace(binding.courseFragmentCatainer.id, CourseListFragment())
            .commit()
        setupToolbarContent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeToolbarContent()
        _binding = null
    }
    private fun setupToolbarContent() {
        val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.toolbar)
        // 기본 타이틀/인셋 제거 (필요시)
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.menu.clear()
        toolbar.contentInsetStartWithNavigation = 0
        toolbar.setContentInsetsAbsolute(0, 0)

        // 이미 추가된 커스텀 뷰가 있으면 제거
        removeToolbarContent()

        // 홈 전용 컨텐츠 삽입
        val v = layoutInflater.inflate(R.layout.toolbar_home_content, toolbar, false)
        val lp = Toolbar.LayoutParams(
            Toolbar.LayoutParams.MATCH_PARENT,
            Toolbar.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.START or Gravity.CENTER_VERTICAL }
        toolbar.addView(v, lp)
        customToolbarView = v

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 최초 1회 최신 데이터 로드
                UserProfileRepository.warmUpIfEmpty()
                // 실시간 반영이 필요하면 켜고, onStop에서 끄세요.
                UserProfileRepository.startListening()

                UserProfileRepository.nicknameFlow.collect { nickname ->
                    v.findViewById<TextView>(R.id.tvGreeting).text = "${nickname}님\n안녕하세요!"
                }
            }
        }
        lifecycleScope.launch {
            val latest = GlucoseRepository.getTodayLatest()
            if (latest != null) {
                binding.tvGlucoseValue.text = latest.value.toString()
                binding.tvGlucoseUnit.text = "mg/dL"
                Log.d("Glucose", "오늘 가장 최근 혈당 = ${latest.value}")
            } else {
                binding.tvGlucoseValue.text = "오늘 혈당 기록 없음"
                binding.tvGlucoseUnit.text = ""
                Log.d("Glucose", "오늘 혈당 기록 없음")
            }
        }
        // 필요시 데이터 바인딩

        v.findViewById<TextView>(R.id.tvDate).text = getFormattedKoreanDate()
        v.findViewById<ShapeableImageView>(R.id.btnProfile).setOnClickListener {
            // TODO: 프로필 클릭 처리
        }
    }

    private fun removeToolbarContent() {
        val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.toolbar)
        customToolbarView?.let { toolbar.removeView(it) }
        customToolbarView = null
    }
    fun getFormattedKoreanDate(): String {
        val today = LocalDate.now()
        val month = today.monthValue
        val day = today.dayOfMonth
        val dayOfWeek = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)

        return "${month}월 ${day}일 ${dayOfWeek}"
    }

}