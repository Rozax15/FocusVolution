package com.focusvolution.brain_timer.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Store singleton para partilhar estado entre serviço e ViewModel.
 */
object TimerServiceStateStore {
    private val _state = MutableStateFlow(TimerServiceState())
    val state: StateFlow<TimerServiceState> = _state.asStateFlow()

    @Volatile
    var sessionFinished: Boolean = false

    fun update(newState: TimerServiceState) {
        _state.value = newState
    }

    fun current(): TimerServiceState = _state.value
}

