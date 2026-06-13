package com.focusvolution.app.data.local

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
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

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

        /**
         * Migração da versão 4 para 5: adiciona colunas isEmailVerified e verificationToken à tabela users.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!columnExists(db, "users", "isEmailVerified")) {
                    db.execSQL("ALTER TABLE users ADD COLUMN isEmailVerified INTEGER NOT NULL DEFAULT 0")
                }
                if (!columnExists(db, "users", "verificationToken")) {
                    db.execSQL("ALTER TABLE users ADD COLUMN verificationToken TEXT DEFAULT NULL")
                }
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_verificationToken` ON `users` (`verificationToken`)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!columnExists(db, "sessions", "tag")) {
                    db.execSQL("ALTER TABLE sessions ADD COLUMN tag TEXT DEFAULT NULL")
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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "focusvolution_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
