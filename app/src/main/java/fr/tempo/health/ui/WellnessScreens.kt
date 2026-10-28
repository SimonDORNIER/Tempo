package fr.tempo.health.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.tempo.health.data.DailyCheckInEntity
import fr.tempo.health.data.DailyHealthEntity
import fr.tempo.health.data.WorkoutHistoryEntity
import fr.tempo.health.domain.Exercise
import fr.tempo.health.domain.ExerciseCategory
import fr.tempo.health.domain.ExerciseLibrary
import fr.tempo.health.domain.RecoveryLevel
import fr.tempo.health.domain.RecoveryResult
import fr.tempo.health.domain.TrainingPreferences
import fr.tempo.health.domain.WorkoutPlan
import fr.tempo.health.ui.theme.TempoColors as C
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val round = RoundedCornerShape(22.dp)

@Composable
private fun Headline(title: String, subtitle: String, action: @Composable (() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = C.text)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = C.textSecondary)
        }
        action?.invoke()
    }
}

@Composable
private fun SectionTitle(title: String, action: String? = null, onClick: (() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 30.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (action != null && onClick != null) {
            Row(modifier = Modifier.clickable(onClick = onClick).defaultMinSize(minHeight = 48.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(action, style = MaterialTheme.typography.labelMedium, color = C.rose)
                Icon(Icons.Rounded.ChevronRight, null, modifier = Modifier.size(19.dp), tint = C.rose)
            }
        }
    }
}

/** Une même carte compacte et discrète dans tous les onglets. */
@Composable
private fun TintedCard(
    modifier: Modifier = Modifier,
    colorStart: Color = C.surface,
    colorEnd: Color = C.surface,
    onClick: (() -> Unit)? = null,
    accent: Color? = null,
    content: @Composable () -> Unit
) {
    val edge = accent?.copy(alpha = 0.36f) ?: C.border.copy(alpha = 0.62f)
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = C.surface),
        border = BorderStroke(0.8.dp, edge)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(
                    colorStart.copy(alpha = 0.44f).compositeOver(C.surface),
                    colorEnd.copy(alpha = 0.20f).compositeOver(C.surface)
                ))
            ).padding(13.dp)
        ) { content() }
    }
}

@Composable
private fun SmallIcon(icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(37.dp).background(tint.copy(alpha = 0.13f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun MetricTile(
    title: String, value: String, subtitle: String, icon: ImageVector,
    accent: Color, first: Color, last: Color, modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    TintedCard(modifier, first, last, onClick, accent) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallIcon(icon, accent)
                Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, color = C.text)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.ChevronRight, null, tint = C.textMuted,
                    modifier = Modifier.size(17.dp))
            }
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                maxLines = 1, color = C.text, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, maxLines = 2,
                overflow = TextOverflow.Ellipsis, color = C.textSecondary)
        }
    }
}

@Composable
private fun PrimaryAction(text: String, icon: ImageVector, onClick: () -> Unit) {
    Button(
        modifier = Modifier.fillMaxWidth().height(52.dp), onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = C.action, contentColor = C.actionText)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(9.dp))
        Text(text, modifier = Modifier.weight(1f, fill = false), maxLines = 1,
            overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TempoStateLabel(title: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Text(title, color = C.textSecondary, style = MaterialTheme.typography.labelSmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}


@Composable
private fun ListLink(icon: ImageVector, title: String, detail: String, onClick: () -> Unit,
    tint: Color = C.profile) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
        .defaultMinSize(minHeight = 64.dp).padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        SmallIcon(icon, tint)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = C.textSecondary, maxLines = 2)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = C.textSecondary)
    }
}

private fun sleepText(minutes: Long?): String =
    minutes?.let { "${it / 60} h ${(it % 60).toString().padStart(2, '0')}" } ?: "—"

private fun dayLabel(): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE))
        .replaceFirstChar { it.uppercase() }

@Composable
private fun TinyTrend(
    title: String, summary: String, icon: ImageVector, color: Color,
    days: List<DailyHealthEntity>, metric: (DailyHealthEntity) -> Number?,
    modifier: Modifier, onClick: () -> Unit, kind: TempoMetricKind = TempoMetricKind.OTHER
) {
    val points = metricByCalendarDay(days, 7) { metric(it)?.toDouble() }
    val usual = TempoHealthSignals.reference(days.drop(7).mapNotNull { metric(it)?.toDouble() })
    val state = TempoHealthSignals.assess(kind, points.lastOrNull { it != null }, usual)
    val isActivity = kind == TempoMetricKind.STEPS || kind == TempoMetricKind.ACTIVE_MINUTES
    val background = when (kind) {
        TempoMetricKind.SLEEP -> C.sleepDeep
        TempoMetricKind.STEPS, TempoMetricKind.ACTIVE_MINUTES -> C.activityDeep
        TempoMetricKind.HRV, TempoMetricKind.RESTING_PULSE -> C.heartDeep
        TempoMetricKind.OTHER -> C.calmDeep
    }
    TintedCard(modifier, background, C.surface, onClick, color) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                SmallIcon(icon, color)
                Text(title, modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 1)
                Icon(Icons.Rounded.ChevronRight, "Voir les détails", tint = C.textMuted,
                    modifier = Modifier.size(18.dp))
            }
            Text(summary, color = C.text, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            TempoTrendGraph(
                values = points, metric = kind, usual = usual,
                kind = if (kind == TempoMetricKind.SLEEP || isActivity)
                    TempoChartKind.BARS else TempoChartKind.LINE, height = 66.dp
            )
            if (isActivity) {
                Text("7 derniers jours", color = C.textSecondary,
                    style = MaterialTheme.typography.labelSmall)
            } else {
                TempoStateLabel(
                    if (state == TempoSignal.UNKNOWN) "Référence insuffisante"
                    else TempoHealthSignals.label(kind, state),
                    if (state == TempoSignal.UNKNOWN) C.unknown
                    else TempoHealthSignals.color(state)
                )
            }
        }
    }
}

@Composable
fun TodayV21Screen(
    days: List<DailyHealthEntity>,
    checkIn: DailyCheckInEntity?,
    recovery: RecoveryResult,
    plan: WorkoutPlan,
    routine: RoutineProgress,
    syncing: Boolean,
    hasPermissions: Boolean,
    onSync: () -> Unit,
    onPermissions: () -> Unit,
    onCheckIn: (Int, Int) -> Unit,
    onToggleHabit: (Int) -> Unit,
    onStart: () -> Unit,
    onCoach: () -> Unit,
    onHealth: () -> Unit,
    onTracking: () -> Unit,
    onPractice: () -> Unit
) {
    val today = LocalDate.now().toString()
    val current = days.firstOrNull { it.date == today }
    // Un sommeil appartient à la date locale du réveil : ne pas remplacer
    // la nuit dernière par une ancienne nuit ayant une mesure.
    val night = current?.takeIf { (it.sleepMinutes ?: 0L) > 0L }
    val readiness = recovery.score?.takeIf { recovery.confidence >= 20 }
    val readinessAccent = if (readiness == null) C.unknown
        else TempoHealthSignals.recoveryColor(recovery.level)
    val readinessLabel = if (readiness == null) "Données insuffisantes" else when (recovery.level) {
        RecoveryLevel.GREEN -> "Bonne préparation"
        RecoveryLevel.ORANGE -> "Récupération modérée"
        RecoveryLevel.RED -> "Repos à privilégier"
        RecoveryLevel.UNKNOWN -> "À déterminer"
    }
    val completedHabits = if (routine.date == today) routine.completed else emptySet()
    val energy = checkIn?.energy ?: 3
    val pain = checkIn?.pain ?: 0

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(C.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 14.dp, end = 14.dp, top = 12.dp, bottom = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        item {
            Headline("Aujourd'hui", dayLabel()) {
                OutlinedButton(
                    onClick = if (hasPermissions) onSync else onPermissions,
                    enabled = !syncing,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 9.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (syncing) "Sync…" else "Sync", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        item {
            TintedCard(onClick = onCoach, colorStart = C.activityDeep, colorEnd = C.surface,
                accent = readinessAccent) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SmallIcon(Icons.Rounded.Favorite, readinessAccent)
                        Spacer(Modifier.width(8.dp))
                        Text("Forme & préparation", modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Rounded.ChevronRight, "Comprendre le score", tint = C.textMuted)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(88.dp)) {
                            CircularProgressIndicator(
                                progress = { (readiness ?: 0).coerceIn(0, 100) / 100f },
                                modifier = Modifier.size(83.dp),
                                color = readinessAccent,
                                trackColor = C.border.copy(alpha = 0.55f),
                                strokeWidth = 7.dp
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(readiness?.toString() ?: "—", color = C.text,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold)
                                Text(if (readiness == null) "Forme" else "/100",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = C.textSecondary)
                            }
                        }
                        Column(modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            TempoStateLabel(readinessLabel, readinessAccent)
                            Text(
                                if (readiness == null) "Synchronise tes mesures pour calculer ta préparation."
                                else recovery.reasons.firstOrNull()
                                    ?: "Adapte ton activité à ton ressenti aujourd'hui.",
                                style = MaterialTheme.typography.bodySmall,
                                color = C.textSecondary,
                                maxLines = 3, overflow = TextOverflow.Ellipsis
                            )
                            if (readiness != null) {
                                Text("Fiabilité ${recovery.confidence} %",
                                    color = C.textMuted, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                MetricTile("Sommeil", sleepText(night?.sleepMinutes),
                    if (night == null) "Aucune mesure cette nuit"
                    else sleepEfficiency(night)?.let { "Efficacité $it % • dernière nuit" }
                        ?: "Dernière nuit",
                    Icons.Rounded.Bedtime, C.sleep, C.sleepDeep, C.surface,
                    Modifier.weight(1f), onTracking)
                MetricTile("Pas", current?.steps?.toString() ?: "—", "Aujourd'hui",
                    Icons.Rounded.DirectionsWalk, C.activity, C.activityDeep, C.surface,
                    Modifier.weight(1f), onTracking)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                MetricTile("FC repos", current?.restingHeartRate
                    ?.let { "${it.roundToInt()} bpm" } ?: "—", "Mesure du jour",
                    Icons.Rounded.Favorite, C.heart, C.heartDeep, C.surface,
                    Modifier.weight(1f), onHealth)
                MetricTile("VFC", current?.hrvRmssdMs
                    ?.let { "${it.roundToInt()} ms" } ?: "—", "Mesure du jour",
                    Icons.Rounded.BarChart, C.heart, C.heartDeep, C.surface,
                    Modifier.weight(1f), onHealth)
            }
        }
        item {
            TintedCard(onClick = onCoach, colorStart = C.calmDeep, colorEnd = C.surface,
                accent = C.calm) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallIcon(Icons.Rounded.Lightbulb, C.calm)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Conseil du coach", fontWeight = FontWeight.SemiBold)
                        Text(recovery.reasons.firstOrNull()
                            ?: "Un peu de mobilité ou de respiration peut faire du bien.",
                            style = MaterialTheme.typography.bodySmall,
                            color = C.textSecondary, maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Rounded.ChevronRight, "Voir les conseils", tint = C.textMuted)
                }
            }
        }
        item {
            PrimaryAction(
                if (recovery.level == RecoveryLevel.RED) "Commencer une séance douce"
                else "Commencer une séance", Icons.Rounded.PlayArrow, onStart
            )
        }
        item {
            TintedCard(onClick = onHealth) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallIcon(Icons.Rounded.Favorite, C.heart)
                    Column(Modifier.weight(1f)) {
                        Text("Santé & activité", fontWeight = FontWeight.SemiBold)
                        Text("Activité " + (current?.exerciseMinutes?.let { "$it min" } ?: "—") +
                            " • Respiration " + (current?.respiratoryRate?.let {
                                String.format(Locale.FRANCE, "%.1f/min", it)
                            } ?: "—"),
                            style = MaterialTheme.typography.bodySmall, color = C.textSecondary,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Rounded.ChevronRight, "Voir la santé", tint = C.textMuted)
                }
            }
        }
        item {
            SectionTitle("Mon ressenti")
            TintedCard {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(if (checkIn == null) "À compléter" else "Enregistré aujourd'hui",
                        style = MaterialTheme.typography.labelSmall, color = C.textMuted)
                    Text("Énergie", style = MaterialTheme.typography.labelMedium, color = C.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        (1..5).forEach { value ->
                            val selected = checkIn != null && energy == value
                            OutlinedButton(onClick = { onCheckIn(value, pain) },
                                modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                border = BorderStroke(1.dp, if (selected) C.action else C.border),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) C.actionDeep else C.surface
                                )) { Text(value.toString(), color = if (selected) C.text else C.textSecondary) }
                        }
                    }
                    Text("Douleurs", style = MaterialTheme.typography.labelMedium,
                        color = C.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("Aucune", "Légères", "Moyennes", "Fortes").forEachIndexed { index, label ->
                            val selected = checkIn != null && pain == index
                            OutlinedButton(onClick = { onCheckIn(energy, index) },
                                modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                border = BorderStroke(1.dp, if (selected) C.action else C.border),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) C.actionDeep else C.surface
                                )) {
                                Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1,
                                    color = if (selected) C.text else C.textSecondary)
                            }
                        }
                    }
                }
            }
        }
        item {
            SectionTitle("Habitudes du jour", "${completedHabits.size}/3", onPractice)
            TintedCard {
                Column {
                    listOf(
                        "Bouger régulièrement",
                        if (recovery.level == RecoveryLevel.RED) "Mobilité douce ou repos"
                        else "Séance ou mobilité",
                        "Préparer un coucher régulier"
                    ).forEachIndexed { index, label ->
                        if (index > 0) HorizontalDivider(color = C.border.copy(alpha = 0.5f))
                        Row(
                            Modifier.fillMaxWidth().defaultMinSize(minHeight = 49.dp)
                                .clickable { onToggleHabit(index) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = index in completedHabits,
                                onCheckedChange = { onToggleHabit(index) })
                            Text(label, style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item {
            Text("Le score Tempo est une estimation, pas un diagnostic médical.",
                style = MaterialTheme.typography.labelSmall, color = C.textMuted)
        }
    }
}

@Composable
fun PracticeHubV21Screen(
    plan: WorkoutPlan,
    onStart: () -> Unit,
    onBreathing: () -> Unit,
    onExplore: (ExerciseCategory) -> Unit,
    onProgram: () -> Unit,
    onTimer: () -> Unit,
    onManage: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(C.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        item { Headline("Pratiquer", "Respirer, bouger, se sentir mieux") }
        item {
            TintedCard(colorStart = C.calmDeep, colorEnd = C.surface, accent = C.calm) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        SmallIcon(Icons.Rounded.PlayArrow, C.calm)
                        Column(Modifier.weight(1f)) {
                            Text("Ma séance conseillée",
                                color = C.textSecondary, style = MaterialTheme.typography.labelMedium)
                            Text(plan.title, style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold, maxLines = 2)
                        }
                    }
                    Text("${plan.estimatedMinutes} min • ${plan.items.size} exercices",
                        style = MaterialTheme.typography.bodySmall, color = C.textSecondary)
                    PrimaryAction("Commencer", Icons.Rounded.PlayArrow, onStart)
                }
            }
        }
        item { SectionTitle("Choisir une pratique") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PracticeTile("Respiration", "Retrouver le calme", Icons.Rounded.Air,
                    C.calm, Modifier.fillMaxWidth(), onBreathing)
                PracticeTile("Mobilité", "Bouger avec souplesse", Icons.Rounded.SelfImprovement,
                    C.mobility, Modifier.fillMaxWidth(), { onExplore(ExerciseCategory.MOBILITY) })
                PracticeTile("Étirements", "Relâcher les tensions", Icons.Rounded.Spa,
                    C.stretch, Modifier.fillMaxWidth(), { onExplore(ExerciseCategory.STRETCHING) })
                PracticeTile("Renforcement", "Gagner en stabilité", Icons.Rounded.FitnessCenter,
                    C.strengthening, Modifier.fillMaxWidth(),
                    { onExplore(ExerciseCategory.FULL_BODY) })
            }
        }
        item {
            SectionTitle("Mes outils")
            TintedCard {
                Column {
                    ListLink(Icons.Rounded.Book, "Mes programmes", "Planifier ma semaine",
                        onProgram, C.calm)
                    HorizontalDivider(color = C.border.copy(alpha = 0.6f))
                    ListLink(Icons.Rounded.Timer, "Minuteur libre", "Définir un temps",
                        onTimer, C.calm)
                    HorizontalDivider(color = C.border.copy(alpha = 0.6f))
                    ListLink(Icons.Rounded.Tune, "Bibliothèque", "Consulter les exercices",
                        onManage, C.profile)
                }
            }
        }
    }
}

@Composable
private fun PracticeTile(
    title: String, detail: String, icon: ImageVector, color: Color,
    modifier: Modifier, onClick: () -> Unit
) {
    val background = when (title) {
        "Respiration" -> C.calmDeep
        "Mobilité" -> C.activityDeep
        "Étirements" -> C.surfaceAlt
        else -> C.heartDeep
    }
    TintedCard(modifier = modifier, colorStart = background, colorEnd = C.surface,
        onClick = onClick, accent = color) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            SmallIcon(icon, color)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall,
                    color = C.textSecondary, maxLines = 1)
            }
            Icon(Icons.Rounded.ChevronRight, "Ouvrir $title",
                tint = C.textMuted, modifier = Modifier.size(21.dp))
        }
    }
}

@Composable
fun TrackingHubV21Screen(
    days: List<DailyHealthEntity>,
    history: List<WorkoutHistoryEntity>,
    recovery: RecoveryResult,
    onHealth: () -> Unit,
    onProgress: () -> Unit
) {
    val today = LocalDate.now()
    val lastSeven = (0L..6L).map { today.minusDays(it).toString() }.toSet()
    val recentDays = days.filter { it.date in lastSeven }
    val earliest = today.minusDays(6)
        .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    val completed = history.count {
        it.completed && it.startedAtEpochMs >= earliest &&
            it.startedAtEpochMs <= System.currentTimeMillis()
    }
    val sleepValues = recentDays.mapNotNull { it.sleepMinutes }
    val stepValues = recentDays.mapNotNull { it.steps }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(C.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Headline("Suivi", "Comprendre ce qui évolue") }
        item {
            TintedCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Les 7 derniers jours", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricStat("Sommeil moyen",
                            sleepText(sleepValues.takeIf { it.isNotEmpty() }?.average()?.toLong()),
                            Modifier.weight(1f), C.text)
                        MetricStat("Séances terminées", completed.toString(),
                            Modifier.weight(1f), C.text)
                    }
                }
            }
        }
        item { SectionTitle("Sommeil & activité", "Détails", onHealth) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                TinyTrend("Sommeil", sleepText(sleepValues.takeIf { it.isNotEmpty() }?.average()?.toLong()),
                    Icons.Rounded.Bedtime, C.sleep, days, { it.sleepMinutes },
                    Modifier.weight(1f), onHealth, TempoMetricKind.SLEEP)
                TinyTrend("Pas", stepValues.takeIf { it.isNotEmpty() }
                    ?.average()?.roundToInt()?.toString() ?: "—",
                    Icons.Rounded.DirectionsWalk, C.activity, days, { it.steps },
                    Modifier.weight(1f), onHealth, TempoMetricKind.STEPS)
            }
        }
        item { SectionTitle("Cœur & récupération", "Détails", onHealth) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                TinyTrend("FC repos", days.firstOrNull { it.restingHeartRate != null }
                    ?.restingHeartRate?.let { "${it.roundToInt()} bpm" } ?: "—",
                    Icons.Rounded.Favorite, C.heart, days, { it.restingHeartRate },
                    Modifier.weight(1f), onHealth, TempoMetricKind.RESTING_PULSE)
                TinyTrend("VFC", days.firstOrNull { it.hrvRmssdMs != null }
                    ?.hrvRmssdMs?.let { "${it.roundToInt()} ms" } ?: "—",
                    Icons.Rounded.BarChart, C.heart, days, { it.hrvRmssdMs },
                    Modifier.weight(1f), onHealth, TempoMetricKind.HRV)
            }
        }
        item {
            SectionTitle("Explorer mes données")
            TintedCard {
                Column {
                    ListLink(Icons.Rounded.Favorite, "Santé",
                        "Sommeil, respiration, cardio, SpO₂", onHealth, C.heart)
                    HorizontalDivider(color = C.border.copy(alpha = 0.6f))
                    ListLink(Icons.Rounded.DirectionsWalk, "Activité",
                        "Pas, distance et minutes actives", onHealth, C.activity)
                    HorizontalDivider(color = C.border.copy(alpha = 0.6f))
                    ListLink(Icons.Rounded.BarChart, "Séances",
                        "Historique et progression", onProgress, C.calm)
                }
            }
        }
        item {
            Text("Les couleurs indiquent les domaines. Les pastilles montrent les écarts éventuels à tes références personnelles.",
                color = C.textMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun MetricStat(title: String, value: String, modifier: Modifier, accent: Color = C.text) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = C.textSecondary)
        Spacer(Modifier.height(5.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun MeHubV21Screen(
    onSettings: () -> Unit,
    onCategories: () -> Unit,
    onExercises: () -> Unit,
    onHealth: () -> Unit,
    onTimer: () -> Unit,
    onUpdates: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().background(C.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Headline("Moi", "Mes objectifs, mes données et mes préférences") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle("Personnalisation")
                TintedCard {
                    Column {
                        ListLink(Icons.Rounded.Edit, "Mes exercices", "Créer, modifier et organiser", onExercises, C.strengthening)
                        HorizontalDivider(color = C.border)
                        ListLink(Icons.Rounded.Spa, "Mes catégories", "Activer, personnaliser les icônes", onCategories, C.mobility)
                        HorizontalDivider(color = C.border)
                        ListLink(Icons.Rounded.Tune, "Objectifs et matériel", "Durée, équipement et préférences", onSettings)
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle("Santé & données")
                TintedCard {
                    Column {
                        ListLink(Icons.Rounded.Favorite, "Santé Connect", "Synchronisation, autorisations et mesures", onHealth, C.heart)
                        HorizontalDivider(color = C.border)
                        ListLink(Icons.Rounded.Settings, "Sauvegarde & restauration", "Google Drive et données locales", onSettings)
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle("Application")
                TintedCard {
                    Column {
                        ListLink(Icons.Rounded.Timer, "Minuteur", "Durées et sons", onTimer)
                        HorizontalDivider(color = C.border)
                        ListLink(Icons.Rounded.Settings, "Paramètres", "Sons, rappels et préférences", onSettings)
                        HorizontalDivider(color = C.border)
                        ListLink(Icons.Rounded.Update, "Vérifier les mises à jour", "Rechercher une nouvelle version", onUpdates, C.calm)
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseExploreV21Screen(
    category: ExerciseCategory?,
    preferences: TrainingPreferences,
    onStart: (Exercise) -> Unit
) {
    val exercises = ExerciseLibrary.effective(preferences)
        .filter {
            (category == null || if (category == ExerciseCategory.FULL_BODY) {
                it.category in setOf(ExerciseCategory.UPPER_BODY, ExerciseCategory.LOWER_BODY,
                    ExerciseCategory.CORE, ExerciseCategory.FULL_BODY)
            } else it.category == category) &&
                it.id !in preferences.avoidedExerciseIds &&
                it.category !in preferences.disabledCategories
        }
    LazyColumn(modifier = Modifier.fillMaxSize().background(C.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Headline("Exercices", "Choisir une pratique") }
        if (exercises.isEmpty()) {
            item { Text("Aucun exercice disponible dans cette catégorie.",
                color = C.textSecondary) }
        }
        items(exercises, key = { it.id }) { exercise ->
            TintedCard(onClick = { onStart(exercise) }) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallIcon(Icons.Rounded.SelfImprovement, C.mobility)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(exercise.name, fontWeight = FontWeight.SemiBold)
                            Text("${preferences.exerciseWorkSeconds[exercise.id] ?: exercise.defaultWorkSeconds} secondes",
                                style = MaterialTheme.typography.labelSmall, color = C.textSecondary)
                        }
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Faire cet exercice",
                            tint = C.cream)
                    }
                    Text(exercise.instructions, style = MaterialTheme.typography.bodySmall, color = C.textSecondary)
                    Text(exercise.cue, style = MaterialTheme.typography.labelSmall, color = C.rose)
                }
            }
        }
    }
}
