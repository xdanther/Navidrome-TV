package com.navidrometv.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.navidrometv.core.designsystem.component.AlbumCard
import com.navidrometv.core.designsystem.component.ArtistCard
import com.navidrometv.core.designsystem.component.GenreCard
import com.navidrometv.core.designsystem.component.PlaylistCard
import com.navidrometv.core.designsystem.component.SectionHeader
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.LocalAppColors
import com.navidrometv.core.designsystem.theme.Space
import com.navidrometv.core.model.Album
import com.navidrometv.core.model.Artist
import com.navidrometv.core.model.Genre
import com.navidrometv.core.model.Playlist

/**
 * Square album carousel. Every card shares one artwork size, so rows read as a
 * steady grid of covers rather than a mix of banners, and 5–7 covers fit on a
 * 1080p screen at the default density.
 */
@Composable
fun AlbumRow(
    title: String,
    albums: List<Album>,
    onOpenAlbum: (String) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    bleed: Boolean = false,
) {
    if (albums.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = title, subtitle = subtitle, modifier = rowHeaderModifier(bleed))
        RowCarousel(bleed = bleed) {
            items(albums, key = { it.id }) { album ->
                AlbumCard(album = album, onClick = { onOpenAlbum(album.id) })
            }
        }
    }
}

@Composable
fun ArtistRow(
    title: String,
    artists: List<Artist>,
    onOpenArtist: (String) -> Unit,
    modifier: Modifier = Modifier,
    bleed: Boolean = false,
) {
    if (artists.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = title, modifier = rowHeaderModifier(bleed))
        RowCarousel(bleed = bleed) {
            items(artists, key = { it.id }) { artist ->
                ArtistCard(artist = artist, onClick = { onOpenArtist(artist.id) })
            }
        }
    }
}

@Composable
fun PlaylistRow(
    title: String,
    playlists: List<Playlist>,
    onOpenPlaylist: (String) -> Unit,
    modifier: Modifier = Modifier,
    bleed: Boolean = false,
) {
    if (playlists.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = title, modifier = rowHeaderModifier(bleed))
        RowCarousel(bleed = bleed) {
            items(playlists, key = { it.id }) { playlist ->
                PlaylistCard(playlist = playlist, onClick = { onOpenPlaylist(playlist.id) })
            }
        }
    }
}

@Composable
fun GenreRow(
    title: String,
    genres: List<Genre>,
    onOpenGenre: (String) -> Unit,
    modifier: Modifier = Modifier,
    bleed: Boolean = false,
) {
    if (genres.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = title, modifier = rowHeaderModifier(bleed))
        RowCarousel(bleed = bleed) {
            items(genres, key = { it.name }) { genre ->
                GenreCard(
                    genre = genre,
                    onClick = { onOpenGenre(genre.name) },
                    colorIndex = genres.indexOf(genre),
                )
            }
        }
    }
}

/** Headers stay on the overscan-safe margin; the carousel may bleed past it. */
private fun rowHeaderModifier(bleed: Boolean): Modifier =
    if (bleed) {
        Modifier.padding(start = Space.screenHorizontal, bottom = Space.sm)
    } else {
        Modifier.padding(bottom = Space.sm)
    }

/**
 * Shared carousel container: uniform 24dp gaps and a little vertical padding so a
 * focused card's ring has room. When [bleed] is set the row spans the full screen
 * width and keeps the side margins as content padding instead, so covers run off
 * the edge like a shelf on a TV instead of stopping inside a narrow column.
 */
@Composable
private fun RowCarousel(
    bleed: Boolean = false,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    val side = if (bleed) Space.screenHorizontal else 6.dp
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.lg),
        contentPadding = PaddingValues(start = side, end = side, top = 18.dp, bottom = 18.dp),
        content = content,
    )
}

/** Compact "see all" style row header with a right-aligned hint. */
@Composable
fun RowHint(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text = text, style = AppType.metadata, color = LocalAppColors.current.secondaryText)
    }
}
