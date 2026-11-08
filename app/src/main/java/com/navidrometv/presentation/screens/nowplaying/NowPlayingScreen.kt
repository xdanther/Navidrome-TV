package com.navidrometv.presentation.screens.nowplaying

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.navidrometv.core.designsystem.theme.NavidromeColors
import com.navidrometv.core.designsystem.theme.Space
import com.navidrometv.core.model.Lyrics
import com.navidrometv.core.model.RepeatMode
import com.navidrometv.core.model.StreamQuality
import com.navidrometv.core.model.Track
import com.navidrometv.data.prefs.AppSettings
import com.navidrometv.playback.AudioPlaybackInfo
import com.navidrometv.playback.PlaybackRepository
import com.navidrometv.playback.PlaybackState
import com.navidrometv.presentation.common.ArtworkColors
import com.navidrometv.presentation.common.rememberArtworkColors
import kotlinx.coroutines.delay

/**
 * The signature screen: artwork-derived background, a two-column layout and
 * transport, queue, lyrics and honest audio information.
 *
 * Playback state is read through [derivedStateOf] so the position tick that fires
 * several times per second only invalidates the progress bar and the lyric line —
 * not the background, the artwork, or the transport row.
 */
@Composable
fun NowPlayingScreen(
    playback: PlaybackRepository,
    settings: AppSettings,
    onBack: () -> Unit,
    viewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val playbackState: State<PlaybackState> = playback.state.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var showQueue by remember { mutableStateOf(false) }
    var ambient by remember { mutableStateOf(false) }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val track by remember(playbackState) { derivedStateOf { playbackState.value.currentTrack } }
    val isPlaying by remember(playbackState) { derivedStateOf { playbackState.value.isPlaying } }
    val shuffle by remember(playbackState) { derivedStateOf { playbackState.value.shuffle } }
    val repeatMode by remember(playbackState) { derivedStateOf { playbackState.value.repeatMode } }
    val durationMs by remember(playbackState) { derivedStateOf { playbackState.value.durationMs } }
    val audioInfo by remember(playbackState) { derivedStateOf { playbackState.value.audioInfo } }
    val queue by remember(playbackState) { derivedStateOf { playbackState.value.queue } }
    val currentIndex by remember(playbackState) { derivedStateOf { playbackState.value.currentIndex } }
    val requestedQuality by remember(playbackState) {
        derivedStateOf { playbackState.value.requestedQuality }
    }
    // Stable lambda: reading it inside a child keeps the tick out of this scope.
    val positionMs: () -> Long = remember(playbackState) { { playbackState.value.positionMs } }

    BackHandler(enabled = !showQueue) { onBack() }

    val colors = rememberArtworkColors(track?.coverArtId)

    // Keep the panel awake only while the user asked for it and audio is playing.
    val view = LocalView.current
    DisposableEffect(isPlaying, settings.keepScreenOn) {
        val window = (view.context as? Activity)?.window
        val flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        if (isPlaying && settings.keepScreenOn) window?.addFlags(flag) else window?.clearFlags(flag)
        onDispose { window?.clearFlags(flag) }
    }

    LaunchedEffect(ambient, isPlaying, settings.ambientModeEnabled) {
        while (true) {
            delay(5_000)
            val idleMs = System.currentTimeMillis() - lastInteraction
            if (!ambient && settings.ambientModeEnabled && isPlaying && idleMs > 90_000) {
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
        PlaybackBackground(
            coverArtId = track?.coverArtId,
            artworkEnabled = settings.artworkBackground,
            colors = colors,
        )

        // Local binding: smart cast is not available on a delegated property.
        val playingTrack = track
        if (playingTrack == null) {
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
            NowPlayingContent(
                track = playingTrack,
                isPlaying = isPlaying,
                shuffle = shuffle,
                repeatMode = repeatMode,
                durationMs = durationMs,
                positionMs = positionMs,
                lyrics = ui.lyrics,
                lyricsLoading = ui.lyricsLoading,
                lyricsUnavailableReason = ui.lyricsUnavailableReason,
                audioInfo = audioInfo,
                requestedQuality = requestedQuality,
                favorite = ui.favoriteOverride ?: playingTrack.isFavorite,
                onSeek = viewModel::seekTo,
                onTogglePlay = viewModel::togglePlayPause,
                onNext = viewModel::next,
                onPrevious = viewModel::previous,
                onToggleShuffle = viewModel::toggleShuffle,
                onCycleRepeat = viewModel::cycleRepeat,
                onToggleFavorite = viewModel::toggleFavorite,
                onOpenQueue = { showQueue = true },
                onRetryLyrics = viewModel::retryLyrics,
            )
        }

        if (showQueue) {
            QueuePanel(
                queue = queue,
                currentIndex = currentIndex,
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
            AmbientOverlay(track = track, positionMs = positionMs, colors = colors)
        }
    }
}

/**
 * Balanced two-column body: artwork and track identity on the left, progress,
 * transport, queue/favourite and lyrics on the right. Artwork stays square and is
 * sized relative to the screen instead of swallowing it.
 */
@Composable
private fun NowPlayingContent(
    track: Track,
    isPlaying: Boolean,
    shuffle: Boolean,
    repeatMode: RepeatMode,
    durationMs: Long,
    positionMs: () -> Long,
    lyrics: Lyrics?,
    lyricsLoading: Boolean,
    lyricsUnavailableReason: String?,
    audioInfo: AudioPlaybackInfo?,
    requestedQuality: StreamQuality,
    favorite: Boolean,
    onSeek: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
    onRetryLyrics: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 64.dp, vertical = Space.lg),
    ) {
        // 240–400dp keeps the cover prominent without letting it become the screen.
        val artworkSize = minOf(maxWidth * 0.34f, maxHeight * 0.58f).coerceIn(240.dp, 400.dp)

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(0.9f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Artwork(
                    coverArtId = track.coverArtId,
                    contentDescription = "${track.albumName ?: track.title} cover",
                    modifier = Modifier.size(artworkSize),
                    shape = RoundedCornerShape(22.dp),
                    requestSizePx = 1000,
                )
                Text(
                    text = track.title,
                    style = AppType.title,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = Space.lg),
                )
                Text(
                    text = track.artistName.orEmpty(),
                    style = AppType.body,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                if (!track.albumName.isNullOrBlank()) {
                    Text(
                        text = track.albumName,
                        style = AppType.bodyMuted,
                        color = Color.White.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
            ) {
                PlaybackProgress(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    onSeek = onSeek,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Space.md),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircleIconButton(
                        icon = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        onClick = onToggleShuffle,
                        size = 52,
                        tint = if (shuffle) NavidromeColors.Accent else Color.White,
                        background = Color.White.copy(alpha = if (shuffle) 0.18f else 0.08f),
                    )
                    CircleIconButton(
                        icon = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        onClick = onPrevious,
                        size = 68,
                        modifier = Modifier.padding(horizontal = Space.sm),
                        background = Color.White.copy(alpha = 0.12f),
                    )
                    CircleIconButton(
                        icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        onClick = onTogglePlay,
                        size = 92,
                        tint = NavidromeColors.Ink,
                        background = Color.White,
                        modifier = Modifier.padding(horizontal = Space.xs),
                    )
                    CircleIconButton(
                        icon = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        onClick = onNext,
                        size = 68,
                        modifier = Modifier.padding(horizontal = Space.sm),
                        background = Color.White.copy(alpha = 0.12f),
                    )
                    CircleIconButton(
                        icon = when (repeatMode) {
                            RepeatMode.ONE -> Icons.Rounded.RepeatOne
                            else -> Icons.Rounded.Repeat
                        },
                        contentDescription = "Repeat",
                        onClick = onCycleRepeat,
                        size = 52,
                        tint = if (repeatMode == RepeatMode.OFF) Color.White else NavidromeColors.Accent,
                        background = Color.White.copy(
                            alpha = if (repeatMode == RepeatMode.OFF) 0.08f else 0.18f,
                        ),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Space.sm),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircleIconButton(
                        icon = if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (favorite) "Remove from favorites" else "Add to favorites",
                        onClick = onToggleFavorite,
                        size = 48,
                        tint = if (favorite) NavidromeColors.Accent else Color.White,
                        background = Color.White.copy(alpha = 0.08f),
                    )
                    CircleIconButton(
                        icon = Icons.Rounded.QueueMusic,
                        contentDescription = "Queue",
                        onClick = onOpenQueue,
                        size = 48,
                        background = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(start = Space.xs),
                    )
                }

                LyricsPanel(
                    lyrics = lyrics,
                    loading = lyricsLoading,
                    unavailableReason = lyricsUnavailableReason,
                    positionMs = positionMs,
                    onRetry = onRetryLyrics,
                )

                AudioInfoPanel(track = track, info = audioInfo, quality = requestedQuality)
            }
        }
    }
}

/**
 * Progress bar isolated behind a lambda provider: the position ticks invalidate
 * only this composable, never the artwork or the background behind it.
 */
@Composable
private fun PlaybackProgress(
    positionMs: () -> Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
) {
    SeekBar(positionMs = positionMs(), durationMs = durationMs, onSeek = onSeek)
}

@Composable
private fun PlaybackBackground(
    coverArtId: String?,
    artworkEnabled: Boolean,
    colors: ArtworkColors,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // Only the artwork id drives the crossfade; playback progress is not a key,
        // so the backdrop stays still for the whole track.
        Crossfade(
            targetState = if (artworkEnabled) coverArtId else null,
            animationSpec = tween(durationMillis = 650),
            label = "artwork-background",
        ) { id ->
            if (id != null) {
                Artwork(
                    coverArtId = id,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        // Slight overscan hides the transparent edge a large blur leaves.
                        .graphicsLayer {
                            scaleX = 1.18f
                            scaleY = 1.18f
                        }
                        .then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Modifier.blur(80.dp)
                            } else {
                                Modifier
                            },
                        ),
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
                            Color(0xF20E0F11),
                            Color(0xFF0E0F11),
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
    positionMs: () -> Long,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Space.lg),
    ) {
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
                modifier = Modifier.padding(top = 2.dp),
            )
            lyrics == null -> Column {
                Text(
                    text = unavailableReason ?: "No lyrics available",
                    style = AppType.bodyMuted,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp),
                )
                ActionButton(
                    text = "Retry",
                    onClick = onRetry,
                    primary = false,
                    modifier = Modifier.padding(top = Space.xs),
                )
            }
            lyrics.synced -> SyncedLyrics(lyrics = lyrics, positionMs = positionMs)
            else -> PlainLyrics(lyrics = lyrics)
        }
    }
}

/**
 * Synchronised lyrics read the position through [derivedStateOf], so recomposition
 * happens only when the active line changes — not on every playback tick.
 */
@Composable
private fun SyncedLyrics(lyrics: Lyrics, positionMs: () -> Long) {
    val index by remember(lyrics) {
        derivedStateOf { lyrics.lines.indexOfLast { (it.startMs ?: 0L) <= positionMs() } }
    }
    val current = lyrics.lines.getOrNull(index)?.text
    val next = lyrics.lines.getOrNull(index + 1)?.text
    LyricsTextBlock(current = current, next = next, highlight = true)
}

@Composable
private fun PlainLyrics(lyrics: Lyrics) {
    val body = remember(lyrics) { lyrics.lines.take(10).joinToString("\n") { it.text } }
    LyricsTextBlock(current = body, next = null, highlight = false)
}

/**
 * Lyrics block with RTL handling. Arabic and Hebrew lines get an RTL layout
 * direction and a generous line height, so shaping, alignment and line spacing are
 * correct instead of being forced into a left-aligned Latin paragraph.
 */
@Composable
private fun LyricsTextBlock(current: String?, next: String?, highlight: Boolean) {
    val rtl = remember(current) { isRightToLeft(current) }
    CompositionLocalProvider(
        LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = current.orEmpty(),
                style = if (highlight) {
                    AppType.title.copy(lineHeight = 40.sp)
                } else {
                    AppType.bodyMuted.copy(lineHeight = 26.sp)
                },
                color = if (highlight) Color.White else Color.White.copy(alpha = 0.82f),
                maxLines = if (highlight) 3 else 8,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
            )
            if (!next.isNullOrBlank()) {
                Text(
                    text = next,
                    style = AppType.bodyMuted.copy(lineHeight = 26.sp),
                    color = Color.White.copy(alpha = 0.55f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}

/** First strong bidirectional character decides the paragraph direction. */
private fun isRightToLeft(text: String?): Boolean {
    if (text.isNullOrBlank()) return false
    for (character in text) {
        when (Character.getDirectionality(character)) {
            Character.DIRECTIONALITY_RIGHT_TO_LEFT,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC,
            -> return true
            Character.DIRECTIONALITY_LEFT_TO_RIGHT -> return false
        }
    }
    return false
}

@Composable
private fun AudioInfoPanel(track: Track, info: AudioPlaybackInfo?, quality: StreamQuality) {
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
    if (delivered.isEmpty() && source.isEmpty()) return

    Column(
        modifier = Modifier
            .padding(top = Space.md)
            .background(NavidromeColors.SurfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = Space.md, vertical = Space.sm),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = "AUDIO", style = AppType.overline, color = Color.White.copy(alpha = 0.45f))
        if (delivered.isNotEmpty()) {
            Text(
                text = "Delivered: ${delivered.joinToString(" · ")}",
                style = AppType.cardSubtitle,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
        if (source.isNotEmpty()) {
            Text(
                text = "On server: ${source.joinToString(" · ")}",
                style = AppType.cardSubtitle,
                color = Color.White.copy(alpha = 0.55f),
            )
        }
        if (info != null) {
            Text(
                text = if (info.transcodingRequested) {
                    "Transcoded · ${quality.label}"
                } else {
                    "Direct stream"
                },
                style = AppType.cardSubtitle,
                color = Color.White.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
private fun QueuePanel(
    queue: List<Track>,
    currentIndex: Int,
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
                        text = Formatters.plural(queue.size, "song") + " in queue",
                        style = AppType.cardSubtitle,
                        color = NavidromeColors.OnSurfaceMuted,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    ActionButton(text = "Clear queue", onClick = onClear, primary = false)
                    ActionButton(text = "Close", onClick = onClose, primary = false)
                }
            }

            if (queue.isEmpty()) {
                InlineNotice(
                    text = "The queue is empty",
                    modifier = Modifier.padding(top = Space.lg),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = Space.md),
                    contentPadding = PaddingValues(bottom = Space.xl),
                ) {
                    itemsIndexed(queue, key = { index, track -> "$index-${track.id}" }) { index, item ->
                        val isCurrent = index == currentIndex
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
                                        color = if (isCurrent) {
                                            NavidromeColors.Accent
                                        } else {
                                            NavidromeColors.OnSurfaceMuted
                                        },
                                        modifier = Modifier.width(28.dp),
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = AppType.body,
                                            color = if (isCurrent) {
                                                NavidromeColors.Accent
                                            } else {
                                                NavidromeColors.OnSurface
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = item.artistName.orEmpty(),
                                            style = AppType.cardSubtitle,
                                            color = NavidromeColors.OnSurfaceMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Text(
                                        text = Formatters.duration(item.durationMs),
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
                                    enabled = index < queue.lastIndex,
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
    track: Track?,
    positionMs: () -> Long,
    colors: ArtworkColors,
) {
    BackHandler { }
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        colors.dominant.copy(alpha = 0.35f),
                        Color(0xFF0E0F11),
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
            AmbientClock(positionMs = positionMs)
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

/** Tiny leaf so the elapsed-time tick does not recompose the ambient gradient. */
@Composable
private fun AmbientClock(positionMs: () -> Long) {
    Text(
        text = Formatters.duration(positionMs()),
        style = AppType.overline,
        color = Color.White.copy(alpha = 0.25f),
        modifier = Modifier.padding(top = Space.md),
    )
}
