package com.navidrometv.presentation.screens.newmusic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.navidrometv.core.designsystem.component.EmptyState
import com.navidrometv.core.designsystem.component.ErrorState
import com.navidrometv.core.designsystem.component.FilterChip
import com.navidrometv.core.designsystem.component.LoadingState
import com.navidrometv.core.designsystem.component.TrackRow
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.NavidromeColors
import com.navidrometv.core.designsystem.theme.Space
import com.navidrometv.presentation.components.AlbumRow
import com.navidrometv.presentation.components.ArtistRow

@Composable
fun NewScreen(
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenNowPlaying: () -> Unit,
    viewModel: NewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.openNowPlaying.collect { onOpenNowPlaying() }
    }

    when {
        state.loading -> LoadingState(label = "Looking for new music")
        state.error != null -> ErrorState(error = state.error!!, onRetry = viewModel::refresh)
        state.albums.isEmpty() && state.tracks.isEmpty() -> EmptyState(
            title = "No new music",
            message = "Nothing has been added to this server recently.",
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
            item(key = "header") {
                Column {
                    Text(text = "New", style = AppType.title, color = NavidromeColors.OnSurface)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Space.xs),
                        modifier = Modifier.padding(top = Space.sm),
                    ) {
                        NewOrder.entries.forEach { order ->
                            FilterChip(
                                label = order.label,
                                selected = state.order == order,
                                onClick = { viewModel.setOrder(order) },
                            )
                        }
                    }
                }
            }

            item(key = "albums") {
                AlbumRow(title = "Recently Added Albums", albums = state.albums, onOpenAlbum = onOpenAlbum)
            }

            if (state.artists.isNotEmpty()) {
                item(key = "artists") {
                    ArtistRow(
                        title = "Artists with New Releases",
                        artists = state.artists,
                        onOpenArtist = onOpenArtist,
                    )
                }
            }

            if (state.tracks.isNotEmpty()) {
                item(key = "tracks") {
                    Column {
                        Text(
                            text = "New Songs",
                            style = AppType.section,
                            color = NavidromeColors.OnSurface,
                        )
                        Text(
                            text = "From the newest albums on this server",
                            style = AppType.cardSubtitle,
                            color = NavidromeColors.OnSurfaceMuted,
                            modifier = Modifier.padding(bottom = Space.sm),
                        )
                        state.tracks.forEachIndexed { index, track ->
                            TrackRow(
                                track = track,
                                index = index,
                                isCurrent = playback.currentTrack?.id == track.id,
                                isPlaying = playback.isPlaying,
                                onPlay = { viewModel.playTrack(index) },
                                onToggleFavorite = { viewModel.toggleTrackFavorite(track) },
                                showAlbum = true,
                            )
                        }
                    }
                }
            }
        }
    }
}
