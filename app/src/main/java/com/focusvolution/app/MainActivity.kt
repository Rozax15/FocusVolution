package com.focusvolution.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.focusvolution.app.data.repository.FocusVolutionRepository
import com.focusvolution.app.ui.main.MainViewModel
import com.focusvolution.app.ui.navigation.AppRoute
import com.focusvolution.app.ui.navigation.AppNavHost
import com.focusvolution.app.ui.theme.FocusvolutionTheme
import kotlinx.coroutines.launch

/**
 * Activity principal com Compose e monitorização de ciclo de vida.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPostNotificationPermissionIfNeeded()

        val app = application as FocusVolutionApp
        val repository = FocusVolutionRepository(app.database)

        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> viewModel.onAppBackground()
                    else -> {}
                }
            }
        )

        handleVerificationLink(intent, repository)

        val startDestination = AppRoute.Login.route

        setContent {
            FocusvolutionTheme {
                AppNavHost(
                    viewModel = viewModel,
                    repository = repository,
                    startDestination = startDestination
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val app = application as FocusVolutionApp
        val repository = FocusVolutionRepository(app.database)
        handleVerificationLink(intent, repository)
    }

    private fun handleVerificationLink(intent: Intent?, repository: FocusVolutionRepository) {
        val uri = intent?.data ?: return
        if (uri.scheme != "focusvolution" || uri.host != "verify-email") return

        val token = uri.getQueryParameter("token")
        if (token.isNullOrBlank()) return

        lifecycleScope.launch {
            val success = repository.verifyEmail(token)
            val message = if (success) {
                "Email verificado com sucesso!"
            } else {
                "Link de verificação inválido ou expirado."
            }
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
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
