package com.example.walking_hadang.ui.store

// JSON의 각 레코드(record) 한 개를 담는 그릇
data class StoreItem(
    val 상품명: String,
    val 원가: String,
    val 할인가: String,
    val 할인율: String,
    val 링크: String,
    val 이미지리소스: String
)

// JSON 최상단 구조의 records 배열을 감싸는 그릇
data class StoreWrapper(
    val records: List<StoreItem>
)
