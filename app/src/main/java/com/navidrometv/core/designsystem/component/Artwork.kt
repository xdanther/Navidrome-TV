package com.navidrometv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    listOf(Color(0xFF2A2C30), Color(0xFF17181B)),
    listOf(Color(0xFF2E3438), Color(0xFF17191B)),
    listOf(Color(0xFF262E36), Color(0xFF141719)),
    listOf(Color(0xFF33302B), Color(0xFF1A1917)),
    listOf(Color(0xFF2B332D), Color(0xFF161A17)),
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
    val context = LocalContext.current
    // Build the request once per URL: a fresh ImageRequest on every recomposition
    // makes Coil restart its work and is a direct cause of artwork flicker.
    val request = remember(url) {
        url?.let { target ->
            ImageRequest.Builder(context)
                .data(target)
                .crossfade(220)
                .build()
        }
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(gradientFor(coverArtId), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (request != null) {
            AsyncImage(
                model = request,
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
