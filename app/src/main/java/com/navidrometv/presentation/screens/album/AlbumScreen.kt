package com.navidrometv.presentation.screens.album

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Space.screenHorizontal, vertical = Space.lg),
                    horizontalArrangement = Arrangement.spacedBy(Space.xxl),
                ) {
                    Column(Modifier.width(360.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircleIconButton(
                                icon = Icons.Rounded.ArrowBack,
                                contentDescription = "Back",
                                onClick = onBack,
                                size = 48,
                            )
                            Text(
                                text = "ALBUM",
                                style = AppType.overline,
                                color = NavidromeColors.OnSurfaceMuted,
                                modifier = Modifier.padding(start = Space.md),
                            )
                        }

                        Artwork(
                            coverArtId = detail.album.coverArtId,
                            contentDescription = "${detail.album.name} cover",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .padding(top = Space.md),
                            shape = RoundedCornerShape(Dimens.panelCorner),
                            requestSizePx = 900,
                        )

                        Text(
                            text = detail.album.name,
                            style = AppType.title,
                            color = NavidromeColors.OnSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = Space.lg),
                        )

                        TvClickable(
                            onClick = { detail.album.artistId?.let(onOpenArtist) },
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = detail.album.artistName.orEmpty(),
                                style = AppType.body,
                                color = NavidromeColors.Accent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }

                        Text(
                            text = albumMetadata(detail.album.year, detail.album.genre, detail.tracks.size, detail.album.durationSeconds),
                            style = AppType.cardSubtitle,
                            color = LocalAppColors.current.secondaryText,
                            modifier = Modifier.padding(top = Space.xs),
                        )

                        formatLine(detail.tracks.firstOrNull())?.let { format ->
                            Text(
                                text = format,
                                style = AppType.cardSubtitle,
                                color = NavidromeColors.OnSurfaceMuted,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }

                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                            ActionButton(
                                text = "Play",
                                onClick = { viewModel.play(0) },
                                icon = Icons.Rounded.PlayArrow,
                            )
                            ActionButton(
                                text = "Shuffle",
                                onClick = viewModel::shuffle,
                                icon = Icons.Rounded.Shuffle,
                                primary = false,
                            )
                            ActionButton(
                                text = "Queue",
                                onClick = viewModel::addAlbumToQueue,
                                icon = Icons.Rounded.QueueMusic,
                                primary = false,
                            )
                            ActionButton(
                                text = if (detail.album.isFavorite) "Favorited" else "Favorite",
                                onClick = viewModel::toggleFavorite,
                                icon = if (detail.album.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                primary = false,
                                enabled = !state.favoritePending,
                            )
                            ActionButton(
                                text = "Add to playlist",
                                onClick = {
                                    viewModel.loadPlaylists()
                                    showPlaylists = true
                                },
                                icon = Icons.Rounded.Add,
                                primary = false,
                            )
                        }

                        Spacer(Modifier.height(Space.lg))

                        LazyColumn(Modifier.fillMaxSize()) {
                            item(key = "header") {
                                Text(
                                    text = "${detail.tracks.size} tracks",
                                    style = AppType.overline,
                                    color = NavidromeColors.OnSurfaceMuted,
                                    modifier = Modifier.padding(bottom = Space.xs),
                                )
                            }
                            itemsIndexed(detail.tracks, key = { _, track -> track.id }) { index, track ->
                                TrackRow(
                                    track = track,
                                    index = index,
                                    isCurrent = playback.currentTrack?.id == track.id,
                                    isPlaying = playback.isPlaying,
                                    onPlay = { viewModel.play(index) },
                                    onToggleFavorite = { viewModel.toggleTrackFavorite(track) },
                                )
                            }
                        }
                    }
                }
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

private fun formatLine(track: Track?): String? {
    if (track == null) return null
    val parts = listOfNotNull(
        track.suffix?.uppercase(),
        Formatters.bitRate(track.bitRateKbps),
        Formatters.fileSize(track.sizeBytes),
    )
    return if (parts.isEmpty()) null else "Source file: ${parts.joinToString(" · ")}"
}
