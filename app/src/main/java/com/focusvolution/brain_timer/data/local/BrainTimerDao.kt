package com.focusvolution.brain_timer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO central com operações para sessões e estado global.
 */
@Dao
interface BrainTimerDao {

    @Query("SELECT * FROM sessions ORDER BY timestamp DESC")
    fun observeSessions(): Flow<List<SessionEntity>>

    @Insert
    suspend fun insertSession(session: SessionEntity)

    @Query("SELECT * FROM app_state WHERE id = 0 LIMIT 1")
    fun observeAppState(): Flow<AppStateEntity?>

    @Query("SELECT * FROM app_state WHERE id = 0 LIMIT 1")
    suspend fun getAppState(): AppStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppState(appState: AppStateEntity)
}
