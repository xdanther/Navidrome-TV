package com.navidrometv.presentation.screens.search

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.navidrometv.core.designsystem.component.ActionButton
import com.navidrometv.core.designsystem.component.EmptyState
import com.navidrometv.core.designsystem.component.ErrorState
import com.navidrometv.core.designsystem.component.FilterChip
import com.navidrometv.core.designsystem.component.LoadingState
import com.navidrometv.core.designsystem.component.SectionHeader
import com.navidrometv.core.designsystem.component.TrackRow
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.LocalAppColors
import com.navidrometv.core.designsystem.theme.NavidromeColors
import com.navidrometv.core.designsystem.theme.Space
import com.navidrometv.presentation.components.AlbumRow
import com.navidrometv.presentation.components.ArtistRow
import com.navidrometv.presentation.components.TvKeyboard

@Composable
fun SearchScreen(
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenNowPlaying: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Voice search is offered only when the device actually has a recogniser
    // activity (Assistant / Google TV voice input). The keyboard always works.
    val voiceAvailable = remember(context) {
        context.packageManager
            .queryIntentActivities(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0)
            .isNotEmpty()
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.onQueryChanged(spoken)
                viewModel.submit()
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Space.screenHorizontal, vertical = Space.md),
        horizontalArrangement = Arrangement.spacedBy(Space.xl),
    ) {
        Column(Modifier.width(660.dp)) {
            Text(text = "Search", style = AppType.title, color = NavidromeColors.OnSurface)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.md)
                    .background(NavidromeColors.SurfaceElevated, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Text(
                    text = state.query.ifBlank { "Type to search artists, albums and songs" },
                    style = AppType.body,
                    color = if (state.query.isBlank()) {
                        NavidromeColors.OnSurfaceMuted.copy(alpha = 0.6f)
                    } else {
                        NavidromeColors.OnSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                modifier = Modifier.padding(top = Space.sm),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                ActionButton(
                    text = "Search",
                    onClick = viewModel::submit,
                    enabled = state.query.trim().length >= 2 && !state.loading,
                )
                if (voiceAvailable) {
                    ActionButton(
                        text = "Voice search",
                        onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                                )
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Search your music")
                            }
                            try {
                                voiceLauncher.launch(intent)
                            } catch (error: ActivityNotFoundException) {
                                // Device advertises a recogniser but cannot launch it.
                            }
                        },
                        icon = Icons.Rounded.Mic,
                        primary = false,
                    )
                }
            }

            if (state.suggestions.isNotEmpty()) {
                Text(
                    text = "Suggestions",
                    style = AppType.overline,
                    color = NavidromeColors.OnSurfaceMuted,
                    modifier = Modifier.padding(top = Space.md),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Space.xs),
                    modifier = Modifier.padding(top = Space.xs),
                ) {
                    state.suggestions.forEach { suggestion ->
                        FilterChip(
                            label = suggestion,
                            selected = false,
                            onClick = { viewModel.useRecent(suggestion) },
                        )
                    }
                }
            }

            Text(
                text = "Recent searches",
                style = AppType.overline,
                color = NavidromeColors.OnSurfaceMuted,
                modifier = Modifier.padding(top = Space.md),
            )
            if (state.recentSearches.isEmpty()) {
                Text(
                    text = "Nothing yet",
                    style = AppType.cardSubtitle,
                    color = NavidromeColors.OnSurfaceMuted,
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Space.xs),
                    modifier = Modifier.padding(top = Space.xs),
                ) {
                    state.recentSearches.take(4).forEach { recent ->
                        FilterChip(label = recent, selected = false, onClick = { viewModel.useRecent(recent) })
                    }
                }
                ActionButton(
                    text = "Clear history",
                    onClick = viewModel::clearHistory,
                    primary = false,
                    modifier = Modifier.padding(top = Space.sm),
                )
            }

            TvKeyboard(
                onCharacter = viewModel::appendCharacter,
                onBackspace = viewModel::backspace,
                onClear = viewModel::clearQuery,
                onSearch = viewModel::submit,
                modifier = Modifier.padding(top = Space.lg),
            )
        }

        Column(Modifier.weight(1f)) {
            when {
                state.loading -> LoadingState(label = "Searching")
                state.error != null -> ErrorState(error = state.error!!, onRetry = viewModel::retry)
                state.results == null -> EmptyState(
                    title = "Search your library",
                    message = "Use the keyboard or the remote's voice button to find music on your server.",
                )
                !state.hasResults -> EmptyState(
                    title = "No results for “${state.submittedQuery}”",
                    message = "Check the spelling or try a different artist, album or song name.",
                )
                else -> {
                    val results = state.results!!
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = Space.contentBottomWithPlayer),
                        verticalArrangement = Arrangement.spacedBy(Space.lg),
                    ) {
                        if (results.artists.isNotEmpty()) {
                            item(key = "artists") {
                                ArtistRow(
                                    title = "Artists",
                                    artists = results.artists,
                                    onOpenArtist = onOpenArtist,
                                )
                            }
                        }
                        if (results.albums.isNotEmpty()) {
                            item(key = "albums") {
                                AlbumRow(
                                    title = "Albums",
                                    albums = results.albums,
                                    onOpenAlbum = onOpenAlbum,
                                )
                            }
                        }
                        if (results.tracks.isNotEmpty()) {
                            item(key = "tracks") {
                                Column {
                                    SectionHeader(title = "Songs")
                                    Column(Modifier.padding(top = Space.sm)) {
                                        results.tracks.forEachIndexed { index, track ->
                                            TrackRow(
                                                track = track,
                                                index = index,
                                                isCurrent = false,
                                                isPlaying = false,
                                                onPlay = { viewModel.playTrack(index) },
                                                onToggleFavorite = { viewModel.toggleTrackFavorite(track) },
                                                showAlbum = true,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
