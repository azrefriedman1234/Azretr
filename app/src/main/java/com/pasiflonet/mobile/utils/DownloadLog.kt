package com.pasiflonet.mobile.utils

import android.content.Context

/**
 * המשתמש ביקש: לא ליצור יותר קבצי לוגים.
 * הכל no-op.
 * מחזיר String כדי לא לשבור קוד שמצפה לערך חזרה.
 */
object DownloadLog {
    const val ENABLED: Boolean = false

    fun i(tag: String, msg: String) { /* no-op */ }
    fun e(tag: String, msg: String, tr: Throwable? = null) { /* no-op */ }

    // חתימות "ספציפיות"
    fun write(text: String): String = ""
    fun write(ctx: Context, text: String): String = ""
    fun write(tag: String, text: String): String = ""
    fun write(ctx: Context, tag: String, text: String): String = ""

    fun writeWithTimestamp(text: String): String = ""
    fun writeWithTimestamp(ctx: Context, text: String): String = ""
    fun writeWithTimestamp(tag: String, text: String): String = ""
    fun writeWithTimestamp(ctx: Context, tag: String, text: String): String = ""

    fun writeCrash(text: String): String = ""
    fun writeCrash(ctx: Context, text: String): String = ""

    fun writeFfmpeg(text: String): String = ""
    fun writeFfmpeg(ctx: Context, text: String): String = ""

    // "פאלבק" גמיש לקריאות עם 2-4 פרמטרים (מונע 'None can be called')
    @Suppress("UNUSED_PARAMETER")
    fun write(a: Any?, b: Any?): String = ""

    @Suppress("UNUSED_PARAMETER")
    fun write(ctx: Context, a: Any?, b: Any?): String = ""

    @Suppress("UNUSED_PARAMETER")
    fun write(ctx: Context, a: Any?, b: Any?, c: Any?): String = ""

    @Suppress("UNUSED_PARAMETER")
    fun writeWithTimestamp(a: Any?, b: Any?): String = ""

    @Suppress("UNUSED_PARAMETER")
    fun writeWithTimestamp(ctx: Context, a: Any?, b: Any?): String = ""

    @Suppress("UNUSED_PARAMETER")
    fun writeWithTimestamp(ctx: Context, a: Any?, b: Any?, c: Any?): String = ""
}
