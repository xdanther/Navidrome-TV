package com.navidrometv.data.security

import kotlinx.serialization.Serializable

/**
 * Credentials for one Navidrome server. Never logged, never serialised to plain
 * DataStore or to disk unencrypted: [SecureCredentialStore] encrypts the encoded
 * form with an Android Keystore AES/GCM key.
 */
@Serializable
data class ServerCredentials(
    val baseUrl: String,
    val username: String,
    val password: String,
)
