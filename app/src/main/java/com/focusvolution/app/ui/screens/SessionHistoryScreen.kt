package com.focusvolution.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.focusvolution.app.data.local.SessionEntity
import com.focusvolution.app.data.repository.FocusVolutionRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryScreen(
    repository: FocusVolutionRepository,
    userId: Long,
    onBackClick: () -> Unit
) {
    var filterTag by remember { mutableStateOf<String?>(null) }

    val sessions by repository.observeSessionsByUserId(userId).collectAsState(initial = emptyList())
    val allTags = remember(sessions) { sessions.mapNotNull { it.tag }.distinct().sorted() }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Histórico de Sessões") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
            }
        )

        if (allTags.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterTag == null,
                    onClick = { filterTag = null },
                    label = { Text("Todas") }
                )
                allTags.forEach { tag ->
                    FilterChip(
                        selected = filterTag == tag,
                        onClick = { filterTag = tag },
                        label = { Text(tag) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        val filteredSessions = if (filterTag == null) sessions else sessions.filter { it.tag == filterTag }

        if (filteredSessions.isEmpty()) {
            Text(
                text = "Ainda não tens sessões concluídas.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(filteredSessions, key = { _, s -> s.id }) { index, session ->
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(initialOffsetY = { it / 2 }) +
                                fadeIn(animationSpec = androidx.compose.animation.core.tween(
                                    delayMillis = index * 80
                                ))
                    ) {
                        SessionItem(session = session)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionItem(session: SessionEntity) {
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
                Text(
                    text = formatSessionDuration(session.duration),
                    color = MaterialTheme.colorScheme.tertiary
                )
                if (session.tag != null) {
                    Text(
                        text = session.tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun formatSessionDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return when {
        minutes > 0 && seconds > 0 -> "${minutes}min ${seconds}seg"
        minutes > 0 -> "${minutes}min"
        else -> "${seconds}seg"
    }
}
