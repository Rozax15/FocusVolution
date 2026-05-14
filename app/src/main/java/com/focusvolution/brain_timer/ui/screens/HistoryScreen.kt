package com.focusvolution.brain_timer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.focusvolution.brain_timer.data.local.SessionEntity
import com.focusvolution.brain_timer.data.repository.BrainTimerRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tela de histórico com data/hora das sessões concluídas do utilizador atual.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    repository: BrainTimerRepository,
    userId: Long,
    onBackClick: () -> Unit
) {
    val sessions by repository.observeSessionsByUserId(userId).collectAsState(initial = emptyList())
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Histórico") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
            }
        )

        if (sessions.isEmpty()) {
            Text(
                text = "Ainda não há sessões concluídas.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sessions, key = { it.id }) { session ->
                    SessionHistoryItem(session = session)
                }
            }
        }
    }
}

@Composable
private fun SessionHistoryItem(session: SessionEntity) {
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val dateText = remember(session.timestamp) { formatter.format(Date(session.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = dateText, fontWeight = FontWeight.SemiBold)
                Text(text = "Duração: ${session.duration / 60} min")
            }
        }
    }
}
