package com.pasiflonet.mobile.utils

import android.content.Context
import org.drinkless.tdlib.TdApi
import java.io.File

object CacheManager {

    // יעד: לא לתת ל-temp להתנפח
    private const val TEMP_MAX_BYTES: Long = 800L * 1024L * 1024L  // 800MB
    // יעד: לא לתת ל-TDLib downloads להתנפח בלי סוף (אפשר להקטין/להגדיל)
    private const val TDLIB_FILES_MAX_BYTES: Long = 2L * 1024L * 1024L * 1024L // 2GB

    fun getCacheSize(context: Context): String {
        return try {
            val size = getDirSize(context.cacheDir) + getDirSize(context.externalCacheDir)
            formatSize(size)
        } catch (_: Exception) { "0 MB" }
    }

    /**
     * "ניקוי" אמיתי: cache + externalCache + temp קבצים + הגבלה של tdlib_files.
     * זה לא מוחק את db (tdlib_db) כדי לא לנתק אותך.
     */
    fun clearAppCache(context: Context) {
        try { context.cacheDir?.deleteRecursively() } catch (_: Exception) {}
        try { context.externalCacheDir?.deleteRecursively() } catch (_: Exception) {}

        // לנקות temp leftovers בתוך filesDir (אם קיימים)
        try {
            val files = context.filesDir
            files.listFiles()?.forEach { f ->
                if (f.isFile && (f.name.startsWith("safe_") || f.name.startsWith("proc_") || f.name.startsWith("processed_") || f.name.startsWith("tmp_"))) {
                    try { f.delete() } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}

        // להגביל הורדות TDLib כדי לא להגיע שוב ל-13GB
        try { pruneDirToMaxBytes(File(context.filesDir, "tdlib_files"), TDLIB_FILES_MAX_BYTES) } catch (_: Exception) {}
    }

    /**
     * מוחק קבצי מדיה מקומיים של ההודעה (רק במקומות שמותר):
     * cache/externalCache/tdlib_files
     */
    fun deleteTempForMessage(context: Context, msg: TdApi.Message) {
        val tdlibDir = File(context.filesDir, "tdlib_files")
        val allowed = listOfNotNull(context.cacheDir, context.externalCacheDir, tdlibDir)

        fun canDelete(f: File): Boolean = try {
            val c = f.canonicalFile
            allowed.any { parent -> c.path.startsWith(parent.canonicalFile.path) }
        } catch (_: Exception) { false }

        fun delPath(path: String?) {
            if (path.isNullOrBlank()) return
            try {
                val f = File(path)
                if (f.exists() && f.isFile && canDelete(f)) f.delete()
            } catch (_: Exception) {}
        }

        when (val c = msg.content) {
            is TdApi.MessagePhoto -> c.photo.sizes.forEach { delPath(it.photo.local.path) }
            is TdApi.MessageVideo -> {
                delPath(c.video.video.local.path)
                delPath(c.video.thumbnail?.file?.local?.path)
            }
            is TdApi.MessageAnimation -> {
                delPath(c.animation.animation.local.path)
                delPath(c.animation.thumbnail?.file?.local?.path)
            }
            is TdApi.MessageDocument -> {
                delPath(c.document.document.local.path)
                delPath(c.document.thumbnail?.file?.local?.path)
            }
        }
    }

    /** ניקוי temp לפי **גודל** (לא לפי כמות) */
    fun pruneAppTempFiles(context: Context) {
        try { pruneDirToMaxBytes(context.cacheDir, TEMP_MAX_BYTES) } catch (_: Exception) {}
        try { pruneDirToMaxBytes(context.externalCacheDir, TEMP_MAX_BYTES) } catch (_: Exception) {}
    }

    /** מוחק את הישנים עד שהספרייה יורדת מתחת ל-maxBytes */
    private fun pruneDirToMaxBytes(dir: File?, maxBytes: Long) {
        if (dir == null || !dir.exists() || !dir.isDirectory) return
        val files = dir.listFiles()?.filter { it.isFile } ?: return
        var total = files.sumOf { it.length() }
        if (total <= maxBytes) return

        val sortedOldestFirst = files.sortedBy { it.lastModified() }
        for (f in sortedOldestFirst) {
            if (total <= maxBytes) break
            val len = f.length()
            try { if (f.delete()) total -= len } catch (_: Exception) {}
        }
    }

    private fun getDirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) getDirSize(file) else file.length()
        }
        return size
    }

    private fun formatSize(size: Long): String {
        val mb = size.toDouble() / (1024 * 1024)
        return String.format("%.2f MB", mb)
    }
}
