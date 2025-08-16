package com.example.walking_hadang.ui.account

import android.app.AlertDialog
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
import com.example.walking_hadang.databinding.FragmentSignInBinding
import com.example.walking_hadang.databinding.FragmentTermsAgreementBinding
import com.example.walking_hadang.ui.MainActivity
import com.example.walking_hadang.util.UserProfileRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class SignInFragment : Fragment() {
    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    private val requestLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            MyApplication.auth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful && MyApplication.checkAuth()) {
                        MyApplication.email = account.email
                        startActivity(Intent(requireContext(), MainActivity::class.java))
                        requireActivity().finish()
                    } else {
                        Toast.makeText(context, "구글 로그인 실패", Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (e: ApiException) {
            Toast.makeText(context, "구글 로그인 에러", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            requireActivity().finish()
        }
        //구글 로그인
        binding.icGoogle.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("3728742656-bolckl5h3ifi1dknlu308unecpe2jgpt.apps.googleusercontent.com")
                .requestEmail()
                .build()

            val signInIntent = GoogleSignIn.getClient(requireActivity(), gso).signInIntent
            requestLauncher.launch(signInIntent)
        }

        // 이메일 계정으로 로그인
        binding.btnLogin.setOnClickListener {
            val email = binding.editUsername.text.toString().trim()
            val password = binding.editPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "이메일과 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            MyApplication.auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task ->
                    val user = MyApplication.auth.currentUser
                    if (task.isSuccessful) {
                        startActivity(Intent(requireContext(), MainActivity::class.java))
                        requireActivity().finish()
                    } else {
                        Toast.makeText(requireContext(), "로그인 실패: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // 회원가입으로 이동
        binding.btnSignIn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.example.walking_hadang.R.id.fragment_container_view, SignUpFormFragment())
                .addToBackStack(null)
                .commit()
        }


    }
    private fun moveToMain() {
        val intent = Intent(requireActivity(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()  // LoginActivity 종료
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}