package com.focusvolution.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO central com operações para sessões e estado global.
 */
@Dao
interface AppDao {

    @Query("SELECT * FROM sessions WHERE userId = :userId ORDER BY timestamp DESC")
    fun observeSessionsByUserId(userId: Long): Flow<List<SessionEntity>>

    @Query("DELETE FROM sessions WHERE userId = :userId")
    suspend fun deleteSessionsByUserId(userId: Long)

    @Insert
    suspend fun insertSession(session: SessionEntity)

    @Query("SELECT * FROM app_state WHERE id = 0 LIMIT 1")
    suspend fun getAppState(): AppStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppState(appState: AppStateEntity)
}
