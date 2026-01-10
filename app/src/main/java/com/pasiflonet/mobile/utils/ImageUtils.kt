package com.pasiflonet.mobile.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    @JvmStatic
    fun getFilePath(context: Context, uri: Uri, onResult: ((String?) -> Unit)? = null): String? {
        val result = when (uri.scheme) {
            "file" -> uri.path
            "content" -> copyContentToCache(context, uri)
            else -> null
        }
        onResult?.invoke(result)
        return result
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
