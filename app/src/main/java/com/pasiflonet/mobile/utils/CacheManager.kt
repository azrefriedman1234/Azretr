package com.pasiflonet.mobile.utils

import android.content.Context
import org.drinkless.tdlib.TdApi
import java.io.File

object CacheManager {

    fun getCacheSize(context: Context): String {
        return try {
            val size = getDirSize(context.cacheDir) + getDirSize(context.externalCacheDir)
            formatSize(size)
        } catch (_: Exception) {
            "0 MB"
        }
    }

    /**
     * ניקוי אמיתי: לא רק cacheDir אלא גם:
     * - cacheDir + externalCacheDir
     * - filesDir/tdlib_files (הורדות TDLib)  ✅ זה מה שתופס אצלך את ה-700MB לרוב
     * - tmp/processed/sent שנשארים ב-filesDir
     *
     * לא מוחק tdlib_db כדי שלא תצטרך להתחבר מחדש.
     */
    fun clearAppCache(context: Context) {
        try { context.cacheDir.deleteRecursively() } catch (_: Exception) {}
        try { context.externalCacheDir?.deleteRecursively() } catch (_: Exception) {}

        // מוחק הורדות TDLib (מדיה)
        try {
            val tdlibFiles = File(context.filesDir, "tdlib_files")
            if (tdlibFiles.exists()) tdlibFiles.deleteRecursively()
            tdlibFiles.mkdirs()
        } catch (_: Exception) {}

        // מוחק קבצי tmp/processed שהאפליקציה יצרה ושמה ב-filesDir
        try {
            val prefixes = listOf("sent_", "safe_", "proc_", "processed_", "tmp_", "draft_")
            context.filesDir.listFiles()?.forEach { f ->
                if (f.isFile && prefixes.any { p -> f.name.startsWith(p) }) {
                    try { f.delete() } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * מוחק קבצי מדיה מקומיים של ההודעה (רק בתוך תיקיות שמותר לנו למחוק):
     * - filesDir/tdlib_files (קבצי TDLib)
     * - cacheDir / externalCacheDir (קבצי temp של האפליקציה)
     */
    fun deleteTempForMessage(context: Context, msg: TdApi.Message) {
        val tdlibDir = File(context.filesDir, "tdlib_files")
        val allowed = listOfNotNull(context.cacheDir, context.externalCacheDir, tdlibDir)

        fun canDelete(f: File): Boolean {
            return try {
                val c = f.canonicalFile
                allowed.any { parent -> c.path.startsWith(parent.canonicalFile.path) }
            } catch (_: Exception) { false }
        }

        fun delPath(path: String?) {
            if (path.isNullOrBlank()) return
            try {
                val f = File(path)
                if (f.exists() && f.isFile && canDelete(f)) f.delete()
            } catch (_: Exception) {}
        }

        when (val c = msg.content) {
            is TdApi.MessagePhoto -> c.photo.sizes.forEach { ps -> delPath(ps.photo.local.path) }
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

    /**
     * ניקוי קבצי temp ישנים (cacheDir) – שומר רק newest "keep"
     */
    fun pruneAppTempFiles(context: Context, keep: Int = 250) {
        val prefixes = listOf("sent_", "safe_", "proc_", "processed_", "tmp_", "draft_")

        fun pruneDir(dir: File?) {
            if (dir == null || !dir.exists()) return
            val files = dir.listFiles()?.filter { it.isFile && prefixes.any { p -> it.name.startsWith(p) } } ?: return
            if (files.size <= keep) return
            val sorted = files.sortedByDescending { it.lastModified() }
            sorted.drop(keep).forEach { f -> try { f.delete() } catch (_: Exception) {} }
        }

        pruneDir(context.cacheDir)
        pruneDir(context.externalCacheDir)
        // גם filesDir (לפעמים נשאר שם processed)
        pruneDir(context.filesDir)
    }

    private fun getDirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0
        var size = 0L
        for (file in dir.listFiles().orEmpty()) {
            size += if (file.isDirectory) getDirSize(file) else file.length()
        }
        return size
    }

    private fun formatSize(size: Long): String {
        val mb = size.toDouble() / (1024 * 1024)
        return String.format("%.2f MB", mb)
    }
}
