package com.navidrometv.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navidrometv.core.common.AppError
import com.navidrometv.core.common.DataResult
import com.navidrometv.core.model.Album
import com.navidrometv.core.model.Artist
import com.navidrometv.core.model.Genre
import com.navidrometv.core.model.Playlist
import com.navidrometv.core.model.Track
import com.navidrometv.domain.repository.AlbumListType
import com.navidrometv.domain.repository.HistoryRepository
import com.navidrometv.domain.repository.MusicRepository
import com.navidrometv.domain.repository.PlaylistRepository
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

data class HomeUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val featured: List<Album> = emptyList(),
    val recentlyPlayed: List<Album> = emptyList(),
    val recentlyAdded: List<Album> = emptyList(),
    val playedArtists: List<Artist> = emptyList(),
    val mostPlayed: List<Track> = emptyList(),
    val favoriteAlbums: List<Album> = emptyList(),
    val favoriteTracks: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val discover: List<Album> = emptyList(),
    val frequentlyPlayed: List<Album> = emptyList(),
    val hasHistory: Boolean = false,
) {
    val isEmpty: Boolean
        get() = featured.isEmpty() && recentlyAdded.isEmpty() && recentlyPlayed.isEmpty() && playlists.isEmpty()
}

/**
 * Home feed. Every section is built from real server data or this device's own
 * listening history; there are no invented "recommendations". Sections with no
 * data are hidden instead of showing placeholders.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val historyRepository: HistoryRepository,
    private val playMedia: PlayMediaUseCase,
    playbackRepository: PlaybackRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val playback: StateFlow<PlaybackState> = playbackRepository.state

    private val _openNowPlaying = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openNowPlaying: SharedFlow<Unit> = _openNowPlaying.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            val historyAlbums = historyRepository.recentlyPlayedAlbums(12)
            val historyArtists = historyRepository.recentlyPlayedArtists(12)
            val mostPlayed = historyRepository.mostPlayedTracks(8)
            val hasHistory = historyRepository.hasHistory()

            coroutineScope {
                val newest = async { musicRepository.getAlbumList(AlbumListType.NEWEST, size = 24) }
                val recent = async { musicRepository.getAlbumList(AlbumListType.RECENTLY_PLAYED, size = 24) }
                val frequent = async { musicRepository.getAlbumList(AlbumListType.FREQUENT, size = 16) }
                val random = async { musicRepository.getAlbumList(AlbumListType.RANDOM, size = 16) }
                val starred = async { musicRepository.getStarred() }
                val playlists = async { playlistRepository.getPlaylists() }
                val genres = async { musicRepository.getGenres() }

                val newestAlbums = newest.await().getOrNull().orEmpty()
                val recentAlbums = recent.await().getOrNull().orEmpty()

                if (newestAlbums.isEmpty() && recentAlbums.isEmpty() && !hasHistory) {
                    val failure = (newest.await() as? DataResult.Failure)?.error
                        ?: (recent.await() as? DataResult.Failure)?.error
                    _state.update { it.copy(loading = false, error = failure) }
                    return@coroutineScope
                }

                val starredContent = starred.await().getOrNull()
                val recentlyPlayed = (historyAlbums + recentAlbums)
                    .distinctBy { it.id }
                    .take(20)

                _state.update { current ->
                    current.copy(
                        loading = false,
                        error = null,
                        hasHistory = hasHistory,
                        featured = (historyAlbums.take(4) + newestAlbums).distinctBy { it.id }.take(6),
                        recentlyPlayed = recentlyPlayed,
                        recentlyAdded = newestAlbums,
                        playedArtists = (historyArtists + (starredContent?.artists.orEmpty())).distinctBy { it.id }.take(20),
                        mostPlayed = mostPlayed,
                        favoriteAlbums = starredContent?.albums.orEmpty(),
                        favoriteTracks = starredContent?.tracks.orEmpty(),
                        playlists = playlists.await().getOrNull().orEmpty().take(12),
                        genres = genres.await().getOrNull().orEmpty().take(10),
                        discover = random.await().getOrNull().orEmpty(),
                        frequentlyPlayed = frequent.await().getOrNull().orEmpty(),
                    )
                }
            }
        }
    }

    fun playMostPlayed(index: Int) {
        val tracks = _state.value.mostPlayed
        if (tracks.isEmpty()) return
        viewModelScope.launch {
            if (playMedia.playTracks(tracks, startIndex = index) is DataResult.Success) {
                _openNowPlaying.tryEmit(Unit)
            }
        }
    }

    fun playFavorites(index: Int) {
        val tracks = _state.value.favoriteTracks
        if (tracks.isEmpty()) return
        viewModelScope.launch {
            if (playMedia.playTracks(tracks, startIndex = index) is DataResult.Success) {
                _openNowPlaying.tryEmit(Unit)
            }
        }
    }

    fun playAlbum(albumId: String) {
        viewModelScope.launch {
            if (playMedia.playAlbum(albumId) is DataResult.Success) {
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
                        mostPlayed = current.mostPlayed.map { if (it.id == track.id) it.copy(isFavorite = favorite) else it },
                        favoriteTracks = if (favorite) {
                            current.favoriteTracks + track.copy(isFavorite = true)
                        } else {
                            current.favoriteTracks.filterNot { it.id == track.id }
                        },
                    )
                }
            }
        }
    }
}
