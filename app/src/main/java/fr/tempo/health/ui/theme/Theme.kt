package fr.tempo.health.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Tempo Coach 2.1 — Prune & Rose poudré, charte validée. */
object TempoColors {
    val background = Color(0xFF1A1D1F)
    val surface = Color(0xFF252A31)
    val surfaceAlt = Color(0xFF22262C)
    val border = Color(0xFF383640)
    val prune = Color(0xFF5B2D58)
    val pruneDark = Color(0xFF352936)
    val rose = Color(0xFFD99BB6)
    val cream = Color(0xFFF0DDD6)
    val sage = Color(0xFF8CA899)
    val sageDark = Color(0xFF37453D)
    val text = Color(0xFFF7F3F1)
    val textSecondary = Color(0xFFCFC4C8)
    val error = Color(0xFFE8A5B5)
}

private val TempoDarkScheme = darkColorScheme(
    primary = TempoColors.cream,
    onPrimary = TempoColors.background,
    primaryContainer = TempoColors.prune,
    onPrimaryContainer = TempoColors.text,
    secondary = TempoColors.rose,
    onSecondary = TempoColors.background,
    secondaryContainer = TempoColors.pruneDark,
    onSecondaryContainer = TempoColors.text,
    tertiary = TempoColors.sage,
    onTertiary = TempoColors.background,
    tertiaryContainer = TempoColors.sageDark,
    onTertiaryContainer = TempoColors.text,
    background = TempoColors.background,
    onBackground = TempoColors.text,
    surface = TempoColors.background,
    onSurface = TempoColors.text,
    surfaceVariant = TempoColors.surface,
    onSurfaceVariant = TempoColors.textSecondary,
    surfaceContainer = TempoColors.surface,
    outline = TempoColors.border,
    outlineVariant = TempoColors.border,
    error = TempoColors.error,
    onError = TempoColors.background
)

@Composable
fun TempoHealthTheme(content: @Composable () -> Unit) {
    // Identité sombre constante : pas de fond photo, ni de changement de
    // palette imprévisible suivant le mode clair/sombre du téléphone.
    MaterialTheme(colorScheme = TempoDarkScheme, content = content)
}
