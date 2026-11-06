package com.navidrometv.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.navidrometv.core.common.Formatters
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.LocalAppColors
import com.navidrometv.core.designsystem.theme.NavidromeColors
import com.navidrometv.core.model.Track

/**
 * One track in a list. The row body and the favourite toggle are siblings rather
 * than nested click targets, so D-pad RIGHT moves into the heart and LEFT returns
 * to the row.
 */
@Composable
fun TrackRow(
    track: Track,
    index: Int,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    showAlbum: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FocusableRow(
            onClick = onPlay,
            modifier = Modifier.weight(1f),
            selected = isCurrent,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                    if (isCurrent) {
                        EqualizerIndicator(
                            playing = isPlaying,
                            modifier = Modifier.size(width = 20.dp, height = 18.dp),
                        )
                    } else {
                        Text(
                            text = (track.trackNumber ?: index + 1).toString(),
                            style = AppType.metadata,
                            color = LocalAppColors.current.secondaryText,
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = AppType.body,
                        color = if (isCurrent) NavidromeColors.Accent else NavidromeColors.OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val secondary = buildString {
                        if (!track.artistName.isNullOrBlank()) append(track.artistName)
                        if (showAlbum && !track.albumName.isNullOrBlank()) {
                            if (isNotEmpty()) append(" · ")
                            append(track.albumName)
                        }
                    }
                    if (secondary.isNotBlank()) {
                        Text(
                            text = secondary,
                            style = AppType.cardSubtitle,
                            color = LocalAppColors.current.secondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (track.hasDuration) {
                    Text(
                        text = Formatters.duration(track.durationMs),
                        style = AppType.metadata,
                        color = LocalAppColors.current.secondaryText,
                    )
                }
            }
        }
        TvClickable(onClick = onToggleFavorite, shape = RoundedCornerShape(percent = 50)) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .size(40.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (track.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (track.isFavorite) NavidromeColors.Accent else LocalAppColors.current.secondaryText,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        trailing?.invoke()
    }
}

/**
 * Three-bar animated equalizer marking the currently playing track. Only runs an
 * infinite transition while audio is actually playing, to keep idle recomposition
 * (and OLED power draw) low.
 */
@Composable
fun EqualizerIndicator(playing: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "equalizer")

    val barOne by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar-one",
    )
    val barTwo by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(360, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar-two",
    )
    val barThree by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar-three",
    )

    val heights = if (playing) listOf(barOne, barTwo, barThree) else listOf(0.5f, 0.5f, 0.5f)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        heights.forEachIndexed { index, value ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((value.coerceIn(0.15f, 1f) * 18f).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(NavidromeColors.Accent),
            )
            if (index == heights.lastIndex) Unit
        }
    }
}
