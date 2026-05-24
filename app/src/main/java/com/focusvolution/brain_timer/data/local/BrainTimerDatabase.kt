package com.focusvolution.brain_timer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Base de dados Room da app.
 */
@Database(
    entities = [SessionEntity::class, AppStateEntity::class, UserEntity::class],
    version = 4,
    exportSchema = false
)
abstract class BrainTimerDatabase : RoomDatabase() {

    abstract fun brainTimerDao(): BrainTimerDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: BrainTimerDatabase? = null

        /**
         * Migração da versão 2 para 3: adiciona coluna userId à tabela sessions, se não existir.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!columnExists(db, "sessions", "userId")) {
                    db.execSQL("ALTER TABLE sessions ADD COLUMN userId INTEGER NOT NULL DEFAULT -1")
                }
            }
        }

        /**
         * Migração da versão 3 para 4: adiciona colunas totalSessions e currentLevel à tabela users, se não existirem.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!columnExists(db, "users", "totalSessions")) {
                    db.execSQL("ALTER TABLE users ADD COLUMN totalSessions INTEGER NOT NULL DEFAULT 0")
                }
                if (!columnExists(db, "users", "currentLevel")) {
                    db.execSQL("ALTER TABLE users ADD COLUMN currentLevel INTEGER NOT NULL DEFAULT 1")
                }
            }
        }

        private fun columnExists(db: SupportSQLiteDatabase, table: String, column: String): Boolean {
            val cursor = db.query("PRAGMA table_info('$table')")
            return cursor.use { c ->
                while (c.moveToNext()) {
                    if (c.getString(1) == column) return@use true
                }
                false
            }
        }

        /**
         * Migração da versão 1 para 2: adiciona a tabela de utilizadores.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `users` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `username` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `passwordHash` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_users_email` ON `users` (`email`)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_users_username` ON `users` (`username`)"
                )
            }
        }

        fun getDatabase(context: Context): BrainTimerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BrainTimerDatabase::class.java,
                    "brain_timer_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
