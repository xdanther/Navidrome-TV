package com.navidrometv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NavidromeTvApplication : Application(), ImageLoaderFactory {

    /**
     * Coil is configured for a TV library: a generous in-memory cache for
     * carousels the user scrolls back and forth, plus a disk cache so artwork
     * survives a cold start and works while the server is unreachable.
     */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .crossfade(true)
        .respectCacheHeaders(false)
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("artwork_cache"))
                .maxSizeBytes(256L * 1024 * 1024)
                .build()
        }
        .build()
}
