package com.navidrometv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.navidrometv.core.common.AppError
import com.navidrometv.core.common.ErrorKind
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.LocalAppColors
import com.navidrometv.core.designsystem.theme.NavidromeColors
import com.navidrometv.core.designsystem.theme.Space

/**
 * Loading state for data-driven screens. Drawn as three pulsing dots so the
 * indicator does not spin expensive blurred layers behind artwork.
 */
@Composable
fun LoadingState(modifier: Modifier = Modifier, label: String = "Loading") {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(3) { index ->
                val alpha = when (index) {
                    0 -> 0.25f
                    1 -> 0.6f
                    else -> 1f
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(NavidromeColors.Accent.copy(alpha = alpha), CircleShape),
                )
            }
        }
        Text(
            text = label,
            style = AppType.bodyMuted,
            color = LocalAppColors.current.secondaryText,
            modifier = Modifier.padding(top = Space.md),
        )
    }
}

@Composable
fun MessageState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    isError: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Space.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (isError) Icons.Rounded.CloudOff else Icons.Rounded.Inbox,
            contentDescription = null,
            tint = NavidromeColors.OnSurfaceMuted,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = title,
            style = AppType.title,
            color = NavidromeColors.OnSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Space.md),
        )
        Text(
            text = message,
            style = AppType.bodyMuted,
            color = LocalAppColors.current.secondaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Space.xs),
        )
        if (actionLabel != null && onAction != null) {
            ActionButton(
                text = actionLabel,
                onClick = onAction,
                icon = Icons.Rounded.Refresh,
                modifier = Modifier.padding(top = Space.lg),
            )
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    MessageState(
        title = title,
        message = message,
        modifier = modifier,
        actionLabel = actionLabel,
        onAction = onAction,
    )
}

@Composable
fun ErrorState(
    error: AppError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (error.kind) {
        ErrorKind.NOT_CONFIGURED -> "Server not connected"
        ErrorKind.NETWORK, ErrorKind.TIMEOUT -> "Can't reach the server"
        ErrorKind.AUTHENTICATION -> "Sign-in failed"
        ErrorKind.AUTHORIZATION -> "Not allowed"
        ErrorKind.NOT_FOUND -> "Nothing here"
        ErrorKind.UNSUPPORTED -> "Not supported by this server"
        else -> "Something went wrong"
    }
    MessageState(
        title = title,
        message = error.message,
        modifier = modifier,
        actionLabel = "Try again",
        onAction = onRetry,
        isError = true,
    )
}

/** Small inline banner for non-blocking failures (favourite, scrobble, ...). */
@Composable
fun InlineNotice(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(NavidromeColors.SurfaceElevated, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text = text, style = AppType.metadata, color = NavidromeColors.OnSurfaceMuted)
    }
}
