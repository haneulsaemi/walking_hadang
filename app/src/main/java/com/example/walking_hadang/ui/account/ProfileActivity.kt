package com.example.walking_hadang.ui.account

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.walking_hadang.MyApplication
import com.example.walking_hadang.R
import com.example.walking_hadang.data.UserProfileData
import com.example.walking_hadang.databinding.ActivityProfileBinding
import com.example.walking_hadang.ui.AlarmSettingsDialog
import com.example.walking_hadang.ui.LoginActivity
import com.example.walking_hadang.util.UserProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {
    private val editFieldLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == Activity.RESULT_OK && res.data != null) {
                val key = res.data!!.getStringExtra("key") ?: return@registerForActivityResult
                val newValue = res.data!!.getStringExtra("value") ?: return@registerForActivityResult
                // key로 해당 TextView 업데이트
                when (key) {
                    "nickname" -> {
                        binding.tvNicknameValue.text = newValue
                        binding.tvName.text = newValue
                        CoroutineScope(Dispatchers.IO).launch {
                            UserProfileRepository.updateNickname(newValue)
                        }
                    }
                    "name"     -> {
                        binding.tvNameValue.text = newValue
                        CoroutineScope(Dispatchers.IO).launch {
                            UserProfileRepository.updateUserName(newValue)
                        }
                    }
                    "height"   -> {
                        binding.tvHeightValue.text = newValue
                        CoroutineScope(Dispatchers.IO).launch {
                            val height = newValue.toIntOrNull()
                            if (height != null) UserProfileRepository.updateHeight(height)
                        }

                    }
                    "weight"   -> {
                        binding.tvWeightValue.text = newValue
                        CoroutineScope(Dispatchers.IO).launch {
                            val weight = newValue.toIntOrNull()
                            if (weight != null) UserProfileRepository.updateWeight(weight)
                        }
                    }
                    "gender"   -> {
                        binding.tvGenderValue.text = newValue
                        CoroutineScope(Dispatchers.IO).launch {
                            UserProfileRepository.updateGender(newValue)
                        }
                    }
                    "region"   -> {
                        binding.tvRegionValue.text = newValue
                        binding.tvRegion.text = newValue
                        CoroutineScope(Dispatchers.IO).launch {
                            UserProfileRepository.updateRegion(newValue)
                        }
                    }
                }
                // TODO: Firestore에 저장도 여기서 같이 호출
            }
        }
    lateinit var binding : ActivityProfileBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        lifecycleScope.launch {
            val profile = UserProfileRepository.loadUserProfileOnce()
            if (profile == null) {
                Log.e("ProfileActivity", "프로필 로딩 실패 또는 로그인 안됨")
                return@launch
            }
            binding.tvName.text = profile.nickname
            binding.tvNicknameValue.text = profile.nickname
            binding.tvNameValue.text = profile.userName
            binding.tvHeightValue.text = profile.heightCm?.toString() ?: ""
            binding.tvWeightValue.text = profile.weightKg?.toString() ?: ""
            binding.tvGenderValue.text = profile.gender
            binding.tvRegionValue.text = profile.region
        }

        binding.topBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.topBar.setOnMenuItemClickListener { menuItem->
            when (menuItem.itemId) {
                R.id.action_notification ->{
                    Log.d("ProfileMenuClick", "알림 메뉴 클릭")
                    AlarmSettingsDialog().show(supportFragmentManager, "alarm")
                    true
                }
                else -> false
            }
        }

        binding.rowNickname.setOnClickListener {
            val current = binding.tvNicknameValue.text.toString()
            openEditField(key = "nickname", label = "닉네임", value = current, inputType = InputType.TYPE_CLASS_TEXT)
        }
        binding.rowName.setOnClickListener {
            val current = binding.tvNameValue.text.toString()
            openEditField("name", "이름", current, InputType.TYPE_CLASS_TEXT)
        }
        binding.rowHeight.setOnClickListener {
            val current = binding.tvHeightValue.text.toString()
            openEditField("height", "키", current, InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        }
        binding.rowWeight.setOnClickListener {
            val current = binding.tvWeightValue.text.toString()
            openEditField("weight", "체중", current, InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        }
        binding.rowGender.setOnClickListener {
            val current = binding.tvGenderValue.text.toString()
            openEditField("gender", "성별", current, InputType.TYPE_CLASS_TEXT)
        }
        binding.rowRegion.setOnClickListener {
            val current = binding.tvRegionValue.text.toString()
            openEditField("region", "거주지역", current, InputType.TYPE_CLASS_TEXT)
        }

        binding.btnLogout.setOnClickListener {
            MyApplication.auth.signOut()
            Toast.makeText(this, "로그아웃 되었습니다", Toast.LENGTH_SHORT).show()
            if (!MyApplication.checkAuth()) {
                startActivity(
                    Intent(this, LoginActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                )
                finish()
            }
        }
    }

    private fun openEditField(key: String, label: String, value: String, inputType: Int) {
        val intent = Intent(this, EditFieldActivity::class.java).apply {
            putExtra("key", key)
            putExtra("label", label)
            putExtra("value", value)
            putExtra("inputType", inputType)
        }
        editFieldLauncher.launch(intent)
    }
}