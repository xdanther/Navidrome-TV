package com.navidrometv.di

import com.navidrometv.core.common.AppLog
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Lenient JSON: Subsonic servers omit optional fields and occasionally send
     * numbers as strings. Unknown keys are ignored so a newer server can add
     * fields without breaking the client.
     */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(PathOnlyLoggingInterceptor())
        .build()
}

/**
 * Logs method, path, status and duration only. Query strings carry the Subsonic
 * token and username, so they are deliberately never logged.
 */
class PathOnlyLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startedAt = System.nanoTime()
        val response = chain.proceed(request)
        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
        AppLog.debug("${request.method} ${request.url.encodedPath} -> ${response.code} (${elapsedMs}ms)")
        return response
    }
}
