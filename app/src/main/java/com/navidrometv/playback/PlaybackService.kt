package com.navidrometv.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import androidx.core.net.toUri
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.navidrometv.MainActivity
import com.navidrometv.core.common.AppLog
import com.navidrometv.core.common.DataResult
import com.navidrometv.core.model.StreamQuality
import com.navidrometv.core.model.Track
import com.navidrometv.domain.repository.MusicRepository
import com.navidrometv.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground media playback service.
 *
 * ExoPlayer owns decoding, audio focus, noisy-headset handling and the wake lock.
 * MediaLibrarySession publishes the session to the system (TV remote, Assistant,
 * notification) and implements a small searchable library so voice search can
 * find tracks on the server.
 */
@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

    @Inject
    lateinit var musicRepository: MusicRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var player: ExoPlayer
    private lateinit var librarySession: MediaLibrarySession

    private var streamingQuality: StreamQuality = StreamQuality.ORIGINAL
    private var lastQuery: String = ""
    private var searchResults: ImmutableList<MediaItem> = ImmutableList.of()

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        librarySession = MediaLibrarySession.Builder(this, player, LibraryCallback())
            .setSessionActivity(sessionActivity)
            .build()

        serviceScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                streamingQuality = settings.streamingQuality
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession =
        librarySession

    override fun onDestroy() {
        serviceScope.cancel()
        librarySession.release()
        player.release()
        super.onDestroy()
    }

    private val libraryRoot: MediaItem
        get() = MediaItem.Builder()
            .setMediaId(LIBRARY_ROOT_ID)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Navidrome TV")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .build(),
            )
            .build()

    private fun Track.toSessionItem(): MediaItem? {
        val streamUrl = musicRepository.streamUrl(id, streamingQuality) ?: return null
        val artwork = musicRepository.coverArtUrl(coverArtId, 640)
        return MediaItem.Builder()
            .setMediaId(this.id)
            .setUri(streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(this.title)
                    .setArtist(this.artistName)
                    .setAlbumTitle(this.albumName)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .apply { if (artwork != null) setArtworkUri(artwork.toUri()) }
                    .build(),
            )
            .build()
    }

    /**
     * Library browsing is intentionally minimal: the TV remote and the app UI are
     * the primary surfaces, while search exists so Assistant/voice queries resolve
     * to real server tracks. Returning an empty folder list is honest rather than
     * exposing a half-built hierarchy, and is documented in COMPATIBILITY.md.
     */
    private inner class LibraryCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(LibraryResult.ofItem(libraryRoot, params))

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
            Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.of(), params))

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> {
            lastQuery = query
            serviceScope.launch {
                when (val result = musicRepository.search(query)) {
                    is DataResult.Success -> {
                        searchResults = ImmutableList.copyOf(result.data.tracks.mapNotNull { it.toSessionItem() })
                    }
                    is DataResult.Failure -> {
                        AppLog.warn("Voice search failed: ${result.error.kind}")
                        searchResults = ImmutableList.of()
                    }
                }
            }
            return Futures.immediateFuture(LibraryResult.ofVoid(params))
        }

        @androidx.media3.common.util.UnstableApi
        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            if (!query.equals(lastQuery, ignoreCase = true)) {
                return Futures.immediateFuture(
                    LibraryResult.ofError(SessionError.ERROR_BAD_VALUE),
                )
            }
            val from = (page * pageSize).coerceAtLeast(0)
            val to = (from + pageSize).coerceAtMost(searchResults.size)
            val slice = if (from <= to) searchResults.subList(from, to) else ImmutableList.of()
            return Futures.immediateFuture(LibraryResult.ofItemList(slice, params))
        }
    }

    private companion object {
        const val LIBRARY_ROOT_ID = "navidrome_tv_root"
    }
}
