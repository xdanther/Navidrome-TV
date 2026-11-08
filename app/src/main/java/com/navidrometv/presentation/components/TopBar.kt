package com.navidrometv.presentation.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
 * Top navigation, built as TV tabs: destinations sit in a single row at the top,
 * the active one is marked with a white label and an accent underline, and focus
 * adds a translucent pill plus a white outline. Tabs are the pattern the TV
 * guidance recommends for primary destinations that stick to the top.
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
            .padding(horizontal = Space.screenHorizontal, vertical = Space.md)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            Text(
                text = "Navidrome",
                style = AppType.section,
                color = NavidromeColors.OnSurface,
                modifier = Modifier.padding(end = Space.md),
            )
            Routes.topLevel.forEach { (route, label) ->
                NavTab(
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
private fun NavTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val pill = RoundedCornerShape(8.dp)
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.05f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "nav-scale",
    )
    val background by animateColorAsState(
        targetValue = if (focused) Color.White.copy(alpha = 0.14f) else Color.Transparent,
        animationSpec = tween(durationMillis = 120),
        label = "nav-fill",
    )
    val underline by animateDpAsState(
        targetValue = if (selected) 22.dp else 0.dp,
        animationSpec = tween(durationMillis = 160),
        label = "nav-underline",
    )
    Column(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(pill)
            .background(background)
            .then(if (focused) Modifier.border(2.dp, Color.White, pill) else Modifier)
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.isFocused }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = AppType.button,
            color = when {
                selected || focused -> NavidromeColors.OnSurface
                else -> NavidromeColors.OnSurfaceMuted
            },
        )
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .width(underline)
                .height(2.dp)
                .background(NavidromeColors.Accent, RoundedCornerShape(1.dp)),
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
            .then(if (focused) Modifier.border(2.dp, Color.White, pill) else Modifier)
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
