package fr.tempo.health.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object TempoPalette {
    val background = Color(0xFF1A1D1F)
    val surface = Color(0xFF252A31)
    val elevated = Color(0xFF22262C)
    val outline = Color(0xFF40404A)
    val cream = Color(0xFFF0DDD6)
    val text = Color(0xFFF7F3F1)
    val muted = Color(0xFFCFC4C8)
    val prune = Color(0xFF5B2D58)
    val rose = Color(0xFFD99BB6)
    val sage = Color(0xFF8CA899)
    val caution = Color(0xFFD9B49B)
}

private val TempoColors = darkColorScheme(
    primary = TempoPalette.cream,
    onPrimary = TempoPalette.background,
    primaryContainer = TempoPalette.prune,
    onPrimaryContainer = TempoPalette.text,
    secondary = TempoPalette.rose,
    onSecondary = TempoPalette.background,
    secondaryContainer = Color(0xFF433043),
    onSecondaryContainer = TempoPalette.text,
    tertiary = TempoPalette.sage,
    onTertiary = TempoPalette.background,
    background = TempoPalette.background,
    onBackground = TempoPalette.text,
    surface = TempoPalette.background,
    onSurface = TempoPalette.text,
    surfaceVariant = TempoPalette.surface,
    onSurfaceVariant = TempoPalette.muted,
    surfaceContainer = TempoPalette.surface,
    surfaceContainerLow = TempoPalette.elevated,
    outline = TempoPalette.outline,
    error = Color(0xFFDB91A4),
    onError = TempoPalette.background
)

@Composable
fun TempoHealthTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TempoColors, typography = Typography(), content = content)
}
