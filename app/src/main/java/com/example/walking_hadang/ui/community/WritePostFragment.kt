package com.example.walking_hadang.ui.community

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.walking_hadang.databinding.FragmentWritePostBinding
import com.google.android.material.snackbar.Snackbar

class WritePostFragment : Fragment() {

    companion object {
        const val RESULT_KEY = "write_post_result"
        const val BUNDLE_TITLE = "title"
        const val BUNDLE_CONTENT = "content"
        const val BUNDLE_ANON = "anonymous"
        const val BUNDLE_IMAGE_URI = "image_uri"
    }

    private var _binding: FragmentWritePostBinding? = null
    private val binding get() = _binding!!

    private var selectedImageUri: Uri? = null

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
        if (uri != null) {
            binding.ivPreview.visibility = View.VISIBLE
            binding.ivPreview.setImageURI(uri)
        } else {
            binding.ivPreview.visibility = View.GONE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWritePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 닫기(X)
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 상단바 오른쪽 '완료' 텍스트 버튼
        binding.toolbar.findViewById<TextView>(com.example.walking_hadang.R.id.tvActionDone)
            .setOnClickListener { submit() }

        // 이미지 첨부
        binding.btnAddImage.setOnClickListener {
            pickImage.launch("image/*")
        }
    }

    private fun submit() {
        val title = binding.etTitle.text?.toString()?.trim().orEmpty()
        val content = binding.etContent.text?.toString()?.trim().orEmpty()
        val anon = binding.cbAnonymous.isChecked

        var hasError = false
        if (title.isBlank()) { binding.tiTitle.error = "제목을 입력해주세요"; hasError = true } else binding.tiTitle.error = null
        if (content.isBlank()) { binding.tiContent.error = "내용을 입력해주세요"; hasError = true } else binding.tiContent.error = null

        if (hasError) {
            Snackbar.make(requireView(), "입력값을 확인해주세요.", Snackbar.LENGTH_SHORT).show()
            return
        }
//테스트
        // 결과 전달
        val bundle = Bundle().apply {
            putString(BUNDLE_TITLE, title)
            putString(BUNDLE_CONTENT, content)
            putBoolean(BUNDLE_ANON, anon)
            putString(BUNDLE_IMAGE_URI, selectedImageUri?.toString())
        }
        parentFragmentManager.setFragmentResult(RESULT_KEY, bundle)
        parentFragmentManager.popBackStack()
    }
}
