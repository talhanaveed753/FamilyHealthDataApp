package com.example.healthconnect_tablet.tokens.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shared token visuals and color palette so the dashboard and history views stay consistent.
 */
object TokenPalette {
    val Blue = Color(0xFF4C6EF5)
    val Yellow = Color(0xFFFFD85C)
    val Red = Color(0xFFFF6B6B)
    val Green = Color(0xFF6CC070)
    val Neutral = Color(0xFFE2E8F0)
}

data class MoodOption(val name: String, val feelings: String, val color: Color)

val MoodOptions = listOf(
    MoodOption("Blue", "Sad • Bored • Tired • Sick", TokenPalette.Blue),
    MoodOption("Yellow", "Silly • Frustrated • Excited • Worrisome", TokenPalette.Yellow),
    MoodOption("Red", "Panicked • Overjoyed • Terrified • Angry", TokenPalette.Red),
    MoodOption("Green", "Calm • Happy • Proud • Focused", TokenPalette.Green)
)

@Composable
fun PhysicalActivityTokenIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val path = Path()
        repeat(6) { index ->
            val angle = Math.toRadians((60 * index - 30).toDouble())
            val point = Offset(
                x = center.x + radius * cos(angle).toFloat(),
                y = center.y + radius * sin(angle).toFloat()
            )
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        path.close()
        drawPath(path = path, color = color)
    }
}

@Composable
fun SleepTokenIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        drawCircle(color = color)
    }
}

@Composable
fun MoodTokenIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        drawCircle(color = Color.White)
        drawCircle(color = color, style = Stroke(width = size.minDimension * 0.12f))
        val strokeWidth = size.minDimension * 0.18f
        val padding = size.minDimension * 0.18f
        val start1 = Offset(padding, size.height - padding)
        val end1 = Offset(size.width - padding, padding)
        val start2 = Offset(padding, padding)
        val end2 = Offset(size.width - padding, size.height - padding)
        drawLine(
            color = color,
            start = start1,
            end = end1,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = start2,
            end = end2,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

fun tokenCategoryLabel(category: String?): String = when (category) {
    "steps" -> "Physical Activity"
    "sleep" -> "Sleep"
    else -> category?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: ""
}

fun tokenCategoryAccent(category: String?): Color = when (category) {
    "steps" -> TokenPalette.Red
    "sleep" -> TokenPalette.Blue
    else -> TokenPalette.Neutral
}
