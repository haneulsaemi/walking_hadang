package com.example.walking_hadang.data

import com.google.firebase.Timestamp

// GlucoseType: 측정 종류
enum class GlucoseType { FASTING, POSTPRANDIAL, BEDTIME }

// MealType: 식후 측정일 때 끼니
enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }

data class GlucoseData(
    val id: String? = null,
    val userId: String = "",
    val recordedAt: Timestamp = Timestamp.now(),
    val dateKey: String = "",               // yyyy-MM-dd (일자별 묶음/쿼리용)
    val postprandialMinutes: Int? = null,   // 기본 120, 식후 몇 분 경과
    val type: GlucoseType = GlucoseType.FASTING,
    val meal: MealType? = null,
    val value: Int = 0,  // 혈당 수치 val dateKey: String = "",
    val memo: String = "",
)
