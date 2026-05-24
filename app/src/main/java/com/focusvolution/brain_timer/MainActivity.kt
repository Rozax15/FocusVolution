package com.focusvolution.brain_timer

import android.Manifest
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

        val startDestination = AppRoute.Login.route

        setContent {
            FocusvolutionTheme {
                BrainTimerNavHost(
                    viewModel = viewModel,
                    repository = repository,
                    startDestination = startDestination
                )
            }
        }
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
