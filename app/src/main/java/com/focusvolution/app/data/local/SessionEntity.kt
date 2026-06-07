package com.focusvolution.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representa uma sessão finalizada pelo utilizador.
 *
 * @property id chave primária autogerada
 * @property timestamp instante de conclusão da sessão em epoch millis
 * @property duration duração da sessão em segundos
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = -1,
    val timestamp: Long,
    val duration: Int
)
