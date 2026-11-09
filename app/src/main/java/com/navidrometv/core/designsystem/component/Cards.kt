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

/** Shadow colour used for the focused lift, so covers separate from each other. */
private val LiftAmbient = Color.Black.copy(alpha = 0.42f)
private val LiftSpot = Color.Black.copy(alpha = 0.55f)

/**
 * Caption under a shelf tile: title on the first line, a quieter secondary line
 * under it, both left-aligned to the artwork's leading edge the way Apple Music
 * labels its shelves on Apple TV.
 */
@Composable
private fun TileCaption(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.Start,
) {
    val textAlign = if (alignment == Alignment.CenterHorizontally) TextAlign.Center else TextAlign.Start
    Column(modifier = modifier.padding(top = Space.xs)) {
        Text(
            text = title,
            style = AppType.cardTitle,
            color = NavidromeColors.OnSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = AppType.cardSubtitle,
                color = NavidromeColors.OnSurfaceMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Square artwork tile: 1:1 cover, flat at rest, lifting only when focused.
 * Apple Music on Apple TV uses square covers rather than posters, so nothing is
 * ever cropped and shelves stay on one rhythm.
 */
@Composable
private fun SquareArtTile(
    coverArtId: String?,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(Dimens.cardCorner),
    overlay: @Composable (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val lift by animateDpAsState(
        targetValue = if (focused) Dimens.focusElevation else 0.dp,
        label = "tile-lift",
    )
    TvClickable(
        onClick = onClick,
        onFocusChanged = { focused = it },
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(lift, shape, ambientColor = LiftAmbient, spotColor = LiftSpot),
        shape = shape,
    ) {
        Artwork(
            coverArtId = coverArtId,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            shape = shape,
        )
        overlay?.invoke()
    }
}

/**
 * Shelf tile built from raw fields, so a "Top Picks" entry can be anything that
 * has a cover and a name (album, mix, playlist) without inventing a card type.
 */
@Composable
fun PosterCard(
    coverArtId: String?,
    contentDescription: String,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    width: Dp = Space.cardPoster,
) {
    Column(modifier = modifier.width(width)) {
        SquareArtTile(
            coverArtId = coverArtId,
            contentDescription = contentDescription,
            onClick = onClick,
        )
        TileCaption(title = title, subtitle = subtitle)
    }
}

/** Album tile: square cover with the album name and artist beneath it. */
@Composable
fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = Space.cardArtwork,
    subtitleOverride: String? = null,
) {
    Column(modifier = modifier.width(width)) {
        SquareArtTile(
            coverArtId = album.coverArtId,
            contentDescription = "${album.name} cover",
            onClick = onClick,
        )
        TileCaption(
            title = album.name,
            subtitle = subtitleOverride ?: album.artistName.orEmpty(),
        )
    }
}

/** Artist tile. Portraits stay circular, as Apple Music shows artists. */
@Composable
fun ArtistCard(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = Space.cardArtwork,
) {
    val shape = RoundedCornerShape(percent = 50)
    Column(modifier = modifier.width(width), horizontalAlignment = Alignment.CenterHorizontally) {
        SquareArtTile(
            coverArtId = artist.coverArtId,
            contentDescription = "${artist.name} portrait",
            onClick = onClick,
            shape = shape,
        )
        TileCaption(
            title = artist.name,
            subtitle = artist.albumCount?.let { Formatters.plural(it, "album") },
            alignment = Alignment.CenterHorizontally,
        )
    }
}

/** Playlist tile. The name sits under the cover, not on top of it. */
@Composable
fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = Space.cardArtwork,
) {
    Column(modifier = modifier.width(width)) {
        SquareArtTile(
            coverArtId = playlist.coverArtId,
            contentDescription = "${playlist.name} cover",
            onClick = onClick,
        )
        TileCaption(
            title = playlist.name,
            subtitle = Formatters.plural(playlist.trackCount, "song"),
        )
    }
}

/**
 * Genre tile: a saturated colour wash with the genre name in dark ink, which is
 * the one place Apple Music lets a non-artwork surface carry colour.
 */
@Composable
fun GenreCard(
    genre: Genre,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 260.dp,
    colorIndex: Int = 0,
) {
    val gradients = listOf(
        listOf(Color(0xFFFFC6A8), Color(0xFFF98A6B)),
        listOf(Color(0xFFB7D9FF), Color(0xFF6FA8F5)),
        listOf(Color(0xFFC8F0D2), Color(0xFF7FD69A)),
        listOf(Color(0xFFE9CDF7), Color(0xFFBE93E8)),
        listOf(Color(0xFFFFE7A8), Color(0xFFF5C74E)),
        listOf(Color(0xFFFFC2CE), Color(0xFFF4718C)),
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
                .height(104.dp)
                .background(Brush.linearGradient(colors), shape)
                .padding(Space.md),
            contentAlignment = Alignment.BottomStart,
        ) {
            Column {
                Text(
                    text = genre.name,
                    style = AppType.title,
                    color = NavidromeColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = Formatters.plural(genre.albumCount, "album"),
                    style = AppType.cardSubtitle,
                    color = NavidromeColors.Ink.copy(alpha = 0.66f),
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
                .background(Color.White.copy(alpha = 0.08f)),
        )
        Box(
            Modifier
                .padding(top = 10.dp)
                .fillMaxWidth(0.7f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.08f)),
        )
    }
}
