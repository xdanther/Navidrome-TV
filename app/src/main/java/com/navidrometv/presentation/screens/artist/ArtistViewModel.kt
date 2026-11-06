package com.navidrometv.presentation.screens.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navidrometv.core.common.AppError
import com.navidrometv.core.common.DataResult
import com.navidrometv.core.model.ArtistDetail
import com.navidrometv.core.model.Track
import com.navidrometv.domain.repository.MusicRepository
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

sealed interface ArtistEvent {
    data object OpenNowPlaying : ArtistEvent
    data class Message(val text: String) : ArtistEvent
}

data class ArtistUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val artistId: String = "",
    val detail: ArtistDetail? = null,
    val biographyExpanded: Boolean = false,
)

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playMedia: PlayMediaUseCase,
    playbackRepository: PlaybackRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ArtistUiState())
    val state: StateFlow<ArtistUiState> = _state.asStateFlow()

    val playback: StateFlow<PlaybackState> = playbackRepository.state

    private val _events = MutableSharedFlow<ArtistEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<ArtistEvent> = _events.asSharedFlow()

    fun load(artistId: String) {
        if (artistId.isBlank()) return
        if (_state.value.artistId == artistId && _state.value.detail != null) return
        _state.update { it.copy(loading = true, error = null, artistId = artistId) }
        viewModelScope.launch {
            when (val result = musicRepository.getArtist(artistId)) {
                is DataResult.Success -> _state.update { it.copy(loading = false, detail = result.data) }
                is DataResult.Failure -> _state.update { it.copy(loading = false, error = result.error) }
            }
        }
    }

    fun retry() = load(_state.value.artistId)

    fun play(shuffle: Boolean = false) {
        val artistId = _state.value.artistId
        viewModelScope.launch {
            when (val result = playMedia.playArtist(artistId, shuffle = shuffle)) {
                is DataResult.Success -> _events.tryEmit(ArtistEvent.OpenNowPlaying)
                is DataResult.Failure -> _events.tryEmit(ArtistEvent.Message(result.error.message))
            }
        }
    }

    fun playTrack(tracks: List<Track>, index: Int) {
        viewModelScope.launch {
            when (val result = playMedia.playTracks(tracks, startIndex = index)) {
                is DataResult.Success -> _events.tryEmit(ArtistEvent.OpenNowPlaying)
                is DataResult.Failure -> _events.tryEmit(ArtistEvent.Message(result.error.message))
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
                            topTracks = detail.topTracks.map {
                                if (it.id == track.id) it.copy(isFavorite = favorite) else it
                            },
                        ),
                    )
                }
                is DataResult.Failure -> _events.tryEmit(ArtistEvent.Message(result.error.message))
            }
        }
    }

    fun toggleBiography() {
        _state.update { it.copy(biographyExpanded = !it.biographyExpanded) }
    }
}
