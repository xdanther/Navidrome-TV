package com.navidrometv.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.navidrometv.core.common.AppLog
import com.navidrometv.core.model.StreamQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "navidrome_tv_settings")

/**
 * DataStore-backed settings. Kept independent of DataStore internals so tests can
 * use an in-memory implementation of [SettingsRepository] instead.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val dataStore: DataStore<Preferences> = context.settingsDataStore

    private object Keys {
        val streamingQuality = stringPreferencesKey("streaming_quality")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val artworkBackground = booleanPreferencesKey("artwork_background")
        val gridDensity = stringPreferencesKey("grid_density")
        val shuffleByDefault = booleanPreferencesKey("shuffle_by_default")
        val scrobbleEnabled = booleanPreferencesKey("scrobble_enabled")
        val ambientModeEnabled = booleanPreferencesKey("ambient_mode_enabled")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val recentSearches = stringSetPreferencesKey("recent_searches")
    }

    val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            AppLog.warn("Failed to read settings: ${error.message}")
            emit(emptyPreferences())
        }
        .map { preferences -> preferences.toSettings() }

    private fun Preferences.toSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            streamingQuality = this[Keys.streamingQuality]
                ?.let { stored -> StreamQuality.entries.firstOrNull { it.name == stored } }
                ?: defaults.streamingQuality,
            reduceMotion = this[Keys.reduceMotion] ?: defaults.reduceMotion,
            artworkBackground = this[Keys.artworkBackground] ?: defaults.artworkBackground,
            gridDensity = this[Keys.gridDensity]
                ?.let { stored -> GridDensity.entries.firstOrNull { it.name == stored } }
                ?: defaults.gridDensity,
            shuffleByDefault = this[Keys.shuffleByDefault] ?: defaults.shuffleByDefault,
            scrobbleEnabled = this[Keys.scrobbleEnabled] ?: defaults.scrobbleEnabled,
            ambientModeEnabled = this[Keys.ambientModeEnabled] ?: defaults.ambientModeEnabled,
            keepScreenOn = this[Keys.keepScreenOn] ?: defaults.keepScreenOn,
            recentSearches = this[Keys.recentSearches]?.toList()?.sorted() ?: defaults.recentSearches,
        )
    }

    suspend fun setStreamingQuality(quality: StreamQuality) = edit { prefs ->
        prefs[Keys.streamingQuality] = quality.name
    }

    suspend fun setReduceMotion(enabled: Boolean) = edit { prefs ->
        prefs[Keys.reduceMotion] = enabled
    }

    suspend fun setArtworkBackground(enabled: Boolean) = edit { prefs ->
        prefs[Keys.artworkBackground] = enabled
    }

    suspend fun setGridDensity(density: GridDensity) = edit { prefs ->
        prefs[Keys.gridDensity] = density.name
    }

    suspend fun setShuffleByDefault(enabled: Boolean) = edit { prefs ->
        prefs[Keys.shuffleByDefault] = enabled
    }

    suspend fun setScrobbleEnabled(enabled: Boolean) = edit { prefs ->
        prefs[Keys.scrobbleEnabled] = enabled
    }

    suspend fun setAmbientModeEnabled(enabled: Boolean) = edit { prefs ->
        prefs[Keys.ambientModeEnabled] = enabled
    }

    suspend fun setKeepScreenOn(enabled: Boolean) = edit { prefs ->
        prefs[Keys.keepScreenOn] = enabled
    }

    suspend fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        edit { prefs ->
            val existing = prefs[Keys.recentSearches].orEmpty()
            val updated = (listOf(trimmed) + existing.filterNot { it.equals(trimmed, ignoreCase = true) })
                .take(MAX_RECENT_SEARCHES)
            prefs[Keys.recentSearches] = updated.toSet()
        }
    }

    suspend fun clearRecentSearches() = edit { prefs ->
        prefs.remove(Keys.recentSearches)
    }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        dataStore.edit { preferences -> block(preferences) }
    }

    private companion object {
        const val MAX_RECENT_SEARCHES = 8
    }
}
