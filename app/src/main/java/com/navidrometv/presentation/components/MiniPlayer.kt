package com.navidrometv.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.navidrometv.core.designsystem.component.Artwork
import com.navidrometv.core.designsystem.component.ProgressLine
import com.navidrometv.core.designsystem.component.TvClickable
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.Dimens
import com.navidrometv.core.designsystem.theme.LocalAppColors
import com.navidrometv.core.designsystem.theme.NavidromeColors
import com.navidrometv.core.designsystem.theme.Space
import com.navidrometv.playback.PlaybackState

/**
 * Persistent playback strip. It stays on screen while browsing, so playback is
 * always one focus step away without losing the user's place.
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.screenHorizontal, vertical = Space.sm)
            .shadow(
                Dimens.cardElevation,
                RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.12f),
                spotColor = Color.Black.copy(alpha = 0.16f),
            )
            .background(NavidromeColors.Glass, RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
            .padding(horizontal = Space.md, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        TvClickable(
            onClick = onOpen,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            focusedScale = 1.01f,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.md),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Artwork(
                    coverArtId = track.coverArtId,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(10.dp),
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
                        color = LocalAppColors.current.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(Modifier.width(160.dp)) {
                    ProgressLine(fraction = state.progress)
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
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

@Composable
private fun MiniControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    TvClickable(onClick = onClick, shape = RoundedCornerShape(percent = 50)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.75f), RoundedCornerShape(percent = 50)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.tv.material3.Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = NavidromeColors.Ink,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
