package com.navidrometv.data.repository

import com.navidrometv.core.model.StreamQuality
import com.navidrometv.data.prefs.AppSettings
import com.navidrometv.data.prefs.GridDensity
import com.navidrometv.data.prefs.SettingsDataStore
import com.navidrometv.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val store: SettingsDataStore,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = store.settings

    override suspend fun setStreamingQuality(quality: StreamQuality) = store.setStreamingQuality(quality)

    override suspend fun setReduceMotion(enabled: Boolean) = store.setReduceMotion(enabled)

    override suspend fun setArtworkBackground(enabled: Boolean) = store.setArtworkBackground(enabled)

    override suspend fun setGridDensity(density: GridDensity) = store.setGridDensity(density)

    override suspend fun setShuffleByDefault(enabled: Boolean) = store.setShuffleByDefault(enabled)

    override suspend fun setScrobbleEnabled(enabled: Boolean) = store.setScrobbleEnabled(enabled)

    override suspend fun setAmbientModeEnabled(enabled: Boolean) = store.setAmbientModeEnabled(enabled)

    override suspend fun setKeepScreenOn(enabled: Boolean) = store.setKeepScreenOn(enabled)

    override suspend fun addRecentSearch(query: String) = store.addRecentSearch(query)

    override suspend fun clearRecentSearches() = store.clearRecentSearches()
}
