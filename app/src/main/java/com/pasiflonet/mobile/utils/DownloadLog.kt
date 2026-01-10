package com.pasiflonet.mobile.utils

/**
 * המשתמש ביקש: לא ליצור יותר קבצי לוגים.
 * לכן הכל no-op, אבל משאירים API "גמיש" כדי שכל הקריאות בקוד יתקמפלו.
 */
object DownloadLog {
    const val ENABLED: Boolean = false

    @JvmStatic fun i(tag: String, msg: String) { /* no-op */ }
    @JvmStatic fun e(tag: String, msg: String, tr: Throwable? = null) { /* no-op */ }

    // תאימות מלאה: מאפשר כל חתימה וכל מספר פרמטרים
    @JvmStatic fun write(vararg any: Any?) { /* no-op */ }
    @JvmStatic fun writeWithTimestamp(vararg any: Any?) { /* no-op */ }

    @JvmStatic fun writeCrash(vararg any: Any?) { /* no-op */ }
    @JvmStatic fun writeFfmpeg(vararg any: Any?) { /* no-op */ }
}
