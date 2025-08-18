package com.example.walking_hadang.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.annotation.RequiresPermission
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.walking_hadang.R
import com.example.walking_hadang.data.AssetCourseData
import com.example.walking_hadang.data.CourseWrapper
import com.example.walking_hadang.data.WalkData
import com.example.walking_hadang.databinding.FragmentMapBinding
import com.example.walking_hadang.util.LocationUtil
import com.example.walking_hadang.util.WalkRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
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
import com.google.firebase.Timestamp
import com.google.gson.Gson
import com.google.maps.android.PolyUtil
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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

    private var runningPolyline: Polyline? = null
    private var runningPath =  mutableListOf<LatLng>()
    private var isRunning: Boolean = false
    private var walkStartMillis: Long? = null

    private var locationRequest: com.google.android.gms.location.LocationRequest? = null
    private lateinit var locationcallback: LocationCallback
    private fun Int.dp(): Int =
        (this * resources.displayMetrics.density).toInt()
    //지도 마커와 카드뷰 연결
    private val markerCourseMap = mutableMapOf<Marker, AssetCourseData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationcallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                val map = googleMap ?: return
                for(loc in result!!.locations){
                    val p = LatLng(loc.latitude, loc.longitude)
                    runningPath.add(p)

                    runningPolyline?.points = runningPath

                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(p, 16f))
                }
            }
        }
    }

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
            if(isRunning){
                Toast.makeText(context, "산책을 종료합니다!", Toast.LENGTH_SHORT).show()
                saveDummyWalk()
                startButton.text = "산책 시작하기"
                val fragment = RecodingFragment.newInstance(2) // 0: 혈당, 1: 식사, 2: 산책
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.main_fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }else{
                mapFragment.view?.visibility = View.VISIBLE
                startButton.text = "산책 종료하기"
                startTracking()
            }
            isRunning = !isRunning


        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isMyLocationButtonEnabled = true


        val courseList = loadCoursesFromAsset(requireContext())
        try {
            getCurrentLocation(
                onSuccess = { location ->
                    Log.d("MapFragment", "현재 위치: ${location.latitude}, ${location.longitude}")
                    val filteredList = LocationUtil.filterCoursesWithinRadius(
                        courseList,
                        location.latitude,
                        location.longitude,
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
                val currentLatLng = LatLng(location.latitude, location.longitude)
                googleMap?.apply {

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

    private fun hideFloatingCard() {
        binding.floatingCardContainer.apply {
            removeAllViews()
            visibility = View.GONE
            isClickable = false     // 다시 맵 터치 가능
            isFocusable = false
        }
    }

    private fun showFloatingCard(marker: Marker, course: AssetCourseData){
        val container = binding.floatingCardContainer
        container.removeAllViews()
        container.apply {
            isClickable = true      // 터치 먹음 → 지도 안 움직임
            isFocusable = true
            visibility = View.VISIBLE
        }
        val cardView = layoutInflater.inflate(R.layout.item_course_card, binding.courseFragmentCatainer, false)

        val imageView = cardView.findViewById<ImageView>(R.id.image)
        val titleView = cardView.findViewById<TextView>(R.id.title)
        val lengthView = cardView.findViewById<TextView>(R.id.lengthInfo)
        val timeView = cardView.findViewById<TextView>(R.id.timeInfo)
        val gotoBtn = cardView.findViewById<Button>(R.id.btnGoto)
        val detailBtn = cardView.findViewById<Button>(R.id.btnDetail)
        val closeBtn = cardView.findViewById<ImageButton>(R.id.btnClose)
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
        closeBtn.setOnClickListener {
            hideFloatingCard()
        }

        detailBtn.setOnClickListener {
            showDetailCard(marker, course)
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

    private fun placeDetailTopCenter(card: View) {
        val lp = (card.layoutParams as FrameLayout.LayoutParams).apply {
            width = FrameLayout.LayoutParams.MATCH_PARENT                   // 가로 꽉 차게
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            val m = 16.dp()
            setMargins(m, m, m, 0)                                          // 좌우 여백 16dp
        }
        card.layoutParams = lp
    }

    private fun showDetailCard(marker: Marker, course: AssetCourseData) {
        binding.floatingCardContainer.apply {
            isClickable = true      // 터치 먹음 → 지도 안 움직임
            isFocusable = true
            visibility = View.VISIBLE
        }
        showInFloating<View>(R.layout.item_course_detail_card) { detail ->
            // 상세 바인딩…
            detail.findViewById<View>(R.id.btnClose).setOnClickListener {
                hideFloatingCard()
            }
            val lat = course.latitude?.toDoubleOrNull()
            val lng = course.longitude?.toDoubleOrNull()

            detail.findViewById<View>(R.id.btnGotoCourse).setOnClickListener {
                if (lat != null && lng != null) {
                    startGoogleNavigation(requireContext(),  lat, lng)
                }
                true
            }
            val tvTitle     = detail.findViewById<TextView>(R.id.tvTitle)
            val tvDuration  = detail.findViewById<TextView>(R.id.tvDuration)
            val chipLevel   = detail.findViewById<TextView>(R.id.chipLevel)
            val tvDistance  = detail.findViewById<TextView>(R.id.tvDistance)
            val tvStart     = detail.findViewById<TextView>(R.id.tvStart)
            val tvEnd       = detail.findViewById<TextView>(R.id.tvEnd)
            val tvRoute     = detail.findViewById<TextView>(R.id.tvRoute)
            val tvPhone     = detail.findViewById<TextView>(R.id.tvPhone)
            val tvOrg       = detail.findViewById<TextView>(R.id.tvOrg)

            // Helper
            fun TextView.setTextOrGone(value: String?, prefix: String = "") {
                if (value.isNullOrBlank()) {
                    visibility = View.GONE
                } else {
                    text = prefix + value
                    visibility = View.VISIBLE
                }
            }

            // 제목
            tvTitle.setTextOrGone(course.name)

            // 거리/시간/난이도
            tvDistance.setTextOrGone(course.length?.let { "  ·  $it" })
            tvDuration.setTextOrGone(course.time)

            // 난이도 필드가 별도로 없으니 기본값 표시
            chipLevel.text = "보통"

            // 시작/도착
            val startText = course.startName
                ?: course.startRoadAddress
                ?: course.startJibunAddress
            tvStart.setTextOrGone(startText, "시작: ")

            val endText = course.endName
                ?: course.endRoadAddress
                ?: course.endJibunAddress
            tvEnd.setTextOrGone(endText, "도착: ")

            // 경로 요약
            tvRoute.setTextOrGone(course.route?.replace("->", "→")?.trim(), "경로: ")

            // 기관 전화/명
            tvPhone.setTextOrGone(course.agencyPhone)
            tvOrg.setTextOrGone(course.agencyName ?: course.orgName)

            placeDetailTopCenter(detail)
        }
    }
    private fun <T: View> showInFloating(
        @LayoutRes layoutRes: Int,
        binder: (T) -> Unit
    ) {
        val container = binding.floatingCardContainer
        container.removeAllViews()

        @Suppress("UNCHECKED_CAST")
        val view = layoutInflater.inflate(layoutRes, container, false) as T
        container.addView(view)
        container.visibility = View.VISIBLE
        binder(view)
    }
    private fun positionNearMarker(view: View, marker: Marker) {
        val screen = googleMap.projection.toScreenLocation(marker.position)
        view.post {
            view.x = screen.x - view.width / 2f
            view.y = screen.y - view.height - 30f
        }
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
                walkStartMillis = System.currentTimeMillis()

                runningPath.clear()
                // 기존 폴리라인 제거 후 새로 생성
                runningPolyline?.remove()
                val opts = PolylineOptions()
                    .color(ContextCompat.getColor(requireContext(), R.color.softSkyBlue))
                    .width(7f)
                runningPolyline = googleMap?.addPolyline(opts)

                // 위치 요청(간격/정확도 조절 가능)
                // 최신 API 권장: LocationRequest.Builder
                locationRequest = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    com.google.android.gms.location.LocationRequest.Builder(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 2000L
                    ).setMinUpdateIntervalMillis(1000L).build()
                } else {
                    @Suppress("DEPRECATION")
                    com.google.android.gms.location.LocationRequest.create().apply {
                        interval = 2000L
                        fastestInterval = 1000L
                        priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
                    }
                }

//                fusedLocationClient.requestLocationUpdates()
                Toast.makeText(context, "산책을 시작합니다!", Toast.LENGTH_SHORT).show()

            },
            onFailure = {
                Toast.makeText(requireContext(), "위치 정보를 사용할 수 없습니다", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 종료하기: 위치업데이트 중지 + (선택) 거리/시간 계산
    private fun onStopTracking() {
        fusedLocationClient.removeLocationUpdates(locationcallback)

        val distanceMeters  = computeTotalDistanceMeters(runningPath)
        val distanceM = distanceMeters.roundToInt()
        val endedAt = Timestamp.now()
        val startedAtTs = walkStartMillis?.let { Timestamp(Date(it)) } ?: Timestamp.now()
        val durationSec = walkStartMillis?.let { ((System.currentTimeMillis() - it) / 1000L).toInt() } ?: 0

        // 시작/종료 좌표
        val start = runningPath.firstOrNull()
        val end = runningPath.lastOrNull()

        // 경로 polyline 인코딩 (nullable 방지용으로 그대로 List<LatLng> 사용)
        val routePolyline = if (runningPath.size >= 2) {
            PolyUtil.encode(runningPath)
        } else {
            null
        }

        val stepsCount = 0


        val walk = WalkData(
            startedAt = startedAtTs,
            endedAt = endedAt,
            durationSec = durationSec,
            distanceM = distanceM,
            steps = stepsCount,
            startLat = start?.latitude,
            startLng = start?.longitude,
            endLat = end?.latitude,
            endLng = end?.longitude,
            routePolyline = routePolyline,
            note = null // 필요 시 UI에서 입력받은 메모 문자열
        )

        WalkRepository.addWalkEntry(walk,
            onSuccess = { id ->
                val kmText = String.format(Locale.getDefault(), "%.2f km", distanceMeters / 1000.0)
                Toast.makeText(requireContext(), "트래킹 종료 - 총 거리: $kmText", Toast.LENGTH_LONG).show()
                Log.d("WalkDebug", "저장 성공: $id")
                end?.let {
                    googleMap?.addMarker(
                        com.google.android.gms.maps.model.MarkerOptions()
                            .position(it)
                            .title("종료 지점")
                    )
                }
            },
            onError = { e ->
                Log.e("WalkDebug", "저장 실패", e)
            }
        )



        // 마지막 지점 마커
        if (runningPath.isNotEmpty()) {
            val last = runningPath.last()
            googleMap?.addMarker(MarkerOptions().position(last).title("종료 지점"))
        }
    }

    // 두 점 사이 거리 합산
    private fun computeTotalDistanceMeters(points: List<LatLng>): Double {
        if (points.size < 2) return 0.0
        val results = FloatArray(1)
        var sum = 0.0
        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, results)
            sum += results[0]
        }
        return sum
    }

    fun saveDummyWalk() {

        // 예시 경로 (단순히 3개 좌표 연결)
        val fakePath = listOf(
            LatLng(37.5830, 127.0005), // 혜화역
            LatLng(37.5815, 127.0025), // 마로니에공원
            LatLng(37.5790, 127.0040)  // 창경궁 입구
        )

        // 경로 polyline 인코딩
        val encodedPolyline = PolyUtil.encode(fakePath)

        // 시작 & 종료 시간 (1시간 차이)
        val startTime = Timestamp(Date(System.currentTimeMillis() - 3600_000))
        val endTime = Timestamp.now()

        // WalkData 형식 맞춰서 생성
        val dummyWalk = WalkData(
            id = null,
            startedAt = Timestamp(Date(System.currentTimeMillis() - 2700_000)), // 45분 전
            endedAt = Timestamp(Date(System.currentTimeMillis() - 900_000)),    // 15분 전
            durationSec = 1800,
            distanceM = 1200,
            steps = 1800,
            startLat = fakePath.first().latitude,
            startLng = fakePath.first().longitude,
            endLat = fakePath.last().latitude,
            endLng = fakePath.last().longitude,
            routePolyline = encodedPolyline,
            note = "대학로 저녁 산책"
        )

        // Firestore에 저장
        WalkRepository.addWalkEntry(dummyWalk,
            onSuccess = { id ->
                Log.e("WalkDebug","임의 데이터 저장 성공: ${id}")
            },
            onError = { e ->
                Log.e("WalkDebug", "임의 데이터 저장 실패", e)
            }
        )
    }

    override fun onPause() {
        super.onPause()
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        toolbar.findViewWithTag<View>("mapTitleView")?.let {
            toolbar.removeView(it)
        }
    }

    override fun onResume() {
        super.onResume()
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        val titleView = LayoutInflater.from(context).inflate(R.layout.toolbar_custom, toolbar, false) as TextView
        titleView.text = "지도"
        titleView.apply {
            tag = "mapTitleView" // 중복 방지용 태그
        }
        toolbar.addView(titleView)
    }

}