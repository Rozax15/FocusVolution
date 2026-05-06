package com.focusvolution.brain_timer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.focusvolution.brain_timer.ui.screens.HistoryScreen
import com.focusvolution.brain_timer.ui.screens.MainScreen
import com.focusvolution.brain_timer.ui.main.MainViewModel

/**
 * Rotas da aplicação.
 */
sealed class AppRoute(val route: String) {
    data object Main : AppRoute("main")
    data object History : AppRoute("history")
}

@Composable
fun BrainTimerNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: MainViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = AppRoute.Main.route
    ) {
        composable(AppRoute.Main.route) {
            MainScreen(
                uiState = state,
                onDurationSelected = viewModel::onDurationSelected,
                onStartClick = viewModel::startOrResume,
                onPauseClick = viewModel::pause,
                onResetClick = viewModel::reset,
                onNavigateHistory = {
                    navController.navigate(AppRoute.History.route)
                }
            )
        }

        composable(AppRoute.History.route) {
            HistoryScreen(
                sessions = state.sessions,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
