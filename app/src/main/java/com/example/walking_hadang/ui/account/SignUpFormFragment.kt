package com.example.walking_hadang.ui.account

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.walking_hadang.MyApplication
import com.example.walking_hadang.R
import com.example.walking_hadang.databinding.FragmentSignUpFormBinding
import com.example.walking_hadang.databinding.FragmentTermsAgreementBinding
import com.example.walking_hadang.ui.MainActivity
import com.example.walking_hadang.util.UserProfileRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class SignUpFormFragment : Fragment() {
    private var _binding: FragmentSignUpFormBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSignUpFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.btnNext.setOnClickListener {
            signUp()
        }
    }
    private fun signUp() {
        val email = binding.editTextId.text.toString().trim()
        val password = binding.editTextPass.text.toString().trim()  // 비밀번호 입력칸이 있으면 거기서 가져오기
        val nickname = binding.editTextNick.text.toString().trim()
        val birthDate = binding.editTextBirth.text.toString().trim()
        val gender = when (binding.btnGender.checkedRadioButtonId) {
            R.id.radioFemale -> "F"
            R.id.radioMale -> "M"
            else -> ""
        }
        val height = binding.userHeight.text.toString().toIntOrNull()
        val weight = binding.userWeight.text.toString().toIntOrNull()

        // 간단한 유효성 체크
        if (email.isBlank() || password.isBlank() || nickname.isBlank()) {
            Toast.makeText(requireContext(), "필수 항목을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val profile = UserProfileRepository.signUpAndSaveProfile(
                    email = email,
                    password = password,
                    nickname = nickname,
                    birthDate = birthDate,
                    gender = gender,
                    heightCm = height,
                    weightKg = weight
                )
                Toast.makeText(
                    requireContext(),
                    "회원가입 완료: ${profile.nickname}",
                    Toast.LENGTH_SHORT
                ).show()
                // TODO: 다음 화면으로 이동
                parentFragmentManager.popBackStack()

            } catch (e: Exception) {
                // 인증 실패 또는 네트워크 등
                Toast.makeText(
                    requireContext(),
                    "회원가입 실패: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

}