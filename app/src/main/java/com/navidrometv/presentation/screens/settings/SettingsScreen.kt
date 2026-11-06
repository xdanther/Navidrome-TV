package com.navidrometv.presentation.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.navidrometv.BuildConfig
import com.navidrometv.core.designsystem.component.ActionButton
import com.navidrometv.core.designsystem.component.FocusableRow
import com.navidrometv.core.designsystem.component.FilterChip
import com.navidrometv.core.designsystem.component.InlineNotice
import com.navidrometv.core.designsystem.component.SectionHeader
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.LocalAppColors
import com.navidrometv.core.designsystem.theme.NavidromeColors
import com.navidrometv.core.designsystem.theme.Space
import com.navidrometv.core.model.StreamQuality
import com.navidrometv.data.prefs.GridDensity

@Composable
fun SettingsScreen(
    onDisconnected: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings
    var confirmDisconnect by remember { mutableStateOf(false) }

    BackHandler(onBack = onDisconnected)

    LaunchedEffect(state.message) {
        if (state.message != null) {
            kotlinx.coroutines.delay(3_000)
            viewModel.consumeMessage()
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Space.screenHorizontal,
                end = Space.screenHorizontal,
                top = Space.md,
                bottom = Space.contentBottomWithPlayer,
            ),
            verticalArrangement = Arrangement.spacedBy(Space.lg),
        ) {
            item(key = "title") {
                Text(text = "Settings", style = AppType.title, color = NavidromeColors.OnSurface)
            }

            item(key = "server") {
                SettingsSection("Server") {
                    LabelValueRow(
                        label = "Address",
                        value = state.connection.serverUrl ?: "Not connected",
                    )
                    LabelValueRow(
                        label = "User",
                        value = state.connection.username ?: "—",
                    )
                    LabelValueRow(
                        label = "Status",
                        value = when {
                            state.testing -> "Testing…"
                            state.connection.connected ->
                                "Connected${state.connection.serverVersion?.let { " · $it" } ?: ""}"
                            else -> "Disconnected"
                        },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        ActionButton(
                            text = "Test connection",
                            onClick = viewModel::testConnection,
                            primary = false,
                            enabled = state.connection.connected && !state.testing,
                        )
                        ActionButton(
                            text = "Disconnect",
                            onClick = { confirmDisconnect = true },
                            primary = false,
                            enabled = state.connection.connected,
                        )
                    }
                    Text(
                        text = "Server address, credentials and library content are managed by Navidrome. " +
                            "Everything below is stored on this device.",
                        style = AppType.cardSubtitle,
                        color = NavidromeColors.OnSurfaceMuted,
                    )
                }
            }

            item(key = "playback") {
                SettingsSection("Playback") {
                    Text(
                        text = "Streaming quality",
                        style = AppType.body,
                        color = NavidromeColors.OnSurface,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        StreamQuality.entries.forEach { quality ->
                            FilterChip(
                                label = quality.label,
                                selected = settings.streamingQuality == quality,
                                onClick = { viewModel.setQuality(quality) },
                            )
                        }
                    }
                    Text(
                        text = "Transcoded qualities ask Navidrome for a maximum bitrate; the delivered format " +
                            "is reported on Now Playing.",
                        style = AppType.cardSubtitle,
                        color = NavidromeColors.OnSurfaceMuted,
                    )
                    ToggleRow(
                        label = "Shuffle when playing albums and playlists",
                        value = settings.shuffleByDefault,
                        onToggle = viewModel::setShuffleByDefault,
                    )
                }
            }

            item(key = "appearance") {
                SettingsSection("Appearance") {
                    ToggleRow(
                        label = "Artwork backgrounds on Now Playing",
                        value = settings.artworkBackground,
                        onToggle = viewModel::setArtworkBackground,
                    )
                    ToggleRow(
                        label = "Reduce motion",
                        value = settings.reduceMotion,
                        onToggle = viewModel::setReduceMotion,
                    )
                    Text(
                        text = "Grid density",
                        style = AppType.body,
                        color = NavidromeColors.OnSurface,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        GridDensity.entries.forEach { density ->
                            FilterChip(
                                label = "${density.label} (${density.columns})",
                                selected = settings.gridDensity == density,
                                onClick = { viewModel.setGridDensity(density) },
                            )
                        }
                    }
                }
            }

            item(key = "tv") {
                SettingsSection("Television") {
                    ToggleRow(
                        label = "Ambient mode when idle",
                        value = settings.ambientModeEnabled,
                        onToggle = viewModel::setAmbientModeEnabled,
                    )
                    ToggleRow(
                        label = "Keep screen awake while playing",
                        value = settings.keepScreenOn,
                        onToggle = viewModel::setKeepScreenOn,
                    )
                    ToggleRow(
                        label = "Scrobble plays to Navidrome",
                        value = settings.scrobbleEnabled,
                        onToggle = viewModel::setScrobbleEnabled,
                    )
                }
            }

            item(key = "storage") {
                SettingsSection("Storage and privacy") {
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        ActionButton(
                            text = "Clear artwork cache",
                            onClick = viewModel::clearArtworkCache,
                            primary = false,
                        )
                        ActionButton(
                            text = "Clear metadata cache",
                            onClick = viewModel::clearMetadataCache,
                            primary = false,
                        )
                        ActionButton(
                            text = "Clear search history",
                            onClick = viewModel::clearSearchHistory,
                            primary = false,
                        )
                        ActionButton(
                            text = "Clear listening history",
                            onClick = viewModel::clearListeningHistory,
                            primary = false,
                        )
                    }
                    Text(
                        text = "Listening history is stored only on this device and powers Recently Played and Most Played.",
                        style = AppType.cardSubtitle,
                        color = NavidromeColors.OnSurfaceMuted,
                    )
                }
            }

            item(key = "about") {
                SettingsSection("About") {
                    LabelValueRow(label = "Version", value = BuildConfig.VERSION_NAME)
                    LabelValueRow(label = "Client", value = "NavidromeTV · Subsonic API 1.16.1")
                    Text(
                        text = "A native Android TV client for Navidrome. Not affiliated with Apple.",
                        style = AppType.cardSubtitle,
                        color = NavidromeColors.OnSurfaceMuted,
                    )
                }
            }
        }

        state.message?.let { message ->
            InlineNotice(
                text = message,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp),
            )
        }

        if (confirmDisconnect) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(NavidromeColors.Ink.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier
                        .background(NavidromeColors.Surface, RoundedCornerShape(24.dp))
                        .padding(Space.lg),
                ) {
                    Text(
                        text = "Disconnect from the server?",
                        style = AppType.section,
                        color = NavidromeColors.OnSurface,
                    )
                    Text(
                        text = "Stored credentials are removed from this device. Playback stops.",
                        style = AppType.bodyMuted,
                        color = LocalAppColors.current.secondaryText,
                        modifier = Modifier.padding(top = Space.xs),
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top = Space.lg),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        ActionButton(
                            text = "Cancel",
                            onClick = { confirmDisconnect = false },
                            primary = false,
                        )
                        ActionButton(
                            text = "Disconnect",
                            onClick = {
                                confirmDisconnect = false
                                viewModel.disconnect(onDisconnected)
                            },
                            modifier = Modifier.padding(start = Space.sm),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .background(NavidromeColors.Surface.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(Space.lg),
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        SectionHeader(title = title)
        content()
    }
}

@Composable
private fun LabelValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = AppType.bodyMuted, color = NavidromeColors.OnSurfaceMuted)
        Text(
            text = value,
            style = AppType.bodyMuted,
            color = NavidromeColors.OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onToggle: (Boolean) -> Unit) {
    FocusableRow(
        onClick = { onToggle(!value) },
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = AppType.body, color = NavidromeColors.OnSurface)
            Text(
                text = if (value) "On" else "Off",
                style = AppType.metadata,
                color = if (value) NavidromeColors.Accent else NavidromeColors.OnSurfaceMuted,
            )
        }
    }
}
