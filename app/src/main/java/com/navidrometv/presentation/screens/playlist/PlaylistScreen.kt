package com.navidrometv.presentation.screens.playlist

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
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
import com.navidrometv.core.designsystem.component.CircleIconButton
import com.navidrometv.core.designsystem.component.EmptyState
import com.navidrometv.core.designsystem.component.ErrorState
import com.navidrometv.core.designsystem.component.InlineNotice
import com.navidrometv.core.designsystem.component.LoadingState
import com.navidrometv.core.designsystem.component.TrackRow
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.LocalAppColors
import com.navidrometv.core.designsystem.theme.NavidromeColors
import com.navidrometv.core.designsystem.theme.Space
import com.navidrometv.presentation.components.TvKeyboard
import kotlinx.coroutines.delay

@Composable
fun PlaylistScreen(
    playlistId: String,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenNowPlaying: () -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    var notice by remember { mutableStateOf<String?>(null) }
    var renaming by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)
    LaunchedEffect(playlistId) { viewModel.load(playlistId) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                PlaylistEvent.OpenNowPlaying -> onOpenNowPlaying()
                PlaylistEvent.Deleted -> onBack()
                is PlaylistEvent.Message -> notice = event.text
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
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = Space.screenHorizontal, vertical = Space.md),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.sm),
                    ) {
                        ActionButton(text = "Back", onClick = onBack, primary = false)
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
                        ActionButton(
                            text = "Queue",
                            onClick = viewModel::addAllToQueue,
                            icon = Icons.Rounded.QueueMusic,
                            primary = false,
                        )
                        ActionButton(
                            text = "Rename",
                            onClick = { renaming = true },
                            icon = Icons.Rounded.Edit,
                            primary = false,
                        )
                        ActionButton(
                            text = "Delete",
                            onClick = { confirmingDelete = true },
                            icon = Icons.Rounded.Delete,
                            primary = false,
                        )
                    }

                    Text(
                        text = detail.playlist.name,
                        style = AppType.display,
                        color = NavidromeColors.OnSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = Space.md),
                    )
                    Text(
                        text = listOfNotNull(
                            Formatters.plural(detail.tracks.size, "song"),
                            detail.playlist.owner?.let { "by $it" },
                            if (detail.playlist.isPublic) "Public" else null,
                        ).joinToString(" · "),
                        style = AppType.cardSubtitle,
                        color = LocalAppColors.current.secondaryText,
                    )
                    Text(
                        text = "The Subsonic API cannot reorder playlist entries; remove and re-add to change order.",
                        style = AppType.cardSubtitle,
                        color = NavidromeColors.OnSurfaceMuted.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp),
                    )

                    if (detail.tracks.isEmpty()) {
                        EmptyState(
                            title = "This playlist is empty",
                            message = "Add songs from an album page.",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Space.md),
                            contentPadding = PaddingValues(bottom = Space.contentBottomWithPlayer),
                        ) {
                            itemsIndexed(detail.tracks, key = { _, track -> track.id }) { index, track ->
                                TrackRow(
                                    track = track,
                                    index = index,
                                    isCurrent = playback.currentTrack?.id == track.id,
                                    isPlaying = playback.isPlaying,
                                    onPlay = { viewModel.playTrack(index) },
                                    onToggleFavorite = { viewModel.toggleFavorite(index) },
                                    showAlbum = true,
                                    trailing = {
                                        CircleIconButton(
                                            icon = Icons.Rounded.Delete,
                                            contentDescription = "Remove from playlist",
                                            onClick = { viewModel.removeAt(index) },
                                            size = 36,
                                        )
                                    },
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

        if (renaming) {
            TextEntryOverlay(
                title = "Rename playlist",
                initialValue = state.detail?.playlist?.name.orEmpty(),
                onDismiss = { renaming = false },
                onConfirm = { name ->
                    viewModel.rename(name)
                    renaming = false
                },
            )
        }

        if (confirmingDelete) {
            ConfirmOverlay(
                title = "Delete this playlist?",
                message = "This removes the playlist from your Navidrome server. The songs stay in your library.",
                confirmLabel = "Delete",
                onDismiss = { confirmingDelete = false },
                onConfirm = {
                    viewModel.delete()
                    confirmingDelete = false
                },
            )
        }
    }
}

@Composable
private fun TextEntryOverlay(
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initialValue) }
    BackHandler(onBack = onDismiss)
    Box(
        Modifier
            .fillMaxSize()
            .background(NavidromeColors.Ink.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .width(720.dp)
                .background(NavidromeColors.Surface, RoundedCornerShape(24.dp))
                .padding(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            Text(text = title, style = AppType.section, color = NavidromeColors.OnSurface)
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(NavidromeColors.SurfaceVariant, RoundedCornerShape(12.dp))
                    .padding(16.dp),
            ) {
                Text(
                    text = value.ifEmpty { "…" },
                    style = AppType.body,
                    color = NavidromeColors.Ink,
                )
            }
            TvKeyboard(
                onCharacter = { value += it },
                onBackspace = { value = value.dropLast(1) },
                onClear = { value = "" },
                onSearch = { if (value.isNotBlank()) onConfirm(value) },
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                ActionButton(text = "Cancel", onClick = onDismiss, primary = false)
                ActionButton(
                    text = "Save",
                    onClick = { if (value.isNotBlank()) onConfirm(value) },
                    modifier = Modifier.padding(start = Space.sm),
                )
            }
        }
    }
}

@Composable
private fun ConfirmOverlay(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    Box(
        Modifier
            .fillMaxSize()
            .background(NavidromeColors.Ink.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .width(560.dp)
                .background(NavidromeColors.Surface, RoundedCornerShape(24.dp))
                .padding(Space.lg),
        ) {
            Text(text = title, style = AppType.section, color = NavidromeColors.OnSurface)
            Text(
                text = message,
                style = AppType.bodyMuted,
                color = LocalAppColors.current.secondaryText,
                modifier = Modifier.padding(top = Space.xs),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = Space.lg),
                horizontalArrangement = Arrangement.End,
            ) {
                ActionButton(text = "Cancel", onClick = onDismiss, primary = false)
                ActionButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                    modifier = Modifier.padding(start = Space.sm),
                )
            }
        }
    }
}
