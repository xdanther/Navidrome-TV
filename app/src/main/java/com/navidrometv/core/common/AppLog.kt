package com.navidrometv.core.common

import android.util.Log

/**
 * Single logging entry point. Call sites must never pass credentials, tokens or
 * full stream URLs: Subsonic authenticates through query parameters, so URLs are
 * treated as secrets and are never logged.
 */
object AppLog {
    private const val TAG = "NavidromeTV"

    fun debug(message: String) = Log.d(TAG, message)
    fun info(message: String) = Log.i(TAG, message)
    fun warn(message: String, error: Throwable? = null) = Log.w(TAG, message, error)
    fun error(message: String, error: Throwable? = null) = Log.e(TAG, message, error)
}
