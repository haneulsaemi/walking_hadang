package com.example.walking_hadang

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.walking_hadang.databinding.FragmentCourseListBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CourseListFragment : Fragment() {
    private var _binding: FragmentCourseListBinding? = null
    private val binding get() = _binding!!
    private lateinit var courseAdapter: CourseCardAdapter

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
        courseAdapter = CourseCardAdapter(emptyList())
        recyclerView.adapter = courseAdapter
        fetchCourse(recyclerView)
    }

    private fun fetchCourse(recyclerView: RecyclerView){
        CourseRetrofit.courseApiService.getWalkingCourses(
            serviceKey = BuildConfig.WALK_API_KEY,
            pageNo = 1,
            numOfRows = 100,
            type = "json"
        ).enqueue(object : Callback<CourseResponse>{

            override fun onResponse(call: Call<CourseResponse?>, response: Response<CourseResponse?>) {
                Log.d("Retrofit", "Response success: ${response.isSuccessful}")
                Log.d("Retrofit", "Response: ${response}")
                Log.d("Retrofit", "Response body: ${response.body()}")
                Log.e("Retrofit", "Raw Error: ${response.errorBody()?.string()}")
                if (response.isSuccessful) {
                    val body = response.body()
                    val courseList = body?.response?.body?.items?: emptyList<CourseData>()
                    courseAdapter = CourseCardAdapter(courseList as List<CourseData>)
                    recyclerView.adapter = courseAdapter
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