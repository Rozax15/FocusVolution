package com.focusvolution.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Estado agregado da aplicação.
 *
 * Mantemos sempre uma única linha com id = 0 para facilitar consultas.
 */
@Entity(tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey val id: Int = 0,
    val totalSessions: Int,
    val currentLevel: Int
)
