package com.focusvolution.app.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusvolution.app.data.repository.FocusVolutionRepository
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    repository: FocusVolutionRepository,
    userId: Long,
    onBackClick: () -> Unit
) {
    val sessions by repository.observeSessionsByUserId(userId).collectAsState(initial = emptyList())

    val days = remember(sessions) {
        val dayNames = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
        val totals = IntArray(7)
        val calendar = Calendar.getInstance()
        for (s in sessions) {
            calendar.timeInMillis = s.timestamp
            val day = calendar.get(Calendar.DAY_OF_WEEK) - 1
            totals[day] += s.duration
        }
        dayNames.mapIndexed { i, name ->
            DayData(name, totals[i])
        }
    }

    val maxMinutes = remember(days) { days.maxOfOrNull { it.totalMinutes } ?: 1 }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Gráfico de Sessões") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar"
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Tempo focado por dia da semana",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (sessions.isEmpty()) {
                Text(
                    text = "Ainda não tens sessões para mostrar.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        BarChart(
                            data = days,
                            maxMinutes = maxMinutes,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        days.forEach { day ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = day.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatMinutes(day.totalMinutes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class DayData(
    val name: String,
    val totalSeconds: Int
) {
    val totalMinutes get() = totalSeconds / 60
}

private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}min"
        h > 0 -> "${h}h"
        else -> "${m}min"
    }
}

@Composable
private fun BarChart(
    data: List<DayData>,
    maxMinutes: Int,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val gridColor = remember { Color.Gray.copy(alpha = 0.2f) }

    Canvas(modifier = modifier) {
        val barCount = data.size
        val totalWidth = size.width
        val totalHeight = size.height
        val barSpacing = totalWidth / barCount
        val barWidth = barSpacing * 0.55f
        val chartBottom = totalHeight - 24f

        // Grid lines
        for (i in 0..4) {
            val y = chartBottom - (chartBottom - 8f) * (i / 4f)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(totalWidth, y),
                strokeWidth = 1f
            )
        }

        val textPaint = android.graphics.Paint().apply {
            color = onSurface.hashCode()
            textSize = 28f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }

        data.forEachIndexed { i, day ->
            val barHeight = if (maxMinutes > 0) {
                (day.totalMinutes.toFloat() / maxMinutes) * (chartBottom - 8f)
            } else 0f

            val centerX = i * barSpacing + barSpacing / 2f
            val x = centerX - barWidth / 2f
            val y = chartBottom - barHeight

            drawRoundRect(
                color = primary,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )

            drawContext.canvas.nativeCanvas.drawText(
                day.name,
                centerX,
                totalHeight - 2f,
                textPaint
            )
        }
    }
}
