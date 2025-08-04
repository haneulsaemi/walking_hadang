package com.example.walking_hadang

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CourseRetrofit {
    companion object{
        private const val BASE_URL = "http://api.data.go.kr/openapi/"
        val gson : Gson = GsonBuilder().setLenient().create()
        var courseApiService: CourseApiService
        val courseRetrofit: Retrofit
            get() = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        init {
            courseApiService = courseRetrofit.create(CourseApiService::class.java)
        }
    }
}