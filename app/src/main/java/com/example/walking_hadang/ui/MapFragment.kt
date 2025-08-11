package com.example.walking_hadang.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
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

    private var selectMarker: Marker? = null

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

        requireActivity().findViewById<TextView>(R.id.toolbarTitle).text = "지도"

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

        val courseList = loadCoursesFromAsset(requireContext())
        try {
            getCurrentLocation(
                onSuccess = { location ->
                    Log.d("MapFragment", "현재 위치: ${location.latitude}, ${location.longitude}")
                    val filteredList = LocationUtil.filterCoursesWithinRadius(
                        courseList,
                        35.573418,
                        129.189629,
                        radiusInKm = 20.0
                    )
                    Log.d("MapFragment", "필터링된 코스 개수: ${filteredList.size}")
                    if (filteredList.isNotEmpty()) {
                        addCourseMarkers(filteredList)
                    } else {
                        Toast.makeText(requireContext(), "추천 코스가 없습니다.", Toast.LENGTH_SHORT).show()
                        // 필요하다면 전체 코스 표시
                        // addCourseMarkers(courseList)  // 또는 아무 것도 안함
                    }
                    showCurrentLocation()

                },
                onFailure = {
                    Toast.makeText(requireContext(), "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT)
                        .show()
                    Log.e("MapFragment", "현재 위치를 가져올 수 없습니다.")
                    // fallback: 전체 코스 표시
                    addCourseMarkers(courseList)
                }
            )
        }catch (e: Exception){
            Log.e("MapFragment", "onMapReady에서 예외 발생: ${e.message}", e)
        }
        // 마커 클릭 이벤트
        googleMap.setOnMarkerClickListener { marker ->
            val course = markerCourseMap[marker]
            if(course != null){
                Log.d("MapFragment", "마커 클릭: ${course.name}")
                selectMarker = marker
                //마커 클릭 시 카메라 위로 보정하는 코드
                val currentZoom = googleMap.cameraPosition.zoom

                val projection = googleMap.projection
                val markerPoint = projection.toScreenLocation(marker.position)

                // Y좌표를 위로 이동 (값이 클수록 위로 올라감)
                val offsetY = 200  // px 단위, 필요에 따라 조절
                markerPoint.y -= offsetY

                // 다시 LatLng로 변환
                val newLatLng = projection.fromScreenLocation(markerPoint)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newLatLng, currentZoom))
                showFloatingCard(marker, course)
            }else {
                Log.e("MapFragment", "마커에 해당하는 코스가 없습니다.")
            }
            true
        }



        // 지도 화면 이동 시 이벤트핸들러
        googleMap.setOnCameraMoveListener {
            selectMarker?.let{ marker ->
                updateCardPosition(marker)
            }
        }



        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            showCurrentLocation()  // 🔁 마커 추가 후 현재 위치 표시
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun updateCardPosition(marker: Marker){
        val projection = googleMap.projection
        val screenPosition = projection.toScreenLocation(marker.position)

        val cardView = binding.floatingCardContainer.getChildAt(0) ?: return

        cardView.post {
            cardView.x = screenPosition.x - cardView.width / 2f
            cardView.y = screenPosition.y - cardView.height - 30f
        }
    }

    private fun showCurrentLocation() {
        getCurrentLocation(
            onSuccess = { location ->
                val currentLatLng = LatLng(35.573418, 129.189629)
                googleMap?.apply {
//                                clear()
//                    addMarker(MarkerOptions().position(currentLatLng).title("현재 위치"))
                    moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 13f))
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
        Log.d("MapFragment", "FloatingCard 표시: ${course.name}, 좌표: ${course.latitude}, ${course.longitude}")

        val container = binding.floatingCardContainer
        container.removeAllViews()

        val cardView = layoutInflater.inflate(R.layout.item_course_card, binding.courseFragmentCatainer, false)

        val imageView = cardView.findViewById<ImageView>(R.id.image)
        val titleView = cardView.findViewById<TextView>(R.id.title)
        val lengthView = cardView.findViewById<TextView>(R.id.lengthInfo)
        val timeView = cardView.findViewById<TextView>(R.id.timeInfo)
        val gotoBtn = cardView.findViewById<Button>(R.id.btnGoto)

        val lat = course.latitude?.toDoubleOrNull()
        val lng = course.longitude?.toDoubleOrNull()

        imageView.setImageResource(R.drawable.ic_road)
        titleView.text = course.name ?: "알 수 없음"
        lengthView.text = "총 길이: ${course.length ?: "?"} km"
        timeView.text = "예상 시간: ${course.time ?: "?"} 분"

        //네비게이션 연동
        gotoBtn.setOnClickListener {
            if (lat != null && lng != null) {
                startGoogleNavigation(requireContext(),  lat, lng)
            }
            true
        }

        cardView.isClickable = true
        cardView.isFocusableInTouchMode = true
        cardView.bringToFront()

        container.addView(cardView)
        cardView.post {
            val projection = googleMap.projection
            val screenPosition = projection.toScreenLocation(marker.position)
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
                        Log.e("MapFragment", "fusedLocationClient.lastLocation 반환값이 null")
                        onFailure()
                    }
                }
        } else {
            Log.e("MapFragment", "위치 권한이 없습니다.")
            onFailure()
        }
    }

    private fun startGoogleNavigation(context: Context, lat: Double, lng: Double){
        val uri = Uri.parse("google.navigation:q=$lat,$lng&mode=w") // w: 걷기, d: 운전, r: 대중교통
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "구글 지도 앱이 설치되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
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