package com.pasiflonet.mobile.utils

import android.content.Context

/**
 * המשתמש ביקש: לא ליצור יותר קבצי לוגים.
 * לכן הכל כאן no-op.
 * חשוב: מחזירים String כדי לא לשבור קוד שמצפה לערך חזרה.
 */
object DownloadLog {
    const val ENABLED: Boolean = false

    fun i(tag: String, msg: String) { /* no-op */ }
    fun e(tag: String, msg: String, tr: Throwable? = null) { /* no-op */ }

    fun write(text: String): String = ""
    fun write(ctx: Context, text: String): String = ""

    fun writeWithTimestamp(text: String): String = ""
    fun writeWithTimestamp(ctx: Context, text: String): String = ""

    fun writeCrash(text: String): String = ""
    fun writeCrash(ctx: Context, text: String): String = ""

    fun writeFfmpeg(text: String): String = ""
    fun writeFfmpeg(ctx: Context, text: String): String = ""
}
