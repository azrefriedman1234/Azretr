package com.pasiflonet.mobile.utils
object DownloadLog {
    const val ENABLED: Boolean = false
    fun i(tag: String, msg: String) {}
    fun e(tag: String, msg: String, tr: Throwable? = null) {}
    fun writeCrash(text: String) {}
    fun writeFfmpeg(text: String) {}
}
