package com.navidrometv.presentation.screens.party

import androidx.lifecycle.ViewModel
import com.navidrometv.party.PartyModeManager
import com.navidrometv.party.PartyState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PartyModeViewModel @Inject constructor(
    private val manager: PartyModeManager,
) : ViewModel() {

    val state: StateFlow<PartyState> = manager.state

    fun start() {
        manager.start()
    }

    fun stop() {
        manager.stop()
    }

    fun approve(requestId: String) {
        manager.approve(requestId)
    }

    fun reject(requestId: String) {
        manager.reject(requestId)
    }

    fun consumeNotice() {
        manager.consumeNotice()
    }
}
