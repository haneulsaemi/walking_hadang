package com.example.walking_hadang.data

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface CourseApiService {
    @GET("tn_pubr_public_stret_tursm_info_api")
    fun getWalkingCourses(
        @Query(value = "serviceKey", encoded = true) serviceKey: String,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 1000,
        @Query("type") type: String = "json"
    ): Call<CourseResponse>
}