package com.focusvolution.brain_timer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta focada no contraste entre o Azul Escuro e o Branco
private val DarkColors = darkColorScheme(
    primary = Color(0xFF1E3A8A),      // O teu azul #1e3a8a
    onPrimary = Color.White,          // Texto branco sobre o azul

    secondary = Color.White,          // Branco como cor secundária para destaque
    onSecondary = Color(0xFF1E3A8A),  // Azul sobre o branco

    background = Color(0xFF0F172A),   // Fundo quase preto (Slate)
    onBackground = Color.White,       // Todo o texto principal em Branco

    surface = Color(0xFF1E293B),      // Cards num azul levemente mais claro
    onSurface = Color.White,          // Texto nos cards em Branco

    tertiary = Color(0xFF94A3B8)      // Cinza azulado para elementos menos importantes
)

@Composable
fun FocusvolutionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}