-- ============================================================
-- FOCUSVOLUTION — Schema da Base de Dados (SQLite / Room)
-- Versão: 6
-- ============================================================

-- Tabela: users
-- Armazena os utilizadores registados (incluindo conta admin).
-- A password é guardada como hash SHA-256.
CREATE TABLE IF NOT EXISTS `users` (
    `id`                INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `username`          TEXT NOT NULL,
    `email`             TEXT NOT NULL,
    `passwordHash`      TEXT NOT NULL,
    `totalSessions`     INTEGER NOT NULL DEFAULT 0,
    `currentLevel`      INTEGER NOT NULL DEFAULT 1,
    `isEmailVerified`   INTEGER NOT NULL DEFAULT 0,   -- 0 = false, 1 = true
    `verificationToken` TEXT DEFAULT NULL
);

-- Índices únicos
CREATE UNIQUE INDEX IF NOT EXISTS `index_users_email`               ON `users` (`email`);
CREATE UNIQUE INDEX IF NOT EXISTS `index_users_username`            ON `users` (`username`);
CREATE UNIQUE INDEX IF NOT EXISTS `index_users_verificationToken`   ON `users` (`verificationToken`);

-- Tabela: sessions
-- Cada linha representa uma sessão de foco concluída.
-- A coluna `tag` permite categorizar sessões (ex: "Estudo", "Trabalho").
CREATE TABLE IF NOT EXISTS `sessions` (
    `id`        INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `userId`    INTEGER NOT NULL DEFAULT -1,          -- -1 = modo convidado
    `timestamp` INTEGER NOT NULL,                      -- epoch millis
    `duration`  INTEGER NOT NULL,                      -- segundos
    `tag`       TEXT DEFAULT NULL
);

-- Tabela: app_state
-- Estado global usado apenas em modo convidado (userId = -1).
-- Contém sempre uma única linha com id = 0.
CREATE TABLE IF NOT EXISTS `app_state` (
    `id`             INTEGER PRIMARY KEY NOT NULL DEFAULT 0,
    `totalSessions`  INTEGER NOT NULL,
    `currentLevel`   INTEGER NOT NULL
);
