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
import androidx.tv.material3.darkColorScheme

/**
 * Colour roles for the dark, artwork-led look Apple Music uses on Apple TV.
 *
 * The canvas is black and the chrome is quiet: white type, a 60% white secondary
 * label, hairline white borders and translucent dark glass for bars. Colour comes
 * from the artwork; the one system tint (Apple Music red) only marks playback
 * state and favourites.
 */
object NavidromeColors {
    /** Screen canvas. Apple Music on tvOS sits on black. */
    val Background = Color(0xFF000000)

    /** Cards, sheets and panels. */
    val Surface = Color(0xFF1C1C1E)
    val SurfaceElevated = Color(0xFF2C2C2E)

    /** Quiet fill on the dark canvas (icon discs, list rows, skeleton tiles). */
    val SurfaceVariant = Color(0x1AFFFFFF)

    /** Near-black used for text and glyphs on top of a white focused surface. */
    val Ink = Color(0xFF0B0B0D)

    val OnSurface = Color(0xFFFFFFFF)
    val OnSurfaceMuted = Color(0xFFA1A1A8)
    val Accent = Color(0xFFFA233B)
    val AccentMuted = Color(0x33FA233B)
    val Border = Color(0x24FFFFFF)
    val Scrim = Color(0xCC000000)

    /** White fill for the focused element. */
    val Focus = Color(0xFFFFFFFF)

    /**
     * Outer edge of the in-bounds focus ring. On a black canvas and over artwork a
     * white band is enough on its own, so the edge stays a soft shadow.
     */
    val FocusRing = Color(0xE6000000)

    /** Translucent dark chrome: navigation bar, mini player, keyboard. */
    val Glass = Color(0x99141416)
    val GlassSubtle = Color(0x1FFFFFFF)

    // Backdrop stops. Deliberately near-black: the artwork should be the only
    // bright thing on screen, so these read as depth, not as colour.
    val AmbientSand = Color(0xFF17171B)
    val AmbientCream = Color(0xFF121215)
    val AmbientSage = Color(0xFF0E0E11)
    val AmbientSky = Color(0xFF08080A)
    val AmbientMist = Color(0xFF000000)
}

private val NavidromeDarkColorScheme = darkColorScheme(
    primary = NavidromeColors.Accent,
    onPrimary = Color.White,
    primaryContainer = NavidromeColors.AccentMuted,
    onPrimaryContainer = Color.White,
    secondary = Color.White,
    onSecondary = NavidromeColors.Ink,
    background = NavidromeColors.Background,
    onBackground = NavidromeColors.OnSurface,
    surface = NavidromeColors.Surface,
    onSurface = NavidromeColors.OnSurface,
    surfaceVariant = NavidromeColors.SurfaceVariant,
    onSurfaceVariant = NavidromeColors.OnSurfaceMuted,
    error = Color(0xFFFF453A),
    onError = Color.White,
)

/** Roles that Material's scheme does not model but the 10-foot UI needs. */
@Immutable
data class AppColors(
    val cardScrim: Color = Color(0x00000000),
    val focusedBorder: Color = NavidromeColors.FocusRing,
    val secondaryText: Color = NavidromeColors.OnSurfaceMuted,
    val divider: Color = Color(0x1AFFFFFF),
    val accentGradientStart: Color = NavidromeColors.Accent,
    val accentGradientEnd: Color = Color(0xFFFF5A6E),
    val overlay: Color = Color(0xCC000000),
)

val LocalAppColors = staticCompositionLocalOf { AppColors() }

/**
 * TV type scale.
 *
 * Sizes are Apple's tvOS text styles halved, because a television reports about
 * 960x540dp where Apple's layout canvas is 1920x1080pt: Title 2 (57pt) becomes
 * 28sp, Title 3 (48pt) 24sp, Body (29pt) roughly 15sp, Caption 1 (25pt) 13sp.
 * Shelf captions stay a step larger than Caption 1 so they read from across a room.
 */
object AppType {
    private val Family = FontFamily.SansSerif

    val hero = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.6).sp,
    )
    val display = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.4).sp,
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
        fontSize = 16.sp,
        lineHeight = 21.sp,
    )
    val cardSubtitle = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 19.sp,
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
        fontSize = 16.sp,
        lineHeight = 23.sp,
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
 * Spacing grid derived from Apple's tvOS safe area and layout grid, halved for
 * this canvas: an 80pt side margin becomes 40dp, 60pt of top/bottom margin becomes
 * 30dp, and the five-column grid (320pt items, 40pt gaps) becomes 160dp cards with
 * 20dp gaps, which puts exactly five covers across a 960dp screen.
 */
object Space {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val screenHorizontal = 40.dp
    val screenTop = 30.dp
    val railWidth = 280.dp
    val contentBottomWithPlayer = 112.dp
    val cardWidth = 160.dp
    val cardArtwork = 160.dp
    val cardPoster = 160.dp
    val rowSpacing = 20.dp
    val topBarHeight = 60.dp
}

object Dimens {
    val cardCorner = 6.dp
    val panelCorner = 14.dp
    val pillCorner = 100.dp
    val focusBorderWidth = 6.dp
    val cardElevation = 10.dp
    val focusElevation = 20.dp
    val floatingElevation = 16.dp
    const val FOCUS_SCALE = 1.08f
}

/**
 * The backdrop: a near-black vertical gradient with one soft glow at the top, so
 * shelves sit on depth rather than on a flat black rectangle while artwork stays
 * the brightest thing on screen. Drawn once behind the whole app, edge to edge.
 */
@Composable
fun AmbientBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    NavidromeColors.AmbientSand,
                    NavidromeColors.AmbientCream,
                    NavidromeColors.AmbientSage,
                    NavidromeColors.AmbientSky,
                    NavidromeColors.AmbientMist,
                ),
                startY = 0f,
                endY = size.height,
            ),
        )
        // Soft top glow, the same trick tvOS uses behind the navigation bar.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x14FFFFFF),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.5f, size.height * -0.05f),
                radius = size.width * 0.8f,
            ),
        )
    }
}

@Composable
fun NavidromeTvTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppColors provides AppColors()) {
        MaterialTheme(colorScheme = NavidromeDarkColorScheme) {
            content()
        }
    }
}
