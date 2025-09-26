package com.example.healthconnect_tablet.tokens.storage

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Simple SharedPreferences-backed storage for NFC token scans.
 */
object PrefsStorage {
    private const val PREFS = "tokens"
    private const val KEY_SCANS_LOG = "scans_log"

    data class ScanRecord(
        val id: String,
        val userId: String,
        val type: String,
        val category: String?,
        val mood: String?,
        val amount: Int,
        val timestamp: Long
    )

    fun saveScan(
        context: Context,
        userId: String,
        type: String,
        category: String?,
        mood: String?,
        amount: Int,
        timestamp: Long
    ): ScanRecord {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(KEY_SCANS_LOG, "[]"))
        val obj = JSONObject().apply {
            put("id", UUID.randomUUID().toString())
            put("userId", userId)
            put("type", type)
            put("category", category)
            put("mood", mood)
            put("amount", amount)
            put("timestamp", timestamp)
        }
        arr.put(obj)
        prefs.edit().putString(KEY_SCANS_LOG, arr.toString()).apply()
        return obj.toScanRecord()
    }

    fun getAllScans(context: Context, userId: String): List<ScanRecord> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(KEY_SCANS_LOG, "[]"))
        val out = mutableListOf<ScanRecord>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.optString("userId") != userId) continue
            out.add(o.toScanRecord())
        }
        return out.sortedByDescending { it.timestamp }
    }

    fun getTodayAutomatedCount(context: Context, userId: String, category: String): Int {
        val all = getAllScans(context, userId)
        val start = startOfDayMillis()
        val end = start + 24L * 60L * 60L * 1000L - 1L
        return all.filter { it.type == "automated" && it.category == category && it.timestamp in start..end }
            .sumOf { it.amount }
    }

    fun clearAllScans(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SCANS_LOG, "[]").apply()
    }

    fun clearUserScans(context: Context, userId: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(KEY_SCANS_LOG, "[]"))
        val filtered = JSONArray()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            if (obj.optString("userId") != userId) filtered.put(obj)
        }
        prefs.edit().putString(KEY_SCANS_LOG, filtered.toString()).apply()
    }

    fun clearTodayScansForUser(context: Context, userId: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(KEY_SCANS_LOG, "[]"))
        val filtered = JSONArray()
        val start = startOfDayMillis()
        val end = start + 24L * 60L * 60L * 1000L - 1L
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val ts = obj.optLong("timestamp", 0L)
            val uid = obj.optString("userId")
            val isTodayForUser = uid == userId && ts in start..end
            if (!isTodayForUser) filtered.put(obj)
        }
        prefs.edit().putString(KEY_SCANS_LOG, filtered.toString()).apply()
    }

    fun formatTimestamp(ts: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))

    private fun startOfDayMillis(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun JSONObject.toScanRecord(): ScanRecord = ScanRecord(
        id = optString("id"),
        userId = optString("userId"),
        type = optString("type"),
        category = if (has("category") && !isNull("category")) optString("category") else null,
        mood = if (has("mood") && !isNull("mood")) optString("mood") else null,
        amount = optInt("amount", 0),
        timestamp = optLong("timestamp", 0L)
    )
}
