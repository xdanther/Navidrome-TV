package com.navidrometv.presentation.screens.nowplaying

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.navidrometv.core.common.Formatters
import com.navidrometv.core.designsystem.component.ActionButton
import com.navidrometv.core.designsystem.component.Artwork
import com.navidrometv.core.designsystem.component.CircleIconButton
import com.navidrometv.core.designsystem.component.InlineNotice
import com.navidrometv.core.designsystem.component.SeekBar
import com.navidrometv.core.designsystem.component.TvClickable
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.Dimens
import com.navidrometv.core.designsystem.theme.LocalAppColors
import com.navidrometv.core.designsystem.theme.NavidromeColors
import com.navidrometv.core.designsystem.theme.Space
import com.navidrometv.core.model.Lyrics
import com.navidrometv.core.model.RepeatMode
import com.navidrometv.data.prefs.AppSettings
import com.navidrometv.playback.PlaybackRepository
import com.navidrometv.playback.PlaybackState
import com.navidrometv.presentation.common.rememberArtworkColors
import kotlinx.coroutines.delay

/**
 * The signature screen: artwork-derived background, large artwork, transport
 * controls, queue panel, lyrics and honest audio information.
 */
@Composable
fun NowPlayingScreen(
    playback: PlaybackRepository,
    settings: AppSettings,
    onBack: () -> Unit,
    viewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val state by playback.state.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var showQueue by remember { mutableStateOf(false) }
    var ambient by remember { mutableStateOf(false) }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }

    BackHandler(enabled = !showQueue) { onBack() }

    val track = state.currentTrack
    val colors = rememberArtworkColors(track?.coverArtId)

    // Keep the panel awake only while the user asked for it and audio is playing.
    val view = LocalView.current
    DisposableEffect(state.isPlaying, settings.keepScreenOn) {
        val window = (view.context as? Activity)?.window
        val flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        if (state.isPlaying && settings.keepScreenOn) window?.addFlags(flag) else window?.clearFlags(flag)
        onDispose { window?.clearFlags(flag) }
    }

    LaunchedEffect(ambient, state.isPlaying, settings.ambientModeEnabled) {
        while (true) {
            delay(5_000)
            val idleMs = System.currentTimeMillis() - lastInteraction
            if (!ambient && settings.ambientModeEnabled && state.isPlaying && idleMs > 90_000) {
                ambient = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    lastInteraction = System.currentTimeMillis()
                    if (ambient) {
                        ambient = false
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            },
    ) {
        PlaybackBackground(state = state, settings = settings, colors = colors)

        if (track == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Nothing playing",
                        style = AppType.title,
                        color = NavidromeColors.OnSurface,
                    )
                    ActionButton(
                        text = "Back to browsing",
                        onClick = onBack,
                        modifier = Modifier.padding(top = Space.md),
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 72.dp, vertical = Space.lg),
                horizontalArrangement = Arrangement.spacedBy(Space.xxl),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1.1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Artwork(
                        coverArtId = track.coverArtId,
                        contentDescription = "${track.albumName ?: track.title} cover",
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .fillMaxHeight(0.72f),
                        shape = RoundedCornerShape(20.dp),
                        requestSizePx = 1200,
                    )

                    Text(
                        text = track.title,
                        style = AppType.display,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = Space.lg),
                    )
                    Text(
                        text = listOfNotNull(track.artistName, track.albumName).joinToString(" · "),
                        style = AppType.body,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                ) {
                    SeekBar(
                        positionMs = state.positionMs,
                        durationMs = state.durationMs,
                        onSeek = viewModel::seekTo,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Space.lg),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircleIconButton(
                            icon = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            onClick = viewModel::toggleShuffle,
                            tint = if (state.shuffle) NavidromeColors.Accent else Color.White,
                            background = Color.White.copy(alpha = if (state.shuffle) 0.18f else 0.08f),
                        )
                        CircleIconButton(
                            icon = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            onClick = viewModel::previous,
                            size = 64,
                            modifier = Modifier.padding(horizontal = Space.sm),
                            background = Color.White.copy(alpha = 0.1f),
                        )
                        CircleIconButton(
                            icon = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            onClick = viewModel::togglePlayPause,
                            size = 88,
                            tint = NavidromeColors.Ink,
                            background = Color.White,
                            modifier = Modifier.padding(horizontal = Space.xs),
                        )
                        CircleIconButton(
                            icon = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            onClick = viewModel::next,
                            size = 64,
                            modifier = Modifier.padding(horizontal = Space.sm),
                            background = Color.White.copy(alpha = 0.1f),
                        )
                        CircleIconButton(
                            icon = when (state.repeatMode) {
                                RepeatMode.ONE -> Icons.Rounded.RepeatOne
                                else -> Icons.Rounded.Repeat
                            },
                            contentDescription = "Repeat",
                            onClick = viewModel::cycleRepeat,
                            tint = if (state.repeatMode == RepeatMode.OFF) Color.White else NavidromeColors.Accent,
                            background = Color.White.copy(
                                alpha = if (state.repeatMode == RepeatMode.OFF) 0.08f else 0.18f,
                            ),
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Space.md),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircleIconButton(
                            icon = if (ui.favoriteOverride ?: track.isFavorite) {
                                Icons.Rounded.Favorite
                            } else {
                                Icons.Rounded.FavoriteBorder
                            },
                            contentDescription = "Favorite",
                            onClick = viewModel::toggleFavorite,
                            tint = if (ui.favoriteOverride ?: track.isFavorite) {
                                NavidromeColors.Accent
                            } else {
                                Color.White
                            },
                            background = Color.White.copy(alpha = 0.08f),
                            modifier = Modifier.padding(horizontal = Space.xs),
                        )
                        CircleIconButton(
                            icon = Icons.Rounded.QueueMusic,
                            contentDescription = "Queue",
                            onClick = { showQueue = true },
                            background = Color.White.copy(alpha = 0.08f),
                            modifier = Modifier.padding(horizontal = Space.xs),
                        )
                        ActionButton(
                            text = "Back",
                            onClick = onBack,
                            primary = false,
                            modifier = Modifier.padding(start = Space.sm),
                        )
                    }

                    LyricsPanel(
                        lyrics = ui.lyrics,
                        loading = ui.lyricsLoading,
                        unavailableReason = ui.lyricsUnavailableReason,
                        positionMs = state.positionMs,
                        onRetry = viewModel::retryLyrics,
                    )

                    AudioInfoPanel(state = state)
                }
            }
        }

        if (showQueue) {
            QueuePanel(
                state = state,
                onClose = { showQueue = false },
                onPlayIndex = viewModel::playQueueIndex,
                onRemove = viewModel::removeFromQueue,
                onMoveUp = { index -> viewModel.moveInQueue(index, index - 1) },
                onMoveDown = { index -> viewModel.moveInQueue(index, index + 1) },
                onClear = {
                    viewModel.clearQueue()
                    showQueue = false
                },
            )
        }

        if (ambient) {
            AmbientOverlay(state = state, colors = colors)
        }
    }
}

@Composable
private fun PlaybackBackground(
    state: PlaybackState,
    settings: AppSettings,
    colors: com.navidrometv.presentation.common.ArtworkColors,
) {
    val track = state.currentTrack
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Crossfade(targetState = track?.coverArtId, label = "artwork-background") { coverArtId ->
            if (settings.artworkBackground && coverArtId != null) {
                Artwork(
                    coverArtId = coverArtId,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(90.dp) else Modifier),
                    shape = RoundedCornerShape(0.dp),
                    requestSizePx = 640,
                    showFallbackIcon = false,
                )
            } else {
                Box(Modifier.fillMaxSize())
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            colors.background.copy(alpha = 0.72f),
                            Color(0xF20A0A0F),
                            Color(0xFF08080A),
                        ),
                    ),
                ),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0x99000000), Color(0x22000000), Color(0x99000000)),
                    ),
                ),
        )
    }
}

@Composable
private fun LyricsPanel(
    lyrics: Lyrics?,
    loading: Boolean,
    unavailableReason: String?,
    positionMs: Long,
    onRetry: () -> Unit,
) {
    Column(Modifier.padding(top = Space.lg)) {
        Text(
            text = "LYRICS",
            style = AppType.overline,
            color = Color.White.copy(alpha = 0.5f),
        )
        when {
            loading -> Text(
                text = "Looking for lyrics…",
                style = AppType.bodyMuted,
                color = Color.White.copy(alpha = 0.6f),
            )
            lyrics == null -> Column {
                Text(
                    text = unavailableReason ?: "No lyrics available",
                    style = AppType.bodyMuted,
                    color = Color.White.copy(alpha = 0.6f),
                )
                ActionButton(
                    text = "Retry",
                    onClick = onRetry,
                    primary = false,
                    modifier = Modifier.padding(top = Space.xs),
                )
            }
            lyrics.synced -> {
                val activeIndex = lyrics.lines.indexOfLast { line ->
                    (line.startMs ?: 0L) <= positionMs
                }
                Text(
                    text = lyrics.lines.getOrNull(activeIndex)?.text.orEmpty(),
                    style = AppType.title,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = lyrics.lines.getOrNull(activeIndex + 1)?.text.orEmpty(),
                    style = AppType.bodyMuted,
                    color = Color.White.copy(alpha = 0.55f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            else -> Text(
                text = lyrics.lines.take(6).joinToString("\n") { it.text },
                style = AppType.bodyMuted,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AudioInfoPanel(state: PlaybackState) {
    val info = state.audioInfo
    val track = state.currentTrack ?: return
    val delivered = listOfNotNull(
        info?.mimeType?.substringAfter('/')?.uppercase(),
        Formatters.sampleRate(info?.sampleRateHz),
        Formatters.bitRate(info?.bitrateKbps),
        info?.channelCount?.takeIf { it > 0 }?.let { "${it}ch" },
    )
    val source = listOfNotNull(
        track.suffix?.uppercase()?.let { "source $it" },
        Formatters.bitRate(track.bitRateKbps),
    )

    Column(
        modifier = Modifier
            .padding(top = Space.lg)
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
            .padding(horizontal = Space.md, vertical = Space.sm),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = "AUDIO", style = AppType.overline, color = Color.White.copy(alpha = 0.45f))
        Text(
            text = if (delivered.isEmpty()) "Delivered format unavailable" else "Delivered: ${delivered.joinToString(" · ")}",
            style = AppType.cardSubtitle,
            color = Color.White.copy(alpha = 0.8f),
        )
        if (source.isNotEmpty()) {
            Text(
                text = "On server: ${source.joinToString(" · ")}",
                style = AppType.cardSubtitle,
                color = Color.White.copy(alpha = 0.55f),
            )
        }
        Text(
            text = if (info?.transcodingRequested == true) {
                "Transcoding requested (${state.requestedQuality.label})"
            } else {
                "Direct stream requested"
            },
            style = AppType.cardSubtitle,
            color = Color.White.copy(alpha = 0.55f),
        )
    }
}

@Composable
private fun QueuePanel(
    state: PlaybackState,
    onClose: () -> Unit,
    onPlayIndex: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onClear: () -> Unit,
) {
    BackHandler(onBack = onClose)
    Box(
        Modifier
            .fillMaxSize()
            .background(NavidromeColors.Ink.copy(alpha = 0.96f)),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 72.dp, vertical = Space.lg),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(text = "Up Next", style = AppType.title, color = NavidromeColors.OnSurface)
                    Text(
                        text = Formatters.plural(state.queue.size, "song") + " in queue",
                        style = AppType.cardSubtitle,
                        color = NavidromeColors.OnSurfaceMuted,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    ActionButton(text = "Clear queue", onClick = onClear, primary = false)
                    ActionButton(text = "Close", onClick = onClose, primary = false)
                }
            }

            if (state.queue.isEmpty()) {
                InlineNotice(
                    text = "The queue is empty",
                    modifier = Modifier.padding(top = Space.lg),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(top = Space.md),
                    contentPadding = PaddingValues(bottom = Space.xl),
                ) {
                    itemsIndexed(state.queue, key = { index, track -> "$index-${track.id}" }) { index, track ->
                        val isCurrent = index == state.currentIndex
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TvClickable(
                                onClick = { onPlayIndex(index) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Space.md),
                                ) {
                                    Text(
                                        text = if (isCurrent) "▶" else (index + 1).toString(),
                                        style = AppType.metadata,
                                        color = if (isCurrent) NavidromeColors.Accent else NavidromeColors.OnSurfaceMuted,
                                        modifier = Modifier.width(28.dp),
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            style = AppType.body,
                                            color = if (isCurrent) NavidromeColors.Accent else NavidromeColors.OnSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = track.artistName.orEmpty(),
                                            style = AppType.cardSubtitle,
                                            color = NavidromeColors.OnSurfaceMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Text(
                                        text = Formatters.duration(track.durationMs),
                                        style = AppType.metadata,
                                        color = NavidromeColors.OnSurfaceMuted,
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                CircleIconButton(
                                    icon = Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = "Move up",
                                    onClick = { onMoveUp(index) },
                                    size = 36,
                                    enabled = index > 0,
                                )
                                CircleIconButton(
                                    icon = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Move down",
                                    onClick = { onMoveDown(index) },
                                    size = 36,
                                    enabled = index < state.queue.lastIndex,
                                )
                                CircleIconButton(
                                    icon = Icons.Rounded.Delete,
                                    contentDescription = "Remove",
                                    onClick = { onRemove(index) },
                                    size = 36,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AmbientOverlay(
    state: PlaybackState,
    colors: com.navidrometv.presentation.common.ArtworkColors,
) {
    val track = state.currentTrack
    BackHandler { }
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        colors.dominant.copy(alpha = 0.35f),
                        Color(0xFF050507),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Artwork(
                coverArtId = track?.coverArtId,
                contentDescription = null,
                modifier = Modifier.size(360.dp),
                shape = RoundedCornerShape(20.dp),
                requestSizePx = 800,
            )
            Text(
                text = track?.title.orEmpty(),
                style = AppType.title,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.padding(top = Space.xl),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track?.artistName.orEmpty(),
                style = AppType.bodyMuted,
                color = Color.White.copy(alpha = 0.35f),
            )
            Text(
                text = Formatters.duration(state.positionMs),
                style = AppType.overline,
                color = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.padding(top = Space.md),
            )
        }
        Text(
            text = "Press any button to return",
            style = AppType.cardSubtitle,
            color = Color.White.copy(alpha = 0.25f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Space.lg),
        )
    }
}
