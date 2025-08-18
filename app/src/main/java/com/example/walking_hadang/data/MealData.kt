package com.example.walking_hadang.data

import com.google.firebase.Timestamp


data class MealItemLine(
    val name: String = "",
    val amount: String? = null,
    val kcal: Int? = null
)

/** 날짜 카드: /users/{uid}/meals/{dateKey} */
data class MealDayDoc(
    val userId: String = "",
    val dateKey: String = "",                 // "yyyy-MM-dd" – 문서 ID와 동일 권장
    val itemCount: Int = 0,                   // 해당 날짜 총 아이템 개수
    val totalKcal: Int? = null,               // 총 칼로리 (없으면 null)
    val firstLoggedAt: Timestamp? = null,     // 첫 기록 시각
    val lastLoggedAt: Timestamp? = null,      // 마지막 기록 시각(리스트 정렬에 사용)
    val coverImageUrl: String? = null,        // 카드 썸네일
    val memo: String? = null                  // 날짜 메모(선택)
)

/** 한 끼: /users/{uid}/meals/{dateKey}/mealEntries/{entryId} */
data class MealEntryDoc(
    val userId: String = "",
    val entryId: String = "",                 // 문서 id 저장(편의)
    val mealType: String = "BREAKFAST",       // "BREAKFAST"/"LUNCH"/"DINNER"/"SNACK" 또는 한글
    val eatenAt: Timestamp? = null,           // "오전 9:20" 같은 배지용 시간
    val photoUrl: String? = null,
    val items: List<MealItemLine> = emptyList(),
    val totalKcal: Int? = null,               // items 합계(없으면 null)
    val eatenAll: Boolean? = null,            // "다 먹음!"
    val note: String? = null,
    val orderIndex: Int = 0,                  // 상세 정렬용
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)