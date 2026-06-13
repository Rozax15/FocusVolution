package com.focusvolution.app.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.focusvolution.app.FocusVolutionApp
import com.focusvolution.app.data.repository.FocusVolutionRepository
import com.focusvolution.app.service.TimerForegroundService
import com.focusvolution.app.service.TimerServiceStateStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as? FocusVolutionApp
        ?: throw IllegalStateException("Application must be FocusVolutionApp")
    private val repository = FocusVolutionRepository(app.database)

    /** Fluxo de ID do utilizador atual para reação a mudanças de utilizador */
    private val _currentUserId = MutableStateFlow(-1L)
    var currentUserId: Long
        get() = _currentUserId.value
        set(value) { _currentUserId.value = value }

    // Conta sessões falhadas (saiu da app durante sessão)
    private val _failedSessions = MutableStateFlow(0)

    // Regista se o utilizador saiu da app durante uma sessão ativa
    private var leftAppDuringSession = false

    // Guarda localmente a tag selecionada para associar à sessão
    var selectedTag: String? = null

    // Guarda localmente o valor selecionado pelo utilizador para evitar race conditions com o serviço
    private var pendingDurationSeconds: Int = 0

    @OptIn(ExperimentalCoroutinesApi::class)
    private val userStateFlow = _currentUserId.flatMapLatest { id ->
        if (id == -1L) flowOf(null) else repository.observeUser(id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val totalFocusFlow = _currentUserId.flatMapLatest { id ->
        if (id == -1L) flowOf(0) else repository.observeTotalFocusSeconds(id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val uiState: StateFlow<MainUiState> = combine(
        userStateFlow,
        TimerServiceStateStore.state,
        _failedSessions,
        totalFocusFlow
    ) { user, timerState, failed, totalFocus ->
        MainUiState(
            selectedDurationSeconds = timerState.selectedDurationSeconds,
            remainingSeconds = timerState.remainingSeconds,
            isRunning = timerState.isRunning,
            totalSessions = user?.totalSessions ?: 0,
            totalFocusSeconds = totalFocus,
            currentLevel = user?.currentLevel ?: 1,
            failedSessions = failed
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
            TimerServiceStateStore.userLeftDuringSession = true
        }
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
                    repository.penalizeUser(currentUserId)
                }
            } else {
                // Sessão concluída com sucesso
                _failedSessions.value = 0
                val actualDuration = uiState.value.selectedDurationSeconds
                repository.completeSession(actualDuration, currentUserId, selectedTag)
                selectedTag = null
                TimerServiceStateStore.userLeftDuringSession = false
            }
            leftAppDuringSession = false
        }
    }
}