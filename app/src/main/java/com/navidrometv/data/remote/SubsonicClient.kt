package com.navidrometv.data.remote

import com.navidrometv.core.model.StreamQuality
import com.navidrometv.data.security.ServerCredentials
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/** Everything needed to talk to one configured server. */
class SubsonicSession(
    val credentials: ServerCredentials,
    val api: SubsonicApi,
    val urls: SubsonicUrlBuilder,
)

/**
 * Builds authenticated media URLs. These URLs embed the token, so they are treated
 * as secrets: never logged, never shown in the UI, never persisted.
 */
class SubsonicUrlBuilder(
    private val baseUrl: String,
    private val credentials: ServerCredentials,
) {

    private fun authQuery(): String {
        val salt = SubsonicAuth.randomSalt()
        return buildString {
            append("u=").append(encode(credentials.username))
            append("&t=").append(SubsonicAuth.token(credentials.password, salt))
            append("&s=").append(salt)
            append("&v=").append(SubsonicAuth.API_VERSION)
            append("&c=").append(SubsonicAuth.CLIENT_NAME)
            append("&f=json")
        }
    }

    fun coverArt(coverArtId: String, sizePx: Int): String =
        "${baseUrl}rest/getCoverArt.view?id=${encode(coverArtId)}&size=$sizePx&${authQuery()}"

    /**
     * Stream URL for a track. When the quality setting allows transcoding, the
     * Subsonic `maxBitRate`/`format` parameters are sent and the server decides
     * what it can deliver; the actual delivered format is reported from the
     * player, never assumed.
     */
    fun stream(trackId: String, quality: StreamQuality): String = buildString {
        append(baseUrl).append("rest/stream.view?id=").append(encode(trackId))
        quality.maxBitRateKbps?.let { append("&maxBitRate=").append(it) }
        quality.format?.let { append("&format=").append(encode(it)) }
        append('&').append(authQuery())
    }

    fun download(trackId: String): String =
        "${baseUrl}rest/download.view?id=${encode(trackId)}&${authQuery()}"

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}

/**
 * Owns the single active server session. Retrofit instances are built at runtime
 * because the base URL is chosen by the user, not known at compile time.
 */
@Singleton
class SubsonicClientProvider @Inject constructor(
    private val baseClient: OkHttpClient,
    private val json: Json,
) {

    @Volatile
    private var activeSession: SubsonicSession? = null

    fun current(): SubsonicSession? = activeSession

    fun activate(credentials: ServerCredentials): SubsonicSession {
        val session = build(credentials)
        activeSession = session
        return session
    }

    fun clear() {
        activeSession = null
    }

    /** Used by "Test connection" before anything is persisted. */
    fun build(credentials: ServerCredentials): SubsonicSession {
        val normalized = credentials.copy(
            baseUrl = SubsonicAuth.normalizeBaseUrl(credentials.baseUrl),
        )
        val client = baseClient.newBuilder()
            .addInterceptor(SubsonicAuthInterceptor(normalized))
            .build()

        val api = Retrofit.Builder()
            .baseUrl(normalized.baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SubsonicApi::class.java)

        return SubsonicSession(
            credentials = normalized,
            api = api,
            urls = SubsonicUrlBuilder(normalized.baseUrl, normalized),
        )
    }
}
