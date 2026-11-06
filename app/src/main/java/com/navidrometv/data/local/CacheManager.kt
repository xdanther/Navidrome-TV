package com.navidrometv.data.local

import android.content.Context
import coil.imageLoader
import com.navidrometv.data.prefs.SettingsDataStore
import com.navidrometv.playback.QueueStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cache maintenance. These actions are only reachable from Settings, and each one
 * does exactly what its label says: artwork, metadata, search history, local
 * listening history or the restored queue.
 */
@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cacheDao: MusicCacheDao,
    private val historyDao: PlayHistoryDao,
    private val queueStore: QueueStore,
    private val settingsStore: SettingsDataStore,
) {

    fun clearArtworkCache() {
        context.imageLoader.memoryCache?.clear()
        context.imageLoader.diskCache?.clear()
    }

    suspend fun clearMetadataCache() {
        cacheDao.clearAlbums()
        cacheDao.clearTracks()
        queueStore.clear()
    }

    suspend fun clearListeningHistory() {
        historyDao.clear()
    }

    suspend fun clearSearchHistory() {
        settingsStore.clearRecentSearches()
    }
}
