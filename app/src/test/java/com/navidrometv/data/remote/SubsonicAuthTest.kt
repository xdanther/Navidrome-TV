package com.navidrometv.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubsonicAuthTest {

    @Test
    fun `md5 matches the known digest`() {
        assertEquals("5d41402abc4b2a76b9719d911017c592", SubsonicAuth.md5("hello"))
    }

    @Test
    fun `token is md5 of password plus salt`() {
        val salt = "c19b2d"
        assertEquals(SubsonicAuth.md5("sesame$salt"), SubsonicAuth.token("sesame", salt))
    }

    @Test
    fun `plain host gets http scheme and trailing slash`() {
        assertEquals("http://192.168.1.20:4533/", SubsonicAuth.normalizeBaseUrl("192.168.1.20:4533"))
    }

    @Test
    fun `hostname is normalised`() {
        assertEquals("http://music.home/", SubsonicAuth.normalizeBaseUrl(" music.home "))
    }

    @Test
    fun `https and reverse proxy sub path are preserved`() {
        assertEquals("https://music.example.com/", SubsonicAuth.normalizeBaseUrl("https://music.example.com"))
        assertEquals(
            "https://example.com/navidrome/",
            SubsonicAuth.normalizeBaseUrl("https://example.com/navidrome"),
        )
    }

    @Test
    fun `valid addresses are accepted`() {
        assertTrue(SubsonicAuth.isValidBaseUrl("192.168.1.20:4533"))
        assertTrue(SubsonicAuth.isValidBaseUrl("https://music.example.com/navidrome"))
    }

    @Test
    fun `invalid addresses are rejected`() {
        assertFalse(SubsonicAuth.isValidBaseUrl(""))
        assertFalse(SubsonicAuth.isValidBaseUrl("   "))
        assertFalse(SubsonicAuth.isValidBaseUrl("http://"))
    }

    @Test
    fun `random salt is short and alphanumeric`() {
        val salt = SubsonicAuth.randomSalt()
        assertEquals(12, salt.length)
        assertTrue(salt.all { it.isLetterOrDigit() })
    }
}
