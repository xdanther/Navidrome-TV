package com.navidrometv.presentation.screens.library

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryCategory(val label: String) {
    RECENTLY_ADDED("Recently Added"),
    RECENTLY_PLAYED("Recently Played"),
    PLAYLISTS("Playlists"),
    ARTISTS("Artists"),
    ALBUMS("Albums"),
    SONGS("Songs"),
    GENRES("Genres"),
    FAVORITES("Favorites"),
    HISTORY("Listening History"),
}

enum class LibraryLayout { GRID, LIST }

enum class AlbumSort(val label: String) {
    RECENT("Recently added"),
    ALPHABETICAL("A–Z"),
    ARTIST("By artist"),
}

data class LibraryUiState(
    val category: LibraryCategory = LibraryCategory.RECENTLY_ADDED,
    val layout: LibraryLayout = LibraryLayout.GRID,
    val sort: AlbumSort = AlbumSort.RECENT,
    val loading: Boolean = true,
    val error: AppError? = null,
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val tracks: List<Track> = emptyList(),
    val note: String? = null,
) {
    val isEmpty: Boolean
        get() = albums.isEmpty() && artists.isEmpty() && playlists.isEmpty() && genres.isEmpty() && tracks.isEmpty()
}

/**
 * Library browsing. Sorting, layout and category selection are all real queries:
 * nothing is re-sorted client-side behind a fake toggle.
 */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val historyRepository: HistoryRepository,
    private val playMedia: PlayMediaUseCase,
    playbackRepository: PlaybackRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    val playback: StateFlow<PlaybackState> = playbackRepository.state

    init {
        load(LibraryCategory.RECENTLY_ADDED)
    }

    fun select(category: LibraryCategory) {
        if (_state.value.category == category && !_state.value.isEmpty) return
        _state.update { it.copy(category = category) }
        load(category)
    }

    fun setLayout(layout: LibraryLayout) {
        _state.update { it.copy(layout = layout) }
    }

    fun setSort(sort: AlbumSort) {
        _state.update { it.copy(sort = sort) }
        load(_state.value.category)
    }

    fun retry() = load(_state.value.category)

    private fun load(category: LibraryCategory) {
        _state.update { it.copy(loading = true, error = null, note = null) }
        viewModelScope.launch {
            val sort = _state.value.sort
            when (category) {
                LibraryCategory.RECENTLY_ADDED -> loadAlbums(AlbumListType.NEWEST, note = null)
                LibraryCategory.RECENTLY_PLAYED -> {
                    val history = historyRepository.recentlyPlayedAlbums(30)
                    val result = musicRepository.getAlbumList(AlbumListType.RECENTLY_PLAYED, size = 60)
                    when (result) {
                        is DataResult.Success -> applyAlbums((history + result.data).distinctBy { it.id })
                        is DataResult.Failure -> if (history.isNotEmpty()) {
                            applyAlbums(history)
                        } else {
                            _state.update { it.copy(loading = false, error = result.error) }
                        }
                    }
                }
                LibraryCategory.PLAYLISTS -> when (val result = playlistRepository.getPlaylists()) {
                    is DataResult.Success -> _state.update {
                        it.copy(loading = false, playlists = result.data, albums = emptyList(), tracks = emptyList(), artists = emptyList(), genres = emptyList())
                    }
                    is DataResult.Failure -> _state.update { it.copy(loading = false, error = result.error) }
                }
                LibraryCategory.ARTISTS -> when (val result = musicRepository.getArtists()) {
                    is DataResult.Success -> _state.update {
                        it.copy(loading = false, artists = result.data, albums = emptyList(), tracks = emptyList(), playlists = emptyList(), genres = emptyList())
                    }
                    is DataResult.Failure -> _state.update { it.copy(loading = false, error = result.error) }
                }
                LibraryCategory.ALBUMS -> loadAlbums(
                    when (sort) {
                        AlbumSort.RECENT -> AlbumListType.NEWEST
                        AlbumSort.ALPHABETICAL -> AlbumListType.ALPHABETICAL_BY_NAME
                        AlbumSort.ARTIST -> AlbumListType.ALPHABETICAL_BY_ARTIST
                    },
                    note = null,
                )
                LibraryCategory.SONGS -> {
                    // Subsonic has no "all songs" endpoint. Rather than invent one, the
                    // Songs category presents a server-side random sample of the library
                    // and says so; search is the precise way to find a specific song.
                    when (val result = musicRepository.getRandomTracks(size = 100)) {
                        is DataResult.Success -> _state.update {
                            it.copy(
                                loading = false,
                                tracks = result.data,
                                albums = emptyList(),
                                artists = emptyList(),
                                playlists = emptyList(),
                                genres = emptyList(),
                                note = "Random sample from your library — use Search for a specific song",
                            )
                        }
                        is DataResult.Failure -> _state.update { it.copy(loading = false, error = result.error) }
                    }
                }
                LibraryCategory.GENRES -> when (val result = musicRepository.getGenres()) {
                    is DataResult.Success -> _state.update {
                        it.copy(loading = false, genres = result.data, albums = emptyList(), artists = emptyList(), playlists = emptyList(), tracks = emptyList())
                    }
                    is DataResult.Failure -> _state.update { it.copy(loading = false, error = result.error) }
                }
                LibraryCategory.FAVORITES -> when (val result = musicRepository.getStarred()) {
                    is DataResult.Success -> _state.update {
                        it.copy(
                            loading = false,
                            albums = result.data.albums,
                            tracks = result.data.tracks,
                            artists = result.data.artists,
                            playlists = emptyList(),
                            genres = emptyList(),
                        )
                    }
                    is DataResult.Failure -> _state.update { it.copy(loading = false, error = result.error) }
                }
                LibraryCategory.HISTORY -> {
                    val albums = historyRepository.recentlyPlayedAlbums(30)
                    val tracks = historyRepository.recentlyPlayedTracks(50)
                    _state.update {
                        it.copy(
                            loading = false,
                            albums = albums,
                            tracks = tracks,
                            artists = emptyList(),
                            playlists = emptyList(),
                            genres = emptyList(),
                            note = if (albums.isEmpty() && tracks.isEmpty()) "Nothing played on this device yet" else null,
                        )
                    }
                }
            }
        }
    }

    private suspend fun loadAlbums(type: AlbumListType, note: String?) {
        when (val result = musicRepository.getAlbumList(type, size = 60)) {
            is DataResult.Success -> _state.update {
                it.copy(
                    loading = false,
                    albums = result.data,
                    artists = emptyList(),
                    tracks = emptyList(),
                    playlists = emptyList(),
                    genres = emptyList(),
                    note = note,
                )
            }
            is DataResult.Failure -> _state.update { it.copy(loading = false, error = result.error) }
        }
    }

    private fun applyAlbums(albums: List<Album>) {
        _state.update {
            it.copy(loading = false, albums = albums, artists = emptyList(), tracks = emptyList(), playlists = emptyList(), genres = emptyList())
        }
    }

    fun playTrack(index: Int) {
        val tracks = _state.value.tracks
        if (index !in tracks.indices) return
        viewModelScope.launch { playMedia.playTracks(tracks, startIndex = index) }
    }

    fun toggleTrackFavorite(track: Track) {
        viewModelScope.launch {
            val favorite = !track.isFavorite
            if (musicRepository.setTrackFavorite(track.id, favorite) is DataResult.Success) {
                _state.update { current ->
                    current.copy(
                        tracks = current.tracks.map {
                            if (it.id == track.id) it.copy(isFavorite = favorite) else it
                        },
                    )
                }
            }
        }
    }
}
