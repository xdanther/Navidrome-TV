package com.navidrometv.core.designsystem.theme

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.lightColorScheme

/**
 * Colour roles for the light "ambient" look.
 *
 * The interface sits on a soft, warm-to-cool gradient instead of a flat dark
 * surface: text is near-black ink, cards are translucent white glass, and a single
 * system blue marks state (playing, active toggles, progress). Artwork supplies the
 * strong colour, so the chrome stays quiet.
 */
object NavidromeColors {
    /** Primary text and solid dark fills (buttons, dim overlays). */
    val Ink = Color(0xFF16181C)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceElevated = Color(0xFFFFFFFF)

    /** Low-contrast tint used for quiet fills on top of the light backdrop. */
    val SurfaceVariant = Color(0x14000000)
    val OnSurface = Color(0xFF16181C)
    val OnSurfaceMuted = Color(0xFF64686F)
    val Accent = Color(0xFF0A84FF)
    val AccentMuted = Color(0xFFDCEAFE)
    val Border = Color(0x1A000000)
    val Scrim = Color(0x99000000)

    /** White fill for the focused element. */
    val Focus = Color(0xFFFFFFFF)

    /** Outline drawn outside a focused element; must stay visible on light chrome. */
    val FocusRing = Color(0xFF16181C)

    /** Translucent white panels: navigation pill, mini player, quiet buttons. */
    val Glass = Color(0xF2FFFFFF)
    val GlassSubtle = Color(0x8CFFFFFF)

    // Backdrop gradient stops (warm sand, cream, sage, sky). Every stop carries a
    // visible tint: a near-white stop reads as a white margin at the screen edges.
    val AmbientSand = Color(0xFFF2E3CB)
    val AmbientCream = Color(0xFFF9F2E6)
    val AmbientSage = Color(0xFFDCEADF)
    val AmbientSky = Color(0xFFD6E3F1)
    val AmbientMist = Color(0xFFE4EBF4)
}

private val NavidromeLightColorScheme = lightColorScheme(
    primary = NavidromeColors.Accent,
    onPrimary = Color.White,
    primaryContainer = NavidromeColors.AccentMuted,
    onPrimaryContainer = Color(0xFF0B3D7A),
    secondary = NavidromeColors.Ink,
    onSecondary = Color.White,
    background = NavidromeColors.AmbientMist,
    onBackground = NavidromeColors.OnSurface,
    surface = NavidromeColors.Surface,
    onSurface = NavidromeColors.OnSurface,
    surfaceVariant = NavidromeColors.SurfaceVariant,
    onSurfaceVariant = NavidromeColors.OnSurfaceMuted,
    error = Color(0xFFB3261E),
    onError = Color.White,
)

/** Roles that Material's scheme does not model but the 10-foot UI needs. */
@Immutable
data class AppColors(
    val cardScrim: Color = Color(0x00000000),
    val focusedBorder: Color = NavidromeColors.FocusRing,
    val secondaryText: Color = NavidromeColors.OnSurfaceMuted,
    val divider: Color = Color(0x14000000),
    val accentGradientStart: Color = Color(0xFF0A84FF),
    val accentGradientEnd: Color = Color(0xFF4F9BFF),
    val overlay: Color = Color(0x99000000),
)

val LocalAppColors = staticCompositionLocalOf { AppColors() }

/**
 * TV type scale. Sizes follow the guidance that TV typography runs noticeably
 * larger than mobile, uses Roboto, and avoids thin weights. Section and page
 * titles are bold, matching how a media app labels shelves.
 */
object AppType {
    private val Family = FontFamily.SansSerif

    val hero = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 54.sp,
        letterSpacing = (-0.8).sp,
    )
    val display = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.4).sp,
    )
    val title = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.3).sp,
    )
    val section = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp,
    )
    val cardTitle = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 21.sp,
    )
    val cardSubtitle = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 17.sp,
    )
    val body = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    )
    val bodyMuted = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    )
    val button = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    )
    val overline = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.2.sp,
    )
    val metadata = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    )
}

/**
 * Spacing grid. The horizontal and vertical screen margins are the overscan-safe
 * values from the TV layout guidance (48dp sides, 27dp top/bottom), so primary
 * content never sits in the region a television may crop.
 */
object Space {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val screenHorizontal = 48.dp
    val screenTop = 27.dp
    val railWidth = 280.dp
    val contentBottomWithPlayer = 136.dp
    val cardWidth = 200.dp
    val cardArtwork = 200.dp
    val cardPoster = 204.dp
    val rowSpacing = 24.dp
    val topBarHeight = 72.dp
}

object Dimens {
    val cardCorner = 6.dp
    val panelCorner = 16.dp
    val pillCorner = 100.dp
    val focusBorderWidth = 6.dp
    val cardElevation = 10.dp
    val focusElevation = 22.dp
    val floatingElevation = 20.dp
    const val FOCUS_SCALE = 1.10f
}

/**
 * The ambient backdrop: one diagonal wash, warm sand at the top-left into cream,
 * sage and cool sky at the bottom-right, plus two soft colour blooms. It is drawn
 * once behind the whole app and covers the full canvas edge to edge, so no screen
 * ever shows a bare white margin around the content.
 */
@Composable
fun AmbientBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    NavidromeColors.AmbientSand,
                    NavidromeColors.AmbientCream,
                    NavidromeColors.AmbientSage,
                    NavidromeColors.AmbientSky,
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height),
            ),
        )
        // Warm bloom in the lower-left corner.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    NavidromeColors.AmbientSand.copy(alpha = 0.75f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.06f, size.height * 1.02f),
                radius = size.width * 0.7f,
            ),
        )
        // Cool bloom in the upper-right corner.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    NavidromeColors.AmbientSky.copy(alpha = 0.8f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.95f, size.height * -0.02f),
                radius = size.width * 0.65f,
            ),
        )
    }
}

@Composable
fun NavidromeTvTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppColors provides AppColors()) {
        MaterialTheme(colorScheme = NavidromeLightColorScheme) {
            content()
        }
    }
}
