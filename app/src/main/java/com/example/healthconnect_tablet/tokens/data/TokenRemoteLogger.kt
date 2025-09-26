package com.example.healthconnect_tablet.tokens.data

import com.example.healthconnect_tablet.tokens.storage.PrefsStorage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar

/**
 * Helper for mirroring token scan history to Firestore.
 */
object TokenRemoteLogger {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun saveScan(familyName: String, scan: PrefsStorage.ScanRecord) {
        firestore.collection(familyName)
            .document(scan.userId)
            .collection("tokenScans")
            .document(scan.id)
            .set(
                mapOf(
                    "id" to scan.id,
                    "userId" to scan.userId,
                    "type" to scan.type,
                    "category" to scan.category,
                    "mood" to scan.mood,
                    "amount" to scan.amount,
                    "timestamp" to scan.timestamp
                )
            )
            .await()
    }

    suspend fun clearUserScans(familyName: String, userId: String) {
        val scans = firestore.collection(familyName)
            .document(userId)
            .collection("tokenScans")
            .get()
            .await()
        val batch = firestore.batch()
        scans.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    suspend fun clearTodayScans(familyName: String, userId: String) {
        val todayRange = todayRange()
        val scans = firestore.collection(familyName)
            .document(userId)
            .collection("tokenScans")
            .whereGreaterThanOrEqualTo("timestamp", todayRange.first)
            .whereLessThanOrEqualTo("timestamp", todayRange.second)
            .get()
            .await()
        val batch = firestore.batch()
        scans.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    private fun todayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        val end = start + 24L * 60L * 60L * 1000L - 1L
        return start to end
    }
}
