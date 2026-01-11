package com.pasiflonet.mobile.utils

import android.content.Context

/**
 * המשתמש ביקש: לא ליצור יותר לוגים לקבצים.
 * אז הכל כאן NO-OP. מחזירים String ריק כדי להתאים לקוד שמצפה למסלול/טקסט.
 */
object DownloadLog {
    const val ENABLED: Boolean = false

    @JvmStatic fun i(tag: String, msg: String) { /* no-op */ }
    @JvmStatic fun e(tag: String, msg: String, tr: Throwable? = null) { /* no-op */ }

    // generic writers (some code expects a String return)
    @JvmStatic fun write(text: String): String = ""
    @JvmStatic fun write(ctx: Context, text: String): String = ""

    @JvmStatic fun writeWithTimestamp(text: String): String = ""
    @JvmStatic fun writeWithTimestamp(ctx: Context, text: String): String = ""

    // specific helpers (also return String to avoid "Unit but String expected")
    @JvmStatic fun writeCrash(text: String): String = ""
    @JvmStatic fun writeCrash(ctx: Context, text: String): String = ""

    @JvmStatic fun writeFfmpeg(text: String): String = ""
    @JvmStatic fun writeFfmpeg(ctx: Context, text: String): String = ""
}
