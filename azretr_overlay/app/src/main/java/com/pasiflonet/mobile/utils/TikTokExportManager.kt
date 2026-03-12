package com.pasiflonet.mobile.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TikTokExportManager {
    private const val RELATIVE_PATH = "Download/Pasiflonet/TikTokExport/"

    fun exportVideoPackage(
        context: Context,
        processedVideoPath: String,
        title: String,
        caption: String,
        cookiesUriString: String
    ): Boolean {
        return try {
            val videoFile = File(processedVideoPath)
            if (!videoFile.exists() || title.isBlank()) return false

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val baseName = "azretr_tiktok_$timestamp"
            val cookiesBytes = openInputStreamAny(context, cookiesUriString)?.use { it.readBytes() } ?: return false

            saveToDownloads(context, "$baseName.mp4", "video/mp4", FileInputStream(videoFile))
            saveToDownloads(context, "$baseName.cookies.txt", "text/plain", cookiesBytes.inputStream())

            val payload = JSONObject().apply {
                put("title", title)
                put("caption", caption)
                put("video_file", "$baseName.mp4")
                put("cookies_file", "$baseName.cookies.txt")
                put("created_at", timestamp)
                put("source", "Azretr mobile export package")
            }
            saveToDownloads(context, "$baseName.json", "application/json", payload.toString(2).byteInputStream())
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun openInputStreamAny(context: Context, uriString: String): InputStream? {
        return try {
            val uri = Uri.parse(uriString)
            when (uri.scheme) {
                ContentResolver.SCHEME_CONTENT -> context.contentResolver.openInputStream(uri)
                ContentResolver.SCHEME_FILE, null -> FileInputStream(File(uri.path ?: uriString))
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun saveToDownloads(context: Context, displayName: String, mimeType: String, input: InputStream) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values)
                ?: throw IllegalStateException("Failed to create download entry")
            resolver.openOutputStream(uri, "w")?.use { output ->
                input.use { source -> source.copyTo(output) }
            } ?: throw IllegalStateException("Failed to open output stream")
            resolver.update(uri, ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }, null, null)
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Pasiflonet/TikTokExport")
            if (!dir.exists()) dir.mkdirs()
            val outFile = File(dir, displayName)
            outFile.outputStream().use { output ->
                input.use { source -> source.copyTo(output) }
            }
        }
    }
}
