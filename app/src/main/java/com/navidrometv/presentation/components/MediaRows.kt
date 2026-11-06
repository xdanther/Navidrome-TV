package com.navidrometv.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.navidrometv.core.designsystem.component.AlbumCard
import com.navidrometv.core.designsystem.component.ArtistCard
import com.navidrometv.core.designsystem.component.Artwork
import com.navidrometv.core.designsystem.component.GenreCard
import com.navidrometv.core.designsystem.component.PlaylistCard
import com.navidrometv.core.designsystem.component.SectionHeader
import com.navidrometv.core.designsystem.component.TvClickable
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
 * Large featured carousel for the Home screen. Cards are wide so artwork reads
 * from across a room, and horizontal D-pad movement pages through them without
 * changing the vertical layout.
 */
@Composable
fun FeaturedRow(
    albums: List<Album>,
    onOpenAlbum: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "FEATURED",
) {
    if (albums.isEmpty()) return
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.lg),
    ) {
        items(albums, key = { it.id }) { album ->
            TvClickable(
                onClick = { onOpenAlbum(album.id) },
                modifier = Modifier
                    .width(620.dp)
                    .height(340.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Box(Modifier.fillMaxSize()) {
                    Artwork(
                        coverArtId = album.coverArtId,
                        contentDescription = "${album.name} cover",
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(24.dp),
                        requestSizePx = 1200,
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xE6000000), Color(0x33000000)),
                                ),
                            ),
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(Space.xl)
                            .fillMaxWidth(0.75f),
                    ) {
                        Text(
                            text = label,
                            style = AppType.overline,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                        Text(
                            text = album.name,
                            style = AppType.display,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = Space.xs),
                        )
                        Text(
                            text = listOfNotNull(album.artistName, album.year?.toString()).joinToString(" · "),
                            style = AppType.bodyMuted,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumRow(
    title: String,
    albums: List<Album>,
    onOpenAlbum: (String) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    if (albums.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = title, subtitle = subtitle, modifier = Modifier.padding(bottom = Space.md))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
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
) {
    if (artists.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = title, modifier = Modifier.padding(bottom = Space.md))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
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
) {
    if (playlists.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = title, modifier = Modifier.padding(bottom = Space.md))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
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
) {
    if (genres.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = title, modifier = Modifier.padding(bottom = Space.md))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
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

/** Compact "see all" style row header with a right-aligned hint. */
@Composable
fun RowHint(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text = text, style = AppType.metadata, color = LocalAppColors.current.secondaryText)
    }
}
