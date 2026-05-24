package com.focusvolution.brain_timer.data.repository

import androidx.room.withTransaction
import com.focusvolution.brain_timer.data.local.AppStateEntity
import com.focusvolution.brain_timer.data.local.BrainTimerDatabase
import com.focusvolution.brain_timer.data.local.SessionEntity
import com.focusvolution.brain_timer.data.local.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

/**
 * Camada de acesso a dados (Repository).
 *
 * Centraliza regras de persistência e cálculo de nível.
 */
/**
 * Resultado de uma operação de autenticação.
 */
sealed class AuthResult {
    data class Success(val user: UserEntity) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class BrainTimerRepository(
    private val database: BrainTimerDatabase
) {
    private val dao = database.brainTimerDao()
    private val userDao = database.userDao()

    val sessionsFlow: Flow<List<SessionEntity>> = dao.observeSessions()

    val appStateFlow: Flow<AppStateEntity> = dao.observeAppState().map { current ->
        current ?: AppStateEntity(totalSessions = 0, currentLevel = 1)
    }

    fun observeUser(userId: Long): Flow<UserEntity?> = userDao.observeUser(userId)

    // ─── Admin ───────────────────────────────────────────────────────────

    fun observeAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()

    suspend fun deleteUser(user: UserEntity) = userDao.deleteUser(user)

    fun observeSessionsByUserId(userId: Long): Flow<List<SessionEntity>> =
        dao.observeSessionsByUserId(userId)

    suspend fun deleteUserAndSessions(user: UserEntity) {
        database.withTransaction {
            dao.deleteSessionsByUserId(user.id)
            userDao.deleteUser(user)
        }
    }

    // ──────────────────────────────────────────────────────────────────────

    suspend fun ensureInitialState() {
        if (dao.getAppState() == null) {
            dao.upsertAppState(AppStateEntity(totalSessions = 0, currentLevel = 1))
        }
    }

    /**
     * Regista sessão concluída e atualiza total/nível do utilizador de forma atómica.
     */
    suspend fun recordCompletedSession(durationSeconds: Int, userId: Long = -1) {
        database.withTransaction {
            val user = if (userId != -1L) userDao.getUserById(userId) else null
            val previousTotal = user?.totalSessions ?: 0
            val newTotal = previousTotal + 1
            val calculatedLevel = ((newTotal / 5) + 1).coerceAtMost(10)

            dao.insertSession(
                SessionEntity(
                    timestamp = System.currentTimeMillis(),
                    duration = durationSeconds,
                    userId = userId
                )
            )

            if (user != null) {
                userDao.updateUserStats(user.id, newTotal, calculatedLevel)
            } else {
                val previous = dao.getAppState() ?: AppStateEntity(totalSessions = 0, currentLevel = 1)
                dao.upsertAppState(
                    previous.copy(
                        totalSessions = newTotal,
                        currentLevel = calculatedLevel
                    )
                )
            }
        }
    }
    /**
     * Alias para registar sessão concluída com sucesso (sem saída da app).
     */
    suspend fun completeSession(durationSeconds: Int, userId: Long = -1) {
        recordCompletedSession(durationSeconds, userId)
    }

    /**
     * Penalização: desce o total de sessões concluídas em 1 e recalcula o nível.
     */
    suspend fun penalizeUser(userId: Long = -1) {
        database.withTransaction {
            if (userId != -1L) {
                val user = userDao.getUserById(userId) ?: return@withTransaction
                val newTotal = (user.totalSessions - 1).coerceAtLeast(0)
                val newLevel = ((newTotal / 5) + 1).coerceAtMost(10)
                userDao.updateUserStats(userId, newTotal, newLevel)
            } else {
                val previous = dao.getAppState() ?: AppStateEntity(totalSessions = 0, currentLevel = 1)
                val newTotal = (previous.totalSessions - 1).coerceAtLeast(0)
                val newLevel = ((newTotal / 5) + 1).coerceAtMost(10)
                dao.upsertAppState(previous.copy(totalSessions = newTotal, currentLevel = newLevel))
            }
        }
    }

    /**
     * Penalização: desce um nível do utilizador (mínimo nível 1).
     */
    suspend fun decrementLevel(userId: Long = -1) {
        database.withTransaction {
            if (userId != -1L) {
                val user = userDao.getUserById(userId) ?: return@withTransaction
                val newLevel = (user.currentLevel - 1).coerceAtLeast(1)
                userDao.updateUserStats(userId, user.totalSessions, newLevel)
            } else {
                val previous = dao.getAppState() ?: AppStateEntity(totalSessions = 0, currentLevel = 1)
                val newLevel = (previous.currentLevel - 1).coerceAtLeast(1)
                dao.upsertAppState(previous.copy(currentLevel = newLevel))
            }
        }
    }

    // ─── Autenticação ────────────────────────────────────────────────────────

    /**
     * Regista um novo utilizador.
     * Valida unicidade de email e username antes de inserir.
     */
    suspend fun register(username: String, email: String, password: String): AuthResult {
        if (username.isBlank()) return AuthResult.Error("O nome de utilizador não pode estar vazio.")
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return AuthResult.Error("Endereço de email inválido.")
        }
        if (password.length < 6) return AuthResult.Error("A palavra-passe deve ter pelo menos 6 caracteres.")

        if (userDao.countByEmail(email.trim().lowercase()) > 0) {
            return AuthResult.Error("Já existe uma conta com este email.")
        }
        if (userDao.countByUsername(username.trim()) > 0) {
            return AuthResult.Error("Este nome de utilizador já está em uso.")
        }

        val user = UserEntity(
            username = username.trim(),
            email = email.trim().lowercase(),
            passwordHash = hashPassword(password)
        )
        return try {
            val id = userDao.insertUser(user)
            AuthResult.Success(user.copy(id = id))
        } catch (e: Exception) {
            AuthResult.Error("Erro ao criar conta. Tenta novamente.")
        }
    }

    /**
     * Autentica um utilizador com email ou nome de utilizador + palavra-passe.
     */
    suspend fun login(emailOrUsername: String, password: String): AuthResult {
        if (emailOrUsername.isBlank()) return AuthResult.Error("Introduz o email ou nome de utilizador.")
        if (password.isBlank()) return AuthResult.Error("Introduz a palavra-passe.")

        val identifier = emailOrUsername.trim()
        val user = if (identifier.contains("@")) {
            userDao.getUserByEmail(identifier.lowercase())
        } else {
            userDao.getUserByUsername(identifier)
        }

        if (user == null) return AuthResult.Error("Utilizador não encontrado.")
        if (user.passwordHash != hashPassword(password)) {
            return AuthResult.Error("Palavra-passe incorreta.")
        }
        return AuthResult.Success(user)
    }

    /**
     * Obtém um utilizador pelo ID (para restaurar sessão ativa).
     */
    suspend fun getUserById(id: Long): UserEntity? = userDao.getUserById(id)

    /**
     * Gera um hash SHA-256 da palavra-passe.
     */
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
