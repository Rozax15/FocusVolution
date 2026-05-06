package com.focusvolution.brain_timer.ui.main

import com.focusvolution.brain_timer.data.local.SessionEntity

/**
 * Estado agregado consumido pela UI principal.
 */
data class MainUiState(
    val selectedDurationSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false,
    val totalSessions: Int = 0,
    val currentLevel: Int = 1,
    val failedSessions: Int = 0,
    val sessions: List<SessionEntity> = emptyList()
)
