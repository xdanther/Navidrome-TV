package com.navidrometv.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.navidrometv.core.common.Formatters
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.LocalAppColors
import com.navidrometv.core.designsystem.theme.NavidromeColors

/**
 * Seek bar for the remote: LEFT/RIGHT seek by [stepMillis] and the bar itself is a
 * focus target with a visible ring, so scrubbing is discoverable with the D-pad.
 */
@Composable
fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    stepMillis: Long = 5_000L,
    showLabels: Boolean = true,
    labelsBelow: Boolean = false,
    filledColor: Color = NavidromeColors.OnSurface,
    trackColor: Color = NavidromeColors.SurfaceVariant,
    labelColor: Color? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val animatedFraction by animateFloatAsState(targetValue = fraction, label = "seek-fraction")
    val labels = labelColor ?: LocalAppColors.current.secondaryText
    val elapsed = Formatters.duration(positionMs)
    val remaining = Formatters.duration((durationMs - positionMs).coerceAtLeast(0L))

    if (showLabels && labelsBelow) {
        Column(modifier = modifier.fillMaxWidth()) {
            SeekBarTrack(
                positionMs = positionMs,
                durationMs = durationMs,
                fraction = animatedFraction,
                filledColor = filledColor,
                trackColor = trackColor,
                stepMillis = stepMillis,
                onSeek = onSeek,
                onFocusedChange = { focused = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = elapsed, style = AppType.metadata, color = labels)
                Text(text = remaining, style = AppType.metadata, color = labels)
            }
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (showLabels) {
                Text(
                    text = elapsed,
                    style = AppType.metadata,
                    color = if (focused) NavidromeColors.OnSurface else labels,
                    modifier = Modifier.width(56.dp),
                )
            }
            SeekBarTrack(
                positionMs = positionMs,
                durationMs = durationMs,
                fraction = animatedFraction,
                filledColor = filledColor,
                trackColor = trackColor,
                stepMillis = stepMillis,
                onSeek = onSeek,
                onFocusedChange = { focused = it },
                modifier = Modifier.weight(1f),
            )
            if (showLabels) {
                Text(
                    text = remaining,
                    style = AppType.metadata,
                    color = labels,
                    modifier = Modifier.width(56.dp),
                )
            }
        }
    }
}

/**
 * The focusable bar itself, shared by both label arrangements so the remote seek
 * handling exists once. It thickens and brightens while focused.
 */
@Composable
private fun SeekBarTrack(
    positionMs: Long,
    durationMs: Long,
    fraction: Float,
    filledColor: Color,
    trackColor: Color,
    stepMillis: Long,
    onSeek: (Long) -> Unit,
    onFocusedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .height(32.dp)
            .onFocusChanged {
                focused = it.isFocused
                onFocusedChange(it.isFocused)
            }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        onSeek((positionMs - stepMillis).coerceAtLeast(0L))
                        true
                    }
                    Key.DirectionRight -> {
                        onSeek((positionMs + stepMillis).coerceIn(0L, durationMs.coerceAtLeast(0L)))
                        true
                    }
                    else -> false
                }
            }
            .focusable(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (focused) 8.dp else 4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(trackColor.copy(alpha = if (focused) 1f else 0.72f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(if (focused) 8.dp else 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(filledColor),
            )
        }
    }
}

/**
 * Read-only progress used on cards and in the mini player, where seeking requires
 * entering Now Playing first.
 */
@Composable
fun ProgressLine(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(NavidromeColors.SurfaceVariant.copy(alpha = 0.6f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(3.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(NavidromeColors.Accent),
        )
    }
}
