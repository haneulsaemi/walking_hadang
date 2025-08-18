package com.example.walking_hadang.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.example.walking_hadang.R
import com.example.walking_hadang.databinding.FragmentCommunityBinding
import com.example.walking_hadang.ui.community.FragmentSugarBoard
import com.example.walking_hadang.ui.community.HomeHotFragment
import com.example.walking_hadang.ui.community.FragmentExerciseBoard
import com.example.walking_hadang.ui.community.FragmentWalkBoard
import com.example.walking_hadang.ui.community.FragmentFoodBoard
import com.example.walking_hadang.ui.community.FragmentFreeBoard

class CommunityFragment : Fragment() {
    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommunityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 메뉴(툴바) 설정 그대로 유지
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.menu_community_toolbar, menu)
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        // 버튼 클릭 리스너
        with(binding) {
            btnHome.setOnClickListener {
                select(btnHome.text.toString())
                childFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.communityContainer, HomeHotFragment())
                    .commit()
            }
            btnBlood.setOnClickListener {
                select(btnBlood.text.toString())
                childFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.communityContainer, FragmentSugarBoard())
                    .commit()
            }
            btnExercise.setOnClickListener {
                select(btnExercise.text.toString())
                childFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.communityContainer, FragmentExerciseBoard())
                    .commit()
            }
            btnWalk.setOnClickListener {
                select(btnWalk.text.toString())
                childFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.communityContainer, FragmentWalkBoard())
                    .commit()
            }
            btnFood.setOnClickListener {
                select(btnFood.text.toString())
                childFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.communityContainer, FragmentFoodBoard())
                    .commit()
            }
            btnFree.setOnClickListener {
                select(btnFree.text.toString())
                childFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.communityContainer, FragmentFreeBoard())
                    .commit()
            }
        }

// 기본 진입은 홈(HOT)
        binding.btnHome.performClick()

    }

    /** 상단 제목과 버튼 선택 상태 갱신 */
    private fun select(titleText: String) {
        binding.boardTitle.text = when {
            titleText.contains("홈") -> " \uD83D\uDD25 HOT 게시판"
            titleText.contains("혈당") -> "\uD83E\uDE78 혈당 공유 게시판"
            titleText.contains("운동") -> "\uD83C\uDFCB 운동 공유 게시판"
            titleText.contains("산책") -> "\uD83D\uDEB6 산책 게시판"
            titleText.contains("식품") -> "\uD83E\uDD57 식품 공유 게시판"
            titleText.contains("자유") -> "\uD83D\uDE4B 자유게시판"
            else -> titleText
        }
        // 버튼 selected 상태 갱신 (색상 셀렉터 사용 중이어야 함)
        val all = listOf(
            binding.btnHome, binding.btnBlood, binding.btnExercise,
            binding.btnWalk, binding.btnFood, binding.btnFree
        )
        all.forEach { it.isSelected = false }
        all.firstOrNull { it.text.toString() in titleText }?.isSelected = true
    }

    override fun onPause() {
        super.onPause()
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        toolbar?.findViewWithTag<View>("communityTitleView")?.let {
            toolbar.removeView(it)
        }
    }

    override fun onResume() {
        super.onResume()
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar) ?: return
        val titleView = LayoutInflater.from(context)
            .inflate(R.layout.toolbar_custom, toolbar, false) as TextView
        titleView.text = "커뮤니티"
        titleView.tag = "communityTitleView"
        toolbar.addView(titleView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
