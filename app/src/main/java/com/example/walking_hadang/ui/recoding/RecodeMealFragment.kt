package com.example.walking_hadang.ui.recoding

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.walking_hadang.MyApplication
import com.example.walking_hadang.adapter.MealAdapter
import com.example.walking_hadang.R
import com.example.walking_hadang.data.MealEntryDoc
import com.example.walking_hadang.data.MealItemLine
import com.example.walking_hadang.databinding.FragmentRecodeMealBinding
import com.example.walking_hadang.util.MealRepository
import com.example.walking_hadang.util.MealRepository.dateKeyFrom
import com.example.walking_hadang.util.RecodingSharedViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.storage.storage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import kotlin.getValue

class RecodeMealFragment : Fragment() {

    private var _binding: FragmentRecodeMealBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: MealAdapter

    private var currentDateKey: String = ""
    private var pendingPhotoUrl: String? = null      // 업로드 완료된 URL 저장
    private var pendingPhotoUri: Uri? = null

    private val sharedVM: RecodingSharedViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRecodeMealBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rv = binding.rvMeals
        rv.layoutManager = LinearLayoutManager(requireContext())

        adapter = MealAdapter(
            onClick = { entry ->
                // TODO: 상세/편집 화면으로 이동 등
            },
            onDelete = { entry ->
                // suspend 함수는 코루틴으로 호출
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        MealRepository.deleteMealEntry(currentDateKey, entry.entryId)
                        Snackbar.make(requireView(), "삭제되었습니다.", Snackbar.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Snackbar.make(
                            requireView(),
                            e.localizedMessage ?: "삭제 실패",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
        rv.adapter = adapter



        // 2) 날짜 변경 수집 → 해당 날짜 데이터만 로드
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // collectLatest: 날짜가 다시 바뀌면 이전 요청은 무시
                sharedVM.selectedDate.collectLatest { date: LocalDate ->
                    val dateKey = dateKeyFrom(date) // 이미 문자열이면 그냥 date 사용
                    currentDateKey = dateKey

                    MealRepository.loadMealsByDate(
                        dateKey = dateKey,
                        onSuccess = { list -> adapter.submitList(list) },
                        onError = { /* TODO: 에러 처리(UI 토스트 등) */ }
                    )
                }
            }
        }
        binding.btnClearPreview.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                // (선택) 이미 업로드된 파일도 지우고 싶다면:
//                pendingPhotoUrl?.let { url ->
//                    runCatching {
//                        // KTX 없이도 동작하는 버전
//                        com.google.firebase.storage.FirebaseStorage
//                            .getInstance()
//                            .getReferenceFromUrl(url)
//                            .delete()
//                            .await()
//                    }
//                }
                // 로컬 상태 초기화
                pendingPhotoUrl = null
                pendingPhotoUri = null
                binding.ivPreview.setImageDrawable(null)
                binding.previewContainer.visibility = View.GONE
                binding.btnAddPhoto.text = "사진"
                com.google.android.material.snackbar.Snackbar
                    .make(requireView(), "사진을 제거했어요.", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                    .show()
            }
        }
        if (pendingPhotoUri != null) {
            binding.ivPreview.visibility = View.VISIBLE
            Glide.with(binding.ivPreview)
                .load(pendingPhotoUri)
                .centerCrop()
                .into(binding.ivPreview)
        }
        binding.btnAddPhoto.setOnClickListener {
            if (currentDateKey.isBlank()) {
                Snackbar.make(requireView(), "날짜를 먼저 선택하세요.", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // 사진 피커 사용 가능 여부 체크

            val req = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

            // 1) 먼저 시도
            runCatching {
                pickPhotoLauncher.launch(req)
            }.onFailure { e ->
                // 2) 일부 기기/에뮬레이터에서 PICK_IMAGES 인텐트 핸들러가 없어 A N F E 발생
                if (e is android.content.ActivityNotFoundException) {
                    // → 안전한 폴백
                    getContentLauncher.launch("image/*")
                } else {
                    Snackbar.make(requireView(), e.localizedMessage ?: "사진 선택 시작 실패", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
        binding.btnAddFood.setOnClickListener {
            val dateKey = currentDateKey
            if (dateKey.isBlank()) {
                Snackbar.make(requireView(), "날짜를 먼저 선택하세요.", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val mealType = getSelectedMealTypeOrNull()
            if (mealType == null) {
                Snackbar.make(requireView(), "끼니를 선택하세요.", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val name = binding.etFoodName.text?.toString()?.trim().orEmpty()
            if (name.isBlank()) {
                binding.tilFoodName.error = "음식 이름을 입력하세요"
                return@setOnClickListener
            } else {
                binding.tilFoodName.error = null
            }

            val kcalText = binding.etKcal.text?.toString()?.trim().orEmpty()
            val kcal = kcalText.toIntOrNull()  // 없거나 숫자 아니면 null

            val items = listOf(
                MealItemLine(
                    name = name,
                    amount = null,     // 원하면 수량 입력칸 추가해서 채우면 됨
                    kcal = kcal
                )
            )

            val entry = MealEntryDoc(
                userId = "",                 // Repo에서 uid 세팅
                entryId = "",                // Repo에서 새 id 부여
                mealType = mealType.toString(),         // "아침"/"점심"/"저녁"/"간식"
                eatenAt = Timestamp.now(),   // 지금 시간
                photoUrl = pendingPhotoUrl,             // 사진 선택 로직 붙이면 채우기
                items = items,
                totalKcal = kcal,            // 여러 줄이면 합산해서 설정
                eatenAll = null,
                note = null,
                orderIndex = 0,
                createdAt = null,
                updatedAt = null
            )

            // 2) 저장 (suspend 이므로 코루틴)
            binding.btnAddFood.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    MealRepository.upsertMealEntry(dateKey, entry)
                    clearInputs() // 입력칸 비우기
                    binding.previewContainer.visibility = View.GONE
                    binding.ivPreview.setImageDrawable(null)
                    binding.btnAddPhoto.text = "사진"

                    // 3) 리스트 재로딩 (스냅샷리스너가 아니므로 한 번 더 가져오기)
                    MealRepository.loadMealsByDate(
                        dateKey = dateKey,
                        onSuccess = { list -> adapter.submitList(list) },
                        onError = { e ->
                            Snackbar.make(
                                requireView(),
                                e.localizedMessage ?: "새로고침 실패",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    )

                    Snackbar.make(requireView(), "추가되었습니다.", Snackbar.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Snackbar.make(
                        requireView(),
                        e.localizedMessage ?: "추가 실패",
                        Snackbar.LENGTH_SHORT
                    ).show()
                } finally {
                    binding.btnAddFood.isEnabled = true
                }
            }
        }
    }
    private fun getSelectedMealTypeOrNull(): String? {
        val id = binding.chipMealGroup.checkedChipId
        if (id == View.NO_ID) return null
        val chip = binding.root.findViewById<Chip>(id)
        return chip?.text?.toString()
    }
    private val pickPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { handleSelectedUri(it) }
    }
    private val getContentLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleSelectedUri(it) }
    }
    private fun handleSelectedUri(uri: Uri){
        if (uri != null) {
            // 날짜가 정해져 있어야 경로(users/uid/meals/{dateKey}/photos/...)에 올림
            binding.ivPreview?.let {
                it.visibility = View.VISIBLE
                Glide.with(it).load(uri).centerCrop().into(it)
            }
            pendingPhotoUri = uri
            binding.btnAddPhoto.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val url = uploadPhotoToStorage(currentDateKey, uri) // ← 기존 함수 그대로 사용
                    pendingPhotoUrl = url
                    binding.btnAddPhoto.text = "사진 변경"
                    Snackbar.make(requireView(), "사진 업로드 완료", Snackbar.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Snackbar.make(requireView(), e.localizedMessage ?: "사진 업로드 실패", Snackbar.LENGTH_SHORT).show()
                } finally {
                    binding.btnAddPhoto.isEnabled = true
                }
            }
        }
    }
    private suspend fun uploadPhotoToStorage(dateKey: String, uri: Uri): String {
        val uid = MyApplication.auth.currentUser?.uid
            ?: throw IllegalStateException("로그인이 필요합니다.")

        // 확장자 추출 (MIME 기준)
        val mime = requireContext().contentResolver.getType(uri) ?: "image/jpeg"
        val ext = when {
            mime.endsWith("png") -> "png"
            mime.endsWith("webp") -> "webp"
            else -> "jpg"
        }

        // 저장 경로 예: users/{uid}/meals/{dateKey}/photos/{millis}.jpg
        val path = "users/$uid/meals/$dateKey/photos/${System.currentTimeMillis()}.$ext"
        val ref = Firebase.storage.reference.child(path)

        // 필요시 metadata 지정 가능:
        // val metadata = storageMetadata { contentType = mime }
        // ref.putFile(uri, metadata).await()

        ref.putFile(uri).await()                 // 업로드
        return ref.downloadUrl.await().toString()// 다운로드 URL
    }
    /** 입력칸 초기화 */
    private fun clearInputs() {
        binding.etFoodName.setText("")
        binding.etKcal.setText("")
        // 끼니 선택 유지하고 싶으면 주석 처리
        // binding.chipMealGroup.clearCheck()
        binding.tilFoodName.error = null
    }
}