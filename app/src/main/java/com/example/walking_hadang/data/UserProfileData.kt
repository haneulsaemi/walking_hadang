package com.example.walking_hadang.data

data class UserProfileData(
    val userId: String = "",
    val email: String = "",
    val userName: String = "",
    val nickname: String = "",
    val birthDate: String = "",   // yyyy-MM-dd (또는 Timestamp 사용 가능)
    val gender: String = "",      // "F" or "M"
    val heightCm: Int? = null,
    val weightKg: Int? = null,
    val region: String = "",
    val createdAt: com.google.firebase.Timestamp? = null,
    val updatedAt: com.google.firebase.Timestamp? = null
)
