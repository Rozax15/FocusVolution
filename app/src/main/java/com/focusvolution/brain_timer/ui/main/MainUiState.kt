package com.focusvolution.brain_timer.ui.main

/**
 * Estado agregado consumido pela UI principal.
 */
data class MainUiState(
    val selectedDurationSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false,
    val totalSessions: Int = 0,
    val currentLevel: Int = 1,
    val failedSessions: Int = 0
)
