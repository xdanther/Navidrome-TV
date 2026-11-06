package com.navidrometv.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.navidrometv.core.common.AppLog
import com.navidrometv.core.model.PlaybackRequest
import com.navidrometv.core.model.RepeatMode
import com.navidrometv.core.model.StreamQuality
import com.navidrometv.core.model.Track
import com.navidrometv.data.prefs.AppSettings
import com.navidrometv.domain.repository.HistoryRepository
import com.navidrometv.domain.repository.MusicRepository
import com.navidrometv.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Delivered audio format, read from the player rather than assumed. */
data class AudioPlaybackInfo(
    val mimeType: String? = null,
    val codec: String? = null,
    val bitrateKbps: Int? = null,
    val sampleRateHz: Int? = null,
    val channelCount: Int? = null,
    val transcodingRequested: Boolean = false,
)

data class PlaybackState(
    val connected: Boolean = false,
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val audioInfo: AudioPlaybackInfo? = null,
    val requestedQuality: StreamQuality = StreamQuality.ORIGINAL,
    val error: String? = null,
) {
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    val hasNext: Boolean get() = currentIndex in 0 until queue.lastIndex
    val hasPrevious: Boolean get() = currentIndex > 0 || positionMs > 3_000L
}

/**
 * Application-scoped bridge to the Media3 session in [PlaybackService].
 *
 * The UI never touches MediaController directly: it observes [state] and calls the
 * intent-style commands below. The controller is connected once and outlives every
 * screen, which is what keeps audio playing while the user browses.
 */
@Singleton
class PlaybackRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val queueStore: QueueStore,
    private val scrobblePolicy: ScrobblePolicy,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var tickerJob: Job? = null
    private var settings = AppSettings()
    private var queueTracks: List<Track> = emptyList()
    private var pendingRequest: PlaybackRequest? = null
    private var restoreSnapshot: QueueSnapshot? = null
    private var countedTrackId: String? = null
    private var startedTrackId: String? = null

    init {
        scope.launch {
            settingsRepository.settings.collect { settings = it }
        }
        scope.launch {
            restoreSnapshot = queueStore.load()
            restoreSnapshot?.let { snapshot ->
                queueTracks = snapshot.tracks
                _state.update {
                    it.copy(
                        queue = snapshot.tracks,
                        currentIndex = snapshot.index.coerceIn(0, (snapshot.tracks.size - 1).coerceAtLeast(0)),
                        currentTrack = snapshot.tracks.getOrNull(snapshot.index),
                        positionMs = snapshot.positionMs,
                        durationMs = snapshot.tracks.getOrNull(snapshot.index)?.durationMs ?: 0L,
                        shuffle = snapshot.shuffle,
                        repeatMode = snapshot.repeatMode,
                        requestedQuality = snapshot.quality,
                    )
                }
            }
        }
    }

    fun connect() {
        if (controllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                val connectedController = runCatching { future.get() }.getOrNull()
                if (connectedController == null) {
                    AppLog.warn("Media controller could not connect")
                    _state.update { it.copy(connected = false) }
                    return@addListener
                }
                controller = connectedController
                connectedController.addListener(playerListener)
                _state.update { it.copy(connected = true) }
                restoreIntoPlayer(connectedController)
                pendingRequest?.let { request ->
                    pendingRequest = null
                    playTracks(request)
                }
                syncState()
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    private fun restoreIntoPlayer(player: MediaController) {
        val snapshot = restoreSnapshot ?: return
        if (snapshot.tracks.isEmpty() || player.mediaItemCount > 0) return
        val items = snapshot.tracks.mapNotNull { buildMediaItem(it, snapshot.quality) }
        if (items.isEmpty()) return
        player.setMediaItems(
            items,
            snapshot.index.coerceIn(0, items.lastIndex),
            snapshot.positionMs,
        )
        player.prepare()
        player.shuffleModeEnabled = snapshot.shuffle
        player.repeatMode = snapshot.repeatMode.toPlayerRepeatMode()
        restoreSnapshot = null
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncState()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            countedTrackId = null
            startedTrackId = null
            syncState()
            persistQueue()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            syncState()
            if (isPlaying) {
                startTicker()
                reportPlaybackStart()
            } else {
                stopTicker()
                persistQueue()
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateAudioInfo(tracks)
        }

        override fun onPlayerError(error: PlaybackException) {
            AppLog.warn("Playback error (${error.errorCodeName})")
            _state.update { it.copy(error = error.message ?: "Playback failed") }
        }
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (isActive) {
                delay(500)
                val player = controller ?: break
                _state.update {
                    it.copy(
                        positionMs = player.currentPosition.coerceAtLeast(0L),
                        durationMs = player.duration.takeIf { duration -> duration > 0L }
                            ?: it.currentTrack?.durationMs
                            ?: 0L,
                    )
                }
                checkScrobbleThreshold()
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun reportPlaybackStart() {
        val track = _state.value.currentTrack ?: return
        if (startedTrackId == track.id) return
        startedTrackId = track.id
        if (!settings.scrobbleEnabled) return
        scope.launch { musicRepository.scrobble(track, submission = false) }
    }

    private fun checkScrobbleThreshold() {
        val state = _state.value
        val track = state.currentTrack ?: return
        if (countedTrackId == track.id) return
        val duration = if (state.durationMs > 0) state.durationMs else track.durationMs
        if (!scrobblePolicy.shouldCount(state.positionMs, duration)) return
        countedTrackId = track.id
        scope.launch {
            historyRepository.recordPlay(track)
            if (settings.scrobbleEnabled) {
                musicRepository.scrobble(track, submission = true)
            }
        }
    }

    private fun updateAudioInfo(tracks: Tracks) {
        val format = tracks.groups
            .asSequence()
            .flatMap { group -> (0 until group.length).asSequence().map { group.getTrackFormat(it) } }
            .firstOrNull { it.sampleMimeType?.startsWith("audio/") == true }
            ?: return
        _state.update {
            it.copy(
                audioInfo = AudioPlaybackInfo(
                    mimeType = format.sampleMimeType,
                    codec = format.codecs,
                    bitrateKbps = format.bitrate.takeIf { value -> value > 0 }?.div(1000),
                    sampleRateHz = format.sampleRate.takeIf { value -> value > 0 },
                    channelCount = format.channelCount.takeIf { value -> value > 0 },
                    transcodingRequested = _state.value.requestedQuality.isTranscoded,
                ),
            )
        }
    }

    private fun syncState() {
        val player = controller ?: return
        val mediaId = player.currentMediaItem?.mediaId
        val index = player.currentMediaItemIndex
        val current = queueTracks.firstOrNull { it.id == mediaId }
            ?: _state.value.currentTrack.takeIf { mediaId == null }
        _state.update {
            it.copy(
                connected = true,
                currentTrack = current,
                isPlaying = player.isPlaying,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = player.duration.takeIf { duration -> duration > 0L } ?: current?.durationMs ?: 0L,
                shuffle = player.shuffleModeEnabled,
                repeatMode = player.repeatMode.toRepeatMode(),
                queue = queueTracks,
                currentIndex = index,
                error = null,
            )
        }
    }

    // Commands -------------------------------------------------------------

    fun playTracks(request: PlaybackRequest) {
        val player = controller
        if (player == null) {
            pendingRequest = request
            connect()
            return
        }
        val quality = settings.streamingQuality
        val items = request.tracks.mapNotNull { buildMediaItem(it, quality) }
        if (items.isEmpty()) {
            _state.update { it.copy(error = "Connect to a server before playing music") }
            return
        }
        queueTracks = request.tracks
        _state.update { it.copy(queue = request.tracks, requestedQuality = quality, error = null) }
        player.setMediaItems(items, request.startIndex.coerceIn(0, items.lastIndex), 0L)
        player.shuffleModeEnabled = request.shuffle || settings.shuffleByDefault
        player.repeatMode = Player.REPEAT_MODE_OFF
        player.prepare()
        player.play()
        persistQueue()
    }

    fun playNext(track: Track) {
        val player = controller ?: return
        val item = buildMediaItem(track, settings.streamingQuality) ?: return
        val insertIndex = (player.currentMediaItemIndex + 1).coerceIn(0, player.mediaItemCount)
        queueTracks = queueTracks.toMutableList().apply { add(insertIndex.coerceAtMost(size), track) }
        player.addMediaItem(insertIndex, item)
        _state.update { it.copy(queue = queueTracks) }
        persistQueue()
    }

    fun addToQueue(track: Track) {
        val player = controller ?: return
        val item = buildMediaItem(track, settings.streamingQuality) ?: return
        queueTracks = queueTracks + track
        player.addMediaItem(item)
        _state.update { it.copy(queue = queueTracks) }
        persistQueue()
    }

    fun addAllToQueue(tracks: List<Track>) {
        tracks.forEach { addToQueue(it) }
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) player.pause() else player.play()
        syncState()
    }

    fun play() {
        controller?.play()
    }

    fun pause() {
        controller?.pause()
    }

    fun next() {
        controller?.let { player ->
            if (player.hasNextMediaItem()) player.seekToNextMediaItem() else player.seekTo(0L)
        }
    }

    fun previous() {
        val player = controller ?: return
        if (player.currentPosition > 3_000L) {
            player.seekTo(0L)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0L)
        }
        syncState()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
        _state.update { it.copy(positionMs = positionMs.coerceAtLeast(0L)) }
    }

    fun toggleShuffle() {
        val player = controller ?: return
        player.shuffleModeEnabled = !player.shuffleModeEnabled
        syncState()
        persistQueue()
    }

    fun cycleRepeatMode() {
        val player = controller ?: return
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        syncState()
        persistQueue()
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val player = controller ?: return
        if (fromIndex !in queueTracks.indices) return
        val target = toIndex.coerceIn(0, queueTracks.lastIndex)
        player.moveMediaItem(fromIndex, target)
        queueTracks = queueTracks.toMutableList().apply { add(target, removeAt(fromIndex)) }
        _state.update { it.copy(queue = queueTracks) }
        persistQueue()
    }

    fun removeQueueItem(index: Int) {
        val player = controller ?: return
        if (index !in queueTracks.indices) return
        player.removeMediaItem(index)
        queueTracks = queueTracks.toMutableList().apply { removeAt(index) }
        _state.update { it.copy(queue = queueTracks) }
        persistQueue()
    }

    fun playQueueIndex(index: Int) {
        val player = controller ?: return
        if (index !in 0 until player.mediaItemCount) return
        player.seekToDefaultPosition(index)
        player.play()
        syncState()
    }

    fun clearQueue() {
        controller?.clearMediaItems()
        queueTracks = emptyList()
        _state.update { PlaybackState(connected = true) }
        scope.launch { queueStore.clear() }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private fun persistQueue() {
        val state = _state.value
        if (queueTracks.isEmpty()) return
        val snapshot = QueueSnapshot(
            tracks = queueTracks,
            index = state.currentIndex.coerceAtLeast(0),
            positionMs = state.positionMs,
            shuffle = state.shuffle,
            repeatMode = state.repeatMode,
            quality = state.requestedQuality,
        )
        scope.launch { queueStore.save(snapshot) }
    }

    private fun buildMediaItem(track: Track, quality: StreamQuality): MediaItem? {
        val streamUrl = musicRepository.streamUrl(track.id, quality) ?: return null
        val artworkUrl = musicRepository.coverArtUrl(track.coverArtId, 640)
        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artistName)
                    .setAlbumTitle(track.albumName)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .apply { if (artworkUrl != null) setArtworkUri(artworkUrl.toUri()) }
                    .build(),
            )
            .build()
    }

    fun release() {
        stopTicker()
        controller?.release()
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }

    private fun Int.toRepeatMode(): RepeatMode = when (this) {
        Player.REPEAT_MODE_ALL -> RepeatMode.ALL
        Player.REPEAT_MODE_ONE -> RepeatMode.ONE
        else -> RepeatMode.OFF
    }

    private fun RepeatMode.toPlayerRepeatMode(): Int = when (this) {
        RepeatMode.OFF -> Player.REPEAT_MODE_OFF
        RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        RepeatMode.ONE -> Player.REPEAT_MODE_ONE
    }
}
