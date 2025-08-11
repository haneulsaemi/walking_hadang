package com.example.walking_hadang.data

import com.google.firebase.Timestamp


data class MealData(
    val id: String? = null,              // Firestore 문서 ID
    val eatenAt: Timestamp = Timestamp.now(),
    val mealType: String = "",           // "BREAKFAST" | "LUNCH" | "DINNER" | "SNACK"
    val photoUri: String? = null,        // 대표 사진 URL
    val totalCalories: Int = 0,          // 캐시 필드
    val note: String? = null,
)

data class MealItem(
    val id: String? = null,
    val foodName: String = "",
    val portion: String? = null,         // "150g"
    val calories: Int = 0,
    val photoUri: String? = null
)