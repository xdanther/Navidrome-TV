package com.navidrometv.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.navidrometv.core.common.Formatters
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.Dimens
import com.navidrometv.core.designsystem.theme.NavidromeColors
import com.navidrometv.core.designsystem.theme.Space
import com.navidrometv.core.model.Album
import com.navidrometv.core.model.Artist
import com.navidrometv.core.model.Genre
import com.navidrometv.core.model.Playlist

/**
 * Album card: square artwork with a two-line caption, the standard TV media card.
 * Focus scales the card and draws a white outline; the artwork itself is never
 * washed out or recoloured, so covers keep their true colours.
 */
@Composable
fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = Space.cardArtwork,
    subtitleOverride: String? = null,
) {
    Column(modifier = modifier.width(width)) {
        TvClickable(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.cardCorner),
        ) {
            Artwork(
                coverArtId = album.coverArtId,
                contentDescription = "${album.name} cover",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(Dimens.cardCorner),
            )
            // A focused cover lifts a little in brightness: on very dark artwork the
            // outline alone would be the only cue.
            val lift by animateFloatAsState(
                targetValue = if (it) 1f else 0f,
                animationSpec = tween(durationMillis = 150),
                label = "album-lift",
            )
            if (lift > 0.01f) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(Color.White.copy(alpha = 0.08f * lift)),
                )
            }
        }
        Text(
            text = album.name,
            style = AppType.cardTitle,
            color = NavidromeColors.OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            text = subtitleOverride ?: album.artistName.orEmpty(),
            style = AppType.cardSubtitle,
            color = NavidromeColors.OnSurfaceMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Artist card with the circular portrait used throughout the design. */
@Composable
fun ArtistCard(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = Space.cardArtwork,
) {
    Column(modifier = modifier.width(width), horizontalAlignment = Alignment.CenterHorizontally) {
        TvClickable(onClick = onClick, shape = RoundedCornerShape(percent = 50)) {
            Artwork(
                coverArtId = artist.coverArtId,
                contentDescription = "${artist.name} portrait",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(percent = 50),
            )
        }
        Text(
            text = artist.name,
            style = AppType.cardTitle,
            color = NavidromeColors.OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp),
        )
        if (artist.albumCount != null) {
            Text(
                text = Formatters.plural(artist.albumCount, "album"),
                style = AppType.cardSubtitle,
                color = NavidromeColors.OnSurfaceMuted,
                maxLines = 1,
            )
        }
    }
}

/** Square playlist tile: cover art with a neutral scrim and the playlist name. */
@Composable
fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = Space.cardArtwork,
) {
    Column(modifier = modifier.width(width)) {
        TvClickable(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(Dimens.cardCorner))
                    .background(Brush.linearGradient(listOf(NavidromeColors.SurfaceVariant, NavidromeColors.Surface))),
            ) {
                Artwork(
                    coverArtId = playlist.coverArtId,
                    contentDescription = "${playlist.name} cover",
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(Dimens.cardCorner),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))),
                )
                Text(
                    text = playlist.name,
                    style = AppType.cardTitle,
                    color = NavidromeColors.OnSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(Space.md),
                )
            }
        }
        Text(
            text = Formatters.plural(playlist.trackCount, "song"),
            style = AppType.cardSubtitle,
            color = NavidromeColors.OnSurfaceMuted,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

/**
 * Genre tile. The tiles use quiet, desaturated gradient surfaces instead of a
 * rainbow so the palette stays neutral; the label and album count carry the
 * information.
 */
@Composable
fun GenreCard(
    genre: Genre,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 300.dp,
    colorIndex: Int = 0,
) {
    val gradients = listOf(
        listOf(Color(0xFF2C3440), Color(0xFF171A1E)),
        listOf(Color(0xFF2A3536), Color(0xFF161B1B)),
        listOf(Color(0xFF34302A), Color(0xFF1B1916)),
        listOf(Color(0xFF2B2E38), Color(0xFF16181D)),
        listOf(Color(0xFF2C332E), Color(0xFF171B18)),
    )
    val colors = gradients[colorIndex % gradients.size]
    TvClickable(onClick = onClick, modifier = modifier.width(width)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .background(Brush.linearGradient(colors), RoundedCornerShape(Dimens.cardCorner))
                .padding(Space.lg),
            contentAlignment = Alignment.BottomStart,
        ) {
            Column {
                Text(
                    text = genre.name,
                    style = AppType.section,
                    color = NavidromeColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = Formatters.plural(genre.albumCount, "album"),
                    style = AppType.cardSubtitle,
                    color = NavidromeColors.OnSurfaceMuted,
                )
            }
        }
    }
}

/** Static placeholder tile that keeps carousel height stable while loading. */
@Composable
fun SkeletonTile(modifier: Modifier = Modifier, width: Dp = Space.cardArtwork) {
    Column(modifier.width(width)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(Dimens.cardCorner))
                .background(NavidromeColors.SurfaceElevated),
        )
        Box(
            Modifier
                .padding(top = 10.dp)
                .fillMaxWidth(0.7f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(NavidromeColors.SurfaceElevated),
        )
    }
}
