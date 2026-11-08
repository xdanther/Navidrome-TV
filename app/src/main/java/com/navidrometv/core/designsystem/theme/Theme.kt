package com.navidrometv.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/**
 * Colour roles, built the way the Android TV guidance describes: a dark, neutral
 *Material scheme generated from a single key colour, with light text on dark
 * surfaces.
 *
 * The neutral scale is almost colourless on purpose — the interface should read as
 * charcoal, not tinted — and the artwork supplies the only strong colour. The one
 * accent is Google blue, used for state (playing, active toggles, progress), while
 * focus itself is white per the TV focus guidance.
 */
object NavidromeColors {
    val Ink = Color(0xFF0E0F11)
    val Surface = Color(0xFF17181B)
    val SurfaceElevated = Color(0xFF1F2124)
    val SurfaceVariant = Color(0xFF2A2C30)
    val OnSurface = Color(0xFFF1F2F4)
    val OnSurfaceMuted = Color(0xFF9AA0A6)
    val Accent = Color(0xFF8AB4F8)
    val AccentMuted = Color(0xFF1F3A5F)
    val Border = Color(0x3DFFFFFF)
    val Scrim = Color(0xB3000000)
    val Focus = Color(0xFFFFFFFF)
}

private val NavidromeDarkColorScheme = darkColorScheme(
    primary = NavidromeColors.Accent,
    onPrimary = NavidromeColors.Ink,
    primaryContainer = NavidromeColors.AccentMuted,
    onPrimaryContainer = Color(0xFFD7E6FF),
    secondary = Color(0xFFC4C7CC),
    onSecondary = NavidromeColors.Ink,
    background = NavidromeColors.Ink,
    onBackground = NavidromeColors.OnSurface,
    surface = NavidromeColors.Surface,
    onSurface = NavidromeColors.OnSurface,
    surfaceVariant = NavidromeColors.SurfaceVariant,
    onSurfaceVariant = NavidromeColors.OnSurfaceMuted,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

/** Roles that Material's scheme does not model but the 10-foot UI needs. */
@Immutable
data class AppColors(
    val cardScrim: Color = Color(0x00000000),
    val focusedBorder: Color = NavidromeColors.Focus,
    val secondaryText: Color = NavidromeColors.OnSurfaceMuted,
    val divider: Color = Color(0x1FFFFFFF),
    val accentGradientStart: Color = Color(0xFF8AB4F8),
    val accentGradientEnd: Color = Color(0xFF5B8DEF),
    val overlay: Color = Color(0xCC000000),
)

val LocalAppColors = staticCompositionLocalOf { AppColors() }

/**
 * TV type scale. Sizes follow the guidance that TV typography runs noticeably
 * larger than mobile, uses Roboto, and avoids thin weights. Every style is also
 * scaled by the system font scale, so a 4K panel with a 2x density keeps the same
 * physical proportions as 1080p.
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
        fontWeight = FontWeight.Medium,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.4).sp,
    )
    val title = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    )
    val section = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    )
    val cardTitle = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    )
    val cardSubtitle = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
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
        letterSpacing = 0.2.sp,
    )
    val overline = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Medium,
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
    val rowSpacing = 24.dp
    val topBarHeight = 72.dp
}

object Dimens {
    val cardCorner = 12.dp
    val panelCorner = 16.dp
    val pillCorner = 100.dp
    val focusBorderWidth = 2.dp
    const val FOCUS_SCALE = 1.10f
}

@Composable
fun NavidromeTvTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppColors provides AppColors()) {
        MaterialTheme(colorScheme = NavidromeDarkColorScheme) {
            content()
        }
    }
}
