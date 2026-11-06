package com.navidrometv.presentation.screens.artist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.navidrometv.core.common.Formatters
import com.navidrometv.core.designsystem.component.ActionButton
import com.navidrometv.core.designsystem.component.ArtistCard
import com.navidrometv.core.designsystem.component.Artwork
import com.navidrometv.core.designsystem.component.CircleIconButton
import com.navidrometv.core.designsystem.component.ErrorState
import com.navidrometv.core.designsystem.component.LoadingState
import com.navidrometv.core.designsystem.component.SectionHeader
import com.navidrometv.core.designsystem.component.TrackRow
import com.navidrometv.core.designsystem.component.TvClickable
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.NavidromeColors
import com.navidrometv.core.designsystem.theme.Space
import com.navidrometv.presentation.components.AlbumRow

@Composable
fun ArtistScreen(
    artistId: String,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenNowPlaying: () -> Unit,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()

    BackHandler(onBack = onBack)
    LaunchedEffect(artistId) { viewModel.load(artistId) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ArtistEvent.OpenNowPlaying -> onOpenNowPlaying()
                is ArtistEvent.Message -> Unit
            }
        }
    }

    when {
        state.loading -> LoadingState()
        state.error != null -> ErrorState(error = state.error!!, onRetry = viewModel::retry)
        state.detail != null -> {
            val detail = state.detail!!
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Space.screenHorizontal,
                    end = Space.screenHorizontal,
                    top = Space.lg,
                    bottom = Space.contentBottomWithPlayer,
                ),
                verticalArrangement = Arrangement.spacedBy(Space.xl),
            ) {
                item(key = "header") {
                    Row(verticalAlignment = Alignment.Top) {
                        CircleIconButton(
                            icon = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            onClick = onBack,
                            size = 48,
                        )
                        Artwork(
                            coverArtId = detail.artist.coverArtId,
                            contentDescription = "${detail.artist.name} portrait",
                            modifier = Modifier
                                .padding(start = Space.md)
                                .size(220.dp),
                            shape = RoundedCornerShape(percent = 50),
                            requestSizePx = 600,
                        )
                        Column(
                            Modifier
                                .padding(start = Space.xl)
                                .weight(1f),
                        ) {
                            Text(text = "ARTIST", style = AppType.overline, color = NavidromeColors.OnSurfaceMuted)
                            Text(
                                text = detail.artist.name,
                                style = AppType.display,
                                color = NavidromeColors.OnSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = listOfNotNull(
                                    detail.artist.albumCount?.let { Formatters.plural(it, "album") },
                                    detail.topTracks.size.takeIf { it > 0 }?.let { "${Formatters.plural(it, "song")} in top songs" },
                                ).joinToString(" · "),
                                style = AppType.cardSubtitle,
                                color = NavidromeColors.OnSurfaceMuted,
                                modifier = Modifier.padding(top = 2.dp),
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                                modifier = Modifier.padding(top = Space.md),
                            ) {
                                ActionButton(
                                    text = "Play",
                                    onClick = { viewModel.play(shuffle = false) },
                                    icon = Icons.Rounded.PlayArrow,
                                )
                                ActionButton(
                                    text = "Shuffle",
                                    onClick = { viewModel.play(shuffle = true) },
                                    icon = Icons.Rounded.Shuffle,
                                    primary = false,
                                )
                            }

                            detail.artist.biography?.let { biography ->
                                Text(
                                    text = biography,
                                    style = AppType.bodyMuted,
                                    color = NavidromeColors.OnSurfaceMuted,
                                    maxLines = if (state.biographyExpanded) Int.MAX_VALUE else 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .padding(top = Space.md)
                                        .fillMaxWidth(0.85f),
                                )
                                TvClickable(
                                    onClick = viewModel::toggleBiography,
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text(
                                        text = if (state.biographyExpanded) "Show less" else "Read more",
                                        style = AppType.metadata,
                                        color = NavidromeColors.Accent,
                                        modifier = Modifier.padding(vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                if (detail.topTracks.isNotEmpty()) {
                    item(key = "top-tracks") {
                        Column {
                            SectionHeader(title = "Top Songs", subtitle = "from your server")
                            Column(Modifier.padding(top = Space.sm)) {
                                detail.topTracks.take(8).forEachIndexed { index, track ->
                                    TrackRow(
                                        track = track,
                                        index = index,
                                        isCurrent = playback.currentTrack?.id == track.id,
                                        isPlaying = playback.isPlaying,
                                        onPlay = { viewModel.playTrack(detail.topTracks, index) },
                                        onToggleFavorite = { viewModel.toggleTrackFavorite(track) },
                                        showAlbum = true,
                                    )
                                }
                            }
                        }
                    }
                }

                if (detail.albums.isNotEmpty()) {
                    item(key = "albums") {
                        AlbumRow(title = "Albums", albums = detail.albums, onOpenAlbum = onOpenAlbum)
                    }
                }

                if (detail.similarArtists.isNotEmpty()) {
                    item(key = "similar") {
                        Column {
                            SectionHeader(title = "Similar Artists")
                            androidx.compose.foundation.lazy.LazyRow(
                                modifier = Modifier.padding(top = Space.md),
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                            ) {
                                items(detail.similarArtists.size) { index ->
                                    val artist = detail.similarArtists[index]
                                    ArtistCard(artist = artist, onClick = { onOpenArtist(artist.id) })
                                }
                            }
                        }
                    }
                }

                if (detail.topTracks.isEmpty() && detail.albums.isEmpty()) {
                    item(key = "empty") {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "This artist has no music on the server yet.",
                                style = AppType.bodyMuted,
                                color = NavidromeColors.OnSurfaceMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}
