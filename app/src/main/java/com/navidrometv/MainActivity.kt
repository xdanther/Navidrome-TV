package com.navidrometv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.navidrometv.core.designsystem.component.LocalCoverArtUrl
import com.navidrometv.core.designsystem.theme.NavidromeTvTheme
import com.navidrometv.data.prefs.AppSettings
import com.navidrometv.domain.repository.MusicRepository
import com.navidrometv.domain.repository.SettingsRepository
import com.navidrometv.playback.PlaybackRepository
import com.navidrometv.presentation.NavidromeTvApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var musicRepository: MusicRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var playbackRepository: PlaybackRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Connect the media controller early so transport controls and the mini
        // player are ready before the first screen is composed.
        playbackRepository.connect()

        setContent {
            val settings by settingsRepository.settings.collectAsState(initial = AppSettings())
            NavidromeTvTheme {
                CompositionLocalProvider(
                    LocalCoverArtUrl provides { coverArtId, sizePx ->
                        musicRepository.coverArtUrl(coverArtId, sizePx)
                    },
                ) {
                    NavidromeTvApp(
                        playback = playbackRepository,
                        settings = settings,
                    )
                }
            }
        }
    }
}
