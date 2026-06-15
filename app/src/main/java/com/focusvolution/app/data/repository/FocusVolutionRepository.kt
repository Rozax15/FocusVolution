package com.focusvolution.app.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.focusvolution.app.FocusVolutionApp
import com.focusvolution.app.data.local.AppStateEntity
import com.focusvolution.app.data.local.AppDatabase
import com.focusvolution.app.data.local.SessionEntity
import com.focusvolution.app.data.local.UserEntity
import com.focusvolution.app.email.EmailService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

/**
 * Camada de acesso a dados (Repository).
 *
 * Centraliza regras de persistência e cálculo de nível.
 */
/**
 * Resultado de uma operação de autenticação.
 */
sealed class AuthResult {
    data class Success(val user: UserEntity?) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class FocusVolutionRepository(
    private val database: AppDatabase
) {
    private val dao = database.appDao()
    private val userDao = database.userDao()

    fun observeUser(userId: Long): Flow<UserEntity?> = userDao.observeUser(userId)

    // ─── Admin ───────────────────────────────────────────────────────────

    fun observeAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()

    fun observeSessionsByUserId(userId: Long): Flow<List<SessionEntity>> =
        dao.observeSessionsByUserId(userId)

    fun observeSessionsByUserIdAndTag(userId: Long, tag: String?): Flow<List<SessionEntity>> =
        dao.observeSessionsByUserIdAndTag(userId, tag)

    fun observeTotalFocusSeconds(userId: Long): Flow<Int> =
        dao.observeTotalFocusSeconds(userId)

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
    suspend fun recordCompletedSession(durationSeconds: Int, userId: Long = -1, tag: String? = null) {
        database.withTransaction {
            val user = if (userId != -1L) userDao.getUserById(userId) else null
            val previousTotal = user?.totalSessions ?: 0
            val newTotal = previousTotal + 1
            val calculatedLevel = ((newTotal / 5) + 1).coerceAtMost(10)

            dao.insertSession(
                SessionEntity(
                    timestamp = System.currentTimeMillis(),
                    duration = durationSeconds,
                    userId = userId,
                    tag = tag
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
    suspend fun completeSession(durationSeconds: Int, userId: Long = -1, tag: String? = null) {
        recordCompletedSession(durationSeconds, userId, tag)
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

    // ─── Autenticação ────────────────────────────────────────────────────────

    /**
     * Regista um novo utilizador de forma pendente.
     * A conta só é criada na BD quando o email for verificado.
     */
    suspend fun register(username: String, email: String, password: String): AuthResult {
        if (username.isBlank()) return AuthResult.Error("O nome de utilizador não pode estar vazio.")
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return AuthResult.Error("Endereço de email inválido.")
        }
        if (password.length < 6) return AuthResult.Error("A palavra-passe deve ter pelo menos 6 caracteres.")

        val trimmedUsername = username.trim()
        val trimmedEmail = email.trim().lowercase()

        if (userDao.countByEmail(trimmedEmail) > 0) {
            return AuthResult.Error("Já existe uma conta com este email.")
        }
        if (userDao.countByUsername(trimmedUsername) > 0) {
            return AuthResult.Error("Este nome de utilizador já está em uso.")
        }

        if (isEmailPending(trimmedEmail)) {
            return AuthResult.Error("Já existe um registo pendente com este email. Verifica a tua caixa de entrada.")
        }
        if (isUsernamePending(trimmedUsername)) {
            return AuthResult.Error("Este nome de utilizador já está em uso num registo pendente.")
        }

        val verificationToken = UUID.randomUUID().toString()
        val verificationCode = (100000..999999).random().toString()
        val passwordHash = hashPassword(password)

        savePendingRegistration(
            token = verificationToken,
            code = verificationCode,
            username = trimmedUsername,
            email = trimmedEmail,
            passwordHash = passwordHash
        )

        val emailResult = withContext(Dispatchers.IO) {
            EmailService.sendVerificationEmail(
                toEmail = trimmedEmail,
                username = trimmedUsername,
                verificationCode = verificationCode
            )
        }
        return if (emailResult.isSuccess) {
            AuthResult.Success(null)
        } else {
            removePendingRegistration(verificationToken)
            android.util.Log.e("FocusVolutionRepository", "Erro ao enviar email", emailResult.exceptionOrNull())
            AuthResult.Error("Erro ao enviar email de verificação. Tenta novamente.")
        }
    }

    /**
     * Verifica o email através do token e só então cria a conta na BD.
     */
    suspend fun verifyEmail(token: String): Boolean {
        val pending = getPendingRegistration(token) ?: return false
        return createUserFromPending(token, pending)
    }

    /**
     * Verifica o email através do código de 6 dígitos.
     */
    suspend fun verifyEmailWithCode(code: String): Boolean {
        val (token, pending) = findPendingByCode(code) ?: return false
        return createUserFromPending(token, pending)
    }

    private suspend fun createUserFromPending(token: String, pending: PendingRegistration): Boolean {
        if (System.currentTimeMillis() - pending.timestamp > 24 * 60 * 60 * 1000) {
            removePendingRegistration(token)
            return false
        }

        if (userDao.countByEmail(pending.email) > 0 || userDao.countByUsername(pending.username) > 0) {
            removePendingRegistration(token)
            return false
        }

        return try {
            userDao.insertUser(
                UserEntity(
                    username = pending.username,
                    email = pending.email,
                    passwordHash = pending.passwordHash,
                    isEmailVerified = true
                )
            )
            removePendingRegistration(token)
            true
        } catch (_: Exception) {
            false
        }
    }

    // ─── Registos pendentes (SharedPreferences) ───────────────────────────

    private data class PendingRegistration(
        val username: String,
        val email: String,
        val passwordHash: String,
        val verificationCode: String,
        val timestamp: Long
    )

    private val pendingPrefs by lazy {
        FocusVolutionApp.instance.getSharedPreferences("pending_registrations", Context.MODE_PRIVATE)
    }

    private fun savePendingRegistration(token: String, code: String, username: String, email: String, passwordHash: String) {
        val json = JSONObject().apply {
            put("username", username)
            put("email", email)
            put("passwordHash", passwordHash)
            put("verificationCode", code)
            put("timestamp", System.currentTimeMillis())
        }
        pendingPrefs.edit().putString("pending_$token", json.toString()).apply()
    }

    private fun getPendingRegistration(token: String): PendingRegistration? {
        val jsonStr = pendingPrefs.getString("pending_$token", null) ?: return null
        return parsePendingJson(jsonStr)
    }

    private fun findPendingByCode(code: String): Pair<String, PendingRegistration>? {
        val all = pendingPrefs.all
        for ((key, value) in all) {
            if (key.startsWith("pending_") && value is String) {
                val pending = parsePendingJson(value) ?: continue
                if (pending.verificationCode == code) {
                    return key.removePrefix("pending_") to pending
                }
            }
        }
        return null
    }

    private fun parsePendingJson(jsonStr: String): PendingRegistration? {
        return try {
            val json = JSONObject(jsonStr)
            PendingRegistration(
                username = json.getString("username"),
                email = json.getString("email"),
                passwordHash = json.getString("passwordHash"),
                verificationCode = json.getString("verificationCode"),
                timestamp = json.getLong("timestamp")
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun removePendingRegistration(token: String) {
        pendingPrefs.edit().remove("pending_$token").apply()
    }

    /**
     * Remove registos pendentes com mais de 24h.
     */
    fun cleanupExpiredPendingRegistrations() {
        val all = pendingPrefs.all
        val now = System.currentTimeMillis()
        for ((key, value) in all) {
            if (key.startsWith("pending_") && value is String) {
                try {
                    val json = JSONObject(value)
                    val ts = json.getLong("timestamp")
                    if (now - ts > 24 * 60 * 60 * 1000) {
                        pendingPrefs.edit().remove(key).apply()
                    }
                } catch (_: Exception) { }
            }
        }
    }

    private fun isEmailPending(email: String): Boolean {
        val all = pendingPrefs.all
        return all.any { (key, value) ->
            key.startsWith("pending_") && value is String && try {
                JSONObject(value).getString("email") == email
            } catch (_: Exception) { false }
        }
    }

    private fun isUsernamePending(username: String): Boolean {
        val all = pendingPrefs.all
        return all.any { (key, value) ->
            key.startsWith("pending_") && value is String && try {
                JSONObject(value).getString("username") == username
            } catch (_: Exception) { false }
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
     * Atualiza o username de um utilizador.
     */
    suspend fun updateUsername(userId: Long, newUsername: String): AuthResult {
        val trimmed = newUsername.trim()
        if (trimmed.isBlank()) return AuthResult.Error("O nome de utilizador não pode estar vazio.")
        if (userDao.countByUsername(trimmed) > 0) {
            return AuthResult.Error("Este nome de utilizador já está em uso.")
        }
        userDao.updateUsername(userId, trimmed)
        return AuthResult.Success(null)
    }

    /**
     * Atualiza a palavra-passe de um utilizador.
     */
    suspend fun updatePassword(userId: Long, currentPassword: String, newPassword: String): AuthResult {
        val user = userDao.getUserById(userId) ?: return AuthResult.Error("Utilizador não encontrado.")
        if (user.passwordHash != hashPassword(currentPassword)) {
            return AuthResult.Error("Palavra-passe atual incorreta.")
        }
        if (newPassword.length < 6) {
            return AuthResult.Error("A nova palavra-passe deve ter pelo menos 6 caracteres.")
        }
        userDao.updatePassword(userId, hashPassword(newPassword))
        return AuthResult.Success(null)
    }

    /**
     * Gera um hash SHA-256 da palavra-passe.
     */
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
