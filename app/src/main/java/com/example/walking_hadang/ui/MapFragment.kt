package com.example.walking_hadang.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.walking_hadang.R
import com.example.walking_hadang.data.AssetCourseData
import com.example.walking_hadang.data.CourseWrapper
import com.example.walking_hadang.databinding.FragmentMapBinding
import com.example.walking_hadang.util.LocationUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.gson.Gson

class MapFragment : Fragment(), OnMapReadyCallback {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var googleMap: GoogleMap
    private lateinit var startButton: Button
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var recyclerView: RecyclerView

    //지도 마커와 카드뷰 연결
    private val markerCourseMap = mutableMapOf<Marker, AssetCourseData>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        startButton = binding.btnStartWalking

        startButton.setOnClickListener {
            mapFragment.view?.visibility = View.VISIBLE
            startButton.visibility = View.GONE
            startTracking()
        }
//        childFragmentManager.beginTransaction()
//            .replace(binding.courseFragmentCatainer.id, CourseListFragment())
//            .commit()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        var lat1: Double? = 0.0;
        var lat2: Double? = 0.0;
        var lng1: Double? = 0.0;
        var lng2: Double? = 0.0;

        val courseList = loadCoursesFromAsset(requireContext())
        val filteredList = LocationUtil.filterCoursesWithinRadius(courseList, 37.5665, 126.9780)
        Log.d("MapFragment", "총 ${filteredList.size}개 로드됨")
        filteredList.forEach {
            Log.d("MapFragment", "이름: ${it.name}, 위치: (${it.latitude}, ${it.longitude})")

        }
        lat1 = filteredList[0].latitude?.toDouble();
        lat2 = filteredList[1].latitude?.toDouble();
        lng1 = filteredList[0].longitude?.toDouble();
        lng2 = filteredList[1].longitude?.toDouble();

        val start = LatLng(lat1!!, lng1!!)
        val end = LatLng(lat2!!, lng2!!)



        addCourseMarkers(filteredList)  // 🔁 먼저 마커 추가

        googleMap.setOnMarkerClickListener { marker ->
            val course = markerCourseMap[marker]
            if(course != null){
                showFloatingCard(marker, course)
            }
            true
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            showCurrentLocation()  // 🔁 마커 추가 후 현재 위치 표시
            googleMap.addPolyline(
                PolylineOptions()
                    .add(start,end)
                    .width(5f)
                    .color(0xFF0000FF.toInt())
            )
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }



    private fun showCurrentLocation() {
        getCurrentLocation(
            onSuccess = { location ->
                val currentLatLng = LatLng(location.latitude, location.longitude)
                googleMap?.apply {
//                                clear()
                    addMarker(MarkerOptions().position(currentLatLng).title("현재 위치"))
                    moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    try {
                        isMyLocationEnabled = true
                    }catch (e: SecurityException){
                        Log.e("MapFragment", "위치 권한 없음: ${e.message}")
                    }
                }
            },
            onFailure = {
                Toast.makeText(requireContext(), "위치를 가져올 수 없습니다", Toast.LENGTH_SHORT).show()
            }
        )

    }

    private fun addCourseMarkers(courseList: List<AssetCourseData>) {
        val builder = LatLngBounds.Builder()
        for (course in courseList) {
            val lat = course.latitude?.toDoubleOrNull()
            val lng = course.longitude?.toDoubleOrNull()

            if (lat != null && lng != null) {
                val position = LatLng(lat, lng)
                val markerOptions = MarkerOptions()
                    .position(position)
                    .title(course.name ?: "코스") // name은 "길명"에 해당하는 필드
//                Log.w("MapFragment", "좌표가 마커 작성함: ${course.name}")

                val marker = googleMap.addMarker(markerOptions)

                if(marker != null){
                    markerCourseMap[marker] = course
                }

                builder.include(LatLng(lat, lng))
            }else{
                Log.w("MapFragment", "좌표가 잘못되어 마커 건너뜀: ${course.name}")
            }
        }
        val bounds = builder.build()
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
    }
    private fun loadCoursesFromAsset(context: Context): List<AssetCourseData> {
        val json = context.assets.open("walking_courses.json").bufferedReader().use { it.readText() }
        val wrapper = Gson().fromJson(json, CourseWrapper::class.java)
        return wrapper.records
    }

    private fun showFloatingCard(marker: Marker, course: AssetCourseData){
        val projection = googleMap.projection
        val screenPosition = projection.toScreenLocation(marker.position)

        val container = binding.floatingCardContainer
        container.removeAllViews()

        val cardView = layoutInflater.inflate(R.layout.item_course_card, binding.courseFragmentCatainer, false)

        val imageView = cardView.findViewById<ImageView>(R.id.image)
        val titleView = cardView.findViewById<TextView>(R.id.title)
        val lengthView = cardView.findViewById<TextView>(R.id.lengthInfo)
        val timeView = cardView.findViewById<TextView>(R.id.timeInfo)

        imageView.setImageResource(R.drawable.ic_road)
        titleView.text = course.name ?: "알 수 없음"
        lengthView.text = "총 길이: ${course.length ?: "?"} km"
        timeView.text = "예상 시간: ${course.time ?: "?"} 분"


        container.addView(cardView)
        cardView.post {
            // 화면 중심 기준 마커 위에 카드 위치 조정
            cardView.x = screenPosition.x - cardView.width / 2f
            cardView.y = screenPosition.y - cardView.height - 30f // 마커 위 살짝 띄우기
        }
        container.visibility = View.VISIBLE
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == LOCATION_PERMISSION_REQUEST_CODE){
            if((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                showCurrentLocation()
            }else{
                Toast.makeText(requireContext(), "위치 권한이 필요합니다.", Toast.LENGTH_SHORT)
            }
        }
    }

    private fun getCurrentLocation(onSuccess: (Location) -> Unit, onFailure: () -> Unit){
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        onSuccess(location)
                    } else {
                        onFailure()
                    }
                }
        } else {
            onFailure()
        }
    }

    private fun startTracking() {
        // TODO: 위치 권한 요청 및 위치 업데이트 로직 구현
        getCurrentLocation(
            onSuccess = {location ->
                val userLat = location.latitude
                val userLng = location.longitude
            },
            onFailure = {
                Toast.makeText(requireContext(), "위치 정보를 사용할 수 없습니다", Toast.LENGTH_SHORT).show()
            }
        )
        Toast.makeText(context, "산책을 시작합니다!", Toast.LENGTH_SHORT).show()
    }

}