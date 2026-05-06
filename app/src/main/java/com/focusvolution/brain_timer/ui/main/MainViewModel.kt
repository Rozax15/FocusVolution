package com.focusvolution.brain_timer.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.focusvolution.brain_timer.BrainTimerApplication
import com.focusvolution.brain_timer.data.repository.BrainTimerRepository
import com.focusvolution.brain_timer.service.TimerForegroundService
import com.focusvolution.brain_timer.service.TimerServiceStateStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BrainTimerRepository(
        (application as BrainTimerApplication).database
    )

    // Conta sessões falhadas (saiu da app durante sessão)
    private val _failedSessions = MutableStateFlow(0)

    // Regista se o utilizador saiu da app durante uma sessão ativa
    private var leftAppDuringSession = false

    // Guarda localmente o valor selecionado pelo utilizador para evitar race conditions com o serviço
    private var pendingDurationSeconds: Int = 0

    val uiState: StateFlow<MainUiState> = combine(
        repository.appStateFlow,
        repository.sessionsFlow,
        TimerServiceStateStore.state,
        _failedSessions
    ) { appState, sessions, timerState, failed ->
        MainUiState(
            selectedDurationSeconds = timerState.selectedDurationSeconds,
            remainingSeconds = timerState.remainingSeconds,
            isRunning = timerState.isRunning,
            totalSessions = appState.totalSessions,
            currentLevel = appState.currentLevel,
            failedSessions = failed,
            sessions = sessions
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureInitialState()
        }
        viewModelScope.launch {
            TimerServiceStateStore.state.collect {
                if (TimerServiceStateStore.sessionFinished) {
                    onSessionFinished()
                    TimerServiceStateStore.sessionFinished = false
                }
            }
        }
    }

    fun onDurationSelected(totalSeconds: Int) {
        if (!uiState.value.isRunning && totalSeconds > 0) {
            pendingDurationSeconds = totalSeconds
            TimerForegroundService.configure(getApplication(), totalSeconds)
        }
    }

    fun startOrResume() {
        val state = uiState.value
        leftAppDuringSession = false // reset ao iniciar
        val isPausedWithRemaining = !state.isRunning && state.remainingSeconds in 1 until state.selectedDurationSeconds

        if (isPausedWithRemaining) {
            TimerForegroundService.resume(getApplication())
        } else {
            // Usa pendingDurationSeconds se o estado do serviço ainda não foi atualizado (race condition)
            val durationToUse = if (pendingDurationSeconds > 0) pendingDurationSeconds else state.selectedDurationSeconds
            TimerForegroundService.startNew(
                getApplication(),
                durationToUse
            )
        }
    }

    fun pause() {
        TimerForegroundService.pause(getApplication())
    }

    fun reset() {
        leftAppDuringSession = false
        TimerForegroundService.reset(
            getApplication(),
            uiState.value.selectedDurationSeconds
        )
    }

    // Chamado quando o utilizador sai da app (vai para background)
    fun onAppBackground() {
        if (uiState.value.isRunning) {
            leftAppDuringSession = true
        }
    }

    // Chamado quando o utilizador volta à app (vem para foreground)
    fun onAppForeground() {
        // Não faz nada por agora, mas podes usar para logs ou UI
    }

    // Chamado quando o temporizador termina (chamas isto do teu serviço ou observer)
    fun onSessionFinished() {
        viewModelScope.launch {
            if (leftAppDuringSession) {
                // Sessão falhada
                val newFailed = _failedSessions.value + 1
                _failedSessions.value = newFailed

                // A cada 3 sessões falhadas, desce um nível
                if (newFailed >= 3) {
                    _failedSessions.value = 0
                    repository.decrementLevel()
                }
            } else {
                // Sessão concluída com sucesso
                _failedSessions.value = 0
                repository.completeSession()
            }
            leftAppDuringSession = false
        }
    }
}