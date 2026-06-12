package com.pasiflonet.mobile.utils

import android.content.Context
import org.drinkless.tdlib.TdApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CyberAlertCounter {
    private const val PREF = "cyber_alert_counters"
    private const val DATE_KEY = "counter_date"
    private const val SEEN_KEY = "seen_ids"

    private val regions: Map<String, List<String>> = mapOf(
        "iran" to listOf("איראן", "טהרן", "iran", "tehran", "طهران", "إيران", "ايران"),
        "yemen" to listOf("תימן", "צנעא", "yemen", "sanaa", "صنعاء", "اليمن", "يمن"),
        "syria" to listOf("סוריה", "דמשק", "syria", "damascus", "سوريا", "دمشق"),
        "iraq" to listOf("עיראק", "בגדד", "iraq", "baghdad", "العراق", "بغداد"),
        "israel" to listOf("ישראל", "נתבג", "תל אביב", "חיפה", "israel", "tel aviv", "ben gurion", "تل أبيب", "اسرائيل", "إسرائيل"),
        "gulf" to listOf("המפרץ", "קטאר", "בחריין", "אמירויות", "דובאי", "qatar", "bahrain", "uae", "dubai", "الخليج", "قطر", "دبي")
    )

    fun updateFromMessages(context: Context, messages: List<TdApi.Message>) {
        try {
            val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            resetIfNewDay(prefs)

            val seenOld = prefs.getStringSet(SEEN_KEY, emptySet()) ?: emptySet()
            val seen = seenOld.toMutableSet()
            val edit = prefs.edit()
            var changed = false

            val limited = if (messages.size > 80) messages.take(80) else messages

            for (msg in limited) {
                val id = msg.chatId.toString() + "_" + msg.id.toString()
                if (seen.contains(id)) continue

                val text = extractText(msg).lowercase(Locale.getDefault())
                if (text.isBlank()) continue

                val matchedRegions = mutableListOf<String>()

                for ((region, words) in regions) {
                    for (word in words) {
                        if (text.contains(word.lowercase(Locale.getDefault()))) {
                            matchedRegions.add(region)
                            break
                        }
                    }
                }

                if (matchedRegions.isNotEmpty()) {
                    seen.add(id)
                    for (region in matchedRegions) {
                        val oldCount = prefs.getInt("count_$region", 0)
                        edit.putInt("count_$region", oldCount + 1)
                    }
                    changed = true
                }
            }

            if (changed) {
                val trimmed = seen.toList().takeLast(600).toSet()
                edit.putStringSet(SEEN_KEY, trimmed)
                edit.apply()
            }
        } catch (_: Exception) {
        }
    }

    fun getCounts(context: Context): Map<String, Int> {
        return try {
            val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            resetIfNewDay(prefs)
            mapOf(
                "iran" to prefs.getInt("count_iran", 0),
                "yemen" to prefs.getInt("count_yemen", 0),
                "syria" to prefs.getInt("count_syria", 0),
                "iraq" to prefs.getInt("count_iraq", 0),
                "israel" to prefs.getInt("count_israel", 0),
                "gulf" to prefs.getInt("count_gulf", 0)
            )
        } catch (_: Exception) {
            mapOf(
                "iran" to 0,
                "yemen" to 0,
                "syria" to 0,
                "iraq" to 0,
                "israel" to 0,
                "gulf" to 0
            )
        }
    }

    private fun resetIfNewDay(prefs: android.content.SharedPreferences) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val saved = prefs.getString(DATE_KEY, "")

        if (saved != today) {
            prefs.edit()
                .putString(DATE_KEY, today)
                .putStringSet(SEEN_KEY, emptySet())
                .putInt("count_iran", 0)
                .putInt("count_yemen", 0)
                .putInt("count_syria", 0)
                .putInt("count_iraq", 0)
                .putInt("count_israel", 0)
                .putInt("count_gulf", 0)
                .apply()
        }
    }

    private fun extractText(message: TdApi.Message): String {
        return when (val c = message.content) {
            is TdApi.MessageText -> c.text.text
            is TdApi.MessagePhoto -> c.caption.text
            is TdApi.MessageVideo -> c.caption.text
            is TdApi.MessageAnimation -> c.caption.text
            is TdApi.MessageDocument -> c.caption.text
            is TdApi.MessageAudio -> c.caption.text
            is TdApi.MessageVoiceNote -> c.caption.text
            else -> ""
        }
    }
}
