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
 * Colour roles. The neutral scale is intentionally near-black so album artwork is
 * the only strong colour on screen; contextual gradients are derived per album at
 * runtime (see rememberArtworkColors).
 */
object NavidromeColors {
    val Ink = Color(0xFF08080A)
    val Surface = Color(0xFF141418)
    val SurfaceElevated = Color(0xFF1D1D24)
    val SurfaceVariant = Color(0xFF2A2A34)
    val OnSurface = Color(0xFFF6F6F8)
    val OnSurfaceMuted = Color(0xFFA0A0AE)
    val Accent = Color(0xFFF5334F)
    val AccentMuted = Color(0xFF8E2333)
    val Border = Color(0x1FFFFFFF)
    val Scrim = Color(0xB3000000)
}

private val NavidromeDarkColorScheme = darkColorScheme(
    primary = NavidromeColors.Accent,
    onPrimary = Color.White,
    primaryContainer = NavidromeColors.AccentMuted,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFD8D8E0),
    onSecondary = Color(0xFF141418),
    background = NavidromeColors.Ink,
    onBackground = NavidromeColors.OnSurface,
    surface = NavidromeColors.Surface,
    onSurface = NavidromeColors.OnSurface,
    surfaceVariant = NavidromeColors.SurfaceVariant,
    onSurfaceVariant = NavidromeColors.OnSurfaceMuted,
    error = Color(0xFFFF6B6B),
    onError = Color.Black,
)

/** Roles that Material's scheme does not model but the 10-foot UI needs. */
@Immutable
data class AppColors(
    val cardScrim: Color = Color(0x00000000),
    val focusedBorder: Color = Color(0xE6FFFFFF),
    val secondaryText: Color = NavidromeColors.OnSurfaceMuted,
    val divider: Color = Color(0x14FFFFFF),
    val accentGradientStart: Color = Color(0xFFF5334F),
    val accentGradientEnd: Color = Color(0xFF8E2DE2),
    val overlay: Color = Color(0xCC000000),
)

val LocalAppColors = staticCompositionLocalOf { AppColors() }

/**
 * Type scale. Sized for a 10-foot viewing distance on 1080p and scaled by the
 * system font scale, so it stays readable on 4K panels.
 */
object AppType {
    private val Family = FontFamily.SansSerif

    val hero = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Bold,
        fontSize = 54.sp,
        lineHeight = 58.sp,
        letterSpacing = (-1.2).sp,
    )
    val display = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.8).sp,
    )
    val title = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp,
    )
    val section = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
    )
    val cardTitle = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
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
        fontSize = 17.sp,
        lineHeight = 24.sp,
    )
    val bodyMuted = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    )
    val button = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.2.sp,
    )
    val overline = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 1.4.sp,
    )
    val metadata = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.2.sp,
    )
}

/** Consistent spacing grid (8dp base, 4dp half-step). */
object Space {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val screenHorizontal = 56.dp
    val screenTop = 40.dp
    val railWidth = 300.dp
    val contentBottomWithPlayer = 148.dp
    val cardWidth = 200.dp
    val cardArtwork = 200.dp
    val rowSpacing = 24.dp
    val topBarHeight = 64.dp
}

object Dimens {
    val cardCorner = 16.dp
    val panelCorner = 24.dp
    val pillCorner = 28.dp
    const val FOCUS_SCALE = 1.06f
}

@Composable
fun NavidromeTvTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppColors provides AppColors()) {
        MaterialTheme(colorScheme = NavidromeDarkColorScheme) {
            content()
        }
    }
}
