package com.example.walking_hadang

// JSON 데이터 구조를 확인한 뒤 해당 클래스 구조화 해야 함.
data class CourseResponse(
    val response: CourseOuterResponse?
)

data class CourseOuterResponse(
    val body: CourseBody?
)

data class CourseBody(
    val items: List<CourseData>?
)


data class CourseData(
    val stretNm: String?, //길명
    val stretIntrcn: String?, //길소개
    val stretLt: String?, //총길이
    val reqreTime: String?, //총소요시간
    val beginSpotNm: String?, //시작지점명
    val beginRdnmadr: String?, //시작지점도로명주소
    val endSpotNm: String?, //종료지점명
    val endRdnmadr: String?,//종료지점도로명주소
    val coursInfo: String?, // 경로정보
    val institutionNm: String?, //관리기관명
    val insttNm: String? //제공기관명
)
