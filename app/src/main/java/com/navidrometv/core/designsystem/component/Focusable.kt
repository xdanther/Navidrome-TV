package com.navidrometv.core.designsystem.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.Dimens
import com.navidrometv.core.designsystem.theme.NavidromeColors
import com.navidrometv.core.designsystem.theme.Space

/**
 * The base interactive surface.
 *
 * Focus combines the three indications the Android TV focus guidance recommends —
 * a modest scale (1.05x, the middle of the 1.025/1.05/1.1 range), a 2dp white
 * outline, and a surface colour change supplied by the caller. Scaling happens on
 * the graphics layer and never changes the measured size, so a row does not reflow
 * as focus moves.
 */
@Composable
fun TvClickable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    focusedScale: Float = Dimens.FOCUS_SCALE,
    shape: Shape = RoundedCornerShape(Dimens.cardCorner),
    ringWidth: Dp = Dimens.focusBorderWidth,
    ringColor: Color = NavidromeColors.FocusRing,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) focusedScale else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "focus-scale",
    )
    val ring by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "focus-ring",
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .zIndex(if (focused) 2f else 0f)
            // The ring is drawn *inside* the element's bounds: a white band with a
            // dark outer edge, following the element's own rounded shape. Drawing it
            // inset means no scroll container, capsule or card clip can swallow it,
            // and the two-tone band reads on artwork (white) and on white chrome
            // (dark edge) alike.
            .drawWithContent {
                drawContent()
                if (ring > 0.01f) {
                    val band = minOf(ringWidth.toPx() * ring, size.minDimension / 2f)
                    if (band > 0.5f) {
                        val edge = band * 0.38f
                        val baseRadius = when (shape) {
                            is RoundedCornerShape -> shape.topStart.toPx(size, this)
                            else -> size.minDimension / 2f
                        }
                        val center = band / 2f
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.98f * ring),
                            topLeft = Offset(center, center),
                            size = Size(size.width - band, size.height - band),
                            cornerRadius = CornerRadius((baseRadius - center).coerceAtLeast(0f)),
                            style = Stroke(width = band),
                        )
                        val edgeCenter = edge / 2f
                        drawRoundRect(
                            color = ringColor.copy(alpha = ring),
                            topLeft = Offset(edgeCenter, edgeCenter),
                            size = Size(size.width - edge, size.height - edge),
                            cornerRadius = CornerRadius((baseRadius - edgeCenter).coerceAtLeast(0f)),
                            style = Stroke(width = edge),
                        )
                    }
                }
            }
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
            .onFocusChanged {
                if (focused != it.isFocused) {
                    focused = it.isFocused
                    onFocusChanged?.invoke(it.isFocused)
                }
            },
    ) {
        content(focused)
    }
}

/**
 * Pill action. The primary action is a solid ink pill with white text; the focused
 * state lifts it to white with dark text and a heavy outline, so the active button
 * separates cleanly from the light backdrop without a glow.
 */
@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = true,
    enabled: Boolean = true,
) {
    val pill = RoundedCornerShape(Dimens.pillCorner)
    TvClickable(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = pill,
    ) { focused ->
        val background = when {
            !enabled -> NavidromeColors.SurfaceVariant
            focused -> NavidromeColors.Surface
            primary -> NavidromeColors.Ink
            else -> NavidromeColors.GlassSubtle
        }
        val contentColor = when {
            !enabled -> NavidromeColors.OnSurfaceMuted
            focused -> NavidromeColors.Ink
            primary -> Color.White
            else -> NavidromeColors.OnSurface
        }
        Row(
            modifier = Modifier
                .background(background, pill)
                .then(
                    if (primary || !enabled) {
                        Modifier
                    } else {
                        Modifier.border(1.dp, NavidromeColors.Border, pill)
                    },
                )
                .defaultMinSize(minHeight = 48.dp)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.defaultMinSize(minWidth = 20.dp, minHeight = 20.dp),
                )
            }
            Text(text = text, style = AppType.button, color = contentColor)
        }
    }
}

/**
 * Circular icon control. Unfocused it is a quiet translucent disc; focused it fills
 * solid white, which is how TV media controls signal the active remote target.
 */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 56,
    tint: Color = NavidromeColors.OnSurface,
    background: Color = NavidromeColors.SurfaceVariant,
    enabled: Boolean = true,
    ringColor: Color = NavidromeColors.FocusRing,
) {
    val circle = RoundedCornerShape(percent = 50)
    TvClickable(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = circle,
        ringColor = ringColor,
    ) { focused ->
        val surface = when {
            !enabled -> background.copy(alpha = 0.35f)
            focused -> NavidromeColors.Surface
            else -> background
        }
        val iconTint = when {
            !enabled -> tint.copy(alpha = 0.35f)
            focused -> NavidromeColors.Ink
            else -> tint
        }
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = size.dp, minHeight = size.dp)
                .background(surface, circle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.defaultMinSize(
                    minWidth = (size * 0.45f).dp,
                    minHeight = (size * 0.45f).dp,
                ),
            )
        }
    }
}

/**
 * Focusable row used by lists (tracks, settings, sidebar sections). Selection is a
 * quiet filled surface plus a leading accent bar; focus adds the white outline and
 * a brighter fill, which keeps the two states distinguishable.
 */
@Composable
fun FocusableRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    selected: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    TvClickable(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        focusedScale = 1.015f,
    ) { focused ->
        Box(
            modifier = Modifier
                .background(
                    when {
                        focused -> Color.Black.copy(alpha = 0.09f)
                        selected -> Color.Black.copy(alpha = 0.05f)
                        else -> Color.Transparent
                    },
                    shape,
                )
                .padding(contentPadding),
        ) {
            content(focused)
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(3.dp)
                        .fillMaxHeight(0.6f)
                        .background(NavidromeColors.Accent, RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

/** Text-only chip for sort and filter controls. */
@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pill = RoundedCornerShape(percent = 50)
    TvClickable(
        onClick = onClick,
        modifier = modifier,
        shape = pill,
        ringWidth = 4.dp,
    ) { focused ->
        Box(
            modifier = Modifier
                .background(
                    when {
                        selected -> NavidromeColors.Ink
                        focused -> NavidromeColors.Surface
                        else -> Color.Transparent
                    },
                    pill,
                )
                .then(
                    if (selected || focused) {
                        Modifier
                    } else {
                        Modifier.border(1.dp, NavidromeColors.Border, pill)
                    },
                )
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            Text(
                text = label,
                style = AppType.metadata,
                color = when {
                    selected -> Color.White
                    focused -> NavidromeColors.Ink
                    else -> NavidromeColors.OnSurface
                },
            )
        }
    }
}

/** Section title with an optional trailing action. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            Text(text = title, style = AppType.section, color = NavidromeColors.OnSurface)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = AppType.metadata,
                    color = NavidromeColors.OnSurfaceMuted,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            TvClickable(
                onClick = onAction,
                shape = RoundedCornerShape(Dimens.pillCorner),
            ) { focused ->
                Text(
                    text = actionLabel,
                    style = AppType.metadata,
                    color = if (focused) Color.White else NavidromeColors.OnSurfaceMuted,
                    modifier = Modifier
                        .background(
                            if (focused) NavidromeColors.Ink else Color.Transparent,
                            RoundedCornerShape(Dimens.pillCorner),
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
    }
}
