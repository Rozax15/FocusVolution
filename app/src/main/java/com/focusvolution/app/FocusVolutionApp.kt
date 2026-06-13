package com.focusvolution.app

import android.app.Application
import com.focusvolution.app.data.local.AppDatabase
import com.focusvolution.app.data.repository.FocusVolutionRepository
import com.focusvolution.app.email.EmailConfig

/**
 * Application principal.
 *
 * Deixamos a base de dados inicializada de forma lazy para ser reutilizada
 * tanto pelo ViewModel quanto pelo serviço em foreground.
 */
class FocusVolutionApp : Application() {

    companion object {
        lateinit var instance: FocusVolutionApp
            private set
    }

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    val settingsManager: SettingsManager by lazy {
        SettingsManager(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        val repo = FocusVolutionRepository(database)
        repo.cleanupExpiredPendingRegistrations()

        // ─── CONFIGURAÇÃO SMTP (GMAIL) ───────────────────────────────────
        // 1. Cria/usa uma conta Gmail
        // 2. Ativa 2FA: https://myaccount.google.com/security
        // 3. Gera App Password: https://myaccount.google.com/apppasswords
        // 4. Substitui os valores abaixo com as tuas credenciais
        //
        EmailConfig.smtpUsername = "focusvolution.verifica@gmail.com"
        EmailConfig.smtpPassword = "fvom dxpr pcxh pufm"
        EmailConfig.fromEmail    = "focusvolution.verifica@gmail.com"
        // ──────────────────────────────────────────────────────────────────
    }
}
