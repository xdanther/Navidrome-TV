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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.Dimens
import com.navidrometv.core.designsystem.theme.LocalAppColors
import com.navidrometv.core.designsystem.theme.NavidromeColors
import com.navidrometv.core.designsystem.theme.Space

/**
 * The base interactive surface for the TV UI.
 *
 * Focus is signalled three ways at once — a filled/high-contrast body treatment in
 * the caller, an animated ring drawn above the content, and a small scale-up — so it
 * reads from several metres away instead of depending on a thin outline. The scale
 * happens on the graphics layer, which never changes the measured size, so
 * neighbours do not jump while the user moves through a row.
 */
@Composable
fun TvClickable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    focusedScale: Float = Dimens.FOCUS_SCALE,
    shape: Shape = RoundedCornerShape(Dimens.cardCorner),
    ringWidth: Dp = 3.dp,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) focusedScale else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "focus-scale",
    )
    val ring by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "focus-ring",
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .zIndex(if (focused) 2f else 0f)
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
        if (ring > 0.01f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(
                        width = ringWidth * ring,
                        color = Color.White.copy(alpha = 0.92f * ring),
                        shape = shape,
                    ),
            )
        }
    }
}

/**
 * Large pill action used for Play / Shuffle / Queue rows on detail screens. Focus
 * inverts the button to a solid white surface with dark text, matching the filled
 * focus language used by the cards.
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
        focusedScale = Dimens.FOCUS_SCALE,
    ) { focused ->
        val background = when {
            !enabled -> NavidromeColors.SurfaceVariant.copy(alpha = 0.4f)
            focused -> Color.White
            primary -> NavidromeColors.Accent
            else -> NavidromeColors.SurfaceElevated.copy(alpha = 0.92f)
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
 * Circular icon control for transport buttons and per-row actions. The focused
 * state fills with the accent colour so the active transport button is obvious
 * without any glow.
 */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 56,
    tint: Color = NavidromeColors.OnSurface,
    background: Color = NavidromeColors.SurfaceElevated.copy(alpha = 0.7f),
    enabled: Boolean = true,
) {
    val circle = RoundedCornerShape(percent = 50)
    TvClickable(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = circle,
    ) { focused ->
        val surface = when {
            !enabled -> background.copy(alpha = 0.35f)
            focused -> NavidromeColors.Accent
            else -> background
        }
        val iconTint = when {
            !enabled -> tint.copy(alpha = 0.35f)
            focused -> Color.White
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
 * Focusable row used by lists (tracks, settings, playlists). Keeps a stable
 * height and paints a bright filled surface plus a ring behind the content, so a
 * focused track is unmistakable next to its neighbours.
 */
@Composable
fun FocusableRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    selected: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    val colors = LocalAppColors.current
    TvClickable(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        focusedScale = 1.015f,
        ringWidth = 2.dp,
    ) { focused ->
        Box(
            modifier = Modifier
                .background(
                    when {
                        focused -> Color.White.copy(alpha = 0.2f)
                        selected -> NavidromeColors.SurfaceElevated.copy(alpha = 0.72f)
                        else -> Color.Transparent
                    },
                    shape,
                )
                .then(
                    if (selected && !focused) {
                        Modifier.border(1.dp, colors.focusedBorder.copy(alpha = 0.28f), shape)
                    } else {
                        Modifier
                    },
                )
                .padding(contentPadding),
        ) {
            content(focused)
        }
    }
}

/** Simple text-only focusable chip, used for sort/filter controls. */
@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pill = RoundedCornerShape(percent = 50)
    val colors = LocalAppColors.current
    TvClickable(
        onClick = onClick,
        modifier = modifier,
        shape = pill,
        ringWidth = 2.dp,
    ) { focused ->
        Box(
            modifier = Modifier
                .background(
                    when {
                        focused -> Color.White
                        selected -> NavidromeColors.SurfaceVariant
                        else -> NavidromeColors.Surface.copy(alpha = 0.6f)
                    },
                    pill,
                )
                .then(
                    if (selected && !focused) {
                        Modifier.border(1.5.dp, colors.focusedBorder.copy(alpha = 0.5f), pill)
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            Text(
                text = label,
                style = AppType.metadata,
                color = if (focused) NavidromeColors.Ink else NavidromeColors.OnSurface,
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
                    color = LocalAppColors.current.secondaryText,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            TvClickable(
                onClick = onAction,
                shape = RoundedCornerShape(Dimens.pillCorner),
                ringWidth = 2.dp,
            ) { focused ->
                Text(
                    text = actionLabel,
                    style = AppType.metadata,
                    color = if (focused) NavidromeColors.Ink else NavidromeColors.OnSurfaceMuted,
                    modifier = Modifier
                        .background(
                            if (focused) Color.White else Color.Transparent,
                            RoundedCornerShape(Dimens.pillCorner),
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
    }
}
