package com.example.healthconnect_tablet.tokens.nfc

import android.content.Context
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import com.example.healthconnect_tablet.tokens.storage.PrefsStorage
import com.example.healthconnect_tablet.tokens.storage.PrefsStorage.ScanRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.nio.charset.Charset

/**
 * Parses NFC tag payloads, enforces per-category limits, and persists scans.
 */
object TokenScanService {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    @JvmStatic
    fun processOnceWithLimits(
        context: Context,
        userId: String,
        familyName: String?,
        message: NdefMessage,
        stepsLimit: Int,
        sleepLimit: Int,
        heartLimit: Int,
        remoteLogger: (ScanRecord) -> Unit
    ): String? {
        val records = message.records ?: return null
        for (record in records) {
            val json = extractJson(record) ?: continue
            val parsed = parseToken(json) ?: continue

            if (parsed.type == "automated" && parsed.category != null) {
                val current = PrefsStorage.getTodayAutomatedCount(context, userId, parsed.category)
                val limit = when (parsed.category) {
                    "steps" -> stepsLimit
                    "sleep" -> sleepLimit
                    "heart" -> heartLimit
                    else -> 0
                }
                if (limit <= 0) {
                    return "No ${parsed.category} tokens available for today."
                }
                if (current + parsed.amount > limit) {
                    return "No ${parsed.category} tokens remaining for today."
                }
            }

            val scan = PrefsStorage.saveScan(
                context = context.applicationContext,
                userId = userId,
                type = parsed.type,
                category = parsed.category,
                mood = parsed.mood,
                amount = parsed.amount,
                timestamp = System.currentTimeMillis()
            )

            if (!familyName.isNullOrBlank()) {
                ioScope.launch {
                    try {
                        remoteLogger(scan)
                    } catch (_: Exception) {
                        // Remote logging failures should not block the UX.
                    }
                }
            }

            return when (parsed.type) {
                "automated" -> "Saved: ${parsed.amount} ${parsed.category} token(s)."
                "mood" -> "Saved mood: ${parsed.mood}."
                else -> "Scan saved."
            }
        }
        return null
    }

    private fun extractJson(record: NdefRecord): String? = try {
        when {
            record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT) -> {
                val payload = record.payload
                val isUtf8 = payload[0].toInt() and 0x80 == 0
                val langLen = payload[0].toInt() and 0x3F
                val charset = if (isUtf8) Charsets.UTF_8 else Charsets.UTF_16
                String(payload, 1 + langLen, payload.size - 1 - langLen, charset)
            }
            record.tnf == NdefRecord.TNF_MIME_MEDIA && String(record.type) == "application/json" -> {
                String(record.payload, Charset.forName("UTF-8"))
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }

    private data class ParsedToken(
        val type: String,
        val category: String?,
        val mood: String?,
        val amount: Int
    )

    private fun parseToken(jsonString: String): ParsedToken? {
        val obj = JSONObject(jsonString)
        val type = obj.optString("type", "")
        return when (type) {
            "automated" -> {
                val category = obj.optString("category", null)
                val amount = obj.optInt("amount", 0).coerceAtLeast(0)
                if (category in listOf("steps", "sleep", "heart") && amount > 0) {
                    ParsedToken(type, category, null, amount)
                } else {
                    null
                }
            }
            "mood" -> {
                val mood = obj.optString("mood", null)
                if (!mood.isNullOrBlank()) {
                    ParsedToken(type, null, mood, 1)
                } else {
                    null
                }
            }
            else -> null
        }
    }
}
