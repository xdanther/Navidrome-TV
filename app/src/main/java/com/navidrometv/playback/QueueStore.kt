package com.navidrometv.playback

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.navidrometv.core.common.AppLog
import com.navidrometv.core.model.RepeatMode
import com.navidrometv.core.model.StreamQuality
import com.navidrometv.core.model.Track
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Persisted playback queue (survives process death, restored paused). */
val Context.playbackQueueDataStore: DataStore<Preferences> by preferencesDataStore(name = "playback_queue")

@Serializable
data class QueueSnapshot(
    val tracks: List<Track> = emptyList(),
    val index: Int = 0,
    val positionMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val quality: StreamQuality = StreamQuality.ORIGINAL,
)

/**
 * Persists the queue across process death. Playback is restored *paused* at the
 * saved position: resuming audio without the user pressing play would be
 * surprising on a television.
 */
@Singleton
class QueueStore @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val dataStore: DataStore<Preferences> = context.playbackQueueDataStore
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    suspend fun save(snapshot: QueueSnapshot) {
        runCatching {
            dataStore.edit { prefs ->
                prefs[KEY_QUEUE] = json.encodeToString(QueueSnapshot.serializer(), snapshot)
            }
        }.onFailure { AppLog.warn("Could not persist playback queue") }
    }

    suspend fun load(): QueueSnapshot? = runCatching {
        val stored = dataStore.data.first()[KEY_QUEUE] ?: return@runCatching null
        json.decodeFromString(QueueSnapshot.serializer(), stored)
    }.getOrNull()

    suspend fun clear() {
        dataStore.edit { it.remove(KEY_QUEUE) }
    }

    private companion object {
        val KEY_QUEUE = stringPreferencesKey("queue_snapshot")
    }
}
