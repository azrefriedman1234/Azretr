package com.pasiflonet.mobile.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    @JvmStatic
        /**
     * מחזיר נתיב קובץ אמיתי.
     * אם זה content:// -> מעתיק ל-cacheDir ומחזיר נתיב פנימי (זה גם פותר הרשאות + isolated process).
     */
    fun getFilePath(context: android.content.Context, uri: android.net.Uri, onResult: (String) -> Unit = {}): String? {
        return try {
            // file://
            if (uri.scheme == "file") {
                val p = uri.path
                if (p != null && java.io.File(p).exists()) {
                    onResult(p)
                    return p
                }
            }

            // content:// => copy to cache
            val cr = context.contentResolver
            val mime = cr.getType(uri) ?: ""
            val ext = when {
                mime.contains("png") -> "png"
                mime.contains("webp") -> "webp"
                mime.contains("jpeg") or mime.contains("jpg") -> "jpg"
                mime.contains("mp4") -> "mp4"
                else -> "bin"
            }

            val out = java.io.File(context.cacheDir, "import_${System.currentTimeMillis()}.$ext")
            cr.openInputStream(uri)?.use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            } ?: return null

            onResult(out.absolutePath)
            out.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun copyContentToCache(context: Context, uri: Uri): String? = try {
        val name = queryDisplayName(context, uri) ?: ("picked_" + System.currentTimeMillis())
        val outFile = File(context.cacheDir, name)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(outFile).use { output -> input.copyTo(output) }
        } ?: return null
        outFile.absolutePath
    } catch (_: Throwable) { null }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        var c: Cursor? = null
        return try {
            c = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (c != null && c.moveToFirst()) c.getString(0) else null
        } finally { c?.close() }
    }
}
