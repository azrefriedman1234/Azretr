package com.pasiflonet.mobile.utils

/**
 * המשתמש ביקש: לא ליצור יותר קבצי לוגים.
 * משאירים את ה-API כדי שקבצים קיימים שמייבאים write/writeWithTimestamp לא יישברו.
 */
object DownloadLog {
    const val ENABLED: Boolean = false

    @JvmStatic fun i(tag: String, msg: String) { /* no-op */ }
    @JvmStatic fun e(tag: String, msg: String, tr: Throwable? = null) { /* no-op */ }

    // תאימות לקוד קיים:
    @JvmStatic fun write(text: String) { /* no-op */ }
    @JvmStatic fun writeWithTimestamp(text: String) { /* no-op */ }

    @JvmStatic fun writeCrash(text: String) { /* no-op */ }
    @JvmStatic fun writeFfmpeg(text: String) { /* no-op */ }
}
