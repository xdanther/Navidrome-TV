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
import androidx.compose.ui.draw.shadow
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
 * Album card: square artwork with a two-line caption below it, the standard media
 * card for a light stage. Artwork carries a soft drop shadow so it lifts off the
 * ambient backdrop; focus scales the card and draws the ink outline.
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
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    Dimens.cardElevation,
                    RoundedCornerShape(Dimens.cardCorner),
                    ambientColor = Color.Black.copy(alpha = 0.10f),
                    spotColor = Color.Black.copy(alpha = 0.14f),
                ),
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
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x99000000)))),
                )
                Text(
                    text = playlist.name,
                    style = AppType.cardTitle,
                    color = Color.White,
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
 * Genre tile. Tiles use soft, fully saturated pastel washes (the one place the
 * chrome is allowed colour) so shelves of genres read like album-cover walls on a
 * light background.
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
        listOf(Color(0xFFFFE3CF), Color(0xFFF8C7A8)),
        listOf(Color(0xFFDCEAFB), Color(0xFFBBD7F7)),
        listOf(Color(0xFFE1F2E4), Color(0xFFC3E3CB)),
        listOf(Color(0xFFF1E3F6), Color(0xFFDCC8EB)),
        listOf(Color(0xFFFAEDE4), Color(0xFFEBD3C4)),
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
                    color = NavidromeColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = Formatters.plural(genre.albumCount, "album"),
                    style = AppType.cardSubtitle,
                    color = NavidromeColors.Ink.copy(alpha = 0.62f),
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
                .background(Color.White.copy(alpha = 0.45f)),
        )
        Box(
            Modifier
                .padding(top = 10.dp)
                .fillMaxWidth(0.7f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.45f)),
        )
    }
}
