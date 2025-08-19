package com.example.walking_hadang.ui.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.walking_hadang.databinding.FragmentHomeHotBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeHotFragment : Fragment() {

    private var _binding: FragmentHomeHotBinding? = null
    private val binding get() = _binding!!
    //테스트
    private val db by lazy { FirebaseFirestore.getInstance() }
    private lateinit var adapter: HomeHotAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeHotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = HomeHotAdapter { post ->
            context?.let {
                Toast.makeText(it, "클릭: ${post.title}", Toast.LENGTH_SHORT).show()
            }
        }
        binding.rvHot.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHot.adapter = adapter

        loadHotPosts()
    }

    private fun loadHotPosts() {
        // 인덱스 없이 돌리기 위해 각 보드에서 최신글 넉넉히 가져온 다음 메모리 정렬
        val tasks = listOf(
            fetchLatestFrom("sugar_posts",    badge = "혈당공유게시판"),
            fetchLatestFrom("exercise_posts", badge = "운동공유게시판"),
            fetchLatestFrom("walk_posts",     badge = "산책게시판"),
            fetchLatestFrom("food_posts",     badge = "식품공유게시판"),
            fetchLatestFrom("free_posts",     badge = "자유게시판")
        )

        val result = mutableListOf<CommunityPost>()
        var remaining = tasks.size
        var errorShown = false

        fun tryFinish() {
            remaining--
            if (remaining == 0) {
                // ★ 메모리에서 ‘핫’ 정렬: likes desc -> comments desc -> createdAt desc
                result.sortWith(
                    compareByDescending<CommunityPost> { it.likes }
                        .thenByDescending { it.comments }
                        .thenByDescending { it.createdAt }
                )
                if (!isAdded || _binding == null) return
                adapter.submitList(result)
            }
        }

        tasks.forEach { task ->
            task(
                { posts ->
                    result.addAll(posts)
                    tryFinish()
                },
                { e ->
                    if (!errorShown) {
                        context?.let {
                            Toast.makeText(it, "HOT 불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        errorShown = true
                    }
                    tryFinish()
                }
            )
        }
    }

    /**
     * 인덱스 없이 안정적으로 동작하도록 createdAt 단일 정렬로 최근 글만 가져온다.
     * limit은 상황에 맞게 조정(예: 30~80). 여기선 50.
     */
    private fun fetchLatestFrom(
        collection: String,
        badge: String,
        limit: Long = 50
    ): ((onSuccess: (List<CommunityPost>) -> Unit, onError: (Throwable) -> Unit) -> Unit) {
        return { onSuccess, onError ->
            db.collection(collection)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener { qs ->
                    // 프래그먼트 분리 이후 콜백이 와도 안전하게 가드
                    if (!isAdded) {
                        onSuccess(emptyList())
                        return@addOnSuccessListener
                    }
                    val list = qs.documents.map { doc ->
                        val created = doc.getLong("createdAt")
                            ?: doc.getTimestamp("createdAt")?.toDate()?.time
                            ?: 0L
                        CommunityPost(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            preview = doc.getString("preview") ?: "",
                            likes = (doc.getLong("likes") ?: 0L).toInt(),
                            comments = (doc.getLong("comments") ?: 0L).toInt(),
                            createdAt = created,
                            badge = badge
                        )
                    }
                    onSuccess(list)
                }
                .addOnFailureListener { e ->
                    onError(e)
                }
        }
    }
}
