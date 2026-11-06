package com.navidrometv.presentation.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.navidrometv.core.common.Formatters
import com.navidrometv.core.designsystem.component.AlbumCard
import com.navidrometv.core.designsystem.component.ArtistCard
import com.navidrometv.core.designsystem.component.EmptyState
import com.navidrometv.core.designsystem.component.ErrorState
import com.navidrometv.core.designsystem.component.FilterChip
import com.navidrometv.core.designsystem.component.FocusableRow
import com.navidrometv.core.designsystem.component.GenreCard
import com.navidrometv.core.designsystem.component.LoadingState
import com.navidrometv.core.designsystem.component.TrackRow
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.LocalAppColors
import com.navidrometv.core.designsystem.theme.NavidromeColors
import com.navidrometv.core.designsystem.theme.Space
import com.navidrometv.core.model.Playlist

@Composable
fun LibraryScreen(
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenGenre: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Space.screenHorizontal, vertical = Space.md),
        horizontalArrangement = Arrangement.spacedBy(Space.xl),
    ) {
        Column(Modifier.width(230.dp).fillMaxHeight()) {
            Text(text = "Library", style = AppType.title, color = NavidromeColors.OnSurface)
            Column(
                modifier = Modifier.padding(top = Space.md),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                LibraryCategory.entries.forEach { category ->
                    FocusableRow(
                        onClick = { viewModel.select(category) },
                        modifier = Modifier.fillMaxWidth(),
                        selected = state.category == category,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
                    ) {
                        Text(
                            text = category.label,
                            style = AppType.body,
                            color = if (state.category == category) NavidromeColors.Accent else NavidromeColors.OnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Space.xs),
                modifier = Modifier.padding(top = Space.lg),
            ) {
                FilterChip(
                    label = "Grid",
                    selected = state.layout == LibraryLayout.GRID,
                    onClick = { viewModel.setLayout(LibraryLayout.GRID) },
                )
                FilterChip(
                    label = "List",
                    selected = state.layout == LibraryLayout.LIST,
                    onClick = { viewModel.setLayout(LibraryLayout.LIST) },
                )
            }

            if (state.category == LibraryCategory.ALBUMS) {
                Column(
                    modifier = Modifier.padding(top = Space.sm),
                    verticalArrangement = Arrangement.spacedBy(Space.xs),
                ) {
                    AlbumSort.entries.forEach { sort ->
                        FilterChip(
                            label = sort.label,
                            selected = state.sort == sort,
                            onClick = { viewModel.setSort(sort) },
                        )
                    }
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxHeight()) {
            when {
                state.loading -> LoadingState()
                state.error != null -> ErrorState(error = state.error!!, onRetry = viewModel::retry)
                state.isEmpty -> EmptyState(
                    title = "Nothing in ${state.category.label}",
                    message = state.note ?: "This server has no content for this category yet.",
                )
                else -> LibraryContent(
                    state = state,
                    playingTrackId = playback.currentTrack?.id,
                    isPlaying = playback.isPlaying,
                    onOpenAlbum = onOpenAlbum,
                    onOpenArtist = onOpenArtist,
                    onOpenPlaylist = onOpenPlaylist,
                    onOpenGenre = onOpenGenre,
                    onPlayTrack = viewModel::playTrack,
                    onToggleFavorite = viewModel::toggleTrackFavorite,
                )
            }
        }
    }
}

@Composable
private fun LibraryContent(
    state: LibraryUiState,
    playingTrackId: String?,
    isPlaying: Boolean,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenGenre: (String) -> Unit,
    onPlayTrack: (Int) -> Unit,
    onToggleFavorite: (com.navidrometv.core.model.Track) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        state.note?.let { note ->
            Text(
                text = note,
                style = AppType.cardSubtitle,
                color = LocalAppColors.current.secondaryText,
                modifier = Modifier.padding(bottom = Space.sm),
            )
        }

        when {
            state.playlists.isNotEmpty() -> LazyColumn(Modifier.fillMaxSize()) {
                items(state.playlists.size) { index ->
                    val playlist = state.playlists[index]
                    PlaylistRowItem(playlist = playlist, onClick = { onOpenPlaylist(playlist.id) })
                }
            }

            state.tracks.isNotEmpty() && state.albums.isEmpty() -> LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(state.tracks, key = { _, track -> track.id }) { index, track ->
                    TrackRow(
                        track = track,
                        index = index,
                        isCurrent = playingTrackId == track.id,
                        isPlaying = isPlaying,
                        onPlay = { onPlayTrack(index) },
                        onToggleFavorite = { onToggleFavorite(track) },
                        showAlbum = true,
                    )
                }
            }

            state.tracks.isNotEmpty() && state.albums.isNotEmpty() -> LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Space.lg),
            ) {
                item(key = "albums") {
                    AlbumGrid(albums = state.albums, onOpenAlbum = onOpenAlbum, columns = 5)
                }
                item(key = "tracks") {
                    Column {
                        state.tracks.take(10).forEachIndexed { index, track ->
                            TrackRow(
                                track = track,
                                index = index,
                                isCurrent = playingTrackId == track.id,
                                isPlaying = isPlaying,
                                onPlay = { onPlayTrack(index) },
                                onToggleFavorite = { onToggleFavorite(track) },
                                showAlbum = true,
                            )
                        }
                    }
                }
            }

            state.artists.isNotEmpty() -> GridOfArtists(
                artists = state.artists,
                onOpenArtist = onOpenArtist,
            )

            state.genres.isNotEmpty() -> GenreGrid(genres = state.genres, onOpenGenre = onOpenGenre)

            else -> AlbumGrid(
                albums = state.albums,
                onOpenAlbum = onOpenAlbum,
                columns = if (state.layout == LibraryLayout.GRID) 6 else 4,
            )
        }
    }
}

@Composable
private fun AlbumGrid(
    albums: List<com.navidrometv.core.model.Album>,
    onOpenAlbum: (String) -> Unit,
    columns: Int,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val spacing = 18.dp
        val cardWidth = (maxWidth - spacing * (columns - 1)) / columns
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            contentPadding = PaddingValues(bottom = Space.contentBottomWithPlayer),
        ) {
            items(albums, key = { it.id }) { album ->
                AlbumCard(
                    album = album,
                    onClick = { onOpenAlbum(album.id) },
                    width = cardWidth,
                    subtitleOverride = album.artistName,
                )
            }
        }
    }
}

@Composable
private fun GridOfArtists(
    artists: List<com.navidrometv.core.model.Artist>,
    onOpenArtist: (String) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = 6
        val spacing = 18.dp
        val cardWidth = (maxWidth - spacing * (columns - 1)) / columns
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            contentPadding = PaddingValues(bottom = Space.contentBottomWithPlayer),
        ) {
            items(artists, key = { it.id }) { artist ->
                ArtistCard(artist = artist, onClick = { onOpenArtist(artist.id) }, width = cardWidth)
            }
        }
    }
}

@Composable
private fun GenreGrid(
    genres: List<com.navidrometv.core.model.Genre>,
    onOpenGenre: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(bottom = Space.contentBottomWithPlayer),
    ) {
        items(genres, key = { it.name }) { genre ->
            GenreCard(
                genre = genre,
                onClick = { onOpenGenre(genre.name) },
                width = 320.dp,
                colorIndex = genres.indexOf(genre),
            )
        }
    }
}

@Composable
private fun PlaylistRowItem(playlist: Playlist, onClick: () -> Unit) {
    FocusableRow(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = playlist.name,
                style = AppType.body,
                color = NavidromeColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = Formatters.plural(playlist.trackCount, "song"),
                style = AppType.metadata,
                color = LocalAppColors.current.secondaryText,
            )
        }
    }
}
