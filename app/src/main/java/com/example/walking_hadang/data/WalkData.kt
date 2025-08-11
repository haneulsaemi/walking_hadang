package com.example.walking_hadang.data

import com.google.firebase.Timestamp


data class WalkData(
    val id: String? = null,
    val startedAt: Timestamp = Timestamp.now(),
    val endedAt: Timestamp? = null,
    val durationSec: Int = 0,
    val distanceM: Int = 0,
    val steps: Int = 0,
    val startLat: Double? = null,
    val startLng: Double? = null,
    val endLat: Double? = null,
    val endLng: Double? = null,
    val routePolyline: String? = null,
    val note: String? = null
)
