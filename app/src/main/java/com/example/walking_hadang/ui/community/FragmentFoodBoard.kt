package com.example.walking_hadang.ui.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.walking_hadang.R
import com.example.walking_hadang.databinding.FragmentFoodBoardBinding

class FragmentFoodBoard : Fragment() {
    //테스트
    private var _binding: FragmentFoodBoardBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: SugarPostAdapter

    private val boardsVm: CommunityBoardsViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFoodBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SugarPostAdapter { post ->
            context?.let { Toast.makeText(it, "클릭: ${post.title}", Toast.LENGTH_SHORT).show() }
        }
        binding.rvBoardPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBoardPosts.adapter = adapter

        boardsVm.food.observe(viewLifecycleOwner) { list -> adapter.submitList(list) }
        boardsVm.initFoodIfEmpty { makeDummy() }

        parentFragmentManager.setFragmentResultListener(WritePostFragment.RESULT_KEY, viewLifecycleOwner) { _, result ->
            val title = result.getString(WritePostFragment.BUNDLE_TITLE).orEmpty()
            val content = result.getString(WritePostFragment.BUNDLE_CONTENT).orEmpty()
            val anonymous = result.getBoolean(WritePostFragment.BUNDLE_ANON, false)
            val post = CommunityPost(
                id = System.currentTimeMillis().toString(),
                title = if (anonymous) "[익명] $title" else title,
                preview = content, likes = 0, comments = 0,
                createdAt = System.currentTimeMillis(), badge = null
            )
            boardsVm.addFood(post)
            binding.rvBoardPosts.post { binding.rvBoardPosts.scrollToPosition(0) }
            ensureFabVisible()
        }

        binding.fabAddPost.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.communityContainer, WritePostFragment())
                .addToBackStack(null)
                .commit()
        }

        ensureFabVisible()
        parentFragmentManager.addOnBackStackChangedListener(backstackListener)
    }

    override fun onResume() {
        super.onResume(); ensureFabVisible()
    }

    override fun onDestroyView() {
        parentFragmentManager.removeOnBackStackChangedListener(backstackListener)
        _binding = null
        super.onDestroyView()
    }

    private fun ensureFabVisible() {
        binding.fabAddPost.show()
        binding.fabAddPost.visibility = View.VISIBLE
        binding.fabAddPost.isEnabled = true
        binding.fabAddPost.alpha = 1f
        binding.fabAddPost.scaleX = 1f
        binding.fabAddPost.scaleY = 1f
        binding.fabAddPost.bringToFront()
    }

    private val backstackListener = FragmentManager.OnBackStackChangedListener {
        if (isVisible && view != null) ensureFabVisible()
    }

    private fun makeDummy(): List<CommunityPost> {
        val now = System.currentTimeMillis()
        return listOf(
            CommunityPost("fd1","식단 정기 구독 추천 부탁해요!","기존 브랜드 외 다른 곳 궁금합니다.",11,5, now-6*60*60*1000L, null),
            CommunityPost("fd2","저당 요거트 찾았어요","단맛 괜찮고 성분도 깔끔합니다.",8,3, now-45*60*1000L, null),
            CommunityPost("fd3","샐러드 드레싱 레시피","올리브유+레몬즙+머스타드 OK",5,2, now-12*60*1000L, null),
        )
    }
}

