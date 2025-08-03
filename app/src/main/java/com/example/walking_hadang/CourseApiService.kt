package com.example.walking_hadang

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface CourseApiService {
    @GET
    fun getWalkingCourses(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 1000,
        @Query("type") type: String = "json"
    ): Call<CourseResponse>
}