package com.example.walking_hadang.data

import android.net.Uri
import com.google.android.gms.maps.model.LatLng


data class StaticMapConfig(
    val width: Int = 300,
    val height: Int = 180,
    val scale: Int = 2,                 // 레티나 품질
    val mapType: String = "roadmap",    // roadmap | satellite | hybrid | terrain
    val language: String = "ko",
    val strokeColor: String = "0x000000ff", // AARRGGBB(알파 포함), 예: 검정 불투명
    val strokeWeight: Int = 5,
    val showStartMarker: Boolean = true,
    val showEndMarker: Boolean = true
)

/** Firestore의 routePolyline(인코딩 문자열)로 Static Maps URL 생성 */
fun buildStaticMapUrl(
    routePolyline: String,
    apiKey: String,
    start: LatLng? = null,
    end: LatLng? = null,
    cfg: StaticMapConfig = StaticMapConfig()
): String {
    val base = "https://maps.googleapis.com/maps/api/staticmap"
    val b = Uri.parse(base).buildUpon()
        .appendQueryParameter("size", "${cfg.width}x${cfg.height}")
        .appendQueryParameter("scale", cfg.scale.toString())
        .appendQueryParameter("maptype", cfg.mapType)
        .appendQueryParameter("language", cfg.language)

    // 경로(폴리라인) – enc: 를 사용하면 인코딩 문자열을 그대로 넣을 수 있음
    val pathParam = "color:${cfg.strokeColor}|weight:${cfg.strokeWeight}|enc:$routePolyline"
    b.appendQueryParameter("path", pathParam)

    // 시작/종료 마커 (옵션)
    if (cfg.showStartMarker && start != null) {
        b.appendQueryParameter(
            "markers",
            "label:S|size:mid|color:green|${start.latitude},${start.longitude}"
        )
    }
    if (cfg.showEndMarker && end != null) {
        b.appendQueryParameter(
            "markers",
            "label:E|size:mid|color:red|${end.latitude},${end.longitude}"
        )
    }

    // 키
    b.appendQueryParameter("key", apiKey)

    return b.build().toString()
}