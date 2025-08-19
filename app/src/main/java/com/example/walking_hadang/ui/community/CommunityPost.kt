package com.example.walking_hadang.ui.community

data class CommunityPost(
    val id: String = "",
    val title: String = "",
    val preview: String = "",
    val likes: Int = 0,
    val comments: Int = 0,
    val createdAt: Long = 0L,   // epoch millis
    val badge: String? = "혈당 공유"
)
//테스트