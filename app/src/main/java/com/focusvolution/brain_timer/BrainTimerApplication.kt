package com.focusvolution.brain_timer

import android.app.Application
import com.focusvolution.brain_timer.data.local.BrainTimerDatabase

/**
 * Application principal.
 *
 * Deixamos a base de dados inicializada de forma lazy para ser reutilizada
 * tanto pelo ViewModel quanto pelo serviço em foreground.
 */
class BrainTimerApplication : Application() {

    val database: BrainTimerDatabase by lazy {
        BrainTimerDatabase.getDatabase(this)
    }
}
