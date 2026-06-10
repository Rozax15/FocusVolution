package com.focusvolution.app.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.focusvolution.app.data.repository.FocusVolutionRepository
import com.focusvolution.app.ui.screens.AdminScreen
import com.focusvolution.app.ui.screens.AdminUserHistoryScreen
import com.focusvolution.app.ui.screens.LoginScreen
import com.focusvolution.app.ui.screens.MainScreen
import com.focusvolution.app.ui.screens.RegisterScreen
import com.focusvolution.app.ui.screens.SessionHistoryScreen
import com.focusvolution.app.ui.screens.VerifyCodeScreen
import com.focusvolution.app.ui.main.MainViewModel

sealed class AppRoute(val route: String) {
    data object Login    : AppRoute("login")
    data object Register : AppRoute("register")
    data object VerifyCode : AppRoute("verify_code")
    data object Main     : AppRoute("main")
    data object Admin    : AppRoute("admin")
    data object SessionHistory : AppRoute("session_history/{userId}") {
        fun createRoute(userId: Long) = "session_history/$userId"
    }
    data object AdminUserHistory : AppRoute("admin_user_history/{userId}") {
        fun createRoute(userId: Long) = "admin_user_history/$userId"
    }
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: MainViewModel = viewModel(),
    repository: FocusVolutionRepository,
    startDestination: String = AppRoute.Login.route
) {
    val state by viewModel.uiState.collectAsState()

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
    ) {
        composable(AppRoute.Login.route) {
            LoginScreen(
                repository = repository,
                onLoginSuccess = { userId ->
                    viewModel.currentUserId = userId
                    navController.navigate(AppRoute.Main.route) {
                        popUpTo(AppRoute.Login.route) { inclusive = true }
                    }
                },
                onNavigateRegister = {
                    navController.navigate(AppRoute.Register.route)
                },
                onAdminLogin = {
                    navController.navigate(AppRoute.Admin.route) {
                        popUpTo(AppRoute.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoute.Register.route) {
            RegisterScreen(
                repository = repository,
                onRegisterSuccess = {
                    navController.navigate(AppRoute.VerifyCode.route) {
                        popUpTo(AppRoute.Register.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(AppRoute.VerifyCode.route) {
            VerifyCodeScreen(
                repository = repository,
                onVerified = {
                    navController.navigate(AppRoute.Login.route) {
                        popUpTo(AppRoute.VerifyCode.route) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoute.Main.route) {
            MainScreen(
                uiState = state,
                onDurationSelected = viewModel::onDurationSelected,
                onStartClick = viewModel::startOrResume,
                onPauseClick = viewModel::pause,
                onResetClick = viewModel::reset,
                onLogout = {
                    viewModel.currentUserId = -1L
                    navController.navigate(AppRoute.Login.route) {
                        popUpTo(AppRoute.Main.route) { inclusive = true }
                    }
                },
                onSessionHistoryClick = {
                    navController.navigate(AppRoute.SessionHistory.createRoute(viewModel.currentUserId)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(AppRoute.Admin.route) {
            AdminScreen(
                repository = repository,
                onLogout = {
                    viewModel.currentUserId = -1L
                    navController.navigate(AppRoute.Login.route) {
                        popUpTo(AppRoute.Admin.route) { inclusive = true }
                    }
                },
                onUserClick = { userId ->
                    navController.navigate(AppRoute.AdminUserHistory.createRoute(userId))
                }
            )
        }

        composable(
            route = AppRoute.AdminUserHistory.route,
            arguments = listOf(navArgument("userId") { type = NavType.LongType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getLong("userId") ?: return@composable
            AdminUserHistoryScreen(
                repository = repository,
                userId = userId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = AppRoute.SessionHistory.route,
            arguments = listOf(navArgument("userId") { type = NavType.LongType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getLong("userId") ?: return@composable
            SessionHistoryScreen(
                repository = repository,
                userId = userId,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
