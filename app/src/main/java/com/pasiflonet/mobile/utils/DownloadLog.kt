package com.pasiflonet.mobile.utils

/**
 * NO-OP: לא יוצר קבצי לוגים בכלל.
 * מחזיר String ריק כדי שכל שימוש בקוד (גם בתוך ביטוי) יתקמפל.
 */
object DownloadLog {
    const val ENABLED: Boolean = false

    @JvmStatic fun write(text: String): String = ""
    @JvmStatic fun writeWithTimestamp(text: String): String = ""
    @JvmStatic fun writeCrash(text: String): String = ""
    @JvmStatic fun writeFfmpeg(text: String): String = ""

    // תאימות לכל קריאה קיימת (Context/Throwable/כמה פרמטרים וכו')
    @JvmStatic fun write(vararg any: Any?): String = ""
    @JvmStatic fun writeWithTimestamp(vararg any: Any?): String = ""
    @JvmStatic fun writeCrash(vararg any: Any?): String = ""
    @JvmStatic fun writeFfmpeg(vararg any: Any?): String = ""

    @JvmStatic fun i(vararg any: Any?): String = ""
    @JvmStatic fun d(vararg any: Any?): String = ""
    @JvmStatic fun w(vararg any: Any?): String = ""
    @JvmStatic fun e(vararg any: Any?): String = ""
}
