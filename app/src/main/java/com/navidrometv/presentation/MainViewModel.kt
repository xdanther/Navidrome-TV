package com.navidrometv.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navidrometv.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AppStartState {
    data object Loading : AppStartState
    data object Onboarding : AppStartState
    data object Ready : AppStartState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _startState = MutableStateFlow<AppStartState>(AppStartState.Loading)
    val startState: StateFlow<AppStartState> = _startState.asStateFlow()

    init {
        viewModelScope.launch {
            val restored = runCatching { authRepository.restoreSession() }.getOrDefault(false)
            _startState.value = if (restored) AppStartState.Ready else AppStartState.Onboarding
        }
    }

    fun onConnected() {
        _startState.value = AppStartState.Ready
    }

    fun onDisconnected() {
        _startState.value = AppStartState.Onboarding
    }
}
