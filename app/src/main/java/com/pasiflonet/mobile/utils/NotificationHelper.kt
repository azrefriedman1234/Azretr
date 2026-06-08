package com.pasiflonet.mobile.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pasiflonet.mobile.MainActivity
import com.pasiflonet.mobile.R

object NotificationHelper {
    const val CHANNEL_MESSAGES = "azretr_messages"
    const val CHANNEL_MEDIA = "azretr_media"
    const val CHANNEL_BACKGROUND = "azretr_background"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channels = listOf(
            NotificationChannel(
                CHANNEL_MESSAGES,
                "הודעות חדשות",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "התראות על הודעות ועדכונים חדשים" },
            NotificationChannel(
                CHANNEL_MEDIA,
                "מדיה והורדות",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "התראות על הורדת קבצים, וידאו ואודיו" },
            NotificationChannel(
                CHANNEL_BACKGROUND,
                "פעולות רקע",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "התראות שקטות על פעולות שהאפליקציה עושה ברקע" }
        )

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(channels)
    }

    fun showSimpleNotification(
        context: Context,
        title: String,
        message: String,
        channelId: String = CHANNEL_MESSAGES,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
