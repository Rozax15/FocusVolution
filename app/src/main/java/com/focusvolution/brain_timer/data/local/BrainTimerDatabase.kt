package com.focusvolution.brain_timer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de dados Room da app.
 */
@Database(
    entities = [SessionEntity::class, AppStateEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BrainTimerDatabase : RoomDatabase() {

    abstract fun brainTimerDao(): BrainTimerDao

    companion object {
        @Volatile
        private var INSTANCE: BrainTimerDatabase? = null

        fun getDatabase(context: Context): BrainTimerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BrainTimerDatabase::class.java,
                    "brain_timer_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
