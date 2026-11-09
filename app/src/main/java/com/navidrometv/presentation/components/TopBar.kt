package com.navidrometv.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.navidrometv.core.designsystem.component.TvClickable
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.NavidromeColors
import com.navidrometv.core.designsystem.theme.Space
import com.navidrometv.presentation.navigation.Routes

/**
 * Top navigation, laid out the way a television app expects it: a full-width row
 * of destinations on the safe-area margin rather than a floating phone-style
 * capsule. The current destination is bold white, the others sit at ~60% white,
 * and focus resolves to a solid white pill with black type so the remote target is
 * unmistakable from across the room.
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
            .fillMaxWidth()
            .padding(
                start = Space.screenHorizontal,
                end = Space.screenHorizontal,
                top = Space.sm,
                bottom = Space.xs,
            )
            .height(Space.topBarHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        navTabs.forEach { (route, label) ->
            NavTab(
                label = label,
                selected = currentRoute == route,
                onClick = { onNavigate(route) },
            )
        }
        Spacer(Modifier.weight(1f))
        NavIcon(
            icon = Icons.Rounded.Search,
            contentDescription = "Search",
            selected = currentRoute == Routes.SEARCH,
            onClick = { onNavigate(Routes.SEARCH) },
        )
        NavIcon(
            icon = Icons.Rounded.Settings,
            contentDescription = "Settings",
            selected = currentRoute == Routes.SETTINGS,
            onClick = onOpenSettings,
        )
        NavTab(label = "Party", selected = currentRoute == Routes.PARTY, onClick = onOpenParty)
    }
}

/** Search lives in the bar as an icon, so it is not repeated as a text tab. */
private val navTabs: List<Pair<String, String>> =
    Routes.topLevel.filterNot { it.first == Routes.SEARCH }

@Composable
private fun NavTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val pill = RoundedCornerShape(percent = 50)
    TvClickable(
        onClick = onClick,
        shape = pill,
        focusedScale = 1.03f,
        ringWidth = 3.dp,
    ) { focused ->
        val fill by animateColorAsState(
            targetValue = if (focused) Color.White else Color.Transparent,
            animationSpec = tween(durationMillis = 140),
            label = "nav-fill",
        )
        Text(
            text = label,
            style = AppType.bodyMuted.copy(
                fontWeight = when {
                    focused || selected -> FontWeight.SemiBold
                    else -> FontWeight.Normal
                },
            ),
            color = when {
                focused -> NavidromeColors.Ink
                selected -> NavidromeColors.OnSurface
                else -> NavidromeColors.OnSurfaceMuted
            },
            modifier = Modifier
                .background(fill, pill)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun NavIcon(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val pill = RoundedCornerShape(percent = 50)
    TvClickable(onClick = onClick, shape = pill, focusedScale = 1.03f, ringWidth = 3.dp) { focused ->
        val fill by animateColorAsState(
            targetValue = if (focused) Color.White else Color.Transparent,
            animationSpec = tween(durationMillis = 140),
            label = "nav-icon-fill",
        )
        Box(
            modifier = Modifier
                .background(fill, pill)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = when {
                    focused -> NavidromeColors.Ink
                    selected -> NavidromeColors.OnSurface
                    else -> NavidromeColors.OnSurfaceMuted
                },
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
