package com.focusvolution.brain_timer.data.repository

import androidx.room.withTransaction
import com.focusvolution.brain_timer.data.local.AppStateEntity
import com.focusvolution.brain_timer.data.local.BrainTimerDatabase
import com.focusvolution.brain_timer.data.local.SessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Camada de acesso a dados (Repository).
 *
 * Centraliza regras de persistência e cálculo de nível.
 */
class BrainTimerRepository(
    private val database: BrainTimerDatabase
) {
    private val dao = database.brainTimerDao()

    val sessionsFlow: Flow<List<SessionEntity>> = dao.observeSessions()

    val appStateFlow: Flow<AppStateEntity> = dao.observeAppState().map { current ->
        current ?: AppStateEntity(totalSessions = 0, currentLevel = 1)
    }

    suspend fun ensureInitialState() {
        if (dao.getAppState() == null) {
            dao.upsertAppState(AppStateEntity(totalSessions = 0, currentLevel = 1))
        }
    }

    /**
     * Regista sessão concluída e atualiza total/nível de forma atómica.
     */
    suspend fun recordCompletedSession(durationSeconds: Int) {
        database.withTransaction {
            val previous = dao.getAppState() ?: AppStateEntity(totalSessions = 0, currentLevel = 1)
            val newTotal = previous.totalSessions + 1
            val calculatedLevel = ((newTotal / 5) + 1).coerceAtMost(10)

            dao.insertSession(
                SessionEntity(
                    timestamp = System.currentTimeMillis(),
                    duration = durationSeconds
                )
            )

            dao.upsertAppState(
                previous.copy(
                    totalSessions = newTotal,
                    currentLevel = calculatedLevel
                )
            )
        }
    }
    /**
     * Alias para registar sessão concluída com sucesso (sem saída da app).
     */
    suspend fun completeSession() {
        val previous = dao.getAppState() ?: AppStateEntity(totalSessions = 0, currentLevel = 1)
        recordCompletedSession(previous.currentLevel)
    }

    /**
     * Penalização: desce um nível (mínimo nível 1).
     */
    suspend fun decrementLevel() {
        database.withTransaction {
            val previous = dao.getAppState() ?: AppStateEntity(totalSessions = 0, currentLevel = 1)
            val newLevel = (previous.currentLevel - 1).coerceAtLeast(1)
            dao.upsertAppState(previous.copy(currentLevel = newLevel))
        }
    }
}
