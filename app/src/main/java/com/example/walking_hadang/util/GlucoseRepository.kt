package com.example.walking_hadang.util

import android.util.Log
import com.example.walking_hadang.MyApplication
import com.example.walking_hadang.data.GlucoseData
import com.example.walking_hadang.data.GlucoseType
import com.example.walking_hadang.data.MealType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object GlucoseRepository {

    private fun uid(): String = MyApplication.auth.currentUser?.uid
        ?: throw IllegalStateException("로그인 필요")
    private fun mealsColl() =
        MyApplication.db.collection("users")
            .document(GlucoseRepository.uid())
            .collection("glucose")

    private fun dayDoc(dateKey: String) =
        mealsColl().document(dateKey)

    private fun entriesColl(dateKey: String) =
        dayDoc(dateKey).collection("glucoseEntries")

    fun addGlucoseEntry(
        raw: GlucoseData,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        // recordedAt 기준으로 dateKey가 비어있으면 채워줌
        val dateKey = if (raw.dateKey.isNotBlank()) raw.dateKey
        else dateKeyFrom(raw.recordedAt)// yyyy-MM-dd

        // 유효성 (식후라면 끼니/분 필수)
        if (raw.type == GlucoseType.POSTPRANDIAL) {
            requireNotNull(raw.meal) { "식후 측정은 meal이 필요합니다." }
            require(raw.postprandialMinutes != null && raw.postprandialMinutes > 0) {
                "식후 측정은 postprandialMinutes(예: 120)가 필요합니다."
            }
        }

        val coll = MyApplication.db.collection("users")
            .document(uid())
            .collection("glucose")
            .document(dateKey)
            .collection("glucoseEntries")

        val docRef = coll.document()
        val toSave = raw.copy(
            id = docRef.id,
            userId = uid(),
            dateKey = dateKey
        )

        docRef.set(toSave)
            .addOnSuccessListener { onSuccess(docRef.id) }
            .addOnFailureListener { onError(it) }
    }

    suspend fun getDaily(date: LocalDate = LocalDate.now()): List<GlucoseData> {
        val key = dateKeyFrom(date)
        Log.d("GluDbg", "read path users/${uid()}/glucose/${key}/glucoseEntries")
        return MyApplication.db.collection("users").document(uid())
            .collection("glucose").document(key)
            .collection("glucoseEntries")
            .orderBy("recordedAt", Query.Direction.ASCENDING)
            .get().await()
            .toObjects(GlucoseData::class.java)
    }

    suspend fun getWeeklyByType(
        type: GlucoseType,
        weekOf: Date = Date(),
        meal: MealType? = null
    ): List<GlucoseData> {

        val (start, endExclusive) = weekRange(weekOf)

        val startTs = Timestamp(start.time / 1000, 0)
        val endTs   = Timestamp(endExclusive.time / 1000, 0)

        var q = MyApplication.db.collectionGroup("glucoseEntries")
            .whereEqualTo("userId", uid())
            .whereGreaterThanOrEqualTo("recordedAt", startTs)
            .whereLessThan("recordedAt", endTs)
            .whereEqualTo("type", type.name)
            .orderBy("recordedAt", Query.Direction.ASCENDING)

        if (type == GlucoseType.POSTPRANDIAL && meal != null) {
            q = q.whereEqualTo("meal", meal.name)
        }

        // ⚠️ 최초 실행 시 콘솔에서 복합 인덱스 생성 요구할 수 있음 (안내 링크 클릭)
        return q.get().await().toObjects(GlucoseData::class.java)
    }
    // 오늘 하루 가장 최근 혈당 기록
    suspend fun getTodayLatest(): GlucoseData? {

        // 오늘 yyyy-MM-dd
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.KOREA)
        val todayKey = sdf.format(java.util.Date())

        return try {
            val snap = MyApplication.db.collection("users")
                .document(uid())
                .collection("glucose")
                .document(todayKey)
                .collection("glucoseEntries")
                .orderBy("recordedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            snap.documents.firstOrNull()?.toObject(GlucoseData::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Timestamp → yyyy-MM-dd (로컬 TZ) */
    private fun dateKeyFrom(ts: Timestamp, zone: TimeZone = TimeZone.getDefault()): String {
        val millis = ts.seconds * 1000L + ts.nanoseconds / 1_000_000L
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).apply {
            timeZone = zone   // 사용자의 로컬 타임존
        }
        return sdf.format(Date(millis))
    }

    /** Date → yyyy-MM-dd (로컬 TZ) */
    private fun dateKeyFrom(date: Date, zone: TimeZone = TimeZone.getDefault()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).apply { timeZone = zone }
        return sdf.format(date)
    }
    private fun dateKeyFrom(localDate: LocalDate, zone: ZoneId = ZoneId.systemDefault()): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withLocale(Locale.KOREA)
        return localDate.format(formatter)
    }


    /** 주의 시작/끝(월요일 시작 기준). end는 다음 주 시작 직전(=exclusive) */
    private fun weekRange(of: Date, zone: TimeZone = TimeZone.getDefault()): Pair<Date, Date> {
        val cal = Calendar.getInstance(zone).apply {
            time = of
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        val start = cal.time
        cal.add(Calendar.DAY_OF_MONTH, 7)
        val endExclusive = cal.time
        return start to endExclusive
    }

    suspend fun deleteGlucoseEntry(
        dateKey: String,
        entryId: String
    ){
        val docRef = entriesColl(dateKey).document(entryId)
        docRef.delete().await()
    }


}