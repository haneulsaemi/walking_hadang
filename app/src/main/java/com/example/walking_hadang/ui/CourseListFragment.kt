package com.example.walking_hadang.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.walking_hadang.adapter.AssetCourseCardAdapter
import com.example.walking_hadang.adapter.CourseCardAdapter
import com.example.walking_hadang.data.AssetCourseData
import com.example.walking_hadang.data.CourseWrapper
import com.example.walking_hadang.databinding.FragmentCourseListBinding
import com.example.walking_hadang.util.LocationUtil
import com.google.gson.Gson

class CourseListFragment : Fragment() {
    private var _binding: FragmentCourseListBinding? = null
    private val binding get() = _binding!!
    private lateinit var courseAdapter: AssetCourseCardAdapter
    private lateinit var homeCourseAdapter: CourseCardAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCourseListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
       val parent = parentFragment
        val fragmentName = parent?.javaClass?.simpleName

        val recyclerView = binding.courseRecyclerView
        recyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val tempList = LocationUtil.filterCoursesWithinRadius(loadCoursesFromAsset(), 37.5665, 126.9780)

        if (fragmentName.equals("HomeFragment")){

            homeCourseAdapter = CourseCardAdapter(emptyList(), parentFragment?.parentFragmentManager)
            recyclerView.adapter = homeCourseAdapter
            homeCourseAdapter.updateData(tempList)
        }else{
            courseAdapter = AssetCourseCardAdapter(emptyList())
            recyclerView.adapter = courseAdapter
            courseAdapter.updateData(tempList)
        }



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

}