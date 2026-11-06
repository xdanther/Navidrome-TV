package com.navidrometv.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrobblePolicyTest {

    private val policy = ScrobblePolicy()

    @Test
    fun `half of a short track counts`() {
        assertFalse(policy.shouldCount(positionMs = 89_000, durationMs = 180_000))
        assertTrue(policy.shouldCount(positionMs = 90_000, durationMs = 180_000))
    }

    @Test
    fun `long tracks cap the threshold at four minutes`() {
        assertFalse(policy.shouldCount(positionMs = 239_000, durationMs = 900_000))
        assertTrue(policy.shouldCount(positionMs = 240_000, durationMs = 900_000))
    }

    @Test
    fun `very short tracks still need the minimum play time`() {
        assertFalse(policy.shouldCount(positionMs = 5_000, durationMs = 12_000))
        assertTrue(policy.shouldCount(positionMs = 10_000, durationMs = 12_000))
    }

    @Test
    fun `unknown duration falls back to thirty seconds`() {
        assertFalse(policy.shouldCount(positionMs = 29_000, durationMs = 0))
        assertTrue(policy.shouldCount(positionMs = 30_000, durationMs = 0))
    }

    @Test
    fun `no position never counts`() {
        assertFalse(policy.shouldCount(positionMs = 0, durationMs = 200_000))
        assertFalse(policy.shouldCount(positionMs = -1, durationMs = 200_000))
    }
}
