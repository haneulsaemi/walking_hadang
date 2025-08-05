package com.example.walking_hadang.data

import com.google.gson.annotations.SerializedName

data class CourseWrapper(
    val records: List<AssetCourseData>
)
data class AssetCourseData(
    @SerializedName("길명") val name: String?,
    @SerializedName("길소개") val description: String?,
    @SerializedName("총길이") val length: String?,
    @SerializedName("총소요시간") val time: String?,
    @SerializedName("시작지점명") val startName: String?,
    @SerializedName("시작지점도로명주소") val startRoadAddress: String?,
    @SerializedName("시작지점소재지지번주소") val startJibunAddress: String?,
    @SerializedName("종료지점명") val endName: String?,
    @SerializedName("종료지점소재지도로명주소") val endRoadAddress: String?,
    @SerializedName("종료지점소재지지번주소") val endJibunAddress: String?,
    @SerializedName("경로정보") val route: String?,
    @SerializedName("관리기관전화번호") val agencyPhone: String?,
    @SerializedName("관리기관명") val agencyName: String?,
    @SerializedName("데이터기준일자") val date: String?,
    @SerializedName("제공기관코드") val orgCode: String?,
    @SerializedName("제공기관명") val orgName: String?,
    @SerializedName("latitude") val latitude: String?,
    @SerializedName("longitude") val longitude: String?
)
