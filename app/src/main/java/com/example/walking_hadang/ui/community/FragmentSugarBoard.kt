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
import com.example.walking_hadang.databinding.FragmentSugarBoardBinding

class FragmentSugarBoard : Fragment() {

    private var _binding: FragmentSugarBoardBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: SugarPostAdapter

    // CommunityFragment 스코프의 공유 ViewModel (탭 전환해도 데이터 유지)
    private val boardsVm: CommunityBoardsViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSugarBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SugarPostAdapter { post ->
            context?.let { Toast.makeText(it, "클릭: ${post.title}", Toast.LENGTH_SHORT).show() }
        }
        binding.rvSugarPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSugarPosts.adapter = adapter

        // ViewModel 관찰 (복귀 시에도 리스트 유지)
        boardsVm.sugar.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }
        boardsVm.initSugarIfEmpty { makeDummy() }

        // 글쓰기 결과 수신 → ViewModel에 추가
        parentFragmentManager.setFragmentResultListener(
            WritePostFragment.RESULT_KEY, viewLifecycleOwner
        ) { _, result ->
            val title = result.getString(WritePostFragment.BUNDLE_TITLE).orEmpty()
            val content = result.getString(WritePostFragment.BUNDLE_CONTENT).orEmpty()
            val anonymous = result.getBoolean(WritePostFragment.BUNDLE_ANON, false)
            val displayTitle = if (anonymous) "[익명] $title" else title

            val newPost = CommunityPost(
                id = System.currentTimeMillis().toString(),
                title = displayTitle,
                preview = content,
                likes = 0,
                comments = 0,
                createdAt = System.currentTimeMillis(),
                badge = null
            )
            boardsVm.addSugar(newPost)
            binding.rvSugarPosts.post { binding.rvSugarPosts.scrollToPosition(0) }
            ensureFabVisible()
        }

        // + 버튼 → 글쓰기 화면
        binding.fabAddPost.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.communityContainer, WritePostFragment())
                .addToBackStack(null)
                .commit()
        }

        // 항상 보이기 보정
        ensureFabVisible()
        parentFragmentManager.addOnBackStackChangedListener(backstackListener)
    }
    //테스트
    override fun onResume() {
        super.onResume()
        ensureFabVisible()
    }

    override fun onDestroyView() {
        parentFragmentManager.removeOnBackStackChangedListener(backstackListener)
        _binding = null
        super.onDestroyView()
    }

    // ─ helpers ─
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
            CommunityPost(
                id = "1",
                title = "고혈압 관리 어떻게들 하시나요ㅠㅠ??",
                preview = "저녁에 운동도 꾸준히 하는데 너무 힘드네요... 팁 부탁드립니다!",
                likes = 8, comments = 5, createdAt = now - 60 * 60 * 1000L, // 1시간 전
                badge = null
            ),
            CommunityPost(
                id = "2",
                title = "혈당 측정 기기 다른 분들 어디서 구매하셨는지요?",
                preview = "온라인/오프라인 추천 부탁드려요. 사용해보신 분들 장단점도!",
                likes = 12, comments = 6, createdAt = now - 15 * 60 * 1000L, // 15분 전
                badge = null
            ),
            CommunityPost(
                id = "3",
                title = "공복혈당 98 찍었어요~",
                preview = "최근 식단 바꿨더니 효과가 있네요. 레시피 공유할게요!",
                likes = 16, comments = 9, createdAt = now - 3 * 60 * 60 * 1000L, // 3시간 전
                badge = null
            )
        )
    }
}
