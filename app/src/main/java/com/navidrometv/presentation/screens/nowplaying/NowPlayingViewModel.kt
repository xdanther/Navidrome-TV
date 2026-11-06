package com.navidrometv.presentation.screens.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navidrometv.core.common.DataResult
import com.navidrometv.core.model.Lyrics
import com.navidrometv.core.model.Track
import com.navidrometv.domain.repository.MusicRepository
import com.navidrometv.playback.PlaybackRepository
import com.navidrometv.playback.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NowPlayingUiState(
    val lyrics: Lyrics? = null,
    val lyricsLoading: Boolean = false,
    val lyricsUnavailableReason: String? = null,
    val favoriteOverride: Boolean? = null,
)

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val playbackRepository: PlaybackRepository,
    private val musicRepository: MusicRepository,
) : ViewModel() {

    val playback: StateFlow<PlaybackState> = playbackRepository.state

    private val _ui = MutableStateFlow(NowPlayingUiState())
    val ui: StateFlow<NowPlayingUiState> = _ui.asStateFlow()

    private var lyricsTrackId: String? = null

    init {
        viewModelScope.launch {
            playback.collectLatest { state ->
                val trackId = state.currentTrack?.id
                if (trackId != null && trackId != lyricsTrackId) {
                    lyricsTrackId = trackId
                    _ui.update { it.copy(lyrics = null, lyricsUnavailableReason = null, favoriteOverride = null) }
                    loadLyrics(trackId)
                }
            }
        }
    }

    private fun loadLyrics(trackId: String) {
        _ui.update { it.copy(lyricsLoading = true) }
        viewModelScope.launch {
            when (val result = musicRepository.getLyrics(trackId)) {
                is DataResult.Success -> _ui.update {
                    it.copy(
                        lyricsLoading = false,
                        lyrics = result.data,
                        lyricsUnavailableReason = if (result.data == null) {
                            "No lyrics on the server for this track"
                        } else {
                            null
                        },
                    )
                }
                is DataResult.Failure -> _ui.update {
                    it.copy(lyricsLoading = false, lyricsUnavailableReason = result.error.message)
                }
            }
        }
    }

    fun retryLyrics() {
        lyricsTrackId?.let { _ui.update { state -> state.copy(lyrics = null) }; loadLyrics(it) }
    }

    fun togglePlayPause() = playbackRepository.togglePlayPause()

    fun next() = playbackRepository.next()

    fun previous() = playbackRepository.previous()

    fun seekTo(positionMs: Long) = playbackRepository.seekTo(positionMs)

    fun toggleShuffle() = playbackRepository.toggleShuffle()

    fun cycleRepeat() = playbackRepository.cycleRepeatMode()

    fun removeFromQueue(index: Int) = playbackRepository.removeQueueItem(index)

    fun moveInQueue(from: Int, to: Int) = playbackRepository.moveQueueItem(from, to)

    fun clearQueue() = playbackRepository.clearQueue()

    fun playQueueIndex(index: Int) {
        playbackRepository.playQueueIndex(index)
    }

    fun toggleFavorite() {
        val track: Track = playback.value.currentTrack ?: return
        val current = _ui.value.favoriteOverride ?: track.isFavorite
        val target = !current
        _ui.update { it.copy(favoriteOverride = target) }
        viewModelScope.launch {
            val result = musicRepository.setTrackFavorite(track.id, target)
            if (result is DataResult.Failure) {
                _ui.update { it.copy(favoriteOverride = current) }
            }
        }
    }
}
