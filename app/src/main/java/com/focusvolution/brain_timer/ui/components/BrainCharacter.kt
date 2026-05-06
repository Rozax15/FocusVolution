package com.focusvolution.brain_timer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Personagem cérebro simplificado em Compose.
 *
 * A cada nível aumentamos:
 * - intensidade da cor
 * - espessura das ligações neurais
 * - número de "nós" desenhados
 */
@Composable
fun BrainCharacter(
    level: Int,
    modifier: Modifier = Modifier
) {
    val safeLevel = level.coerceIn(1, 10)
    val progress = (safeLevel - 1) / 9f

    val startColor = Color(0xFFB39DDB)
    val endColor = Color(0xFF6A1B9A)
    val brainColor = lerpColor(startColor, endColor, progress)
    val strokeWidth = 4f + progress * 8f

    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Formato base do cérebro (oval arredondado).
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(brainColor.copy(alpha = 0.85f), brainColor)
                ),
                topLeft = Offset(width * 0.15f, height * 0.2f),
                size = Size(width * 0.7f, height * 0.62f),
                cornerRadius = CornerRadius(width * 0.35f, width * 0.35f)
            )

            // Sulcos/linhas internas que ficam mais espessos com o nível.
            val lineColor = Color.White.copy(alpha = 0.45f + progress * 0.3f)
            drawLine(
                color = lineColor,
                start = Offset(width * 0.35f, height * 0.3f),
                end = Offset(width * 0.35f, height * 0.75f),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = lineColor,
                start = Offset(width * 0.5f, height * 0.28f),
                end = Offset(width * 0.5f, height * 0.78f),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = lineColor,
                start = Offset(width * 0.65f, height * 0.32f),
                end = Offset(width * 0.65f, height * 0.72f),
                strokeWidth = strokeWidth
            )

            // Nós neurais: aumentam de quantidade de acordo com o nível.
            val nodeCount = 2 + safeLevel
            repeat(nodeCount) { index ->
                val x = width * (0.25f + (index % 5) * 0.12f)
                val y = height * (0.33f + (index / 5) * 0.16f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.55f + progress * 0.35f),
                    radius = 3f + progress * 7f,
                    center = Offset(x, y)
                )
            }

            // Aura exterior para dar sensação de evolução visual.
            drawRoundRect(
                color = brainColor.copy(alpha = 0.12f + progress * 0.2f),
                topLeft = Offset(width * 0.1f, height * 0.15f),
                size = Size(width * 0.8f, height * 0.72f),
                cornerRadius = CornerRadius(width * 0.4f, width * 0.4f),
                style = Stroke(width = 2f + progress * 6f)
            )
        }

        Text(
            text = "Nível $safeLevel",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
    }
}

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = start.alpha + (end.alpha - start.alpha) * f
    )
}
