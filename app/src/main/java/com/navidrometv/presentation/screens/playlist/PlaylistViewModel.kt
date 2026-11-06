package com.navidrometv.presentation.screens.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navidrometv.core.common.AppError
import com.navidrometv.core.common.DataResult
import com.navidrometv.domain.repository.MusicRepository
import com.navidrometv.domain.repository.PlaylistDetail
import com.navidrometv.domain.repository.PlaylistRepository
import com.navidrometv.domain.usecase.PlayMediaUseCase
import com.navidrometv.playback.PlaybackRepository
import com.navidrometv.playback.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistUiState(
    val playlistId: String = "",
    val loading: Boolean = true,
    val error: AppError? = null,
    val detail: PlaylistDetail? = null,
    val busy: Boolean = false,
    val message: String? = null,
)

sealed interface PlaylistEvent {
    data object OpenNowPlaying : PlaylistEvent
    data object Deleted : PlaylistEvent
    data class Message(val text: String) : PlaylistEvent
}

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val musicRepository: MusicRepository,
    private val playMedia: PlayMediaUseCase,
    playbackRepository: PlaybackRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PlaylistUiState())
    val state: StateFlow<PlaylistUiState> = _state.asStateFlow()

    val playback: StateFlow<PlaybackState> = playbackRepository.state

    private val _events = MutableSharedFlow<PlaylistEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<PlaylistEvent> = _events.asSharedFlow()

    fun load(playlistId: String) {
        if (playlistId.isBlank()) return
        if (_state.value.playlistId == playlistId && _state.value.detail != null) return
        _state.update { it.copy(playlistId = playlistId, loading = true, error = null) }
        viewModelScope.launch {
            when (val result = playlistRepository.getPlaylist(playlistId)) {
                is DataResult.Success -> _state.update { it.copy(loading = false, detail = result.data) }
                is DataResult.Failure -> _state.update { it.copy(loading = false, error = result.error) }
            }
        }
    }

    fun retry() = load(_state.value.playlistId)

    fun play(shuffle: Boolean = false) {
        viewModelScope.launch {
            when (val result = playMedia.playPlaylist(_state.value.playlistId, shuffle = shuffle)) {
                is DataResult.Success -> _events.tryEmit(PlaylistEvent.OpenNowPlaying)
                is DataResult.Failure -> _events.tryEmit(PlaylistEvent.Message(result.error.message))
            }
        }
    }

    fun playTrack(index: Int) {
        val tracks = _state.value.detail?.tracks.orEmpty()
        if (index !in tracks.indices) return
        viewModelScope.launch {
            when (val result = playMedia.playTracks(tracks, startIndex = index)) {
                is DataResult.Success -> _events.tryEmit(PlaylistEvent.OpenNowPlaying)
                is DataResult.Failure -> _events.tryEmit(PlaylistEvent.Message(result.error.message))
            }
        }
    }

    fun addAllToQueue() {
        val tracks = _state.value.detail?.tracks.orEmpty()
        if (tracks.isEmpty()) return
        playMedia.addToQueueAll(tracks)
        _events.tryEmit(PlaylistEvent.Message("Added ${tracks.size} songs to the queue"))
    }

    fun removeAt(index: Int) {
        val playlistId = _state.value.playlistId
        viewModelScope.launch {
            when (val result = playlistRepository.removeTrackAt(playlistId, index)) {
                is DataResult.Success -> {
                    // Reload because the server re-numbers playlist entries after a removal.
                    _state.update { it.copy(detail = null, loading = true) }
                    load(playlistId)
                    _events.tryEmit(PlaylistEvent.Message("Removed from playlist"))
                }
                is DataResult.Failure -> _events.tryEmit(PlaylistEvent.Message(result.error.message))
            }
        }
    }

    fun toggleFavorite(index: Int) {
        val detail = _state.value.detail ?: return
        val track = detail.tracks.getOrNull(index) ?: return
        val favorite = !track.isFavorite
        viewModelScope.launch {
            when (val result = musicRepository.setTrackFavorite(track.id, favorite)) {
                is DataResult.Success -> {
                    _state.update { current ->
                        current.copy(
                            detail = current.detail?.copy(
                                tracks = current.detail.tracks.map { item ->
                                    if (item.id == track.id) item.copy(isFavorite = favorite) else item
                                },
                            ),
                        )
                    }
                    _events.tryEmit(
                        PlaylistEvent.Message(if (favorite) "Added to favorites" else "Removed from favorites"),
                    )
                }
                is DataResult.Failure -> _events.tryEmit(PlaylistEvent.Message(result.error.message))
            }
        }
    }

    fun rename(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.length < 1) return
        viewModelScope.launch {
            when (val result = playlistRepository.renamePlaylist(_state.value.playlistId, trimmed)) {
                is DataResult.Success -> {
                    _state.update { current ->
                        val detail = current.detail ?: return@update current
                        current.copy(detail = detail.copy(playlist = detail.playlist.copy(name = trimmed)))
                    }
                    _events.tryEmit(PlaylistEvent.Message("Renamed to $trimmed"))
                }
                is DataResult.Failure -> _events.tryEmit(PlaylistEvent.Message(result.error.message))
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            when (val result = playlistRepository.deletePlaylist(_state.value.playlistId)) {
                is DataResult.Success -> _events.tryEmit(PlaylistEvent.Deleted)
                is DataResult.Failure -> _events.tryEmit(PlaylistEvent.Message(result.error.message))
            }
        }
    }
}
