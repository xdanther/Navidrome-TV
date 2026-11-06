package com.navidrometv.presentation.components

import androidx.compose.animation.core.animateFloatAsState
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
 * Floating top navigation. Horizontal D-pad movement between destinations, with a
 * filled pill for the active destination and a soft surface for focus, so the
 * current location and the focus ring never look the same.
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
            .background(NavidromeColors.Ink.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
            .padding(horizontal = Space.lg, vertical = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.lg),
        ) {
            Text(
                text = "Navidrome",
                style = AppType.section,
                color = NavidromeColors.OnSurface,
            )
            Routes.topLevel.forEach { (route, label) ->
                NavItem(
                    label = label,
                    selected = currentRoute == route,
                    onClick = { onNavigate(route) },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            IconNavItem(
                contentDescription = "Party mode",
                onClick = onOpenParty,
            ) {
                Text(text = "PARTY", style = AppType.overline, color = NavidromeColors.OnSurfaceMuted)
            }
            IconNavItem(contentDescription = "Settings", onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = NavidromeColors.OnSurfaceMuted,
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
    val scale by animateFloatAsState(if (focused) 1.04f else 1f, label = "nav-scale")
    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    selected -> NavidromeColors.OnSurface
                    focused -> NavidromeColors.SurfaceVariant
                    else -> Color.Transparent
                },
            )
            .then(
                if (focused && !selected) {
                    Modifier.border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.isFocused }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = AppType.button,
            color = when {
                selected -> NavidromeColors.Ink
                focused -> NavidromeColors.OnSurface
                else -> NavidromeColors.OnSurfaceMuted
            },
        )
    }
}

@Composable
private fun IconNavItem(
    contentDescription: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (focused) NavidromeColors.SurfaceVariant else Color.Transparent)
            .then(
                if (focused) Modifier.border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                else Modifier,
            )
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.isFocused }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.padding(2.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}
