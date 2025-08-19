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
import com.example.walking_hadang.databinding.FragmentWalkBoardBinding

class FragmentWalkBoard : Fragment() {

    private var _binding: FragmentWalkBoardBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: SugarPostAdapter

    private val boardsVm: CommunityBoardsViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWalkBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SugarPostAdapter { post ->
            context?.let { Toast.makeText(it, "클릭: ${post.title}", Toast.LENGTH_SHORT).show() }
        }
        binding.rvBoardPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBoardPosts.adapter = adapter

        boardsVm.walk.observe(viewLifecycleOwner) { list -> adapter.submitList(list) }
        boardsVm.initWalkIfEmpty { makeDummy() }

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
            boardsVm.addWalk(post)
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
            CommunityPost("wk1","어린이대공원 산책 코스 괜찮네요!","한강보다 한적하고 벤치도 많아요.",9,4, now-90*60*1000L, null),
            CommunityPost("wk2","밤산책 동행 구해요","평일 저녁 9시쯤 30분 코스 같이 걸을 분~",6,2, now-20*60*1000L, null),
            CommunityPost("wk3","계단 많은 코스 추천?","언덕/계단 많은 코스 찾습니다.",3,1, now-5*60*1000L, null),
        )
    }
}

