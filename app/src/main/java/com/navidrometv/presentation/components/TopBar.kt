package com.navidrometv.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.NavidromeColors
import com.navidrometv.core.designsystem.theme.Space
import com.navidrometv.presentation.navigation.Routes

/**
 * Floating top navigation. The active destination is a solid white pill with dark
 * text; focus inverts to the same filled treatment with an accent ring and a small
 * scale-up, so selection and focus are both readable from across a room.
 */
@Composable
fun TopBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenParty: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = Space.screenHorizontal)
            .padding(top = Space.sm, bottom = 2.dp)
            .background(NavidromeColors.Ink.copy(alpha = 0.62f), RoundedCornerShape(20.dp))
            .padding(horizontal = Space.md, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Navidrome",
                style = AppType.section,
                color = NavidromeColors.OnSurface,
                modifier = Modifier.padding(horizontal = Space.xs),
            )
            Routes.topLevel.forEach { (route, label) ->
                NavItem(
                    label = label,
                    selected = currentRoute == route,
                    onClick = { onNavigate(route) },
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconNavItem(contentDescription = "Party mode", onClick = onOpenParty) { focused ->
                Text(
                    text = "PARTY",
                    style = AppType.overline,
                    color = if (focused) NavidromeColors.Ink else NavidromeColors.OnSurfaceMuted,
                )
            }
            IconNavItem(contentDescription = "Settings", onClick = onOpenSettings) { focused ->
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = if (focused) NavidromeColors.Ink else NavidromeColors.OnSurfaceMuted,
                    modifier = Modifier.padding(2.dp),
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val pill = RoundedCornerShape(percent = 50)
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.05f else 1f,
        animationSpec = tween(durationMillis = 140),
        label = "nav-scale",
    )
    val background by animateColorAsState(
        targetValue = when {
            selected -> Color.White
            focused -> Color.White.copy(alpha = 0.92f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 140),
        label = "nav-pill",
    )
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(pill)
            .background(background)
            .then(
                if (focused) Modifier.border(2.dp, NavidromeColors.Accent, pill) else Modifier,
            )
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.isFocused }
            .padding(horizontal = 18.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = AppType.button,
            color = if (selected || focused) NavidromeColors.Ink else NavidromeColors.OnSurfaceMuted,
        )
    }
}

@Composable
private fun IconNavItem(
    contentDescription: String,
    onClick: () -> Unit,
    content: @Composable (focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val pill = RoundedCornerShape(percent = 50)
    Box(
        modifier = Modifier
            .clip(pill)
            .background(if (focused) Color.White else Color.Transparent, pill)
            .then(
                if (focused) Modifier.border(2.dp, NavidromeColors.Accent, pill) else Modifier,
            )
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.isFocused }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.padding(2.dp), contentAlignment = Alignment.Center) {
            content(focused)
        }
    }
}
