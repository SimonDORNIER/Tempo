package fr.tempo.health.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Option B : charte unique pour les quatre onglets et tous les écrans secondaires.
 * Les alias historiques assurent la compatibilité avec les composants préexistants.
 */
object TempoColors {
    val background = Color(0xFF0C1118)
    val surface = Color(0xFF191F2C)
    val surfaceAlt = Color(0xFF232A3A)
    val border = Color(0xFF384158)
    val violet = Color(0xFF9638E8)
    val violetGlow = Color(0xFFC06CFF)
    val violetPale = Color(0xFFE0A6FF)
    val violetDeep = Color(0xFF381653)
    val greenForest = Color(0xFF127E51)
    val greenGlow = Color(0xFF50E49A)
    val greenDeep = Color(0xFF0B3A2B)
    val text = Color(0xFFF8F7FD)
    val textSecondary = Color(0xFFC9CDDE)
    val textMuted = Color(0xFFAEB6CD)
    val amber = Color(0xFFFFBF65)
    val redAlert = Color(0xFFFF7280)
    val error = Color(0xFFFFA5B3)

    // Compatibilité des anciens écrans : éviter les anciennes couleurs délavées.
    val prune = violetDeep
    val pruneDark = Color(0xFF291638)
    val rose = violetGlow
    val cream = violetPale
    val sage = greenGlow
    val sageDark = greenDeep
}

private val TempoOptionBScheme = darkColorScheme(
    primary = TempoColors.violetGlow,
    onPrimary = TempoColors.background,
    primaryContainer = TempoColors.violetDeep,
    onPrimaryContainer = TempoColors.text,
    secondary = TempoColors.violetPale,
    onSecondary = TempoColors.background,
    secondaryContainer = TempoColors.violetDeep,
    onSecondaryContainer = TempoColors.text,
    tertiary = TempoColors.greenGlow,
    onTertiary = TempoColors.background,
    tertiaryContainer = TempoColors.greenDeep,
    onTertiaryContainer = TempoColors.text,
    background = TempoColors.background,
    onBackground = TempoColors.text,
    surface = TempoColors.background,
    onSurface = TempoColors.text,
    surfaceVariant = TempoColors.surface,
    onSurfaceVariant = TempoColors.textSecondary,
    surfaceContainer = TempoColors.surface,
    surfaceContainerLow = TempoColors.surface,
    surfaceContainerHigh = TempoColors.surfaceAlt,
    outline = TempoColors.border,
    outlineVariant = TempoColors.border,
    error = TempoColors.error,
    onError = TempoColors.background
)

/** Une carte partagée : espaces, arrondi et bordure harmonisés dans les sous-écrans. */
@Composable
fun TempoPanelCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    colors: CardColors = CardDefaults.cardColors(containerColor = TempoColors.surface),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    border: BorderStroke? = BorderStroke(0.85.dp, TempoColors.border.copy(alpha = 0.82f)),
    content: @Composable ColumnScope.() -> Unit
) {
    val accentBorder = if (colors.containerColor == TempoOptionBScheme.primaryContainer) {
        BorderStroke(1.dp, TempoColors.violet.copy(alpha = 0.85f))
    } else border
    Card(modifier = modifier, shape = shape, colors = colors, elevation = elevation,
        border = accentBorder, content = content)
}

@Composable
fun TempoHealthTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TempoOptionBScheme, content = content)
}
