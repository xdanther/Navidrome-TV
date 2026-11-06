package com.navidrometv.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
) {
    var focused by remember { mutableStateOf(false) }
    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val animatedFraction by animateFloatAsState(targetValue = fraction, label = "seek-fraction")

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (showLabels) {
            Text(
                text = Formatters.duration(positionMs),
                style = AppType.metadata,
                color = if (focused) NavidromeColors.OnSurface else LocalAppColors.current.secondaryText,
                modifier = Modifier.width(56.dp),
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
                .onFocusChanged { focused = it.isFocused }
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
                    .background(
                        if (focused) NavidromeColors.SurfaceVariant
                        else NavidromeColors.SurfaceVariant.copy(alpha = 0.7f),
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction)
                        .height(if (focused) 8.dp else 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(NavidromeColors.OnSurface),
                )
            }
        }
        if (showLabels) {
            Text(
                text = Formatters.duration((durationMs - positionMs).coerceAtLeast(0L)),
                style = AppType.metadata,
                color = LocalAppColors.current.secondaryText,
                modifier = Modifier.width(56.dp),
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
