package com.navidrometv.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.navidrometv.core.designsystem.theme.AmbientBackdrop
import com.navidrometv.data.prefs.AppSettings
import com.navidrometv.playback.PlaybackRepository
import com.navidrometv.presentation.components.MiniPlayer
import com.navidrometv.presentation.components.LocalTopBarVisibility
import com.navidrometv.presentation.components.TopBar
import com.navidrometv.presentation.components.TopBarVisibility
import com.navidrometv.presentation.navigation.Routes
import com.navidrometv.presentation.screens.album.AlbumScreen
import com.navidrometv.presentation.screens.artist.ArtistScreen
import com.navidrometv.presentation.screens.browse.BrowseScreen
import com.navidrometv.presentation.screens.browse.GenreScreen
import com.navidrometv.presentation.screens.home.HomeScreen
import com.navidrometv.presentation.screens.library.LibraryScreen
import com.navidrometv.presentation.screens.newmusic.NewScreen
import com.navidrometv.presentation.screens.nowplaying.NowPlayingScreen
import com.navidrometv.presentation.screens.onboarding.OnboardingScreen
import com.navidrometv.presentation.screens.party.PartyModeScreen
import com.navidrometv.presentation.screens.playlist.PlaylistScreen
import com.navidrometv.presentation.screens.search.SearchScreen
import com.navidrometv.presentation.screens.settings.SettingsScreen

@Composable
fun NavidromeTvApp(
    playback: PlaybackRepository,
    settings: AppSettings,
) {
    val mainViewModel: MainViewModel = hiltViewModel()
    val startState by mainViewModel.startState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackdrop(Modifier.matchParentSize())
        when (val state = startState) {
            AppStartState.Loading -> SplashScreen()
            AppStartState.Onboarding -> OnboardingScreen(onConnected = mainViewModel::onConnected)
            AppStartState.Ready -> {
                val topBarVisibility = remember { TopBarVisibility() }
                CompositionLocalProvider(LocalTopBarVisibility provides topBarVisibility) {
                    MainShell(
                        playback = playback,
                        settings = settings,
                        onDisconnected = mainViewModel::onDisconnected,
                    )
                }
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        com.navidrometv.core.designsystem.component.LoadingState(label = "Starting Navidrome TV")
    }
}

@Composable
private fun MainShell(
    playback: PlaybackRepository,
    settings: AppSettings,
    onDisconnected: () -> Unit,
) {
    val navController = rememberNavController()
    val playbackState by playback.state.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showChrome = currentRoute != Routes.NOW_PLAYING

    val openAlbum: (String) -> Unit = { id -> navController.navigate(Routes.album(id)) }
    val openArtist: (String) -> Unit = { id -> navController.navigate(Routes.artist(id)) }
    val openPlaylist: (String) -> Unit = { id -> navController.navigate(Routes.playlist(id)) }
    val openGenre: (String) -> Unit = { name -> navController.navigate(Routes.genre(name)) }
    val openNowPlaying: () -> Unit = { navController.navigate(Routes.NOW_PLAYING) }

    val navigateTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val topBarVisibility = LocalTopBarVisibility.current
    // Every destination opens with the navigation available.
    LaunchedEffect(currentRoute) { topBarVisibility.visible = true }
    // While the bar is collapsed, Back brings it back instead of leaving the app.
    BackHandler(enabled = showChrome && !topBarVisibility.visible) {
        topBarVisibility.visible = true
    }

    Column(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = showChrome && topBarVisibility.visible,
            enter = fadeIn(animationSpec = tween(180)) +
                expandVertically(animationSpec = tween(220)),
            exit = fadeOut(animationSpec = tween(140)) +
                shrinkVertically(animationSpec = tween(200)),
        ) {
            TopBar(
                currentRoute = currentRoute,
                onNavigate = navigateTopLevel,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenParty = { navController.navigate(Routes.PARTY) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Box(Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onOpenAlbum = openAlbum,
                        onOpenArtist = openArtist,
                        onOpenPlaylist = openPlaylist,
                        onOpenGenre = openGenre,
                        onOpenNowPlaying = openNowPlaying,
                    )
                }
                composable(Routes.NEW) {
                    NewScreen(
                        onOpenAlbum = openAlbum,
                        onOpenArtist = openArtist,
                        onOpenNowPlaying = openNowPlaying,
                    )
                }
                composable(Routes.BROWSE) {
                    BrowseScreen(
                        onOpenAlbum = openAlbum,
                        onOpenArtist = openArtist,
                        onOpenPlaylist = openPlaylist,
                        onOpenGenre = openGenre,
                    )
                }
                composable(Routes.LIBRARY) {
                    LibraryScreen(
                        onOpenAlbum = openAlbum,
                        onOpenArtist = openArtist,
                        onOpenPlaylist = openPlaylist,
                        onOpenGenre = openGenre,
                    )
                }
                composable(Routes.SEARCH) {
                    SearchScreen(
                        onOpenAlbum = openAlbum,
                        onOpenArtist = openArtist,
                        onOpenNowPlaying = openNowPlaying,
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(onDisconnected = {
                        onDisconnected()
                    })
                }
                composable(Routes.PARTY) {
                    PartyModeScreen(onOpenNowPlaying = openNowPlaying)
                }
                composable(Routes.NOW_PLAYING) {
                    NowPlayingScreen(
                        playback = playback,
                        settings = settings,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(
                    route = Routes.ALBUM,
                    arguments = listOf(navArgument(Routes.ALBUM_ARG) { type = NavType.StringType }),
                ) { entry ->
                    AlbumScreen(
                        albumId = entry.arguments?.getString(Routes.ALBUM_ARG).orEmpty(),
                        onBack = { navController.popBackStack() },
                        onOpenArtist = openArtist,
                        onOpenNowPlaying = openNowPlaying,
                    )
                }
                composable(
                    route = Routes.ARTIST,
                    arguments = listOf(navArgument(Routes.ARTIST_ARG) { type = NavType.StringType }),
                ) { entry ->
                    ArtistScreen(
                        artistId = entry.arguments?.getString(Routes.ARTIST_ARG).orEmpty(),
                        onBack = { navController.popBackStack() },
                        onOpenAlbum = openAlbum,
                        onOpenArtist = openArtist,
                        onOpenNowPlaying = openNowPlaying,
                    )
                }
                composable(
                    route = Routes.PLAYLIST,
                    arguments = listOf(navArgument(Routes.PLAYLIST_ARG) { type = NavType.StringType }),
                ) { entry ->
                    PlaylistScreen(
                        playlistId = entry.arguments?.getString(Routes.PLAYLIST_ARG).orEmpty(),
                        onBack = { navController.popBackStack() },
                        onOpenAlbum = openAlbum,
                        onOpenArtist = openArtist,
                        onOpenNowPlaying = openNowPlaying,
                    )
                }
                composable(
                    route = Routes.GENRE,
                    arguments = listOf(navArgument(Routes.GENRE_ARG) { type = NavType.StringType }),
                ) { entry ->
                    GenreScreen(
                        genreName = entry.arguments?.getString(Routes.GENRE_ARG).orEmpty(),
                        onBack = { navController.popBackStack() },
                        onOpenAlbum = openAlbum,
                    )
                }
            }

            if (showChrome && playbackState.currentTrack != null) {
                MiniPlayer(
                    state = playbackState,
                    onOpen = openNowPlaying,
                    onTogglePlay = playback::togglePlayPause,
                    onNext = playback::next,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}
