package com.pasiflonet.mobile.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import org.drinkless.tdlib.TdApi
import java.util.Locale

object KeywordNotificationHelper {
    private const val CHANNEL_ID = "azretr_keyword_alerts"

    fun notifyIfMatches(context: Context, message: TdApi.Message) {
        try {
            if (message.isOutgoing) return

            val now = System.currentTimeMillis() / 1000L
            if (message.date > 0 && now - message.date > 180) return

            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val raw = prefs.getString("alert_keywords", "") ?: ""
            val keywords = raw
                .split(",", "\n", ";", "|")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            if (keywords.isEmpty()) return

            val text = extractText(message)
            if (text.isBlank()) return

            val lower = text.lowercase(Locale.getDefault())
            val matched = keywords.firstOrNull { lower.contains(it.lowercase(Locale.getDefault())) } ?: return

            createChannel(context)

            val shortText = if (text.length > 180) text.take(180) + "..." else text
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, CHANNEL_ID)
            } else {
                Notification.Builder(context)
                    .setPriority(Notification.PRIORITY_HIGH)
            }

            val notification = builder
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Azretr: נמצאה מילת מפתח")
                .setContentText("$matched — $shortText")
                .setStyle(Notification.BigTextStyle().bigText(shortText))
                .setAutoCancel(true)
                .build()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify((message.chatId xor message.id).toInt(), notification)
        } catch (_: Exception) {
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "התראות לפי מילות מפתח",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "התראה כשנכנסת הודעה שמכילה מילת מפתח שהוגדרה"
        }

        manager.createNotificationChannel(channel)
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
