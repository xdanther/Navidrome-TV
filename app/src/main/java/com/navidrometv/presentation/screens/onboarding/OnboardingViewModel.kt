package com.navidrometv.presentation.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navidrometv.core.common.AppError
import com.navidrometv.core.common.DataResult
import com.navidrometv.core.model.ServerHandshake
import com.navidrometv.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ConnectionCheckState { IDLE, TESTING, SUCCESS, FAILED }

data class OnboardingUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val check: ConnectionCheckState = ConnectionCheckState.IDLE,
    val message: String? = null,
    val error: AppError? = null,
    val handshake: ServerHandshake? = null,
    val busy: Boolean = false,
)

/**
 * Username and password never leave the view model except through the repository,
 * which hashes the password and stores it in the Keystore. They are not logged and
 * not put in saved instance state.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun onServerUrlChanged(value: String) = _state.update { it.copy(serverUrl = value, check = ConnectionCheckState.IDLE, error = null) }

    fun onUsernameChanged(value: String) = _state.update { it.copy(username = value, check = ConnectionCheckState.IDLE, error = null) }

    fun onPasswordChanged(value: String) = _state.update { it.copy(password = value, check = ConnectionCheckState.IDLE, error = null) }

    fun testConnection() {
        val current = _state.value
        if (!current.canSubmit || current.busy) return
        _state.update { it.copy(busy = true, check = ConnectionCheckState.TESTING, message = "Contacting server…", error = null) }
        viewModelScope.launch {
            when (val result = authRepository.testConnection(current.serverUrl, current.username, current.password)) {
                is DataResult.Success -> _state.update {
                    it.copy(
                        busy = false,
                        check = ConnectionCheckState.SUCCESS,
                        handshake = result.data,
                        message = "Connected to ${result.data.serverVersion}",
                        error = null,
                    )
                }
                is DataResult.Failure -> _state.update {
                    it.copy(
                        busy = false,
                        check = ConnectionCheckState.FAILED,
                        message = null,
                        error = result.error,
                    )
                }
            }
        }
    }

    fun connect(onConnected: () -> Unit) {
        val current = _state.value
        if (!current.canSubmit || current.busy) return
        _state.update { it.copy(busy = true, message = "Signing in…", error = null) }
        viewModelScope.launch {
            when (val result = authRepository.connect(current.serverUrl, current.username, current.password)) {
                is DataResult.Success -> {
                    _state.update { it.copy(busy = false, check = ConnectionCheckState.SUCCESS, handshake = result.data) }
                    onConnected()
                }
                is DataResult.Failure -> _state.update {
                    it.copy(busy = false, check = ConnectionCheckState.FAILED, error = result.error)
                }
            }
        }
    }

    val OnboardingUiState.canSubmit: Boolean
        get() = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotEmpty()
}
