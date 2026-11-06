package com.navidrometv.presentation.screens.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navidrometv.core.common.AppError
import com.navidrometv.core.common.DataResult
import com.navidrometv.core.model.Album
import com.navidrometv.core.model.Artist
import com.navidrometv.core.model.Genre
import com.navidrometv.core.model.Playlist
import com.navidrometv.domain.repository.AlbumListType
import com.navidrometv.domain.repository.MusicRepository
import com.navidrometv.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Decade(val label: String, val fromYear: Int, val toYear: Int)

val DefaultDecades = listOf(
    Decade("2020s", 2020, 2029),
    Decade("2010s", 2010, 2019),
    Decade("2000s", 2000, 2009),
    Decade("1990s", 1990, 1999),
    Decade("1980s", 1980, 1989),
    Decade("1970s", 1970, 1979),
    Decade("1960s", 1960, 1969),
    Decade("1950s", 1950, 1959),
)

data class BrowseUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val genres: List<Genre> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val discover: List<Album> = emptyList(),
    val decade: Decade? = null,
    val decadeAlbums: List<Album> = emptyList(),
    val decadeLoading: Boolean = false,
    val decadeError: AppError? = null,
)

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BrowseUiState())
    val state: StateFlow<BrowseUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            coroutineScope {
                val genres = async { musicRepository.getGenres() }
                val artists = async { musicRepository.getArtists() }
                val playlists = async { playlistRepository.getPlaylists() }
                val discover = async { musicRepository.getAlbumList(AlbumListType.RANDOM, size = 24) }

                val genreList = genres.await().getOrNull().orEmpty()
                val artistList = artists.await().getOrNull().orEmpty()
                val playlistList = playlists.await().getOrNull().orEmpty()
                val discoverList = discover.await().getOrNull().orEmpty()

                if (genreList.isEmpty() && artistList.isEmpty() && discoverList.isEmpty()) {
                    val failure = (genres.await() as? DataResult.Failure)?.error
                    if (failure != null) {
                        _state.update { it.copy(loading = false, error = failure) }
                        return@coroutineScope
                    }
                }

                _state.update {
                    it.copy(
                        loading = false,
                        error = null,
                        genres = genreList,
                        artists = artistList.take(24),
                        playlists = playlistList.take(12),
                        discover = discoverList,
                    )
                }
            }
        }
    }

    /** Decades are real server queries (albumList2 type=byYear), not client filters. */
    fun selectDecade(decade: Decade) {
        _state.update { it.copy(decade = decade, decadeLoading = true, decadeError = null, decadeAlbums = emptyList()) }
        viewModelScope.launch {
            val result = musicRepository.getAlbumList(
                type = AlbumListType.BY_YEAR,
                size = 60,
                fromYear = decade.fromYear,
                toYear = decade.toYear,
            )
            when (result) {
                is DataResult.Success -> _state.update {
                    it.copy(decadeLoading = false, decadeAlbums = result.data)
                }
                is DataResult.Failure -> _state.update {
                    it.copy(decadeLoading = false, decadeError = result.error)
                }
            }
        }
    }

    fun clearDecade() {
        _state.update { it.copy(decade = null, decadeAlbums = emptyList(), decadeError = null) }
    }

    fun retryDecade() {
        _state.value.decade?.let(::selectDecade)
    }
}
