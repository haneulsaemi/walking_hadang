package com.example.walking_hadang

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.walking_hadang.databinding.FragmentCourseListBinding

class CourseListFragment : Fragment() {
    private var _binding: FragmentCourseListBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCourseListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView = binding.courseRecyclerView

        val items = listOf(
            CourseData(
                stretNm = "김포 수변길",
                stretIntrcn = "한강변을 따라 걷는 수변 산책로로, 벚꽃이 유명한 명소입니다.",
                stretLt = "5.2km",
                reqreTime = "1시간 10분",
                beginSpotNm = "김포한강로 입구",
                beginRdnmadr = "경기도 김포시 김포한강로 123",
                endSpotNm = "고촌역",
                endRdnmadr = "경기도 김포시 고촌읍 역길 45",
                coursInfo = "강변길 – 벚꽃터널 – 생태탐방로 – 고촌역",
                institutionNm = "김포시청"
            ),
            CourseData(
                stretNm = "평택 섶길",
                stretIntrcn = "평택호를 따라 걷는 조용한 트레킹 코스로, 사계절 자연을 만끽할 수 있습니다.",
                stretLt = "8.1km",
                reqreTime = "1시간 50분",
                beginSpotNm = "평택호 관광단지",
                beginRdnmadr = "경기도 평택시 포승읍 평택호길 25",
                endSpotNm = "평택호 캠핑장",
                endRdnmadr = "경기도 평택시 포승읍 해안로 91",
                coursInfo = "관광단지 – 갈대숲길 – 전망대 – 캠핑장",
                institutionNm = "평택시청"
            ),
            CourseData(
                stretNm = "서울 둘레길",
                stretIntrcn = "서울을 둘러싸고 있는 자연녹지 구간을 따라 걷는 총 157km의 장거리 길",
                stretLt = "18.3km",
                reqreTime = "4시간 20분",
                beginSpotNm = "북한산 입구",
                beginRdnmadr = "서울시 은평구 불광동 북한산로 34",
                endSpotNm = "진관사입구",
                endRdnmadr = "서울시 은평구 진관사길 12",
                coursInfo = "북한산탐방지원센터 – 구기터널 – 불광사 – 진관사입구",
                institutionNm = "서울특별시"
            )
        )

        recyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = CourseCardAdapter(items)
    }
}