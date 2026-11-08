package com.navidrometv.presentation.screens.browse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.Text
import com.navidrometv.core.common.AppError
import com.navidrometv.core.common.DataResult
import com.navidrometv.core.designsystem.component.ActionButton
import com.navidrometv.core.designsystem.component.AlbumCard
import com.navidrometv.core.designsystem.component.EmptyState
import com.navidrometv.core.designsystem.component.ErrorState
import com.navidrometv.core.designsystem.component.LoadingState
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.NavidromeColors
import com.navidrometv.core.designsystem.theme.Space
import com.navidrometv.core.model.Album
import com.navidrometv.domain.repository.AlbumListType
import com.navidrometv.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GenreUiState(
    val genre: String = "",
    val loading: Boolean = true,
    val error: AppError? = null,
    val albums: List<Album> = emptyList(),
)

@HiltViewModel
class GenreViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GenreUiState())
    val state: StateFlow<GenreUiState> = _state.asStateFlow()

    fun load(genre: String) {
        if (genre.isBlank()) return
        if (_state.value.genre == genre && _state.value.albums.isNotEmpty()) return
        _state.update { it.copy(genre = genre, loading = true, error = null) }
        viewModelScope.launch {
            when (val result = musicRepository.getAlbumList(AlbumListType.BY_GENRE, size = 100, genre = genre)) {
                is DataResult.Success -> _state.update { it.copy(loading = false, albums = result.data) }
                is DataResult.Failure -> _state.update { it.copy(loading = false, error = result.error) }
            }
        }
    }

    fun retry() = load(_state.value.genre)
}

@Composable
fun GenreScreen(
    genreName: String,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    viewModel: GenreViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    LaunchedEffect(genreName) { viewModel.load(genreName) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = Space.screenHorizontal, vertical = Space.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            ActionButton(text = "Back", onClick = onBack, primary = false)
            Text(text = genreName, style = AppType.title, color = NavidromeColors.OnSurface)
        }

        when {
            state.loading -> LoadingState()
            state.error != null -> ErrorState(error = state.error!!, onRetry = viewModel::retry)
            state.albums.isEmpty() -> EmptyState(
                title = "No albums in $genreName",
                message = "This server has no albums tagged with this genre.",
            )
            else -> BoxWithConstraints(Modifier.fillMaxSize().padding(top = Space.md)) {
                val columns = 6
                val spacing = 30.dp
                val cardWidth = (maxWidth - spacing * (columns - 1)) / columns
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalArrangement = Arrangement.spacedBy(34.dp),
                    contentPadding = PaddingValues(bottom = Space.contentBottomWithPlayer),
                ) {
                    items(state.albums, key = { it.id }) { album ->
                        AlbumCard(album = album, onClick = { onOpenAlbum(album.id) }, width = cardWidth)
                    }
                }
            }
        }
    }
}
