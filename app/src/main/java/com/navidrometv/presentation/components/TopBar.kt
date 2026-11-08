package com.navidrometv.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.navidrometv.core.designsystem.component.TvClickable
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.Dimens
import com.navidrometv.core.designsystem.theme.NavidromeColors
import com.navidrometv.core.designsystem.theme.Space
import com.navidrometv.presentation.navigation.Routes

/**
 * Top navigation as a floating glass capsule: destinations sit in one row, the
 * current one is a solid white pill, and the utility icons close the capsule. It
 * floats over the ambient background instead of being a full-width bar, which keeps
 * the content the brightest thing on screen.
 */
@Composable
fun TopBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenParty: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val capsule = RoundedCornerShape(percent = 50)
    val shadow = Color.Black.copy(alpha = 0.12f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Space.md, bottom = Space.xs),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .shadow(Dimens.floatingElevation, capsule, ambientColor = shadow, spotColor = shadow)
                .clip(capsule)
                .background(NavidromeColors.Glass)
                .border(1.dp, Color.White.copy(alpha = 0.7f), capsule)
                .padding(horizontal = 8.dp, vertical = 6.dp),
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
            Spacer(Modifier.width(Space.xs))
            Box(
                Modifier
                    .width(1.dp)
                    .height(22.dp)
                    .background(NavidromeColors.Border),
            )
            Spacer(Modifier.width(Space.xs))
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
            NavTextChip(label = "PARTY", onClick = onOpenParty)
        }
    }
}

/** Search lives in the capsule as an icon, so it is not repeated as a text tab. */
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
        focusedScale = 1.04f,
    ) { focused ->
        val fill by animateColorAsState(
            targetValue = when {
                selected -> NavidromeColors.Surface
                focused -> Color.White.copy(alpha = 0.55f)
                else -> Color.Transparent
            },
            animationSpec = tween(durationMillis = 140),
            label = "nav-fill",
        )
        Text(
            text = label,
            style = AppType.button.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (selected || focused) NavidromeColors.Ink else NavidromeColors.OnSurfaceMuted,
            modifier = Modifier
                .background(fill, pill)
                .padding(horizontal = 18.dp, vertical = 9.dp),
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
    TvClickable(onClick = onClick, shape = pill, focusedScale = 1.04f) { focused ->
        val fill by animateColorAsState(
            targetValue = when {
                selected -> NavidromeColors.Surface
                focused -> Color.White.copy(alpha = 0.55f)
                else -> Color.Transparent
            },
            animationSpec = tween(durationMillis = 140),
            label = "nav-icon-fill",
        )
        Box(
            modifier = Modifier
                .background(fill, pill)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (selected) NavidromeColors.Ink else NavidromeColors.OnSurfaceMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun NavTextChip(label: String, onClick: () -> Unit) {
    val pill = RoundedCornerShape(percent = 50)
    TvClickable(onClick = onClick, shape = pill, focusedScale = 1.04f) { focused ->
        Text(
            text = label,
            style = AppType.overline,
            color = if (focused) NavidromeColors.Ink else NavidromeColors.OnSurfaceMuted,
            modifier = Modifier
                .background(if (focused) Color.White.copy(alpha = 0.65f) else Color.Transparent, pill)
                .padding(horizontal = 12.dp, vertical = 12.dp),
        )
    }
}
