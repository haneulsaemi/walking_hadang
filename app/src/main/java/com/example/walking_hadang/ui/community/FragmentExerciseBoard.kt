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
import com.example.walking_hadang.databinding.FragmentExerciseBoardBinding

class FragmentExerciseBoard : Fragment() {

    private var _binding: FragmentExerciseBoardBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: SugarPostAdapter

    private val boardsVm: CommunityBoardsViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExerciseBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SugarPostAdapter { post ->
            context?.let { Toast.makeText(it, "클릭: ${post.title}", Toast.LENGTH_SHORT).show() }
        }
        binding.rvBoardPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBoardPosts.adapter = adapter

        boardsVm.exercise.observe(viewLifecycleOwner) { list -> adapter.submitList(list) }
        boardsVm.initExerciseIfEmpty { makeDummy() }

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
            boardsVm.addExercise(post)
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
            CommunityPost("ex1","운동 같이 할 사람","주 2회 같이 걸을 분 구해요.",5,3, now-2*60*60*1000L, null),
            CommunityPost("ex2","런닝화 추천 부탁!","발볼 넓은 제품 찾는 중입니다.",7,4, now-40*60*1000L, null),
            CommunityPost("ex3","홈트 루틴 공유","전신 20분 루틴 공유드려요.",10,6, now-10*60*1000L, null),
        )
    }
}

