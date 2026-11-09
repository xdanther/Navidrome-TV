package com.navidrometv.presentation.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.navidrometv.core.designsystem.component.ErrorState
import com.navidrometv.core.designsystem.component.LoadingState
import com.navidrometv.core.designsystem.component.SectionHeader
import com.navidrometv.core.designsystem.component.TrackRow
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.NavidromeColors
import com.navidrometv.core.designsystem.theme.Space
import com.navidrometv.presentation.components.AlbumRow
import com.navidrometv.presentation.components.ArtistRow
import com.navidrometv.presentation.components.HideTopBarOnScroll
import com.navidrometv.presentation.components.GenreRow
import com.navidrometv.presentation.components.PlaylistRow
import com.navidrometv.presentation.components.PosterRow
import com.navidrometv.playback.PlaybackState

@Composable
fun HomeScreen(
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenGenre: (String) -> Unit,
    onOpenNowPlaying: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.openNowPlaying.collect { onOpenNowPlaying() }
    }

    when {
        state.loading && state.isEmpty -> LoadingState(label = "Loading your music")
        state.error != null && state.isEmpty -> ErrorState(error = state.error!!, onRetry = viewModel::refresh)
        else -> HomeContent(
            state = state,
            playback = playback,
            onOpenAlbum = onOpenAlbum,
            onOpenArtist = onOpenArtist,
            onOpenPlaylist = onOpenPlaylist,
            onOpenGenre = onOpenGenre,
            onPlayTrack = viewModel::playMostPlayed,
            onPlayFavorite = viewModel::playFavorites,
            onToggleFavorite = viewModel::toggleTrackFavorite,
            onRefresh = viewModel::refresh,
        )
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    playback: PlaybackState,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenGenre: (String) -> Unit,
    onPlayTrack: (Int) -> Unit,
    onPlayFavorite: (Int) -> Unit,
    onToggleFavorite: (com.navidrometv.core.model.Track) -> Unit,
    onRefresh: () -> Unit,
) {
    val listState = rememberLazyListState()
    HideTopBarOnScroll(listState)

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = Space.md,
            bottom = Space.contentBottomWithPlayer,
        ),
        verticalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        // One compact line above the first shelf instead of a hero block: the
        // reference page starts on its first heading, not on a greeting wall.
        item(key = "greeting") {
            Text(
                text = greeting(),
                style = AppType.section,
                color = NavidromeColors.OnSurfaceMuted,
                modifier = Modifier.padding(horizontal = Space.screenHorizontal),
            )
        }

        if (state.discover.isNotEmpty()) {
            item(key = "top-picks") {
                PosterRow(
                    title = "Top Picks for You",
                    albums = state.discover,
                    onOpenAlbum = onOpenAlbum,
                    bleed = true,
                )
            }
        }

        if (state.recentlyPlayed.isNotEmpty()) {
            item(key = "recent") {
                AlbumRow(title = "Recently Played", albums = state.recentlyPlayed, onOpenAlbum = onOpenAlbum, bleed = true)
            }
        }

        if (state.mostPlayed.isNotEmpty()) {
            item(key = "most-played") {
                Column(modifier = Modifier.padding(horizontal = Space.screenHorizontal)) {
                    SectionHeader(title = "Most Played", subtitle = "from this device")
                    Column(Modifier.padding(top = Space.sm)) {
                        state.mostPlayed.forEachIndexed { index, track ->
                            TrackRow(
                                track = track,
                                index = index,
                                isCurrent = playback.currentTrack?.id == track.id,
                                isPlaying = playback.isPlaying,
                                onPlay = { onPlayTrack(index) },
                                onToggleFavorite = { onToggleFavorite(track) },
                                showAlbum = true,
                            )
                        }
                    }
                }
            }
        }

        if (state.recentlyAdded.isNotEmpty()) {
            item(key = "newest") {
                AlbumRow(title = "Recently Added", albums = state.recentlyAdded, onOpenAlbum = onOpenAlbum, bleed = true)
            }
        }

        if (state.playedArtists.isNotEmpty()) {
            item(key = "artists") {
                ArtistRow(title = "Artists You Play", artists = state.playedArtists, onOpenArtist = onOpenArtist, bleed = true)
            }
        }

        if (state.favoriteAlbums.isNotEmpty()) {
            item(key = "favorites") {
                AlbumRow(title = "Favorite Albums", albums = state.favoriteAlbums, onOpenAlbum = onOpenAlbum, bleed = true)
            }
        }

        if (state.favoriteTracks.isNotEmpty()) {
            item(key = "favorite-tracks") {
                Column(modifier = Modifier.padding(horizontal = Space.screenHorizontal)) {
                    SectionHeader(title = "Favorite Songs")
                    Column(Modifier.padding(top = Space.sm)) {
                        state.favoriteTracks.take(6).forEachIndexed { index, track ->
                            TrackRow(
                                track = track,
                                index = index,
                                isCurrent = playback.currentTrack?.id == track.id,
                                isPlaying = playback.isPlaying,
                                onPlay = { onPlayFavorite(index) },
                                onToggleFavorite = { onToggleFavorite(track) },
                                showAlbum = true,
                            )
                        }
                    }
                }
            }
        }

        if (state.playlists.isNotEmpty()) {
            item(key = "playlists") {
                PlaylistRow(title = "Playlists", playlists = state.playlists, onOpenPlaylist = onOpenPlaylist, bleed = true)
            }
        }

        if (state.genres.isNotEmpty()) {
            item(key = "genres") {
                GenreRow(title = "Genres", genres = state.genres, onOpenGenre = onOpenGenre, bleed = true)
            }
        }

        if (state.frequentlyPlayed.isNotEmpty()) {
            item(key = "frequent") {
                AlbumRow(title = "On Repeat", albums = state.frequentlyPlayed, onOpenAlbum = onOpenAlbum, bleed = true)
            }
        }

        if (state.isEmpty) {
            item(key = "empty") {
                Column(
                    Modifier
                        .padding(horizontal = Space.screenHorizontal)
                        .height(240.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Your library looks empty",
                        style = AppType.title,
                        color = NavidromeColors.OnSurface,
                    )
                    Text(
                        text = "Add music to Navidrome and pull to refresh.",
                        style = AppType.bodyMuted,
                        color = NavidromeColors.OnSurfaceMuted,
                    )
                    com.navidrometv.core.designsystem.component.ActionButton(
                        text = "Refresh",
                        onClick = onRefresh,
                        modifier = Modifier.padding(top = Space.md),
                        primary = false,
                    )
                }
            }
        }

        item(key = "footer") {
            Column(Modifier.fillMaxWidth().padding(horizontal = Space.screenHorizontal)) {
                Text(
                    text = "Sections come from your server and this device's play history.",
                    style = AppType.cardSubtitle,
                    color = NavidromeColors.OnSurfaceMuted.copy(alpha = 0.6f),
                )
            }
        }
    }
}

private fun greeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 6 -> "Good night"
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }
}
