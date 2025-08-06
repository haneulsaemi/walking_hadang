package com.example.walking_hadang.util

import android.content.Context
import android.location.Geocoder
import android.location.Location
import com.example.walking_hadang.data.AssetCourseData
import com.google.android.gms.maps.model.LatLng
import java.io.IOException
import java.util.Locale

object LocationUtil {

    fun getLatLngFromAddress(context: Context, address: String): LatLng? {
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val results = geocoder.getFromLocationName(address, 1)
            if (!results.isNullOrEmpty()) {
                LatLng(results[0].latitude, results[0].longitude)
            } else null
        } catch (e: IOException) {
            null
        }
    }

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val start = Location("").apply {
            latitude = lat1
            longitude = lon1
        }
        val end = Location("").apply {
            latitude = lat2
            longitude = lon2
        }
        return start.distanceTo(end).toDouble() // in meters
    }

    fun isWithinRadius(center: LatLng, target: LatLng, radiusMeters: Double): Boolean {
        return calculateDistance(
            center.latitude, center.longitude,
            target.latitude, target.longitude
        ) <= radiusMeters
    }

    fun filterCoursesWithinRadius(
        courses: List<AssetCourseData>,
        currentLatitude: Double,
        currentLongitude: Double,
        radiusInKm: Double = 10.0
    ): List<AssetCourseData> {
        val currentLocation = Location("current").apply {
            latitude = currentLatitude
            longitude = currentLongitude
        }

        return courses.filter { course ->
            val lat = course.latitude?.toDoubleOrNull()
            val lon = course.longitude?.toDoubleOrNull()

            if (lat != null && lon != null) {
                val courseLocation = Location("course").apply {
                    latitude = lat
                    longitude = lon
                }
                val distance = currentLocation.distanceTo(courseLocation) / 100  // meters to km
                distance <= radiusInKm
            } else {
                false
            }
        }
    }
}