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
import com.example.walking_hadang.databinding.FragmentFreeBoardBinding

class FragmentFreeBoard : Fragment() {

    private var _binding: FragmentFreeBoardBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: SugarPostAdapter

    private val boardsVm: CommunityBoardsViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFreeBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SugarPostAdapter { post ->
            context?.let { Toast.makeText(it, "클릭: ${post.title}", Toast.LENGTH_SHORT).show() }
        }
        binding.rvBoardPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBoardPosts.adapter = adapter

        boardsVm.free.observe(viewLifecycleOwner) { list -> adapter.submitList(list) }
        boardsVm.initFreeIfEmpty { makeDummy() }

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
            boardsVm.addFree(post)
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
            CommunityPost("fr1","러닝 입문러인데 추천 러닝화 있을까요","가성비/가심비 뭐든 좋아요!",4,2, now-10*60*60*1000L, null),
            CommunityPost("fr2","동네 병원 추천 부탁","혈압/혈당 내과 잘 보는 곳?",3,1, now-30*60*1000L, null),
            CommunityPost("fr3","비오는 날 산책 사진","비내리는 공원 너무 예쁘네요 ☔",6,3, now-5*60*1000L, null),
        )
    }
}
