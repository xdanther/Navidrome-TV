package com.navidrometv.data.repository

import com.navidrometv.core.common.DataResult
import com.navidrometv.core.common.ErrorKind
import com.navidrometv.core.model.MusicFolder
import com.navidrometv.core.model.ServerHandshake
import com.navidrometv.core.model.ServerLicense
import com.navidrometv.data.remote.SubsonicAuth
import com.navidrometv.data.remote.SubsonicClientProvider
import com.navidrometv.data.remote.SubsonicService
import com.navidrometv.data.security.SecureCredentialStore
import com.navidrometv.data.security.ServerCredentials
import com.navidrometv.domain.repository.AuthRepository
import com.navidrometv.domain.repository.ConnectionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val service: SubsonicService,
    private val clientProvider: SubsonicClientProvider,
    private val credentialStore: SecureCredentialStore,
) : AuthRepository {

    private val _connection = MutableStateFlow(ConnectionStatus(connected = false))
    override val connection: Flow<ConnectionStatus> = _connection.asStateFlow()

    override fun currentStatus(): ConnectionStatus = _connection.value

    override suspend fun testConnection(
        serverUrl: String,
        username: String,
        password: String,
    ): DataResult<ServerHandshake> = handshake(serverUrl, username, password, persist = false)

    override suspend fun connect(
        serverUrl: String,
        username: String,
        password: String,
    ): DataResult<ServerHandshake> = handshake(serverUrl, username, password, persist = true)

    private suspend fun handshake(
        serverUrl: String,
        username: String,
        password: String,
        persist: Boolean,
    ): DataResult<ServerHandshake> {
        if (serverUrl.isBlank() || username.isBlank()) {
            return DataResult.Failure(
                com.navidrometv.core.common.AppError(ErrorKind.NOT_CONFIGURED, "Server address and username are required"),
            )
        }
        if (!SubsonicAuth.isValidBaseUrl(serverUrl)) {
            return DataResult.Failure(
                com.navidrometv.core.common.AppError(ErrorKind.NOT_CONFIGURED, "That server address is not valid"),
            )
        }

        val credentials = ServerCredentials(
            baseUrl = SubsonicAuth.normalizeBaseUrl(serverUrl),
            username = username.trim(),
            password = password,
        )

        val ping = service.withCredentials(
            credentials = credentials,
            block = { session -> session.api.ping() },
            map = { payload -> payload },
        )
        val pingPayload = when (ping) {
            is DataResult.Failure -> return ping
            is DataResult.Success -> ping.data
        }

        // License and music folders are optional metadata; a server that restricts
        // them must still be usable, so failures here are not fatal.
        val license = (service.withCredentials(credentials, { it.api.getLicense() }, { it.license }) as? DataResult.Success)?.data
        val folders = (service.withCredentials(credentials, { it.api.getMusicFolders() }, { it.musicFolders?.musicFolder })
            as? DataResult.Success)?.data.orEmpty()

        val handshake = ServerHandshake(
            serverVersion = pingPayload.serverVersion ?: pingPayload.version.ifBlank { "unknown" },
            license = license?.let { ServerLicense(valid = it.valid, email = it.email, expires = it.licenseExpires) },
            folders = folders.map { MusicFolder(id = it.id, name = it.name) },
        )

        if (persist) {
            clientProvider.activate(credentials)
            credentialStore.save(credentials)
            _connection.value = ConnectionStatus(
                connected = true,
                serverUrl = credentials.baseUrl,
                username = credentials.username,
                serverVersion = handshake.serverVersion,
            )
        }
        return DataResult.Success(handshake)
    }

    override suspend fun restoreSession(): Boolean {
        val stored = credentialStore.load() ?: return false
        clientProvider.activate(stored)
        _connection.value = ConnectionStatus(
            connected = true,
            serverUrl = stored.baseUrl,
            username = stored.username,
        )

        // Verify the stored session, but keep it when the server is merely offline
        // so history and cached artwork still work on the road.
        return try {
            val ping = service.withSession({ it.api.ping() }, { it })
            when (ping) {
                is DataResult.Success -> {
                    _connection.value = _connection.value.copy(
                        serverVersion = ping.data.serverVersion ?: ping.data.version,
                    )
                    true
                }
                is DataResult.Failure -> {
                    if (ping.error.kind == ErrorKind.AUTHENTICATION) {
                        disconnect()
                        false
                    } else {
                        true
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            false
        }
    }

    override suspend fun verifyConnection(): DataResult<ServerHandshake> {
        if (credentialStore.load() == null) {
            return DataResult.Failure(
                com.navidrometv.core.common.AppError(ErrorKind.NOT_CONFIGURED, "No server configured"),
            )
        }
        return when (val ping = service.withSession({ it.api.ping() }, { it })) {
            is DataResult.Failure -> ping
            is DataResult.Success -> {
                val version = ping.data.serverVersion ?: ping.data.version
                _connection.value = _connection.value.copy(connected = true, serverVersion = version)
                DataResult.Success(
                    ServerHandshake(serverVersion = version, license = null, folders = emptyList()),
                )
            }
        }
    }

    override suspend fun disconnect() {
        credentialStore.clear()
        clientProvider.clear()
        _connection.value = ConnectionStatus(connected = false)
    }
}
