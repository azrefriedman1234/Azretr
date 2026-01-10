package com.pasiflonet.mobile.utils

import android.content.Context

/**
 * המשתמש ביקש: לא ליצור יותר קבצי לוגים.
 * לכן הכל כאן NO-OP.
 *
 * חשוב: יש כאן overloadים שמחזירים String כדי לא לשבור קוד שמצפה ל-String.
 */
object DownloadLog {
    const val ENABLED: Boolean = false

    fun i(tag: String, msg: String) {}
    fun e(tag: String, msg: String, tr: Throwable? = null) {}

    fun write(text: String): String = text
    fun write(ctx: Context, text: String): String = text

    fun writeWithTimestamp(text: String): String = text
    fun writeWithTimestamp(ctx: Context, text: String): String = text

    fun writeCrash(text: String) {}
    fun writeFfmpeg(text: String) {}
}
