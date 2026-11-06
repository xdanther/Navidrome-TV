package com.navidrometv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.navidrometv.core.designsystem.theme.NavidromeColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import kotlin.math.absoluteValue

/**
 * Resolves a Subsonic cover-art id into a fully authenticated image URL. The
 * provider lives at the app root because the URL depends on the active server
 * session, which the design system must not know about.
 */
private val NoCoverArtUrl: (String?, Int) -> String? = { _, _ -> null }

val LocalCoverArtUrl = staticCompositionLocalOf { NoCoverArtUrl }

private val PlaceholderGradients = listOf(
    listOf(Color(0xFF2B1B4A), Color(0xFF0F0D1A)),
    listOf(Color(0xFF3D1236), Color(0xFF120B16)),
    listOf(Color(0xFF12303D), Color(0xFF0A1216)),
    listOf(Color(0xFF3A2410), Color(0xFF140D07)),
    listOf(Color(0xFF1B2C1E), Color(0xFF0A100B)),
)

private fun gradientFor(seed: String?): Brush {
    val index = (seed?.hashCode() ?: 0).absoluteValue % PlaceholderGradients.size
    val colors = PlaceholderGradients[index]
    return Brush.linearGradient(colors)
}

/**
 * Album artwork with a deterministic gradient fallback, so a missing cover keeps
 * the layout stable instead of collapsing or flashing white.
 */
@Composable
fun Artwork(
    coverArtId: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    requestSizePx: Int = 640,
    showFallbackIcon: Boolean = true,
) {
    val url = LocalCoverArtUrl.current(coverArtId, requestSizePx)
    Box(
        modifier = modifier
            .clip(shape)
            .background(gradientFor(coverArtId), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(200)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (showFallbackIcon) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = NavidromeColors.OnSurface.copy(alpha = 0.28f),
                modifier = Modifier.fillMaxSize(0.34f),
            )
        }
    }
}
