package com.navidrometv.presentation.screens.browse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.navidrometv.core.designsystem.component.ActionButton
import com.navidrometv.core.designsystem.component.AlbumCard
import com.navidrometv.core.designsystem.component.EmptyState
import com.navidrometv.core.designsystem.component.ErrorState
import com.navidrometv.core.designsystem.component.LoadingState
import com.navidrometv.core.designsystem.component.SectionHeader
import com.navidrometv.core.designsystem.component.TvClickable
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.Dimens
import com.navidrometv.core.designsystem.theme.NavidromeColors
import com.navidrometv.core.designsystem.theme.Space
import com.navidrometv.presentation.components.AlbumRow
import com.navidrometv.presentation.components.ArtistRow
import com.navidrometv.presentation.components.GenreRow
import com.navidrometv.presentation.components.PlaylistRow

@Composable
fun BrowseScreen(
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenGenre: (String) -> Unit,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val decade = state.decade
    if (decade != null) {
        BackHandler(onBack = viewModel::clearDecade)
        DecadeContent(
            state = state,
            onBack = viewModel::clearDecade,
            onRetry = viewModel::retryDecade,
            onOpenAlbum = onOpenAlbum,
        )
        return
    }

    when {
        state.loading -> LoadingState(label = "Browsing your library")
        state.error != null -> ErrorState(error = state.error!!, onRetry = viewModel::load)
        state.genres.isEmpty() && state.artists.isEmpty() && state.discover.isEmpty() -> EmptyState(
            title = "Nothing to browse yet",
            message = "Add music to Navidrome to see genres, artists and playlists here.",
        )
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Space.screenHorizontal,
                end = Space.screenHorizontal,
                top = Space.md,
                bottom = Space.contentBottomWithPlayer,
            ),
            verticalArrangement = Arrangement.spacedBy(Space.xl),
        ) {
            item(key = "title") {
                Text(text = "Browse", style = AppType.title, color = NavidromeColors.OnSurface)
            }
            if (state.genres.isNotEmpty()) {
                item(key = "genres") {
                    GenreRow(title = "Genres", genres = state.genres, onOpenGenre = onOpenGenre)
                }
            }
            item(key = "decades") {
                Column {
                    SectionHeader(title = "Decades")
                    LazyRow(
                        modifier = Modifier.padding(top = Space.md),
                        horizontalArrangement = Arrangement.spacedBy(26.dp),
                    ) {
                        items(DefaultDecades.size) { index ->
                            val decadeItem = DefaultDecades[index]
                            DecadeCard(decade = decadeItem, onClick = { viewModel.selectDecade(decadeItem) })
                        }
                    }
                }
            }
            if (state.playlists.isNotEmpty()) {
                item(key = "playlists") {
                    PlaylistRow(title = "Playlists", playlists = state.playlists, onOpenPlaylist = onOpenPlaylist)
                }
            }
            if (state.artists.isNotEmpty()) {
                item(key = "artists") {
                    ArtistRow(title = "Artists", artists = state.artists, onOpenArtist = onOpenArtist)
                }
            }
            if (state.discover.isNotEmpty()) {
                item(key = "discover") {
                    AlbumRow(title = "Random Selection", albums = state.discover, onOpenAlbum = onOpenAlbum)
                }
            }
        }
    }
}

@Composable
private fun DecadeCard(decade: Decade, onClick: () -> Unit) {
    TvClickable(onClick = onClick, modifier = Modifier.height(112.dp).width(160.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(listOf(NavidromeColors.SurfaceElevated, NavidromeColors.Surface)),
                    RoundedCornerShape(Dimens.cardCorner),
                )
                .padding(Space.lg),
            contentAlignment = Alignment.BottomStart,
        ) {
            Text(text = decade.label, style = AppType.section, color = NavidromeColors.OnSurface)
        }
    }
}

@Composable
private fun DecadeContent(
    state: BrowseUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenAlbum: (String) -> Unit,
) {
    val decade = state.decade ?: return
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = Space.screenHorizontal, vertical = Space.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            ActionButton(text = "Back", onClick = onBack, primary = false)
            Text(text = decade.label, style = AppType.title, color = NavidromeColors.OnSurface)
            Text(
                text = "${decade.fromYear}–${decade.toYear}",
                style = AppType.metadata,
                color = NavidromeColors.OnSurfaceMuted,
            )
        }

        when {
            state.decadeLoading -> LoadingState()
            state.decadeError != null -> ErrorState(error = state.decadeError!!, onRetry = onRetry)
            state.decadeAlbums.isEmpty() -> EmptyState(
                title = "No albums from the ${decade.label}",
                message = "Your server has no albums with release years in this range.",
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
                    items(state.decadeAlbums, key = { it.id }) { album ->
                        AlbumCard(album = album, onClick = { onOpenAlbum(album.id) }, width = cardWidth)
                    }
                }
            }
        }
    }
}
