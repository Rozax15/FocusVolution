package com.focusvolution.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Representa um utilizador registado da aplicação.
 *
 * @property id chave primária autogerada
 * @property username nome de utilizador único
 * @property email email único
 * @property passwordHash hash SHA-256 da palavra-passe
 * @property totalSessions total de sessões concluídas por este utilizador
 * @property currentLevel nível atual do utilizador (1-10)
 * @property isEmailVerified se o email já foi verificado
 * @property verificationToken token único para verificação de email
 */
@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["username"], unique = true),
        Index(value = ["verificationToken"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val email: String,
    val passwordHash: String,
    val totalSessions: Int = 0,
    val currentLevel: Int = 1,
    val isEmailVerified: Boolean = false,
    val verificationToken: String? = null
)
