package com.navidrometv.presentation.screens.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navidrometv.core.common.AppError
import com.navidrometv.core.common.DataResult
import com.navidrometv.core.model.AlbumDetail
import com.navidrometv.core.model.Playlist
import com.navidrometv.core.model.Track
import com.navidrometv.domain.repository.MusicRepository
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

data class AlbumUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val albumId: String = "",
    val detail: AlbumDetail? = null,
    val playlists: List<Playlist> = emptyList(),
    val favoritePending: Boolean = false,
    val message: String? = null,
)

sealed interface AlbumEvent {
    data object OpenNowPlaying : AlbumEvent
    data class Message(val text: String) : AlbumEvent
}

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val playMedia: PlayMediaUseCase,
    playbackRepository: PlaybackRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AlbumUiState())
    val state: StateFlow<AlbumUiState> = _state.asStateFlow()

    val playback: StateFlow<PlaybackState> = playbackRepository.state

    private val _events = MutableSharedFlow<AlbumEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<AlbumEvent> = _events.asSharedFlow()

    fun load(albumId: String) {
        if (albumId.isBlank()) return
        if (_state.value.albumId == albumId && _state.value.detail != null) return
        _state.update { it.copy(loading = true, error = null, albumId = albumId) }
        viewModelScope.launch {
            when (val result = musicRepository.getAlbum(albumId)) {
                is DataResult.Success -> _state.update {
                    it.copy(loading = false, detail = result.data, error = null)
                }
                is DataResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.error)
                }
            }
        }
    }

    fun retry() = load(_state.value.albumId)

    fun play(startIndex: Int = 0) {
        val albumId = _state.value.albumId
        viewModelScope.launch {
            when (val result = playMedia.playAlbum(albumId, startIndex = startIndex)) {
                is DataResult.Success -> _events.tryEmit(AlbumEvent.OpenNowPlaying)
                is DataResult.Failure -> _events.tryEmit(AlbumEvent.Message(result.error.message))
            }
        }
    }

    fun shuffle() {
        val albumId = _state.value.albumId
        viewModelScope.launch {
            when (val result = playMedia.playAlbum(albumId, shuffle = true)) {
                is DataResult.Success -> _events.tryEmit(AlbumEvent.OpenNowPlaying)
                is DataResult.Failure -> _events.tryEmit(AlbumEvent.Message(result.error.message))
            }
        }
    }

    fun addAlbumToQueue() {
        val tracks = _state.value.detail?.tracks.orEmpty()
        if (tracks.isEmpty()) return
        playMedia.addToQueueAll(tracks)
        _events.tryEmit(AlbumEvent.Message("Added ${tracks.size} songs to the queue"))
    }

    fun playNext(track: Track) {
        playMedia.playNext(track)
        _events.tryEmit(AlbumEvent.Message("Playing next: ${track.title}"))
    }

    fun toggleFavorite() {
        val album = _state.value.detail?.album ?: return
        if (_state.value.favoritePending) return
        _state.update { it.copy(favoritePending = true) }
        viewModelScope.launch {
            val favorite = !album.isFavorite
            when (val result = musicRepository.setAlbumFavorite(album.id, favorite)) {
                is DataResult.Success -> _state.update { current ->
                    current.copy(
                        favoritePending = false,
                        detail = current.detail?.copy(album = album.copy(isFavorite = favorite)),
                    )
                }
                is DataResult.Failure -> {
                    _state.update { it.copy(favoritePending = false) }
                    _events.tryEmit(AlbumEvent.Message(result.error.message))
                }
            }
        }
    }

    fun toggleTrackFavorite(track: Track) {
        viewModelScope.launch {
            val favorite = !track.isFavorite
            when (val result = musicRepository.setTrackFavorite(track.id, favorite)) {
                is DataResult.Success -> _state.update { current ->
                    val detail = current.detail ?: return@update current
                    current.copy(
                        detail = detail.copy(
                            tracks = detail.tracks.map {
                                if (it.id == track.id) it.copy(isFavorite = favorite) else it
                            },
                        ),
                    )
                }
                is DataResult.Failure -> _events.tryEmit(AlbumEvent.Message(result.error.message))
            }
        }
    }

    fun loadPlaylists() {
        if (_state.value.playlists.isNotEmpty()) return
        viewModelScope.launch {
            when (val result = playlistRepository.getPlaylists()) {
                is DataResult.Success -> _state.update { it.copy(playlists = result.data) }
                is DataResult.Failure -> _events.tryEmit(AlbumEvent.Message(result.error.message))
            }
        }
    }

    fun addAlbumToPlaylist(playlist: Playlist) {
        val trackIds = _state.value.detail?.tracks.orEmpty().map { it.id }
        if (trackIds.isEmpty()) return
        viewModelScope.launch {
            when (val result = playlistRepository.addTracks(playlist.id, trackIds)) {
                is DataResult.Success -> _events.tryEmit(
                    AlbumEvent.Message("Added ${trackIds.size} songs to ${playlist.name}"),
                )
                is DataResult.Failure -> _events.tryEmit(AlbumEvent.Message(result.error.message))
            }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }
}
