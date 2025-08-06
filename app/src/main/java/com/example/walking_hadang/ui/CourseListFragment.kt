package com.example.walking_hadang.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.walking_hadang.BuildConfig
import com.example.walking_hadang.adapter.AssetCourseCardAdapter
import com.example.walking_hadang.adapter.CourseCardAdapter
import com.example.walking_hadang.data.AssetCourseData
import com.example.walking_hadang.data.CourseData
import com.example.walking_hadang.data.CourseResponse
import com.example.walking_hadang.data.CourseRetrofit
import com.example.walking_hadang.data.CourseWrapper
import com.example.walking_hadang.databinding.FragmentCourseListBinding
import com.example.walking_hadang.util.LocationUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CourseListFragment : Fragment() {
    private var _binding: FragmentCourseListBinding? = null
    private val binding get() = _binding!!
    private lateinit var courseAdapter: AssetCourseCardAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCourseListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView = binding.courseRecyclerView
        recyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        courseAdapter = AssetCourseCardAdapter(emptyList())
        recyclerView.adapter = courseAdapter
        val tempList = LocationUtil.filterCoursesWithinRadius(loadCoursesFromAsset(), 37.5665, 126.9780)
        courseAdapter.updateData(tempList)
//        fetchCourse(recyclerView)
    }

    // 프로젝트 내부에 저장한 json파일로 산책 코스 불러오기
    private fun loadCoursesFromAsset(): List<AssetCourseData> {
        return try {
            val inputStream = requireContext().assets.open("walking_courses.json")
            val json = inputStream.bufferedReader().use { it.readText() }
            Log.d("loadCoursesFromAsset", "jsonString loaded")
            val gson = Gson()
            val wrapper = gson.fromJson(json, CourseWrapper::class.java)
            wrapper.records
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // 공공데이터 api를 이용해 산책 코스 불러오기
    private fun fetchCourse(recyclerView: RecyclerView){
        CourseRetrofit.Companion.courseApiService.getWalkingCourses(
            serviceKey = BuildConfig.WALK_API_KEY,
            pageNo = 1,
            numOfRows = 100,
            type = "json"
        ).enqueue(object : Callback<CourseResponse> {

            override fun onResponse(call: Call<CourseResponse?>, response: Response<CourseResponse?>) {
                Log.d("Retrofit", "Response success: ${response.isSuccessful}")
                Log.d("Retrofit", "Response: ${response}")
                Log.d("Retrofit", "Response body: ${response.body()}")
                Log.e("Retrofit", "Raw Error: ${response.errorBody()?.string()}")
                if (response.isSuccessful) {
                    val body = response.body()
                    val courseList = body?.response?.body?.items?: emptyList<CourseData>()
//                    courseAdapter = CourseCardAdapter(courseList as List<CourseData>)
//                    recyclerView.adapter = courseAdapter
                } else {
                    Log.e("Retrofit", "응답 실패: ${response.code()}")
                }
            }
            override fun onFailure(call: Call<CourseResponse?>, t: Throwable) {

                Log.e("API_ERROR", "onFailure: ${t.message}")
                Toast.makeText(requireContext(), "API 연동 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}