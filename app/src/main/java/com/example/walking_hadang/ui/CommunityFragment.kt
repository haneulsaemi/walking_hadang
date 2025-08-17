package com.example.walking_hadang.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toolbar
import androidx.fragment.app.Fragment
import com.example.walking_hadang.R
import com.example.walking_hadang.databinding.FragmentCommunityBinding

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
        val rv = binding.recyclerViewCommunity
    }
    override fun onPause() {
        super.onPause()
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        toolbar.findViewWithTag<View>("communityTitleView")?.let {        // 태그명 변경해서 사용
            toolbar.removeView(it)
        }
    }

    override fun onResume() {
        super.onResume()
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        val titleView = LayoutInflater.from(context).inflate(R.layout.toolbar_custom, toolbar, false) as TextView
        titleView.text = "커뮤니티"    // 원하는 타이틀로 변경
        titleView.apply {
            tag = "communityTitleView"    // 태그명 변경해서 사용
        }
        toolbar.addView(titleView)
    }

}