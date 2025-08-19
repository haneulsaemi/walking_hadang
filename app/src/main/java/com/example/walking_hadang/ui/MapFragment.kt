package com.example.walking_hadang.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.walking_hadang.R
import com.example.walking_hadang.data.AssetCourseData
import com.example.walking_hadang.data.CourseWrapper
import com.example.walking_hadang.data.WalkData
import com.example.walking_hadang.databinding.FragmentMapBinding
import com.example.walking_hadang.util.LocationUtil
import com.example.walking_hadang.util.WalkRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

class MapFragment : Fragment(), OnMapReadyCallback {

    companion object {
        private const val ARG_COURSE = "arg_course"

        fun newInstance(course: AssetCourseData): MapFragment =
            MapFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_COURSE, course)
                }
            }

        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        // 재검색 튜닝
        private const val MIN_MOVE_FOR_RESCAN_METERS = 800.0
        private const val MIN_RESCAN_INTERVAL_MS = 2000L
        private const val MIN_ZOOM_DIFF_FOR_RESCAN = 0.7f
        private const val SUPPRESS_AFTER_CARD_OPEN_MS = 1500L
        private const val SUPPRESS_AFTER_BUTTON_MS = 1000L
    }

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var googleMap: GoogleMap
    private lateinit var startButton: Button

    private var selectMarker: Marker? = null

    // 트래킹
    private var runningPolyline: Polyline? = null
    private val runningPath = mutableListOf<LatLng>()
    private var isRunning = false
    private var walkStartMillis: Long? = null

    // 코스/마커
    private var allCourses: List<AssetCourseData> = emptyList()
    private val markerCourseMap = mutableMapOf<Marker, AssetCourseData>()
    private val courseMarkers = mutableListOf<Marker>()

    // 자동 재검색 상태
    private var lastRescanCenter: LatLng? = null
    private var lastRescanAt: Long = 0L
    private var lastZoom = 0f
    private var lastMoveByGesture = false
    private var suppressRescanUntil = 0L

    //전달 받은 코스
    private var pendingCourse: AssetCourseData? = null
    private var launchedWithCourse = false
    private var highlightedMarker: Marker? = null

    // 선택 코스 유지
    private var selectedCourseKey: String? = null
    private fun courseKey(c: AssetCourseData) = "${c.name}|${c.latitude}|${c.longitude}"

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        pendingCourse = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("arg_course", AssetCourseData::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable("arg_course")
        }
        launchedWithCourse = pendingCourse != null
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

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        startButton = binding.btnStartWalking
        startButton.setOnClickListener {
            if (isRunning) {
                Toast.makeText(context, "산책을 종료합니다!", Toast.LENGTH_SHORT).show()
                saveDummyWalk()
                startButton.text = "산책 시작하기"
                val fragment = RecodingFragment.newInstance(2)
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.main_fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            } else {
                mapFragment.view?.visibility = View.VISIBLE
                startButton.text = "산책 종료하기"
                startTracking()
            }
            isRunning = !isRunning
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isMyLocationButtonEnabled = true

        // 전체 코스 1회 로드
        allCourses = runCatching { loadCoursesFromAsset(requireContext()) }
            .onFailure { e -> Log.e("MapFragment", "코스 로드 실패", e) }
            .getOrDefault(emptyList())

        // 초기 스캔
        try {
            getCurrentLocation(
                onSuccess = {
                    showCurrentLocation()
                    rescanCoursesForVisibleRegion(force = true)
                },
                onFailure = {
                    Toast.makeText(requireContext(), "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                    rescanCoursesForVisibleRegion(force = true)
                }
            )
        } catch (e: Exception) {
            Log.e("MapFragment", "onMapReady 예외: ${e.message}", e)
        }
        if (launchedWithCourse) {
            // ✅ 전달 코스 우선 포커스
            focusAndShowIncomingCourse()
            // ✅ 이 시점에 재검색 억제 (카드 스치듯 사라짐 방지)
            suppressRescanUntil = System.currentTimeMillis() + 2000
            // 주변 다른 코스는 '지금 카메라 중심(=전달 코스)' 기준으로 로드
            rescanCoursesForVisibleRegion(force = true)
        } else {
            // 평소처럼 현위치 → 초기 스캔
            getCurrentLocation(
                onSuccess = {
                    showCurrentLocation()           // 카메라를 현위치로
                    rescanCoursesForVisibleRegion(force = true)
                },
                onFailure = {
                    Toast.makeText(requireContext(), "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                    rescanCoursesForVisibleRegion(force = true)
                }
            )
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            try { googleMap.isMyLocationEnabled = true } catch (_: SecurityException) {}
        }
        // 마커 클릭: 카메라 이동 X, 카드만 오픈 + suppress
        googleMap.setOnMarkerClickListener { marker ->
            val course = markerCourseMap[marker] ?: return@setOnMarkerClickListener true

            suppressRescanUntil = System.currentTimeMillis() + SUPPRESS_AFTER_CARD_OPEN_MS
            selectMarker = marker
            selectedCourseKey = courseKey(course)
            showFloatingCard(marker, course)
            true
        }

        // 카메라 이동 중 카드 위치만 업데이트
        googleMap.setOnCameraMoveListener {
            selectMarker?.let { updateCardPosition(it) }
        }

        // 제스처 여부 추적
        lastZoom = googleMap.cameraPosition.zoom
        googleMap.setOnCameraMoveStartedListener { reason ->
            lastMoveByGesture = (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE)
        }

        // 이동 종료: 제스처 + 의미 있는 변화일 때만 재검색
        googleMap.setOnCameraIdleListener {
            if (!lastMoveByGesture) {
                lastMoveByGesture = false
                lastZoom = googleMap.cameraPosition.zoom
                return@setOnCameraIdleListener
            }
            autoRescanIfNeeded()
            lastMoveByGesture = false
            lastZoom = googleMap.cameraPosition.zoom
        }

        // 다시검색 버튼
        binding.btnRescan.setOnClickListener {
            rescanCoursesForVisibleRegion(force = true)
            suppressRescanUntil = System.currentTimeMillis() + SUPPRESS_AFTER_BUTTON_MS
        }

        // 권한
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            showCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }
    private fun focusAndShowIncomingCourse() {
        val course = pendingCourse ?: return
        val lat = course.latitude?.toDoubleOrNull()
        val lng = course.longitude?.toDoubleOrNull()
        if (lat == null || lng == null) return

        val pos = LatLng(lat, lng)

        // ✅ 하이라이트 마커(일반 마커와 분리하여 관리)
        highlightedMarker?.remove()
        highlightedMarker = googleMap.addMarker(
            MarkerOptions()
                .position(pos)
                .title(course.name ?: "코스")
            // .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)) // 원하면 다른 색
        )
        highlightedMarker?.let { markerCourseMap[it] = course }

        selectedCourseKey = courseKey(course)
        selectMarker = highlightedMarker

        // 카메라를 전달 코스로 이동
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))

        // 카드 오픈
        showFloatingCard(highlightedMarker!!, course)

        // 초기 완료 후 소거
        pendingCourse = null
    }
    // ------------------- 재검색 -------------------

    private fun autoRescanIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - lastRescanAt < MIN_RESCAN_INTERVAL_MS) return
        if (now < suppressRescanUntil) return

        val center = googleMap.cameraPosition.target
        val prevCenter = lastRescanCenter
        val zoomDiff = abs(googleMap.cameraPosition.zoom - lastZoom)
        val movedMeters = if (prevCenter != null) distanceMeters(prevCenter, center) else Double.MAX_VALUE

        if (movedMeters < MIN_MOVE_FOR_RESCAN_METERS && zoomDiff < MIN_ZOOM_DIFF_FOR_RESCAN) return
        rescanCoursesForVisibleRegion(force = true)
    }

    private fun rescanCoursesForVisibleRegion(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && (now - lastRescanAt < MIN_RESCAN_INTERVAL_MS || now < suppressRescanUntil)) return

        val center = googleMap.cameraPosition.target
        val radiusKm = max(1.0, visibleRadiusKm()) // 현재 화면 반경

        val filtered = LocationUtil.filterCoursesWithinRadius(
            allCourses, center.latitude, center.longitude, radiusInKm = radiusKm
        )

        Log.d("MapFragment", "Rescan center=$center radiusKm=$radiusKm result=${filtered.size}")
        updateCourseMarkers(filtered)

        lastRescanCenter = center
        lastRescanAt = now
    }

    private fun visibleRadiusKm(): Double {
        val bounds: LatLngBounds = googleMap.projection.visibleRegion.latLngBounds
        val center = googleMap.cameraPosition.target
        val corners = listOf(
            bounds.northeast,
            bounds.southwest,
            LatLng(bounds.northeast.latitude, bounds.southwest.longitude),
            LatLng(bounds.southwest.latitude, bounds.northeast.longitude)
        )
        var maxMeters = 0f
        val results = FloatArray(1)
        for (c in corners) {
            Location.distanceBetween(center.latitude, center.longitude, c.latitude, c.longitude, results)
            if (results[0] > maxMeters) maxMeters = results[0]
        }
        return maxMeters / 1000.0
    }

    private fun distanceMeters(a: LatLng, b: LatLng): Double {
        val results = FloatArray(1)
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, results)
        return results[0].toDouble()
    }

    private fun updateCourseMarkers(courseList: List<AssetCourseData>) {
        val keepKey = selectedCourseKey
        clearCourseMarkers()

        if (courseList.isEmpty()) {
            Toast.makeText(requireContext(), "현재 화면에서 추천 코스를 찾지 못했습니다.", Toast.LENGTH_SHORT).show()
            hideFloatingCard()
            return
        }

        var markerToReselect: Marker? = null
        for (course in courseList) {
            val lat = course.latitude?.toDoubleOrNull()
            val lng = course.longitude?.toDoubleOrNull()
            if (lat == null || lng == null) continue

            val pos = LatLng(lat, lng)
            val marker = googleMap.addMarker(MarkerOptions().position(pos).title(course.name ?: "코스"))
            if (marker != null) {
                courseMarkers.add(marker)
                markerCourseMap[marker] = course
                if (keepKey != null && keepKey == courseKey(course)) markerToReselect = marker
            }
        }
        // 선택 코스가 일반 마커로도 다시 그려졌다면 그걸로 카드 유지
        if (markerToReselect != null) {
            selectMarker = markerToReselect
            markerCourseMap[markerToReselect]?.let { showFloatingCard(markerToReselect, it) }
        }
        // 일반 마커 목록에 없더라도 하이라이트는 유지
        highlightedMarker?.let { hm ->
            val hc = markerCourseMap[hm]
            if (hc != null) {
                selectMarker = hm
                showFloatingCard(hm, hc)
                return
            }
        }

        // 둘 다 없으면 카드 닫기
        selectedCourseKey = null
        selectMarker = null
        hideFloatingCard()
    }

    private fun clearCourseMarkers() {
        courseMarkers.forEach { it.remove() }
        courseMarkers.clear()
        markerCourseMap.clear()

        val keep = highlightedMarker
        markerCourseMap.keys.toList().forEach { m ->
            if (m != keep) markerCourseMap.remove(m)
        }
    }

    // ------------------- 카드뷰/배치 -------------------

    private fun updateCardPosition(marker: Marker) {
        val cardView = binding.floatingCardContainer.getChildAt(0) ?: return
        // 측정 전이면 다음 프레임에
        if (binding.floatingCardContainer.width == 0 || binding.floatingCardContainer.height == 0 ||
            cardView.width == 0 || cardView.height == 0) {
            cardView.post { updateCardPosition(marker) }
            return
        }
        cardView.post { positionCardNearMarkerWithConstraints(cardView, marker) }
    }

    private fun hideFloatingCard() {
        binding.floatingCardContainer.apply {
            removeAllViews()
            visibility = View.GONE
            isClickable = false   // 컨테이너는 항상 터치 통과
            isFocusable = false
        }
    }

    private fun showFloatingCard(marker: Marker, course: AssetCourseData) {
        val container = binding.floatingCardContainer
        container.removeAllViews()

        // 컨테이너는 터치 통과 → 지도 제스처 가능
        container.isClickable = false
        container.isFocusable = false
        container.visibility = View.VISIBLE

        val cardView = layoutInflater.inflate(
            R.layout.item_course_card, binding.courseFragmentCatainer, false
        )

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

        gotoBtn.setOnClickListener {
            if (lat != null && lng != null) startGoogleNavigation(requireContext(), lat, lng)
            true
        }
        closeBtn.setOnClickListener { hideFloatingCard() }
        detailBtn.setOnClickListener { showDetailCard(marker, course) }

        // 카드만 클릭 가능
        cardView.isClickable = true
        cardView.isFocusableInTouchMode = true
        cardView.bringToFront()

        container.addView(cardView)
        container.post {
            cardView.post { positionCardNearMarkerWithConstraints(cardView, marker) }
        }
        container.visibility = View.VISIBLE
    }

    /**
     * 마커 위/아래 자동 전환 + 화면 경계 클램프.
     * - 상단 여백 부족하면 아래에 배치
     * - 좌우/상하를 컨테이너 경계 내로 제한
     */
    private fun positionCardNearMarkerWithConstraints(cardView: View, marker: Marker) {
        val parent = binding.floatingCardContainer

        // 아직 측정 전이면 다음 프레임에 재시도
        if (parent.width == 0 || parent.height == 0 || cardView.width == 0 || cardView.height == 0) {
            cardView.post { positionCardNearMarkerWithConstraints(cardView, marker) }
            return
        }

        val projection = googleMap.projection
        val screen = projection.toScreenLocation(marker.position)

        val pW = parent.width.toFloat()
        val pH = parent.height.toFloat()
        val cW = cardView.width.toFloat()
        val cH = cardView.height.toFloat()

        val margin = 30f
        val sideMargin = 16.dp().toFloat()
        val safeTop = 56.dp().toFloat()

        var x = screen.x - cW / 2f
        val yAbove = screen.y - cH - margin
        val placeBelow = yAbove < safeTop
        val yTarget = if (placeBelow) screen.y + margin else yAbove

        // 경계 계산
        val minX = sideMargin
        val maxX = pW - cW - sideMargin
        val minY = safeTop
        val maxY = pH - cH - sideMargin

        // 빈 구간 방지: max < min 이면 중앙/안전 위치로 대체
        x = if (maxX >= minX) x.coerceIn(minX, maxX) else (pW - cW) / 2f
        val y = if (maxY >= minY) yTarget.coerceIn(minY, maxY) else max(safeTop, (pH - cH) / 2f)

        cardView.x = x
        cardView.y = y
    }


    private fun placeDetailTopCenter(card: View) {
        val lp = (card.layoutParams as FrameLayout.LayoutParams).apply {
            width = FrameLayout.LayoutParams.MATCH_PARENT
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            val m = 16.dp()
            setMargins(m, m, m, 0)
        }
        card.layoutParams = lp
    }

    private fun showDetailCard(marker: Marker, course: AssetCourseData) {
        binding.floatingCardContainer.apply {
            // 상세 카드도 컨테이너는 터치 통과
            isClickable = false
            isFocusable = false
            visibility = View.VISIBLE
        }
        showInFloating<View>(R.layout.item_course_detail_card) { detail ->
            detail.findViewById<View>(R.id.btnClose).setOnClickListener { hideFloatingCard() }
            val lat = course.latitude?.toDoubleOrNull()
            val lng = course.longitude?.toDoubleOrNull()

            detail.findViewById<View>(R.id.btnGotoCourse).setOnClickListener {
                if (lat != null && lng != null) startGoogleNavigation(requireContext(), lat, lng)
                true
            }
            val tvTitle = detail.findViewById<TextView>(R.id.tvTitle)
            val tvDuration = detail.findViewById<TextView>(R.id.tvDuration)
            val chipLevel = detail.findViewById<TextView>(R.id.chipLevel)
            val tvDistance = detail.findViewById<TextView>(R.id.tvDistance)
            val tvStart = detail.findViewById<TextView>(R.id.tvStart)
            val tvEnd = detail.findViewById<TextView>(R.id.tvEnd)
            val tvRoute = detail.findViewById<TextView>(R.id.tvRoute)
            val tvPhone = detail.findViewById<TextView>(R.id.tvPhone)
            val tvOrg = detail.findViewById<TextView>(R.id.tvOrg)

            fun TextView.setTextOrGone(value: String?, prefix: String = "") {
                if (value.isNullOrBlank()) {
                    visibility = View.GONE
                } else {
                    text = prefix + value
                    visibility = View.VISIBLE
                }
            }

            tvTitle.setTextOrGone(course.name)
            tvDistance.setTextOrGone(course.length?.let { "  ·  $it" })
            tvDuration.setTextOrGone(course.time)
            chipLevel.text = "보통"

            val startText = course.startName ?: course.startRoadAddress ?: course.startJibunAddress
            tvStart.setTextOrGone(startText, "시작: ")
            val endText = course.endName ?: course.endRoadAddress ?: course.endJibunAddress
            tvEnd.setTextOrGone(endText, "도착: ")
            tvRoute.setTextOrGone(course.route?.replace("->", "→")?.trim(), "경로: ")
            tvPhone.setTextOrGone(course.agencyPhone)
            tvOrg.setTextOrGone(course.agencyName ?: course.orgName)

            placeDetailTopCenter(detail)
        }
    }

    private fun <T : View> showInFloating(@LayoutRes layoutRes: Int, binder: (T) -> Unit) {
        val container = binding.floatingCardContainer
        container.removeAllViews()
        @Suppress("UNCHECKED_CAST")
        val view = layoutInflater.inflate(layoutRes, container, false) as T

        // 상세 카드 자체만 클릭 가능
        view.isClickable = true
        view.isFocusableInTouchMode = true
        view.bringToFront()

        container.addView(view)
        container.visibility = View.VISIBLE
        binder(view)
    }

    // ------------------- 권한/위치 -------------------

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showCurrentLocation()
                rescanCoursesForVisibleRegion(force = true)
            } else {
                Toast.makeText(requireContext(), "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCurrentLocation(onSuccess: (Location) -> Unit, onFailure: () -> Unit) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) onSuccess(location) else onFailure()
                }
                .addOnFailureListener { onFailure() }
        } else {
            onFailure()
        }
    }

    private fun showCurrentLocation() {
        getCurrentLocation(
            onSuccess = { location ->
                val currentLatLng = LatLng(location.latitude, location.longitude)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 13f))
                try {
                    googleMap.isMyLocationEnabled = true
                } catch (e: SecurityException) {
                    Log.e("MapFragment", "위치 권한 없음: ${e.message}")
                }
            },
            onFailure = {
                Toast.makeText(requireContext(), "위치를 가져올 수 없습니다", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ------------------- 네비/트래킹 -------------------

    private fun startGoogleNavigation(context: Context, lat: Double, lng: Double) {
        val uri = Uri.parse("google.navigation:q=$lat,$lng&mode=w")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "구글 지도 앱이 설치되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startTracking() {
        getCurrentLocation(
            onSuccess = { location ->
                val p = LatLng(location.latitude, location.longitude)
                walkStartMillis = System.currentTimeMillis()
                runningPath.clear()
                runningPolyline?.remove()
                runningPolyline = googleMap.addPolyline(
                    PolylineOptions()
                        .color(ContextCompat.getColor(requireContext(), R.color.softSkyBlue))
                        .width(7f)
                )
                Toast.makeText(context, "산책을 시작합니다!", Toast.LENGTH_SHORT).show()
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(p, 16f))
            },
            onFailure = {
                Toast.makeText(requireContext(), "위치 정보를 사용할 수 없습니다", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun onStopTracking() {
        val distanceMeters = computeTotalDistanceMeters(runningPath)
        val distanceM = distanceMeters.roundToInt()
        val endedAt = Timestamp.now()
        val startedAtTs = walkStartMillis?.let { Timestamp(Date(it)) } ?: Timestamp.now()
        val durationSec = walkStartMillis?.let { ((System.currentTimeMillis() - it) / 1000L).toInt() } ?: 0

        val start = runningPath.firstOrNull()
        val end = runningPath.lastOrNull()
        val routePolyline = if (runningPath.size >= 2) PolyUtil.encode(runningPath) else null
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
            note = null
        )

        WalkRepository.addWalkEntry(walk,
            onSuccess = { id ->
                val kmText = String.format(Locale.getDefault(), "%.2f km", distanceMeters / 1000.0)
                Toast.makeText(requireContext(), "트래킹 종료 - 총 거리: $kmText", Toast.LENGTH_LONG).show()
                Log.d("WalkDebug", "저장 성공: $id")
                end?.let {
                    googleMap.addMarker(MarkerOptions().position(it).title("종료 지점"))
                }
            },
            onError = { e -> Log.e("WalkDebug", "저장 실패", e) }
        )

        if (runningPath.isNotEmpty()) {
            val last = runningPath.last()
            googleMap.addMarker(MarkerOptions().position(last).title("종료 지점"))
        }
    }

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
        val fakePath = listOf(
            LatLng(37.5830, 127.0005),
            LatLng(37.5815, 127.0025),
            LatLng(37.5790, 127.0040)
        )
        val encodedPolyline = PolyUtil.encode(fakePath)
        val dummyWalk = WalkData(
            id = null,
            startedAt = Timestamp(Date(System.currentTimeMillis() - 2700_000)),
            endedAt = Timestamp(Date(System.currentTimeMillis() - 900_000)),
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

        WalkRepository.addWalkEntry(dummyWalk,
            onSuccess = { id -> Log.e("WalkDebug", "임의 데이터 저장 성공: $id") },
            onError = { e -> Log.e("WalkDebug", "임의 데이터 저장 실패", e) }
        )
    }

    // ------------------- 라이프사이클 -------------------

    override fun onPause() {
        super.onPause()
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        toolbar.findViewWithTag<View>("mapTitleView")?.let { toolbar.removeView(it) }
    }

    override fun onResume() {
        super.onResume()
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        val titleView = LayoutInflater.from(context).inflate(R.layout.toolbar_custom, toolbar, false) as TextView
        titleView.text = "지도"
        titleView.tag = "mapTitleView"
        toolbar.addView(titleView)
    }

    // ------------------- 기타 -------------------

    private fun loadCoursesFromAsset(context: Context): List<AssetCourseData> {
        val json = context.assets.open("walking_courses.json").bufferedReader().use { it.readText() }
        val wrapper = Gson().fromJson(json, CourseWrapper::class.java)
        return wrapper.records
    }
}
