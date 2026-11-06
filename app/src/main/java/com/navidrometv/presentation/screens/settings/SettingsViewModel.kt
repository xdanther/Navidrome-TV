package com.navidrometv.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navidrometv.core.common.DataResult
import com.navidrometv.core.model.StreamQuality
import com.navidrometv.data.local.CacheManager
import com.navidrometv.data.prefs.AppSettings
import com.navidrometv.data.prefs.GridDensity
import com.navidrometv.domain.repository.AuthRepository
import com.navidrometv.domain.repository.ConnectionStatus
import com.navidrometv.domain.repository.SettingsRepository
import com.navidrometv.playback.PlaybackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val connection: ConnectionStatus = ConnectionStatus(connected = false),
    val testing: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val cacheManager: CacheManager,
    private val playbackRepository: PlaybackRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState(connection = authRepository.currentStatus()))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            authRepository.connection.collect { status ->
                _state.update { it.copy(connection = status) }
            }
        }
    }

    fun setQuality(quality: StreamQuality) {
        viewModelScope.launch {
            settingsRepository.setStreamingQuality(quality)
            _state.update { it.copy(message = "Streaming: ${quality.label}") }
        }
    }

    fun setReduceMotion(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setReduceMotion(enabled) }
    }

    fun setArtworkBackground(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setArtworkBackground(enabled) }
    }

    fun setGridDensity(density: GridDensity) {
        viewModelScope.launch { settingsRepository.setGridDensity(density) }
    }

    fun setShuffleByDefault(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShuffleByDefault(enabled) }
    }

    fun setScrobbleEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setScrobbleEnabled(enabled) }
    }

    fun setAmbientModeEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAmbientModeEnabled(enabled) }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepScreenOn(enabled) }
    }

    fun testConnection() {
        if (_state.value.testing) return
        _state.update { it.copy(testing = true, message = "Testing connection…") }
        viewModelScope.launch {
            // Ping through the stored session; never re-send credentials from here.
            val result = authRepository.verifyConnection()
            _state.update {
                it.copy(
                    testing = false,
                    message = when (result) {
                        is DataResult.Success -> "Server reachable (${result.data.serverVersion})"
                        is DataResult.Failure -> result.error.message
                    },
                )
            }
        }
    }

    fun clearArtworkCache() {
        cacheManager.clearArtworkCache()
        _state.update { it.copy(message = "Artwork cache cleared") }
    }

    fun clearMetadataCache() {
        viewModelScope.launch {
            cacheManager.clearMetadataCache()
            _state.update { it.copy(message = "Cached metadata cleared") }
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            cacheManager.clearSearchHistory()
            _state.update { it.copy(message = "Search history cleared") }
        }
    }

    fun clearListeningHistory() {
        viewModelScope.launch {
            cacheManager.clearListeningHistory()
            _state.update { it.copy(message = "Listening history cleared") }
        }
    }

    fun disconnect(onDisconnected: () -> Unit) {
        viewModelScope.launch {
            playbackRepository.pause()
            playbackRepository.clearQueue()
            authRepository.disconnect()
            onDisconnected()
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }
}
