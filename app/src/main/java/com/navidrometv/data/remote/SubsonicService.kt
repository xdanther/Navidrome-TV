package com.navidrometv.data.remote

import com.navidrometv.core.common.AppDispatchers
import com.navidrometv.core.common.AppError
import com.navidrometv.core.common.DataResult
import com.navidrometv.core.common.ErrorKind
import com.navidrometv.core.common.SubsonicException
import com.navidrometv.core.common.toAppError
import com.navidrometv.data.remote.dto.SubsonicPayload
import com.navidrometv.data.security.ServerCredentials
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes one Subsonic call against the active (or a candidate) session and
 * converts transport problems and `status=failed` envelopes into [DataResult].
 *
 * Repositories stay small because session handling, error classification and
 * threading all happen here.
 */
@Singleton
class SubsonicService @Inject constructor(
    private val clientProvider: SubsonicClientProvider,
    private val dispatchers: AppDispatchers,
) {

    fun session(): SubsonicSession? = clientProvider.current()

    fun coverArtUrl(coverArtId: String?, sizePx: Int): String? {
        if (coverArtId.isNullOrBlank()) return null
        val session = clientProvider.current() ?: return null
        return session.urls.coverArt(coverArtId, sizePx)
    }

    suspend fun <T> withSession(
        block: suspend (SubsonicSession) -> SubsonicPayload,
        map: (SubsonicPayload) -> T,
    ): DataResult<T> {
        val session = clientProvider.current()
            ?: return DataResult.Failure(
                AppError(ErrorKind.NOT_CONFIGURED, "Connect to a Navidrome server first"),
            )
        return execute(session, block, map)
    }

    /** Runs a call with explicit credentials, used by onboarding's Test Connection. */
    suspend fun <T> withCredentials(
        credentials: ServerCredentials,
        block: suspend (SubsonicSession) -> SubsonicPayload,
        map: (SubsonicPayload) -> T,
    ): DataResult<T> {
        val session = try {
            clientProvider.build(credentials)
        } catch (error: IllegalArgumentException) {
            return DataResult.Failure(
                AppError(ErrorKind.NOT_CONFIGURED, "That server address is not valid", error),
            )
        }
        return execute(session, block, map)
    }

    private suspend fun <T> execute(
        session: SubsonicSession,
        block: suspend (SubsonicSession) -> SubsonicPayload,
        map: (SubsonicPayload) -> T,
    ): DataResult<T> = withContext(dispatchers.io) {
        try {
            val payload = block(session)
            if (!payload.isOk) {
                val error = payload.error
                throw SubsonicException(
                    code = error?.code ?: 0,
                    message = error?.message?.takeIf { it.isNotBlank() }
                        ?: "The server rejected the request",
                )
            }
            DataResult.Success(map(payload))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            DataResult.Failure(error.toAppError())
        }
    }
}
