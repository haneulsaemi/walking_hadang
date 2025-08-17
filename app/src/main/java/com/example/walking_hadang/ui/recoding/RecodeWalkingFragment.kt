package com.example.walking_hadang.ui.recoding

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.walking_hadang.BuildConfig
import com.example.walking_hadang.MyApplication
import com.example.walking_hadang.R
import com.example.walking_hadang.adapter.WalkAdapter
import com.example.walking_hadang.data.StaticMapConfig
import com.example.walking_hadang.data.WalkData
import com.example.walking_hadang.data.buildStaticMapUrl
import com.example.walking_hadang.databinding.FragmentRecodeWalkingBinding
import com.example.walking_hadang.ui.RecodingFragment
import com.example.walking_hadang.util.RecodingSharedViewModel
import com.example.walking_hadang.util.WalkRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

class RecodeWalkingFragment : Fragment() {



    private var _binding: FragmentRecodeWalkingBinding? = null
    private  val binding get() = _binding!!

    private lateinit var adapter: WalkAdapter
    private val sharedVM: RecodingSharedViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRecodeWalkingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rv = binding.rvWalks

        adapter = WalkAdapter(
            buildStaticMapUrl = { poly, start, end ->
                buildStaticMapUrl(
                    routePolyline = poly,
                    apiKey = BuildConfig.MAPS_API_KEY,
                    start = start,
                    end = end,
                    cfg = StaticMapConfig(width = 600, height = 360, strokeColor = "0x000000ff")
                )
            },
            onClick = { walk ->
                // TODO: 상세 화면 이동
            }
        )
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(requireContext())

        // 날짜 변경 구독 → 해당 날짜만 불러오기
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedVM.selectedDate.collect { date ->
                    // 로딩 UI가 있다면 여기서 표시
                    WalkRepository.loadWalksByDate(
                        dateKey = date.toString(),
                        limit = 20,
                        descending = true,
                        onSuccess = { list ->
                            adapter.submitList(list)
                            binding.emptyText.isVisible = list.isEmpty()
                            rv.isVisible = list.isNotEmpty()
                        },
                        onError = { e ->
                            binding.emptyText.isVisible = true
                            rv.isVisible = false
                            binding.emptyText.text = "데이터를 불러오지 못했어요.\n${e.localizedMessage}"
                            Log.e("Walk", "loadWalksByDate error", e)
                        }
                    )
                }
            }
        }
        WalkRepository.loadLatestWalks(
            pageSize = 20,
            onSuccess = { list ->
                Log.d("Walk", "loaded count = ${list.size}")
                adapter.submitList(list)
                        },
            onError = { e ->
                Toast.makeText(requireContext(), e.localizedMessage, Toast.LENGTH_SHORT).show()
                Log.d("Walk", e.localizedMessage)
            }
        )

        binding.btnAdd.setOnClickListener {
            Log.d("WalksDebug", "버튼 클릭됨")
            val distanceText = binding.etDistance.text.toString()
            val durationText = binding.etDuration.text.toString()
            Log.d("WalksDebug", "입력된 거리 문자열: '$distanceText'")
            Log.d("WalksDebug", "입력된 시간 문자열: '$durationText'")
            if (distanceText.isBlank() || durationText.isBlank()) {
                Toast.makeText(requireContext(), "산책 거리와 시간을 모두 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val distanceM = distanceText.toFloatOrNull()?.times(1000)?.toInt() ?: 0  // Km → m
            val durationSec = durationText.toIntOrNull()?.times(60) ?: 0             // min → sec
            val walkData = WalkData(
                distanceM = distanceM,
                durationSec = durationSec,
                startedAt = Timestamp.now()
            )

            WalkRepository.addWalkEntry(
                raw = walkData,
                onSuccess = { id ->
                    Toast.makeText(requireContext(), "산책 기록 추가 완료!", Toast.LENGTH_SHORT).show()
                    Log.d("WalksDebug", "저장된 문서 ID: $id")
                    // RecyclerView 리로드
                    WalkRepository.loadLatestWalks(
                        pageSize = 20,
                        onSuccess = { list -> adapter.submitList(list) },
                        onError = { e -> Log.e("WalksDebug", "리로드 실패", e) }
                    )

                },
                onError = { e ->
                    Toast.makeText(requireContext(), "저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("WalksDebug", "저장 오류", e)
                }
            )


        }
    }
}