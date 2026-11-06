package com.navidrometv.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.navidrometv.core.common.AppLog
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android Keystore backed credential storage.
 *
 * A non-exportable AES-256-GCM key lives in the AndroidKeyStore; only the
 * ciphertext (IV + payload, Base64) is written to private SharedPreferences. The
 * password never touches DataStore, logs or backups (`allowBackup=false`).
 */
@Singleton
class SecureCredentialStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun save(credentials: ServerCredentials) {
        val plaintext = json.encodeToString(ServerCredentials.serializer(), credentials)
        val encrypted = encrypt(plaintext.toByteArray(Charsets.UTF_8))
        preferences.edit()
            .putString(KEY_CREDENTIALS, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    fun load(): ServerCredentials? {
        val stored = preferences.getString(KEY_CREDENTIALS, null) ?: return null
        return try {
            val bytes = Base64.decode(stored, Base64.NO_WRAP)
            val plaintext = String(decrypt(bytes), Charsets.UTF_8)
            json.decodeFromString(ServerCredentials.serializer(), plaintext)
        } catch (error: Exception) {
            // A key can be invalidated by a factory reset or lock-screen change.
            // Treat that as "not signed in" instead of crashing on launch.
            AppLog.warn("Stored credentials could not be decrypted; clearing them")
            clear()
            null
        }
    }

    fun clear() {
        preferences.edit().remove(KEY_CREDENTIALS).apply()
    }

    fun hasCredentials(): Boolean = preferences.contains(KEY_CREDENTIALS)

    private fun encrypt(payload: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(payload)
        return ByteArray(iv.size + ciphertext.size).also { result ->
            iv.copyInto(result, 0)
            ciphertext.copyInto(result, iv.size)
        }
    }

    private fun decrypt(payload: ByteArray): ByteArray {
        val ivLength = GCM_IV_BYTES
        require(payload.size > ivLength) { "Encrypted payload is truncated" }
        val iv = payload.copyOfRange(0, ivLength)
        val ciphertext = payload.copyOfRange(ivLength, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFS_FILE = "navidrome_tv_secure"
        const val KEY_CREDENTIALS = "server_credentials"
        const val KEY_ALIAS = "navidrome_tv_credentials_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
    }
}
