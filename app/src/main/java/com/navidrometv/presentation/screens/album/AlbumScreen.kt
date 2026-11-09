package com.navidrometv.presentation.screens.album

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.navidrometv.core.common.Formatters
import com.navidrometv.core.designsystem.component.ActionButton
import com.navidrometv.core.designsystem.component.Artwork
import com.navidrometv.core.designsystem.component.CircleIconButton
import com.navidrometv.core.designsystem.component.ErrorState
import com.navidrometv.core.designsystem.component.InlineNotice
import com.navidrometv.core.designsystem.component.LoadingState
import com.navidrometv.core.designsystem.component.TrackRow
import com.navidrometv.core.designsystem.component.TvClickable
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.Dimens
import com.navidrometv.core.designsystem.theme.LocalAppColors
import com.navidrometv.core.designsystem.theme.NavidromeColors
import com.navidrometv.core.designsystem.theme.Space
import com.navidrometv.core.model.Track
import kotlinx.coroutines.delay

@Composable
fun AlbumScreen(
    albumId: String,
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenNowPlaying: () -> Unit,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    var notice by remember { mutableStateOf<String?>(null) }
    var showPlaylists by remember { mutableStateOf(false) }

    BackHandler(enabled = !showPlaylists, onBack = onBack)

    LaunchedEffect(albumId) { viewModel.load(albumId) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AlbumEvent.OpenNowPlaying -> onOpenNowPlaying()
                is AlbumEvent.Message -> notice = event.text
            }
        }
    }

    LaunchedEffect(notice) {
        if (notice != null) {
            delay(3_000)
            notice = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        when {
            state.loading -> LoadingState()
            state.error != null -> ErrorState(error = state.error!!, onRetry = viewModel::retry)
            state.detail != null -> {
                val detail = state.detail!!
                AlbumDetailLayout(
                    detail = detail,
                    favoritePending = state.favoritePending,
                    currentTrackId = playback.currentTrack?.id,
                    isPlaying = playback.isPlaying,
                    onOpenArtist = onOpenArtist,
                    onPlay = viewModel::play,
                    onShuffle = viewModel::shuffle,
                    onQueue = viewModel::addAlbumToQueue,
                    onToggleAlbumFavorite = viewModel::toggleFavorite,
                    onToggleTrackFavorite = viewModel::toggleTrackFavorite,
                    onAddToPlaylist = {
                        viewModel.loadPlaylists()
                        showPlaylists = true
                    },
                )
            }
        }

        notice?.let { message ->
            InlineNotice(
                text = message,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp),
            )
        }

        if (showPlaylists) {
            PlaylistPicker(
                playlists = state.playlists,
                onDismiss = { showPlaylists = false },
                onSelect = { playlist ->
                    viewModel.addAlbumToPlaylist(playlist)
                    showPlaylists = false
                },
            )
        }
    }
}

/**
 * Album detail at television sizes.
 *
 * A television reports roughly 960x540dp even at 1080p, so the cover cannot take a
 * fixed 360dp column: on a 540dp-tall screen that pushes the title, artist and
 * metadata off the bottom edge. The cover is therefore sized from the available
 * height, and the album actions collapse to two labelled buttons plus icon controls
 * so they never overflow the column into a squeezed, unreadable pill.
 */
@Composable
private fun AlbumDetailLayout(
    detail: com.navidrometv.core.model.AlbumDetail,
    favoritePending: Boolean,
    currentTrackId: String?,
    isPlaying: Boolean,
    onOpenArtist: (String) -> Unit,
    onPlay: (Int) -> Unit,
    onShuffle: () -> Unit,
    onQueue: () -> Unit,
    onToggleAlbumFavorite: () -> Unit,
    onToggleTrackFavorite: (Track) -> Unit,
    onAddToPlaylist: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = Space.screenHorizontal, end = Space.screenHorizontal, top = Space.md),
    ) {
        // Cover on the left, everything else top-aligned beside it: title, artist,
        // metadata, the action row, then the track list filling the column.
        val artworkSize = minOf(maxWidth * 0.30f, maxHeight * 0.70f).coerceIn(150.dp, 340.dp)
        val coverShape = RoundedCornerShape(8.dp)

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(Space.xl),
        ) {
            Artwork(
                coverArtId = detail.album.coverArtId,
                contentDescription = "${detail.album.name} cover",
                modifier = Modifier
                    .size(artworkSize)
                    .shadow(
                        22.dp,
                        coverShape,
                        ambientColor = Color.Black.copy(alpha = 0.16f),
                        spotColor = Color.Black.copy(alpha = 0.24f),
                    ),
                shape = coverShape,
                requestSizePx = 800,
            )

            Column(Modifier.weight(1f).fillMaxHeight()) {
                Text(
                    text = detail.album.name,
                    style = AppType.display,
                    color = NavidromeColors.OnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                TvClickable(
                    onClick = { detail.album.artistId?.let(onOpenArtist) },
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        text = detail.album.artistName.orEmpty(),
                        style = AppType.body.copy(fontSize = 22.sp),
                        color = NavidromeColors.OnSurfaceMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = albumMetadata(
                        detail.album.year,
                        detail.album.genre,
                        detail.tracks.size,
                        detail.album.durationSeconds,
                    ),
                    style = AppType.cardSubtitle,
                    color = LocalAppColors.current.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Space.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    ActionButton(
                        text = "Play",
                        onClick = { onPlay(0) },
                        icon = Icons.Rounded.PlayArrow,
                    )
                    ActionButton(
                        text = "Shuffle",
                        onClick = onShuffle,
                        icon = Icons.Rounded.Shuffle,
                        primary = false,
                    )
                    Box(Modifier.weight(1f))
                    CircleIconButton(
                        icon = if (detail.album.isFavorite) {
                            Icons.Rounded.Favorite
                        } else {
                            Icons.Rounded.FavoriteBorder
                        },
                        contentDescription = if (detail.album.isFavorite) {
                            "Remove album from favorites"
                        } else {
                            "Favorite album"
                        },
                        onClick = onToggleAlbumFavorite,
                        size = 44,
                        tint = if (detail.album.isFavorite) {
                            NavidromeColors.Accent
                        } else {
                            NavidromeColors.OnSurfaceMuted
                        },
                        enabled = !favoritePending,
                    )
                    CircleIconButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = "Add album to playlist",
                        onClick = onAddToPlaylist,
                        size = 44,
                        tint = NavidromeColors.OnSurfaceMuted,
                    )
                    CircleIconButton(
                        icon = Icons.Rounded.QueueMusic,
                        contentDescription = "Add album to queue",
                        onClick = onQueue,
                        size = 44,
                        tint = NavidromeColors.OnSurfaceMuted,
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = Space.md),
                    contentPadding = PaddingValues(bottom = Space.contentBottomWithPlayer),
                ) {
                    itemsIndexed(detail.tracks, key = { _, track -> track.id }) { index, track ->
                        TrackRow(
                            track = track,
                            index = index,
                            isCurrent = currentTrackId == track.id,
                            isPlaying = isPlaying,
                            onPlay = { onPlay(index) },
                            onToggleFavorite = { onToggleTrackFavorite(track) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistPicker(
    playlists: List<com.navidrometv.core.model.Playlist>,
    onDismiss: () -> Unit,
    onSelect: (com.navidrometv.core.model.Playlist) -> Unit,
) {
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavidromeColors.Ink.copy(alpha = 0.94f)),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(560.dp)
                .background(NavidromeColors.Surface, RoundedCornerShape(Dimens.panelCorner))
                .padding(Space.lg),
        ) {
            Text(text = "Add album to playlist", style = AppType.section, color = NavidromeColors.OnSurface)
            Text(
                text = if (playlists.isEmpty()) "No playlists on this server yet." else "Choose a playlist",
                style = AppType.cardSubtitle,
                color = LocalAppColors.current.secondaryText,
                modifier = Modifier.padding(top = 4.dp, bottom = Space.md),
            )
            LazyColumn(Modifier.height(320.dp)) {
                itemsIndexed(playlists, key = { _, playlist -> playlist.id }) { _, playlist ->
                    TvClickable(
                        onClick = { onSelect(playlist) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = playlist.name,
                            style = AppType.body,
                            color = NavidromeColors.OnSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = Space.md), horizontalArrangement = Arrangement.End) {
                ActionButton(text = "Cancel", onClick = onDismiss, primary = false)
            }
        }
    }
}

private fun albumMetadata(year: Int?, genre: String?, trackCount: Int, durationSeconds: Int?): String =
    listOfNotNull(
        year?.toString(),
        genre,
        Formatters.plural(trackCount, "song"),
        Formatters.totalDuration(durationSeconds),
    ).joinToString(" · ")
