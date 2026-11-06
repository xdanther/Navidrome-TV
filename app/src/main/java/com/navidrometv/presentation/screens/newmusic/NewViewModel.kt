package com.navidrometv.presentation.screens.newmusic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navidrometv.core.common.AppError
import com.navidrometv.core.common.DataResult
import com.navidrometv.core.model.Album
import com.navidrometv.core.model.Artist
import com.navidrometv.core.model.Track
import com.navidrometv.domain.repository.AlbumListType
import com.navidrometv.domain.repository.MusicRepository
import com.navidrometv.domain.usecase.PlayMediaUseCase
import com.navidrometv.playback.PlaybackRepository
import com.navidrometv.playback.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val tracks: List<Track> = emptyList(),
    val order: NewOrder = NewOrder.NEWEST,
)

enum class NewOrder(val label: String) {
    NEWEST("Newest"),
    RECENTLY_PLAYED("Recently played"),
}

/**
 * New arrivals. "Recently added songs" is assembled from the tracks of the newest
 * albums, because Subsonic has no endpoint that lists recently added songs
 * directly; the screen states that provenance instead of guessing.
 */
@HiltViewModel
class NewViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playMedia: PlayMediaUseCase,
    playbackRepository: PlaybackRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(NewUiState())
    val state: StateFlow<NewUiState> = _state.asStateFlow()

    val playback: StateFlow<PlaybackState> = playbackRepository.state

    private val _openNowPlaying = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openNowPlaying: SharedFlow<Unit> = _openNowPlaying.asSharedFlow()

    init {
        refresh()
    }

    fun setOrder(order: NewOrder) {
        if (_state.value.order == order) return
        _state.update { it.copy(order = order) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            coroutineScope {
                val type = if (_state.value.order == NewOrder.NEWEST) AlbumListType.NEWEST else AlbumListType.RECENTLY_PLAYED
                val albumsDeferred = async { musicRepository.getAlbumList(type, size = 40) }
                val albumsResult = albumsDeferred.await()
                val albums = when (albumsResult) {
                    is DataResult.Failure -> {
                        _state.update { it.copy(loading = false, error = albumsResult.error) }
                        return@coroutineScope
                    }
                    is DataResult.Success -> albumsResult.data
                }

                val artists = albums.mapNotNull { album ->
                    album.artistId?.takeIf { it.isNotBlank() }?.let { id ->
                        Artist(id = id, name = album.artistName ?: "Unknown artist", coverArtId = "ar-$id")
                    }
                }.distinctBy { it.id }

                // Four newest albums is enough for a "new songs" shelf without
                // hammering the server with a request per album.
                val tracks = albums.take(4).flatMap { album ->
                    (musicRepository.getAlbum(album.id) as? DataResult.Success)?.data?.tracks.orEmpty()
                }

                _state.update {
                    it.copy(
                        loading = false,
                        error = null,
                        albums = albums,
                        artists = artists,
                        tracks = tracks,
                    )
                }
            }
        }
    }

    fun playTrack(index: Int) {
        val tracks = _state.value.tracks
        if (index !in tracks.indices) return
        viewModelScope.launch {
            if (playMedia.playTracks(tracks, startIndex = index) is DataResult.Success) {
                _openNowPlaying.tryEmit(Unit)
            }
        }
    }

    fun toggleTrackFavorite(track: Track) {
        viewModelScope.launch {
            val favorite = !track.isFavorite
            if (musicRepository.setTrackFavorite(track.id, favorite) is DataResult.Success) {
                _state.update { current ->
                    current.copy(
                        tracks = current.tracks.map { if (it.id == track.id) it.copy(isFavorite = favorite) else it },
                    )
                }
            }
        }
    }
}
