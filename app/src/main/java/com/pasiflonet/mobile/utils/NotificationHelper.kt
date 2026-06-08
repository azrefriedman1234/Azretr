package com.pasiflonet.mobile.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationHelper {
    const val CHANNEL_MESSAGES = "azretr_messages"
    const val CHANNEL_DOWNLOADS = "azretr_downloads"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val messages = NotificationChannel(
            CHANNEL_MESSAGES,
            "הודעות חדשות",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "התראות על הודעות חדשות באפליקציה"
        }

        val downloads = NotificationChannel(
            CHANNEL_DOWNLOADS,
            "הורדות מדיה",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "התראות על הורדות ושמירת קבצים"
        }

        manager.createNotificationChannel(messages)
        manager.createNotificationChannel(downloads)
    }
}
