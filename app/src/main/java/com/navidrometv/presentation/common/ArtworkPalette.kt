package com.navidrometv.presentation.common

import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.navidrometv.core.designsystem.component.LocalCoverArtUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Colours derived from the current album's artwork. The palette is computed once
 * per artwork from a small (160px) bitmap on a background dispatcher and then held
 * in a process-wide cache, so scrubbing back and forth through tracks never
 * re-extracts the same cover or falls back to the default palette on the frame a
 * new track starts.
 */
@Immutable
data class ArtworkColors(
    val vibrant: Color,
    val dominant: Color,
    val background: Color,
)

val DefaultArtworkColors = ArtworkColors(
    vibrant = Color(0xFF7B3FA0),
    dominant = Color(0xFF2B1B4A),
    background = Color(0xFF0B0A12),
)

private val PaletteCache = LruCache<String, ArtworkColors>(24)

private fun Color.scale(factor: Float): Color =
    Color(red * factor, green * factor, blue * factor, alpha)

@Composable
fun rememberArtworkColors(coverArtId: String?): ArtworkColors {
    val url = LocalCoverArtUrl.current(coverArtId, 160)
    val context = LocalContext.current
    var colors by remember(url) {
        mutableStateOf(url?.let { PaletteCache.get(it) } ?: DefaultArtworkColors)
    }

    LaunchedEffect(url) {
        if (url == null) {
            colors = DefaultArtworkColors
            return@LaunchedEffect
        }
        PaletteCache.get(url)?.let {
            colors = it
            return@LaunchedEffect
        }
        val computed = withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .size(160)
                    .build()
                val result = context.imageLoader.execute(request) as? SuccessResult
                    ?: return@runCatching DefaultArtworkColors
                val bitmap = result.drawable.toBitmap()
                val palette = Palette.from(bitmap)
                    .maximumColorCount(24)
                    .generate()
                val fallback = DefaultArtworkColors
                val dominant = palette.getDominantColor(fallback.dominant.toArgb())
                val vibrant = palette.getVibrantColor(dominant)
                val muted = palette.getDarkMutedColor(dominant)
                ArtworkColors(
                    vibrant = Color(vibrant),
                    dominant = Color(dominant),
                    background = Color(muted).scale(0.45f),
                )
            }.getOrDefault(DefaultArtworkColors)
        }
        PaletteCache.put(url, computed)
        colors = computed
    }

    return colors
}
