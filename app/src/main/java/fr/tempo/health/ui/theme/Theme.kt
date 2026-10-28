package fr.tempo.health.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp

/**
 * Tempo Coach 2.5 - identité professionnelle et reposante.
 * Une couleur par domaine ; vert/ambre/rouge uniquement pour l'état.
 * Ces couleurs ne présument pas de l'existence de mesures de santé.
 */
object TempoColors {
    val background = Color(0xFF090D12)
    val surface = Color(0xFF1B222B)
    val surfaceAlt = Color(0xFF242C36)
    val border = Color(0xFF35404D)
    val text = Color(0xFFF3F5F8)
    val textSecondary = Color(0xFFCBD3DF)
    val textMuted = Color(0xFFA7B2C1)

    // Identité et navigation. Les boutons sont bleus, jamais rouge/vert « santé ».
    val action = Color(0xFF9BC4EF)
    val actionDeep = Color(0xFF233A53)
    val actionText = Color(0xFF111B26)

    // Couleurs fixes de familles, pastel plutôt que néon.
    val sleep = Color(0xFFB4A5DE)
    val sleepDeep = Color(0xFF2B293E)
    val activity = Color(0xFF81C3AC)
    val activityDeep = Color(0xFF203A34)
    val heart = Color(0xFFDF9FA9)
    val heartDeep = Color(0xFF3A2B35)
    val calm = Color(0xFF9BC4EF)
    val calmDeep = Color(0xFF26384D)
    val mobility = Color(0xFF90C3B0)
    val stretch = Color(0xFFDBBD96)
    val strengthening = Color(0xFFCFA3B2)
    val profile = Color(0xFFB0BDD2)

    // Couleurs contextuelles (métriques suffisamment documentées seulement).
    val good = Color(0xFF87CDAD)
    val amber = Color(0xFFE8C282)
    val redAlert = Color(0xFFE0989E)
    val unknown = Color(0xFF9BA6B4)
    val error = redAlert

    // Alias de rétrocompatibilité de code ; toutes les nouvelles vues emploient
    // explicitement sleep/activity/heart/calm et good/amber/redAlert/unknown.
    val violet = sleep
    val violetGlow = sleep
    val violetPale = Color(0xFFD1C4EC)
    val violetDeep = sleepDeep
    val greenForest = activity
    val greenGlow = activity
    val greenDeep = activityDeep
    val prune = sleepDeep
    val pruneDark = sleepDeep
    val rose = sleep
    val cream = action
    val sage = activity
    val sageDark = activityDeep
}

private val TempoSoftScheme = darkColorScheme(
    primary = TempoColors.action,
    onPrimary = TempoColors.actionText,
    primaryContainer = TempoColors.actionDeep,
    onPrimaryContainer = TempoColors.text,
    secondary = TempoColors.sleep,
    onSecondary = TempoColors.background,
    secondaryContainer = TempoColors.sleepDeep,
    onSecondaryContainer = TempoColors.text,
    tertiary = TempoColors.activity,
    onTertiary = TempoColors.background,
    tertiaryContainer = TempoColors.activityDeep,
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

/** Panneau commun aux 4 onglets et aux vues secondaires. */
@Composable
fun TempoPanelCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(19.dp),
    colors: CardColors = CardDefaults.cardColors(containerColor = TempoColors.surface),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    border: BorderStroke? = BorderStroke(0.8.dp, TempoColors.border.copy(alpha = 0.65f)),
    content: @Composable ColumnScope.() -> Unit
) {
    val actualBorder = if (colors.containerColor == TempoSoftScheme.primaryContainer) {
        BorderStroke(0.8.dp, TempoColors.action.copy(alpha = 0.36f))
    } else border
    Card(modifier = modifier, shape = shape, colors = colors, elevation = elevation,
        border = actualBorder, content = content)
}

@Composable
fun TempoHealthTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TempoSoftScheme, content = content)
}
