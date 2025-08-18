package com.example.walking_hadang.util

import com.example.walking_hadang.MyApplication
import com.example.walking_hadang.data.WalkData
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObjects

object WalkRepository {

    private fun uid(): String = MyApplication.auth.currentUser?.uid
        ?: throw IllegalStateException("로그인 필요")

    fun dateKeyFrom(ts: com.google.firebase.Timestamp): String {
        val millis = ts.seconds * 1000L + ts.nanoseconds / 1_000_000L
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.KOREA)
        sdf.timeZone = java.util.TimeZone.getDefault()
        return sdf.format(java.util.Date(millis))
    }

    /** entries 콜렉션 레퍼런스 */
    private fun entriesColl(dateKey: String) =
        MyApplication.db.collection("users")
            .document(uid())
            .collection("walks")
            .document(dateKey)
            .collection("walkEntries")

    /** 생성 */
    fun addWalkEntry(
        raw: WalkData,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val dk = if (!raw.dateKey.isNullOrBlank()) raw.dateKey!!
        else dateKeyFrom(raw.startedAt)

        val docRef = entriesColl(dk).document()
        val toSave = raw.copy(
            id = docRef.id,
            userId = uid(),
            dateKey = dk
        )

        docRef.set(toSave)
            .addOnSuccessListener { onSuccess(docRef.id) }
            .addOnFailureListener { onError(it) }
    }

    /** 특정 날짜의 모든 entries 읽기 (최근순 기본) */
    fun loadWalksByDate(
        dateKey: String,
        limit: Long? = null,
        descending: Boolean = true,
        onSuccess: (List<WalkData>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        var q: Query = entriesColl(dateKey)
            .orderBy(
                "startedAt",
                if (descending) Query.Direction.DESCENDING else Query.Direction.ASCENDING
            )

        if (limit != null) q = q.limit(limit)

        q.get()
            .addOnSuccessListener { snap -> onSuccess(snap.toObjects()) }
            .addOnFailureListener { onError(it) }
    }

    /**
     * 여러 날짜에 걸친 최신 기록 N개 모아오기.
     * 날짜 문서(walks/{dateKey})는 보통 필드가 없고, 문서ID 자체가 dateKey이므로
     * **문서ID 기준 정렬**로 최근 날짜부터 훑어온다.
     */
    fun loadLatestWalks(
        pageSize: Int = 20,
        onSuccess: (List<WalkData>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        MyApplication.db.collectionGroup("walkEntries")
            .whereEqualTo("userId", uid())
            .orderBy("startedAt", Query.Direction.DESCENDING)
            .limit(pageSize.toLong())
            .get()
            .addOnSuccessListener { snap -> onSuccess(snap.toObjects()) }
            .addOnFailureListener { onError(it) }
    }
}