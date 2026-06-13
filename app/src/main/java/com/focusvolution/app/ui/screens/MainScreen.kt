package com.focusvolution.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import com.focusvolution.app.ui.components.AppCharacter
import com.focusvolution.app.ui.components.LevelDownPopup
import com.focusvolution.app.ui.components.LevelUpPopup
import com.focusvolution.app.FocusVolutionApp
import com.focusvolution.app.ui.main.MainUiState
import kotlinx.coroutines.launch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    onDurationSelected: (Int) -> Unit,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResetClick: () -> Unit,
    onLogout: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSessionHistoryClick: () -> Unit = {}
) {
    var minutesInput by remember { mutableStateOf("") }
    var secondsInput by remember { mutableStateOf("") }

    val context = LocalContext.current
    val app = remember { context.applicationContext as FocusVolutionApp }
    val settings = remember { app.settingsManager }

    LaunchedEffect(Unit) {
        if (settings.rememberLastDuration) {
            val lastMins = settings.lastMinutes
            val lastSecs = settings.lastSeconds
            if (lastMins > 0 || lastSecs > 0) {
                minutesInput = lastMins.toString()
                secondsInput = lastSecs.toString()
            }
        } else if (settings.defaultDurationMinutes > 0 && minutesInput.isBlank() && secondsInput.isBlank()) {
            minutesInput = settings.defaultDurationMinutes.toString()
        }
    }

    var previousLevel by remember { mutableStateOf(uiState.currentLevel) }
    var showLevelUp by remember { mutableStateOf(false) }
    var showLevelDown by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.currentLevel) {
        if (uiState.currentLevel > previousLevel) {
            showLevelUp = true
        } else if (uiState.currentLevel < previousLevel) {
            showLevelDown = true
        }
        previousLevel = uiState.currentLevel
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
        TopAppBar(
            title = { Text("FOCUSVOLUTION") },
            actions = {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Definições"
                    )
                }
                IconButton(onClick = onProfileClick) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "O meu perfil"
                    )
                }
                IconButton(onClick = onLogout) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Terminar sessão"
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Define a duração da sessão",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = minutesInput,
                        onValueChange = { if (it.length <= 3) minutesInput = it },
                        label = { Text("Min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(100.dp),
                        enabled = !uiState.isRunning,
                        singleLine = true
                    )
                    Text(
                        text = "  :  ",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = secondsInput,
                        onValueChange = { if (it.length <= 2) secondsInput = it },
                        label = { Text("Seg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(100.dp),
                        enabled = !uiState.isRunning,
                        singleLine = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val warningActive = uiState.isRunning && uiState.remainingSeconds <= 5 && uiState.remainingSeconds > 0

        val timerColor = if (warningActive) {
            val transition = rememberInfiniteTransition(label = "pulse")
            val pulse by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseValue"
            )
            lerp(MaterialTheme.colorScheme.primary, Color.Red, pulse)
        } else {
            MaterialTheme.colorScheme.primary
        }

        Text(
            text = formatTime(uiState.remainingSeconds),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = timerColor,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        if (uiState.selectedDurationSeconds > 0) {
            val progress = (uiState.remainingSeconds.toFloat() / uiState.selectedDurationSeconds)
                .coerceIn(0f, 1f)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(8.dp),
                color = timerColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        AppCharacter(
            level = uiState.currentLevel,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                val mins = minutesInput.toIntOrNull() ?: 0
                val secs = (secondsInput.toIntOrNull() ?: 0).coerceIn(0, 59)
                val total = (mins * 60) + secs
                if (total > 0) {
                    onDurationSelected(total)
                    onStartClick()
                }
            }, enabled = !uiState.isRunning) {
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.92f else 1f,
                    animationSpec = spring(dampingRatio = 0.6f)
                )
                Box(
                    modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale),
                    contentAlignment = Alignment.Center
                ) { Text("Iniciar") }
            }
            Button(onClick = onPauseClick, enabled = uiState.isRunning) {
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.92f else 1f,
                    animationSpec = spring(dampingRatio = 0.6f)
                )
                Box(
                    modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale),
                    contentAlignment = Alignment.Center
                ) { Text("Pausar") }
            }
            Button(onClick = onResetClick) {
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.92f else 1f,
                    animationSpec = spring(dampingRatio = 0.6f)
                )
                Box(
                    modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale),
                    contentAlignment = Alignment.Center
                ) { Text("Reiniciar") }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.clickable(enabled = uiState.totalSessions > 0) { onSessionHistoryClick() }
            ) {
                Text(
                    text = "Sess\u00f5es conclu\u00eddas",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${uiState.totalSessions}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.totalSessions > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.tertiary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "N\u00edvel atual",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${uiState.currentLevel}/10",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        }

        AnimatedVisibility(
            visible = showLevelUp,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LevelUpPopup(
                level = uiState.currentLevel,
                onDismiss = { showLevelUp = false }
            )
        }

        AnimatedVisibility(
            visible = showLevelDown,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LevelDownPopup(
                level = uiState.currentLevel,
                onDismiss = { showLevelDown = false }
            )
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val minutes = safe / 60
    val seconds = safe % 60
    return String.format("%02d:%02d", minutes, seconds)
}