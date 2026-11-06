package com.navidrometv.presentation.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navidrometv.core.common.AppError
import com.navidrometv.core.common.DataResult
import com.navidrometv.core.model.SearchResults
import com.navidrometv.core.model.Track
import com.navidrometv.domain.repository.MusicRepository
import com.navidrometv.domain.repository.SettingsRepository
import com.navidrometv.domain.usecase.PlayMediaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val error: AppError? = null,
    val results: SearchResults? = null,
    val recentSearches: List<String> = emptyList(),
    val submittedQuery: String = "",
) {
    val suggestions: List<String>
        get() = recentSearches
            .filter { it.contains(query, ignoreCase = true) && !it.equals(query, ignoreCase = true) }
            .take(5)

    val hasResults: Boolean get() = results != null && !results.isEmpty
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val settingsRepository: SettingsRepository,
    private val playMedia: PlayMediaUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { it.copy(recentSearches = settings.recentSearches) }
            }
        }
    }

    fun onQueryChanged(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun appendCharacter(character: Char) {
        _state.update { it.copy(query = it.query + character) }
    }

    fun backspace() {
        _state.update { it.copy(query = it.query.dropLast(1)) }
    }

    fun clearQuery() {
        _state.update { it.copy(query = "", results = null, error = null, submittedQuery = "") }
    }

    fun clearHistory() {
        viewModelScope.launch { settingsRepository.clearRecentSearches() }
    }

    fun useRecent(query: String) {
        _state.update { it.copy(query = query) }
        submit()
    }

    fun submit() {
        val query = _state.value.query.trim()
        if (query.length < 2) return
        _state.update { it.copy(loading = true, error = null, submittedQuery = query) }
        viewModelScope.launch {
            when (val result = musicRepository.search(query)) {
                is DataResult.Success -> _state.update {
                    it.copy(loading = false, results = result.data, error = null)
                }
                is DataResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.error)
                }
            }
            settingsRepository.addRecentSearch(query)
        }
    }

    fun retry() = submit()

    fun playTrack(index: Int) {
        val tracks = _state.value.results?.tracks.orEmpty()
        if (index !in tracks.indices) return
        viewModelScope.launch { playMedia.playTracks(tracks, startIndex = index) }
    }

    fun toggleTrackFavorite(track: Track) {
        viewModelScope.launch {
            val favorite = !track.isFavorite
            if (musicRepository.setTrackFavorite(track.id, favorite) is DataResult.Success) {
                _state.update { current ->
                    val results = current.results ?: return@update current
                    current.copy(
                        results = results.copy(
                            tracks = results.tracks.map {
                                if (it.id == track.id) it.copy(isFavorite = favorite) else it
                            },
                        ),
                    )
                }
            }
        }
    }
}
