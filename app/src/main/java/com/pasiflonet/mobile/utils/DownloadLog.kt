package com.pasiflonet.mobile.utils

import android.content.Context

/**
 * המשתמש ביקש: לא ליצור קבצי לוגים.
 * עדיין שומרים חתימות תואמות כדי שלא יהיו שגיאות קומפילציה.
 */
object DownloadLog {
    @JvmStatic fun i(tag: String, msg: String) {}
    @JvmStatic fun e(tag: String, msg: String, tr: Throwable? = null) {}

    @JvmStatic fun write(text: String): String = ""
    @JvmStatic fun write(context: Context?, text: String): String = ""
    @JvmStatic fun write(context: Context?, fileName: String, text: String): String = ""
    @JvmStatic fun write(tag: String, text: String): String = ""

    @JvmStatic fun writeWithTimestamp(text: String): String = ""
    @JvmStatic fun writeWithTimestamp(context: Context?, text: String): String = ""
    @JvmStatic fun writeWithTimestamp(context: Context?, fileName: String, text: String): String = ""

    @JvmStatic fun writeCrash(text: String) {}
    @JvmStatic fun writeCrash(context: Context?, text: String) {}
    @JvmStatic fun writeFfmpeg(text: String) {}
    @JvmStatic fun writeFfmpeg(context: Context?, text: String) {}
}
