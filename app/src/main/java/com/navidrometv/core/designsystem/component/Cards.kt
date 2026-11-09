package com.navidrometv.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
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

/** Solid footer band carried inside the editorial card, as on the reference shelf. */
private val FooterBand = Color(0xE61A1917)

/** Shadow colour used for the focused lift, so covers separate from each other. */
private val LiftAmbient = Color.Black.copy(alpha = 0.18f)
private val LiftSpot = Color.Black.copy(alpha = 0.28f)

/**
 * Editorial card: a 2:3 poster with a solid footer band carrying the title inside
 * the card, and an optional eyebrow label above it. This is the tall "made for
 * you" tile from the reference shelf, so the caption never hangs outside the art.
 */
@Composable
fun PosterCard(
    coverArtId: String?,
    contentDescription: String,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    eyebrow: String? = null,
    width: Dp = Space.cardPoster,
) {
    var focused by remember { mutableStateOf(false) }
    val lift by animateDpAsState(
        targetValue = if (focused) Dimens.focusElevation else 0.dp,
        label = "poster-lift",
    )
    val shape = RoundedCornerShape(Dimens.cardCorner)
    Column(modifier = modifier.width(width)) {
        if (!eyebrow.isNullOrBlank()) {
            Text(
                text = eyebrow,
                style = AppType.cardSubtitle,
                color = NavidromeColors.OnSurfaceMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = Space.xs, start = 2.dp),
            )
        }
        TvClickable(
            onClick = onClick,
            onFocusChanged = { focused = it },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .shadow(lift, shape, ambientColor = LiftAmbient, spotColor = LiftSpot),
            shape = shape,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Artwork(
                    coverArtId = coverArtId,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    shape = shape,
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(FooterBand)
                        .padding(horizontal = Space.md, vertical = 14.dp),
                ) {
                    Text(
                        text = title,
                        style = AppType.cardTitle,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = AppType.cardSubtitle,
                            color = Color.White.copy(alpha = 0.74f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Album card: square artwork with a centred two-line caption below it. There is no
 * shadow at rest — on the reference the grid is flat and only the focused tile
 * lifts — so focus both scales the card and gives it a floating shadow.
 */
@Composable
fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = Space.cardArtwork,
    subtitleOverride: String? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val lift by animateDpAsState(
        targetValue = if (focused) Dimens.focusElevation else 0.dp,
        label = "album-lift",
    )
    val shape = RoundedCornerShape(Dimens.cardCorner)
    Column(modifier = modifier.width(width), horizontalAlignment = Alignment.CenterHorizontally) {
        TvClickable(
            onClick = onClick,
            onFocusChanged = { focused = it },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(lift, shape, ambientColor = LiftAmbient, spotColor = LiftSpot),
            shape = shape,
        ) {
            Artwork(
                coverArtId = album.coverArtId,
                contentDescription = "${album.name} cover",
                modifier = Modifier.fillMaxSize(),
                shape = shape,
            )
        }
        Text(
            text = album.name,
            style = AppType.cardTitle,
            color = NavidromeColors.OnSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        Text(
            text = subtitleOverride ?: album.artistName.orEmpty(),
            style = AppType.cardSubtitle,
            color = NavidromeColors.OnSurfaceMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
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
    var focused by remember { mutableStateOf(false) }
    val lift by animateDpAsState(
        targetValue = if (focused) Dimens.focusElevation else 0.dp,
        label = "artist-lift",
    )
    val shape = RoundedCornerShape(percent = 50)
    Column(modifier = modifier.width(width), horizontalAlignment = Alignment.CenterHorizontally) {
        TvClickable(
            onClick = onClick,
            onFocusChanged = { focused = it },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(lift, shape, ambientColor = LiftAmbient, spotColor = LiftSpot),
            shape = shape,
        ) {
            Artwork(
                coverArtId = artist.coverArtId,
                contentDescription = "${artist.name} portrait",
                modifier = Modifier.fillMaxSize(),
                shape = shape,
            )
        }
        Text(
            text = artist.name,
            style = AppType.cardTitle,
            color = NavidromeColors.OnSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        if (artist.albumCount != null) {
            Text(
                text = Formatters.plural(artist.albumCount, "album"),
                style = AppType.cardSubtitle,
                color = NavidromeColors.OnSurfaceMuted,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
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
    var focused by remember { mutableStateOf(false) }
    val lift by animateDpAsState(
        targetValue = if (focused) Dimens.focusElevation else 0.dp,
        label = "playlist-lift",
    )
    val shape = RoundedCornerShape(Dimens.cardCorner)
    Column(modifier = modifier.width(width), horizontalAlignment = Alignment.CenterHorizontally) {
        TvClickable(
            onClick = onClick,
            onFocusChanged = { focused = it },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(lift, shape, ambientColor = LiftAmbient, spotColor = LiftSpot),
            shape = shape,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(Brush.linearGradient(listOf(NavidromeColors.SurfaceVariant, NavidromeColors.Surface))),
            ) {
                Artwork(
                    coverArtId = playlist.coverArtId,
                    contentDescription = "${playlist.name} cover",
                    modifier = Modifier.fillMaxSize(),
                    shape = shape,
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
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
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
    var focused by remember { mutableStateOf(false) }
    val lift by animateDpAsState(
        targetValue = if (focused) Dimens.focusElevation else 0.dp,
        label = "genre-lift",
    )
    val colors = gradients[colorIndex % gradients.size]
    val shape = RoundedCornerShape(Dimens.cardCorner)
    TvClickable(
        onClick = onClick,
        onFocusChanged = { focused = it },
        modifier = modifier
            .width(width)
            .shadow(lift, shape, ambientColor = LiftAmbient, spotColor = LiftSpot),
        shape = shape,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .background(Brush.linearGradient(colors), shape)
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
