package com.navidrometv.playback

import kotlin.math.min
import javax.inject.Inject

/**
 * Decides when a play counts as "listened" for local history and for scrobbling.
 *
 * Mirrors the widely used convention: at least half the track, or four minutes,
 * whichever comes first. A track shorter than ten seconds still needs ten seconds
 * of playback, and unknown durations fall back to thirty seconds.
 */
class ScrobblePolicy @Inject constructor() {

    fun shouldCount(positionMs: Long, durationMs: Long): Boolean {
        if (positionMs <= 0L) return false
        if (durationMs <= 0L) return positionMs >= 30_000L
        val threshold = min(durationMs / 2, FOUR_MINUTES).coerceAtLeast(MINIMUM_PLAY_MS)
        return positionMs >= threshold
    }

    companion object {
        const val FOUR_MINUTES = 240_000L
        const val MINIMUM_PLAY_MS = 10_000L
    }
}
