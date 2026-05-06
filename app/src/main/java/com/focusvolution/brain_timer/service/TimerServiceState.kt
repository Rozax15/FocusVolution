package com.focusvolution.brain_timer.service

/**
 * Estado do temporizador que a UI observa via StateFlow.
 */
data class TimerServiceState(
    val selectedDurationSeconds: Int = 0 * 0,
    val remainingSeconds: Int = 0 * 0,
    val isRunning: Boolean = false
)
