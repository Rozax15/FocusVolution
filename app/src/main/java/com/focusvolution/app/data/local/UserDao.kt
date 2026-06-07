package com.focusvolution.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operações de autenticação de utilizadores.
 */
@Dao
interface UserDao {

    /**
     * Insere um novo utilizador. Falha se o email ou username já existirem.
     * Retorna o ID gerado, ou -1 em caso de conflito.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    /**
     * Procura um utilizador pelo email (para login com email).
     */
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    /**
     * Procura um utilizador pelo nome de utilizador (para login com username).
     */
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    /**
     * Procura um utilizador pelo ID (para restaurar sessão ativa).
     */
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    /**
     * Verifica se já existe um utilizador com o email fornecido.
     */
    @Query("SELECT COUNT(*) FROM users WHERE email = :email")
    suspend fun countByEmail(email: String): Int

    /**
     * Verifica se já existe um utilizador com o username fornecido.
     */
    @Query("SELECT COUNT(*) FROM users WHERE username = :username")
    suspend fun countByUsername(username: String): Int

    /**
     * Retorna todos os utilizadores registados (para admin).
     */
    @Query("SELECT * FROM users ORDER BY username ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    /**
     * Elimina um utilizador pelo ID (para admin).
     */
    @Delete
    suspend fun deleteUser(user: UserEntity)

    /**
     * Observa um utilizador pelo ID (Flow para reagir a alterações).
     */
    @Query("SELECT * FROM users WHERE id = :id")
    fun observeUser(id: Long): Flow<UserEntity?>

    /**
     * Atualiza o total de sessões e nível de um utilizador.
     */
    @Query("UPDATE users SET totalSessions = :totalSessions, currentLevel = :currentLevel WHERE id = :id")
    suspend fun updateUserStats(id: Long, totalSessions: Int, currentLevel: Int)

    /**
     * Marca o email de um utilizador como verificado.
     */
    @Query("UPDATE users SET isEmailVerified = 1, verificationToken = NULL WHERE id = :id")
    suspend fun markEmailVerified(id: Long)

    /**
     * Procura um utilizador pelo token de verificação.
     */
    @Query("SELECT * FROM users WHERE verificationToken = :token LIMIT 1")
    suspend fun getUserByVerificationToken(token: String): UserEntity?

    /**
     * Define o token de verificação de um utilizador.
     */
    @Query("UPDATE users SET verificationToken = :token WHERE id = :id")
    suspend fun setVerificationToken(id: Long, token: String)
}
