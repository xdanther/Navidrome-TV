package com.navidrometv.core.common

import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

object Formatters {

    /** "3:07" / "1:02:11" for track lengths and remaining time. */
    fun duration(millis: Long): String {
        val totalSeconds = max(0L, millis / 1000)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    /** "42 min" style summary used for album metadata. */
    fun totalDuration(seconds: Int?): String? {
        if (seconds == null || seconds <= 0) return null
        val minutes = seconds / 60
        return if (minutes >= 60) {
            val hours = minutes / 60
            val rest = minutes % 60
            if (rest == 0) "$hours hr" else "$hours hr $rest min"
        } else {
            "$minutes min"
        }
    }

    /** "now", "12 min ago", "3 days ago" for history rows. */
    fun relativeTime(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
        val delta = abs(now - epochMillis) / 1000
        return when {
            delta < 60 -> "just now"
            delta < 3600 -> "${delta / 60} min ago"
            delta < 86_400 -> "${delta / 3600} hr ago"
            delta < 604_800 -> "${delta / 86_400} ${if (delta / 86_400 == 1L) "day" else "days"} ago"
            delta < 2_592_000 -> "${delta / 604_800} ${if (delta / 604_800 == 1L) "week" else "weeks"} ago"
            else -> "${delta / 2_592_000} ${if (delta / 2_592_000 == 1L) "month" else "months"} ago"
        }
    }

    fun bitRate(kbps: Int?): String? = kbps?.takeIf { it > 0 }?.let { "$it kbps" }

    fun sampleRate(hz: Int?): String? = hz?.takeIf { it > 0 }?.let { "${it / 1000} kHz" }

    fun fileSize(bytes: Long?): String? {
        if (bytes == null || bytes <= 0) return null
        val mb = bytes / 1024.0 / 1024.0
        return String.format(Locale.US, "%.1f MB", mb)
    }

    fun resolutionLabel(width: Int, height: Int): String = "${width}x$height"

    fun plural(count: Int, singular: String, plural: String = "${singular}s"): String =
        if (count == 1) "1 $singular" else "$count $plural"
}
