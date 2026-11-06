package com.navidrometv.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormattersTest {

    @Test
    fun `track durations are formatted as minutes and seconds`() {
        assertEquals("0:00", Formatters.duration(0))
        assertEquals("3:07", Formatters.duration(187_000))
        assertEquals("0:59", Formatters.duration(59_999))
    }

    @Test
    fun `durations over an hour include hours`() {
        assertEquals("1:00:00", Formatters.duration(3_600_000))
        assertEquals("1:02:03", Formatters.duration(3_723_000))
    }

    @Test
    fun `negative durations clamp to zero`() {
        assertEquals("0:00", Formatters.duration(-5_000))
    }

    @Test
    fun `album totals are human readable`() {
        assertNull(Formatters.totalDuration(null))
        assertNull(Formatters.totalDuration(0))
        assertEquals("10 min", Formatters.totalDuration(600))
        assertEquals("1 hr", Formatters.totalDuration(3600))
        assertEquals("1 hr 30 min", Formatters.totalDuration(5400))
    }

    @Test
    fun `relative time covers the common ranges`() {
        val now = 1_700_000_000_000L
        assertEquals("just now", Formatters.relativeTime(now - 30_000, now))
        assertEquals("5 min ago", Formatters.relativeTime(now - 300_000, now))
        assertEquals("3 hr ago", Formatters.relativeTime(now - 3 * 3_600_000L, now))
        assertEquals("2 days ago", Formatters.relativeTime(now - 2 * 86_400_000L, now))
        assertEquals("1 week ago", Formatters.relativeTime(now - 7 * 86_400_000L, now))
    }

    @Test
    fun `plural handles singular and custom plural`() {
        assertEquals("1 song", Formatters.plural(1, "song"))
        assertEquals("2 songs", Formatters.plural(2, "song"))
        assertEquals("0 songs", Formatters.plural(0, "song"))
        assertEquals("3 tracks", Formatters.plural(3, "song", "tracks"))
    }

    @Test
    fun `bitrate and file size hide missing values`() {
        assertNull(Formatters.bitRate(null))
        assertNull(Formatters.bitRate(0))
        assertEquals("320 kbps", Formatters.bitRate(320))
        assertNull(Formatters.fileSize(null))
        assertEquals("1.0 MB", Formatters.fileSize(1024L * 1024L))
    }
}
