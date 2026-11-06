package com.navidrometv.core.designsystem.component

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
import com.navidrometv.core.designsystem.theme.LocalAppColors
import com.navidrometv.core.designsystem.theme.NavidromeColors
import com.navidrometv.core.designsystem.theme.Space
import com.navidrometv.core.model.Album
import com.navidrometv.core.model.Artist
import com.navidrometv.core.model.Genre
import com.navidrometv.core.model.Playlist

/**
 * Album card: artwork plus two lines of metadata. The card never changes its
 * measured size on focus; only the graphics layer scales, so carousels never
 * reflow while the user moves through them.
 */
@Composable
fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = Space.cardWidth,
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
        }
        Text(
            text = album.name,
            style = AppType.cardTitle,
            color = NavidromeColors.OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Space.xs),
        )
        Text(
            text = subtitleOverride ?: album.artistName.orEmpty(),
            style = AppType.cardSubtitle,
            color = LocalAppColors.current.secondaryText,
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
    width: Dp = Space.cardWidth,
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
            modifier = Modifier.padding(top = Space.xs),
        )
        if (artist.albumCount != null) {
            Text(
                text = Formatters.plural(artist.albumCount, "album"),
                style = AppType.cardSubtitle,
                color = LocalAppColors.current.secondaryText,
                maxLines = 1,
            )
        }
    }
}

/** Wide playlist card with a gradient scrim, used on Home and Library. */
@Composable
fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 268.dp,
) {
    Column(modifier = modifier.width(width)) {
        TvClickable(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(Dimens.cardCorner))
                    .background(Brush.linearGradient(listOf(Color(0xFF3A2340), Color(0xFF1A1226)))),
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
                    style = AppType.section,
                    color = Color.White,
                    maxLines = 2,
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
            color = LocalAppColors.current.secondaryText,
            modifier = Modifier.padding(top = Space.xs),
        )
    }
}

/** Wide, colourful genre tile. */
@Composable
fun GenreCard(
    genre: Genre,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 300.dp,
    colorIndex: Int = 0,
) {
    val gradients = listOf(
        listOf(Color(0xFFE23B4E), Color(0xFF7A1230)),
        listOf(Color(0xFF2E6BE6), Color(0xFF122B6B)),
        listOf(Color(0xFF13A37C), Color(0xFF0A3F31)),
        listOf(Color(0xFFB03BE2), Color(0xFF3C1259)),
        listOf(Color(0xFFE28A1E), Color(0xFF5C3207)),
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
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = Formatters.plural(genre.albumCount, "album"),
                    style = AppType.cardSubtitle,
                    color = Color.White.copy(alpha = 0.75f),
                )
            }
        }
    }
}

/** Compact square tile used for dense "recently played artists" rows. */
@Composable
fun SquareArtistCard(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 150.dp,
) {
    Column(modifier = modifier.width(width)) {
        TvClickable(onClick = onClick) {
            Artwork(
                coverArtId = artist.coverArtId,
                contentDescription = artist.name,
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
            modifier = Modifier.padding(top = Space.xs),
        )
    }
}

/** Static placeholder tile that keeps carousel height stable while loading. */
@Composable
fun SkeletonTile(modifier: Modifier = Modifier, width: Dp = Space.cardWidth) {
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
                .padding(top = Space.xs)
                .fillMaxWidth(0.7f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(NavidromeColors.SurfaceElevated),
        )
    }
}
