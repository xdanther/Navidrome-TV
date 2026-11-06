package com.navidrometv.data.remote

import com.navidrometv.data.security.ServerCredentials
import okhttp3.Interceptor
import okhttp3.Response
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

/**
 * Subsonic token authentication: the client sends a random salt and
 * `token = md5(password + salt)` instead of the password in clear text. HTTPS is
 * still required for real confidentiality; token auth only avoids sending the
 * raw password.
 */
object SubsonicAuth {

    const val API_VERSION = "1.16.1"
    const val CLIENT_NAME = "NavidromeTV"

    fun token(password: String, salt: String): String = md5(password + salt)

    fun randomSalt(): String = UUID.randomUUID().toString().replace("-", "").take(12)

    fun md5(value: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    /**
     * Accepts what users actually type: `192.168.1.20:4533`, `music.home`,
     * `https://music.example.com/` or a URL including a sub-path behind a reverse
     * proxy. Plain hosts default to http:// because Navidrome on a LAN commonly
     * runs without TLS.
     */
    fun normalizeBaseUrl(input: String): String {
        var value = input.trim()
        if (value.isEmpty()) return value
        val lower = value.lowercase(Locale.US)
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            value = "http://$value"
        }
        if (!value.endsWith("/")) value = "$value/"
        return value
    }

    fun isValidBaseUrl(input: String): Boolean {
        val normalized = normalizeBaseUrl(input)
        if (normalized.isEmpty()) return false
        return try {
            val url = okhttp3.HttpUrl.Companion.toHttpUrlOrNull(normalized)
            url != null && url.host.isNotBlank()
        } catch (error: IllegalArgumentException) {
            false
        }
    }
}

/**
 * Appends the common Subsonic query parameters to every request. A fresh random
 * salt is used per request, which the protocol explicitly allows.
 */
class SubsonicAuthInterceptor(
    private val credentials: ServerCredentials,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val salt = SubsonicAuth.randomSalt()
        val authenticatedUrl = original.url.newBuilder()
            .addQueryParameter("u", credentials.username)
            .addQueryParameter("t", SubsonicAuth.token(credentials.password, salt))
            .addQueryParameter("s", salt)
            .addQueryParameter("v", SubsonicAuth.API_VERSION)
            .addQueryParameter("c", SubsonicAuth.CLIENT_NAME)
            .addQueryParameter("f", "json")
            .build()
        return chain.proceed(original.newBuilder().url(authenticatedUrl).build())
    }
}
