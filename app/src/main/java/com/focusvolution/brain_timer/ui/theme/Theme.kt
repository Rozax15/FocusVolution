package com.focusvolution.brain_timer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta escura (azuis profundos coesos com o tema)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color.White,
    secondary = Color(0xFF60A5FA),
    onSecondary = Color(0xFF0C1628),
    background = Color(0xFF080E1E),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF111B2E),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1A2744),
    onSurfaceVariant = Color(0xFF94A3B8),
    tertiary = Color(0xFF64748B)
)

// Paleta clara (cores invertidas)
private val LightColors = lightColorScheme(
    primary = Color(0xFF1E3A8A),
    onPrimary = Color.White,
    secondary = Color(0xFF3B82F6),
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    tertiary = Color(0xFF64748B)
)

@Composable
fun FocusvolutionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}