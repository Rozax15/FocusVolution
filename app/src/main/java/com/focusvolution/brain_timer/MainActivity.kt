package com.focusvolution.brain_timer

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.focusvolution.brain_timer.data.repository.BrainTimerRepository
import com.focusvolution.brain_timer.ui.main.MainViewModel
import com.focusvolution.brain_timer.ui.navigation.AppRoute
import com.focusvolution.brain_timer.ui.navigation.BrainTimerNavHost
import com.focusvolution.brain_timer.ui.theme.FocusvolutionTheme

private const val PREFS_NAME = "focusvolution_session"
private const val KEY_LOGGED_IN_USER_ID = "logged_in_user_id"
private const val NO_USER = -1L

/**
 * Activity principal com Compose e monitorização de ciclo de vida.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pede permissão de notificações se for Android 13+
        requestPostNotificationPermissionIfNeeded()

        // Observa se a app vai para background ou foreground para detetar saídas durante a sessão
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> viewModel.onAppBackground()
                    Lifecycle.Event.ON_START -> viewModel.onAppForeground()
                    else -> {}
                }
            }
        )

        val app = application as BrainTimerApplication
        val repository = BrainTimerRepository(app.database)

        // Verifica se existe uma sessão ativa guardada
        val savedUserId = getSavedUserId()
        if (savedUserId != NO_USER) {
            viewModel.currentUserId = savedUserId
        }
        val startDestination = if (savedUserId != NO_USER) {
            AppRoute.Main.route
        } else {
            AppRoute.Login.route
        }

        setContent {
            FocusvolutionTheme {
                BrainTimerNavHost(
                    viewModel = viewModel,
                    repository = repository,
                    startDestination = startDestination,
                    onSaveSession = { userId -> saveUserId(userId) },
                    onClearSession = { clearUserId() }
                )
            }
        }
    }

    // ─── Gestão de sessão com SharedPreferences ───────────────────────────────

    private fun getSavedUserId(): Long {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LOGGED_IN_USER_ID, NO_USER)
    }

    private fun saveUserId(userId: Long) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LOGGED_IN_USER_ID, userId)
            .apply()
    }

    private fun clearUserId() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_LOGGED_IN_USER_ID)
            .apply()
    }

    private fun requestPostNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }
    }
}
