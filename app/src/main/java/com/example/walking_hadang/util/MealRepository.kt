package com.example.walking_hadang.util

import com.example.walking_hadang.MyApplication
import com.example.walking_hadang.data.MealDayDoc
import com.example.walking_hadang.data.MealEntryDoc
import com.example.walking_hadang.data.WalkData
import com.example.walking_hadang.util.WalkRepository.dateKeyFrom
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

object MealRepository {
    private fun uid(): String = MyApplication.auth.currentUser?.uid
        ?: throw IllegalStateException("로그인 필요")

    private fun mealsColl() =
        MyApplication.db.collection("users")
            .document(uid())
            .collection("meals")

    private fun dayDoc(dateKey: String) =
        mealsColl().document(dateKey)

    private fun entriesColl(dateKey: String) =
        dayDoc(dateKey).collection("mealEntries")

    /** 날짜 카드 리스트 로드 */
    fun loadMealDays(
        limit: Long? = null,
        descending: Boolean = true,
        onSuccess: (List<MealDayDoc>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        var q: Query = mealsColl()
            .orderBy(
                "lastLoggedAt",
                if (descending) Query.Direction.DESCENDING else Query.Direction.ASCENDING
            )
        if (limit != null) q = q.limit(limit)
        q.get()
            .addOnSuccessListener { snap -> onSuccess(snap.toObjects()) }
            .addOnFailureListener { onError(it) }
    }

    /** 특정 날짜의 엔트리(한 끼들) 로드 */
    fun loadMealsByDate(
        dateKey: String,
        limit: Long? = null,
        descending: Boolean = true,
        onSuccess: (List<MealEntryDoc>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        var q: Query = entriesColl(dateKey)
            .orderBy(
                "eatenAt",
                if (descending) Query.Direction.DESCENDING else Query.Direction.ASCENDING
            )
        if (limit != null) q = q.limit(limit)
        q.get()
            .addOnSuccessListener { snap ->
                // entryId를 문서 id로 보정해서 넘겨주면 UI에서 편함
                val list = snap.documents.map { d ->
                    (d.toObject<MealEntryDoc>() ?: MealEntryDoc()).copy(entryId = d.id)
                }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it) }
    }

    /** 엔트리 생성(업서트). 상위 날짜 요약(합계/개수/마지막시간/썸네일)도 같이 갱신 */
    suspend fun upsertMealEntry(
        dateKey: String,
        entry: MealEntryDoc
    ) {
        val uid = uid()
        val now = Timestamp.now()

        // 1) 새 엔트리 문서 준비
        val entryRef = entriesColl(dateKey).document() // 새 문서 id
        val withIds = entry.copy(
            userId = uid,
            entryId = entryRef.id,
            createdAt = entry.createdAt ?: now,
            updatedAt = now,
            totalKcal = entry.totalKcal
                ?: entry.items.mapNotNull { it.kcal }.sum().takeIf { it > 0 }
        )

        // 2) 기존 엔트리들 읽어 집계 계산
        val entriesSnap = entriesColl(dateKey).get().await()
        val kcalSumOld = entriesSnap.documents.sumOf {
            it.toObject(MealEntryDoc::class.java)?.totalKcal ?: 0
        }
        val itemCountOld = entriesSnap.documents.sumOf {
            it.toObject(MealEntryDoc::class.java)?.items?.size ?: 0
        }

        val kcalSumNew = (withIds.totalKcal ?: 0) + kcalSumOld
        val itemCountNew = withIds.items.size + itemCountOld

        // 3) 상위 날짜 문서(요약) 준비
        val firstLoggedAtExisting =
            entriesSnap.documents.minByOrNull {
                it.toObject(MealEntryDoc::class.java)?.eatenAt?.seconds ?: Long.MAX_VALUE
            }?.toObject(MealEntryDoc::class.java)?.eatenAt

        val dayDocData = MealDayDoc(
            userId = uid,
            dateKey = dateKey,
            itemCount = itemCountNew,
            totalKcal = kcalSumNew.takeIf { it > 0 },
            firstLoggedAt = firstLoggedAtExisting ?: withIds.eatenAt ?: now,
            lastLoggedAt = withIds.eatenAt ?: now,
            coverImageUrl = withIds.photoUrl,
            memo = null
        )

        // 4) 커밋
        MyApplication.db.runBatch { b ->
            b.set(entryRef, withIds)
            b.set(dayDoc(dateKey), dayDocData) // upsert
        }.await()
    }

    /** 엔트리 삭제(요약값 재계산 포함) */
    suspend fun deleteMealEntry(
        dateKey: String,
        entryId: String
    ) {
        val docRef = entriesColl(dateKey).document(entryId)
        docRef.delete().await()

        // 삭제 후 집계 재계산
        val entriesSnap = entriesColl(dateKey).get().await()
        val kcalSum = entriesSnap.documents.sumOf {
            it.toObject(MealEntryDoc::class.java)?.totalKcal ?: 0
        }
        val itemCount = entriesSnap.documents.sumOf {
            it.toObject(MealEntryDoc::class.java)?.items?.size ?: 0
        }
        val lastLoggedAt = entriesSnap.documents.maxByOrNull {
            it.toObject(MealEntryDoc::class.java)?.eatenAt?.seconds ?: 0L
        }?.toObject(MealEntryDoc::class.java)?.eatenAt

        val firstLoggedAt = entriesSnap.documents.minByOrNull {
            it.toObject(MealEntryDoc::class.java)?.eatenAt?.seconds ?: Long.MAX_VALUE
        }?.toObject(MealEntryDoc::class.java)?.eatenAt

        if (entriesSnap.isEmpty) {
            // 엔트리가 0개면 날짜 문서 자체 삭제
            dayDoc(dateKey).delete().await()
        } else {
            dayDoc(dateKey).update(
                mapOf(
                    "itemCount" to itemCount,
                    "totalKcal" to (kcalSum.takeIf { it > 0 }),
                    "firstLoggedAt" to firstLoggedAt,
                    "lastLoggedAt" to lastLoggedAt
                )
            ).await()
        }
    }
    fun dateKeyFrom(date: LocalDate): String = date.toString()

}