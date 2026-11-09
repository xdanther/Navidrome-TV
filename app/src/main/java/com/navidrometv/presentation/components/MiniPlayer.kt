package com.navidrometv.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.navidrometv.core.designsystem.component.Artwork
import com.navidrometv.core.designsystem.component.TvClickable
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.NavidromeColors
import com.navidrometv.core.designsystem.theme.Space
import com.navidrometv.playback.PlaybackState

/**
 * Persistent playback strip.
 *
 * It is a flush, full-width bar rather than a floating card: a hairline progress
 * line runs edge to edge along its top, the artwork and titles sit on the safe
 * margin, and the transport is plain white glyphs. That is the shape a television
 * app uses for background playback, and it lets the shelf above it use the full
 * width of the screen.
 */
@Composable
fun MiniPlayer(
    state: PlaybackState,
    onOpen: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = state.currentTrack ?: return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NavidromeColors.Glass),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.White.copy(alpha = 0.14f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(state.progress.coerceIn(0f, 1f))
                    .height(2.dp)
                    .background(NavidromeColors.Accent),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.screenHorizontal, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            TvClickable(
                onClick = onOpen,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                focusedScale = 1.01f,
                ringWidth = 3.dp,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    Artwork(
                        coverArtId = track.coverArtId,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(4.dp),
                        requestSizePx = 160,
                        showFallbackIcon = false,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = AppType.cardTitle,
                            color = NavidromeColors.OnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = listOfNotNull(track.artistName, track.albumName).joinToString(" · "),
                            style = AppType.cardSubtitle,
                            color = NavidromeColors.OnSurfaceMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.xxs),
            ) {
                MiniControl(
                    icon = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    onClick = onTogglePlay,
                )
                MiniControl(
                    icon = Icons.Rounded.SkipNext,
                    contentDescription = "Next track",
                    onClick = onNext,
                )
            }
        }
    }
}

@Composable
private fun MiniControl(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val circle = RoundedCornerShape(percent = 50)
    TvClickable(onClick = onClick, shape = circle, ringWidth = 3.dp) { focused ->
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(if (focused) Color.White.copy(alpha = 0.20f) else Color.Transparent, circle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White.copy(alpha = if (focused) 1f else 0.88f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
