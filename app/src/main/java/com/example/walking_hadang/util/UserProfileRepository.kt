package com.example.walking_hadang.util

import android.util.Log
import com.example.walking_hadang.MyApplication
import com.example.walking_hadang.data.UserProfileData
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object UserProfileRepository {
    private fun uid(): String = MyApplication.auth.currentUser?.uid
        ?: throw IllegalStateException("로그인 필요")

    suspend fun signUpAndSaveProfile(
        email: String,
        password: String,
        nickname: String,
        birthDate: String,    // "1999-03-15"
        gender: String,       // "F" or "M"
        heightCm: Int?,
        weightKg: Int?
    ): UserProfileData {
        val authRes = MyApplication.auth
            .createUserWithEmailAndPassword(email, password)
            .await()
        val uid = authRes.user?.uid ?: error("UID 생성 실패")

        // (선택) displayName 설정
        try {
            val req = userProfileChangeRequest { displayName = nickname }
            authRes.user?.updateProfile(req)?.await()
        } catch (_: Exception) { }

        // 프로필 객체 생성
        val profile = UserProfileData(
            userId = uid,
            email = email,
            nickname = nickname,
            birthDate = birthDate,
            gender = gender,
            heightCm = heightCm,
            weightKg = weightKg
        )

        // Firestore에 저장
        saveUserProfile(profile)

        // 서버 타임스탬프 반영된 문서 읽기
        val snap = MyApplication.db.collection("users").document(uid).get().await()
        return snap.toObject(UserProfileData::class.java) ?: profile

    }
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
            MyApplication.db.collection("users").document(uid())
                .set(data, SetOptions.merge()) // merge로 기존 필드 유지
                .await()
        } catch (e: Exception) {
            throw IllegalStateException("프로필 저장 실패: ${e.message}", e)
        }
    }

    suspend fun loadUserProfile(): UserProfileData? {
        val uid = uid()
        if(uid == null){
            Log.e("loadUserProfile", "User is not logged in.")
            return null
        }
        return try {
            val snap = MyApplication.db.collection("users").document(uid).get().await()
            snap.toObject(UserProfileData::class.java)
        } catch (e: Exception) {
            Log.e("loadUserProfile", "Error loading profile", e)
            null
        }


    }
    suspend fun updateUserName(newUserName: String) {
        val uid = MyApplication.auth.currentUser?.uid ?: return
        MyApplication.db.collection("users").document(uid)
            .update(
                mapOf(
                    "userName" to newUserName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
    }

    suspend fun updateNickname(newNickname: String) {
        val uid = MyApplication.auth.currentUser?.uid ?: return
        MyApplication.db.collection("users").document(uid)
            .update(
                mapOf(
                    "nickname" to newNickname,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
    }

    suspend fun updateBirthDate(newBirthDate: String) {
        val uid = MyApplication.auth.currentUser?.uid ?: return
        MyApplication.db.collection("users").document(uid)
            .update(
                mapOf(
                    "birthDate" to newBirthDate,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
    }

    suspend fun updateGender(newGender: String) {
        val uid = MyApplication.auth.currentUser?.uid ?: return
        MyApplication.db.collection("users").document(uid)
            .update(
                mapOf(
                    "gender" to newGender,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
    }

    suspend fun updateHeight(heightCm: Int) {
        val uid = MyApplication.auth.currentUser?.uid ?: return
        MyApplication.db.collection("users").document(uid)
            .update(
                mapOf(
                    "heightCm" to heightCm,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
    }

    suspend fun updateWeight(weightKg: Int) {
        val uid = MyApplication.auth.currentUser?.uid ?: return
        MyApplication.db.collection("users").document(uid)
            .update(
                mapOf(
                    "weightKg" to weightKg,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
    }

    suspend fun updateRegion(region: String) {
        val uid = MyApplication.auth.currentUser?.uid ?: return
        MyApplication.db.collection("users").document(uid)
            .update(
                mapOf(
                    "region" to region,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
    }
    private suspend fun updateField(field: String, value: Any) {
        val uid = MyApplication.auth.currentUser?.uid ?: return
        MyApplication.db.collection("users").document(uid)
            .update(
                mapOf(
                    field to value,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
    }


    // ✅ 재전송도 레포지터리에서 래핑
    suspend fun resendVerificationEmail() {
        val user = MyApplication.auth.currentUser ?: error("로그인 필요")
        user.sendEmailVerification().await()
    }
}