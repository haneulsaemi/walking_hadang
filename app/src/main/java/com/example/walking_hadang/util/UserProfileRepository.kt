package com.example.walking_hadang.util

import android.util.Log
import com.example.walking_hadang.MyApplication
import com.example.walking_hadang.data.UserProfileData
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

object UserProfileRepository {

    // --------- Internal ---------
    private fun uid(): String =
        MyApplication.auth.currentUser?.uid ?: throw IllegalStateException("로그인 필요")

    private val mutex = Mutex()
    private var listenerReg: ListenerRegistration? = null

    private val _profile = MutableStateFlow<UserProfileData?>(null)
    val profileFlow: StateFlow<UserProfileData?> = _profile.asStateFlow()

    // UI에서 닉네임만 필요할 때 사용
    val nicknameFlow: Flow<String> = profileFlow.map { it?.nickname.orEmpty() }.distinctUntilChanged()

    // 현재 메모리값(있으면)
    val profileValue: UserProfileData? get() = _profile.value

    // --------- Sign Up / Load / Refresh ---------
    suspend fun signUpAndSaveProfile(
        email: String,
        password: String,
        nickname: String,
        birthDate: String,
        gender: String,
        heightCm: Int?,
        weightKg: Int?
    ): UserProfileData {
        val authRes = MyApplication.auth
            .createUserWithEmailAndPassword(email, password).await()
        val uid = authRes.user?.uid ?: error("UID 생성 실패")

        // (선택) Firebase Auth displayName 동기화
        runCatching {
            val req = userProfileChangeRequest { displayName = nickname }
            authRes.user?.updateProfile(req)?.await()
        }

        val profile = UserProfileData(
            userId = uid,
            email = email,
            nickname = nickname,
            birthDate = birthDate,
            gender = gender,
            heightCm = heightCm,
            weightKg = weightKg
        )
        saveUserProfile(profile)

        val snap = MyApplication.db.collection("users").document(uid).get().await()
        val saved = snap.toObject(UserProfileData::class.java) ?: profile

        _profile.value = saved
        return saved
    }

    suspend fun loadUserProfileOnce(): UserProfileData? = runCatching {
        val snap = MyApplication.db.collection("users").document(uid()).get().await()
        snap.toObject(UserProfileData::class.java)
    }.onFailure { Log.e("UserProfileRepository", "loadUserProfileOnce error", it) }
        .getOrNull()

    /** 네트워크에서 강제 최신화하고 캐시에 반영 */
    suspend fun refresh() {
        val fresh = loadUserProfileOnce()
        mutex.withLock { _profile.value = fresh }
    }

    /** 앱 진입/로그인 직후 한 번 호출해서 메모리 캐시 예열 */
    suspend fun warmUpIfEmpty() {
        if (_profile.value == null) refresh()
    }

    // --------- Real-time listening ---------
    fun startListening() {
        stopListening() // 중복 방지
        val id = runCatching { uid() }.getOrElse { return }
        listenerReg = MyApplication.db.collection("users").document(id)
            .addSnapshotListener { s, e ->
                if (e != null) {
                    Log.w("UserProfileRepository", "snapshot error", e)
                    return@addSnapshotListener
                }
                val p = s?.toObject(UserProfileData::class.java)
                _profile.value = p
            }
    }

    fun stopListening() {
        listenerReg?.remove()
        listenerReg = null
    }

    // --------- Save / Update ---------
    suspend fun saveUserProfile(profile: UserProfileData) {
        val data = hashMapOf(
            "userId" to profile.userId,
            "email" to profile.email,
            "nickname" to profile.nickname,
            "birthDate" to profile.birthDate,
            "gender" to profile.gender,
            "heightCm" to profile.heightCm,
            "weightKg" to profile.weightKg,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            MyApplication.db.collection("users").document(profile.userId)
                .set(data, SetOptions.merge()).await()
            mutex.withLock { _profile.value = profile.copy() } // 캐시 동기화
        } catch (e: Exception) {
            throw IllegalStateException("프로필 저장 실패: ${e.message}", e)
        }
    }

    // 공통 필드 업데이트 + 캐시 반영
    private suspend fun updateField(field: String, value: Any?) {
        val id = uid()
        val update = mutableMapOf<String, Any?>(
            field to value,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        MyApplication.db.collection("users").document(id)
            .set(update, SetOptions.merge()).await()

        // 캐시에도 반영(필드명 스위치)
        mutex.withLock {
            val cur = _profile.value ?: UserProfileData(userId = id, email = "")
            val newProfile = when (field) {
                "nickname" -> cur.copy(nickname = (value as? String).toString())
                "birthDate" -> cur.copy(birthDate = (value as? String).toString())
                "gender" -> cur.copy(gender = (value as? String).toString())
                "heightCm" -> cur.copy(heightCm = value as? Int)
                "weightKg" -> cur.copy(weightKg = value as? Int)
                "region" -> cur.copy(region = (value as? String).toString())
                "email" -> cur.copy(email = (value as? String).toString())
                "userName" -> cur.copy(userName = (value as? String).toString())
                else -> cur
            }
            _profile.value = newProfile
        }
    }

    // 개별 업데이트 API (필요한 것만 노출)
    suspend fun updateNickname(newNickname: String) {
        updateField("nickname", newNickname)
        // (선택) Auth displayName도 동기화
        runCatching {
            val req = userProfileChangeRequest { displayName = newNickname }
            MyApplication.auth.currentUser?.updateProfile(req)?.await()
        }
    }

    suspend fun updateBirthDate(newBirthDate: String) = updateField("birthDate", newBirthDate)
    suspend fun updateGender(newGender: String) = updateField("gender", newGender)
    suspend fun updateHeight(heightCm: Int) = updateField("heightCm", heightCm)
    suspend fun updateWeight(weightKg: Int) = updateField("weightKg", weightKg)
    suspend fun updateRegion(region: String) = updateField("region", region)
    suspend fun updateUserName(newUserName: String) = updateField("userName", newUserName)

    // --------- Auth ---------
    suspend fun resendVerificationEmail() {
        val user = MyApplication.auth.currentUser ?: error("로그인 필요")
        user.sendEmailVerification().await()
    }
}