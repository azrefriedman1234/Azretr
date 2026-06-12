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

    private val regions = mapOf(
        "iran" to listOf("איראן", "טהרן", "iran", "tehran", "طهران", "إيران", "ايران"),
        "yemen" to listOf("תימן", "צנעא", "yemen", "sanaa", "صنعاء", "اليمن", "يمن"),
        "syria" to listOf("סוריה", "דמשק", "syria", "damascus", "سوريا", "دمشق"),
        "iraq" to listOf("עיראק", "בגדד", "iraq", "baghdad", "العراق", "بغداد"),
        "israel" to listOf("ישראל", "נתבג", "תל אביב", "חיפה", "israel", "tel aviv", "ben gurion", "تل أبيب", "اسرائيل", "إسرائيل"),
        "gulf" to listOf("המפרץ", "קטאר", "בחריין", "אמירויות", "דובאי", "qatar", "bahrain", "uae", "dubai", "الخليج", "قطر", "دبي")
    )

    fun updateFromMessages(context: Context, messages: List<TdApi.Message>) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        resetIfNewDay(prefs)

        val seen = prefs.getStringSet(SEEN_KEY, emptySet())?.toMutableSet() ?: mutableSetOf()
        val edit = prefs.edit()
        var changed = FalseFlag()

        for (msg in messages.take(80)) {
            val id = "${msg.chatId}_${msg.id}"
            if (seen.contains(id)) continue

            val text = extractText(msg).lowercase(Locale.getDefault())
            if (text.isBlank()) continue

            val matched = regions.filterValues { keys ->
                keys.any { text.contains(it.lowercase(Locale.getDefault())) }
            }.keys

            if (matched.isNotEmpty()) {
                seen.add(id)
                for (region in matched) {
                    val old = prefs.getInt("count_$region", 0)
                    edit.putInt("count_$region", old + 1)
                }
                changed.value = true
            }
        }

        if (changed.value) {
            edit.putStringSet(SEEN_KEY, seen.takeLastSafe(600).toSet())
            edit.apply()
        }
    }

    fun getCounts(context: Context): Map<String, Int> {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        resetIfNewDay(prefs)
        return mapOf(
            "iran" to prefs.getInt("count_iran", 0),
            "yemen" to prefs.getInt("count_yemen", 0),
            "syria" to prefs.getInt("count_syria", 0),
            "iraq" to prefs.getInt("count_iraq", 0),
            "israel" to prefs.getInt("count_israel", 0),
            "gulf" to prefs.getInt("count_gulf", 0)
        )
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

    private fun <T> Collection<T>.takeLastSafe(n: Int): List<T> {
        val list = this.toList()
        return if (list.size <= n) list else list.takeLast(n)
    }

    private class FalseFlag(var value: Boolean = false)
}
