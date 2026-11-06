package com.navidrometv.core.common

import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Error categories the UI can react to without knowing transport details. */
enum class ErrorKind {
    NOT_CONFIGURED,
    NETWORK,
    TIMEOUT,
    AUTHENTICATION,
    AUTHORIZATION,
    NOT_FOUND,
    SERVER,
    UNSUPPORTED,
    UNKNOWN,
}

data class AppError(
    val kind: ErrorKind,
    val message: String,
    val cause: Throwable? = null,
)

/**
 * Minimal result type used across layer boundaries. Repositories never throw for
 * expected failures; they return [Failure] so ViewModels can render honest states.
 */
sealed interface DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>
    data class Failure(val error: AppError) : DataResult<Nothing>

    fun getOrNull(): T? = (this as? Success)?.data
    fun errorOrNull(): AppError? = (this as? Failure)?.error
}

inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> = when (this) {
    is DataResult.Success -> DataResult.Success(transform(data))
    is DataResult.Failure -> this
}

inline fun <T> DataResult<T>.onSuccess(action: (T) -> Unit): DataResult<T> {
    if (this is DataResult.Success) action(data)
    return this
}

inline fun <T> DataResult<T>.onFailure(action: (AppError) -> Unit): DataResult<T> {
    if (this is DataResult.Failure) action(error)
    return this
}

fun Throwable.toAppError(fallbackMessage: String = "Something went wrong"): AppError = when (this) {
    is AppException -> error
    is SubsonicException -> AppError(
        kind = when (code) {
            40, 41 -> ErrorKind.AUTHENTICATION
            50 -> ErrorKind.AUTHORIZATION
            70 -> ErrorKind.NOT_FOUND
            30, 20 -> ErrorKind.UNSUPPORTED
            else -> ErrorKind.SERVER
        },
        message = message.ifBlank { fallbackMessage },
        cause = this,
    )
    is SocketTimeoutException -> AppError(ErrorKind.TIMEOUT, "The server took too long to respond", this)
    is UnknownHostException -> AppError(ErrorKind.NETWORK, "Server not reachable. Check the address and network", this)
    is IOException -> AppError(ErrorKind.NETWORK, "Network error: ${message ?: "connection failed"}", this)
    is HttpException -> AppError(ErrorKind.SERVER, "Server returned HTTP ${code()}", this)
    is SerializationException -> AppError(ErrorKind.UNSUPPORTED, "The server returned an unexpected response", this)
    else -> AppError(ErrorKind.UNKNOWN, message ?: fallbackMessage, this)
}

/** Wraps an already classified error so it can be rethrown through coroutines. */
class AppException(val error: AppError) : Exception(error.message, error.cause)

/**
 * Thrown when a Subsonic endpoint answers with status=failed. The numeric code is
 * kept so permission problems can be distinguished from transport problems.
 */
class SubsonicException(val code: Int, message: String) : Exception(message)
