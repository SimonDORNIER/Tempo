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

@Composable
private fun TintedCard(
    modifier: Modifier = Modifier,
    colorStart: Color = C.surface,
    colorEnd: Color = C.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = round,
        colors = CardDefaults.cardColors(containerColor = colorStart),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(colorStart, colorEnd))).padding(14.dp)) {
            content()
        }
    }
}

@Composable
private fun SmallIcon(icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(37.dp).background(tint.copy(alpha = 0.16f), CircleShape),
        contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun MetricTile(
    title: String, value: String, subtitle: String, icon: ImageVector,
    accent: Color, first: Color, last: Color, modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    TintedCard(modifier, first, last, onClick) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SmallIcon(icon, accent)
                Text(title, style = MaterialTheme.typography.labelMedium, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, color = C.textSecondary)
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                maxLines = 1, color = C.text)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, maxLines = 1,
                overflow = TextOverflow.Ellipsis, color = C.textSecondary)
        }
    }
}

@Composable
private fun PrimaryAction(text: String, icon: ImageVector, onClick: () -> Unit) {
    Button(
        modifier = Modifier.fillMaxWidth().height(56.dp), onClick = onClick,
        shape = RoundedCornerShape(29.dp),
        colors = ButtonDefaults.buttonColors(containerColor = C.cream, contentColor = C.background)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(23.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, modifier = Modifier.weight(1f, fill = false), maxLines = 1,
            overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(10.dp))
        Icon(Icons.Rounded.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun ListLink(icon: ImageVector, title: String, detail: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
        .defaultMinSize(minHeight = 64.dp).padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        SmallIcon(icon, C.rose)
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
private fun MiniBars(days: List<DailyHealthEntity>, color: Color, metric: (DailyHealthEntity) -> Number?) {
    val map = days.associateBy { it.date }
    val today = LocalDate.now()
    val points = (6L downTo 0L).map { ago ->
        map[today.minusDays(ago).toString()]?.let(metric)?.toFloat()?.takeIf { it > 0f }
    }
    val max = points.filterNotNull().maxOrNull()?.coerceAtLeast(1f) ?: 1f
    Row(modifier = Modifier.fillMaxWidth().height(43.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.Bottom) {
        points.forEach { p ->
            Box(modifier = Modifier.weight(1f).height(
                if (p == null) 3.dp else (6f + 36f * p / max).dp
            ).background(if (p == null) C.textSecondary.copy(alpha = 0.18f) else color,
                RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)))
        }
    }
}

@Composable
private fun TinyTrend(title: String, summary: String, icon: ImageVector, color: Color,
                      days: List<DailyHealthEntity>, metric: (DailyHealthEntity) -> Number?,
                      modifier: Modifier, onClick: () -> Unit) {
    TintedCard(modifier = modifier, onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
            Text(summary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            MiniBars(days, color, metric)
            Text("7 derniers jours", style = MaterialTheme.typography.labelSmall, color = C.textSecondary)
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
    val night = days.firstOrNull { it.sleepMinutes != null && it.sleepMinutes > 0 }
    val readiness = recovery.score?.takeIf { recovery.confidence >= 20 }
    val preparation = when (recovery.level) {
        RecoveryLevel.GREEN -> "Bonne préparation"
        RecoveryLevel.ORANGE -> "Journée modérée"
        RecoveryLevel.RED -> "Récupération prioritaire"
        RecoveryLevel.UNKNOWN -> "À calculer"
    }
    val done = if (routine.date == today) routine.completed else emptySet()
    val energy = checkIn?.energy ?: 3
    val pain = checkIn?.pain ?: 0
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(C.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 13.dp, bottom = 22.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Headline("Aujourd'hui", dayLabel()) {
                OutlinedButton(onClick = if (hasPermissions) onSync else onPermissions,
                    enabled = !syncing, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)) {
                    Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (syncing) "En cours" else "Sync", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    MetricTile("Sommeil", sleepText(night?.sleepMinutes),
                        if (night == null) "Données manquantes" else "Dernière nuit",
                        Icons.Rounded.Bedtime, C.rose, Color(0xFF54324E),
                        Color(0xFF302731), Modifier.weight(1f), onTracking)
                    MetricTile("Forme & préparation", readiness?.let { "$it / 100" } ?: "—",
                        if (readiness == null) "Fiabilité insuffisante" else "Fiabilité ${recovery.confidence} %",
                        Icons.Rounded.Favorite, C.sage, Color(0xFF415246),
                        Color(0xFF2C3430), Modifier.weight(1f), onCoach)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    MetricTile("Pas", current?.steps?.toString() ?: "—", "Aujourd'hui",
                        Icons.Rounded.DirectionsWalk, C.sage, C.surface,
                        C.surfaceAlt, Modifier.weight(1f), onTracking)
                    MetricTile("Activité", current?.exerciseMinutes?.let { "$it min" } ?: "—", "Minutes aujourd'hui",
                        Icons.Rounded.SelfImprovement, C.rose, Color(0xFF432E40),
                        C.surface, Modifier.weight(1f), onTracking)
                }
            }
        }
        item {
            PrimaryAction(
                if (recovery.level == RecoveryLevel.RED) "Faire une séance douce" else "Démarrer ma séance",
                Icons.Rounded.PlayArrow, onStart
            )
        }
        item {
            TintedCard(colorStart = Color(0xFF442F42), colorEnd = Color(0xFF302935), onClick = onCoach) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallIcon(Icons.Rounded.Lightbulb, C.rose)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Conseil du coach", fontWeight = FontWeight.SemiBold)
                        Text(recovery.reasons.firstOrNull() ?: preparation,
                            style = MaterialTheme.typography.bodySmall, color = C.textSecondary,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Voir les explications", tint = C.rose)
                }
            }
        }
        item {
            TintedCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Mon ressenti", fontWeight = FontWeight.SemiBold)
                        Text(if (checkIn == null) "À compléter" else "Enregistré",
                            style = MaterialTheme.typography.labelSmall, color = C.sage)
                    }
                    Text("Énergie", style = MaterialTheme.typography.labelMedium, color = C.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                        (1..5).forEach { e ->
                            val selected = checkIn != null && energy == e
                            OutlinedButton(onClick = { onCheckIn(e, pain) }, modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                border = BorderStroke(1.dp, if (selected) C.rose else C.border),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) C.prune else Color.Transparent
                                )) {
                                Text(e.toString(), color = if (selected) C.text else C.textSecondary)
                            }
                        }
                    }
                    Text("Douleurs", style = MaterialTheme.typography.labelMedium, color = C.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("Aucune", "Légères", "Moyennes", "Fortes").forEachIndexed { i, label ->
                            val selected = checkIn != null && pain == i
                            OutlinedButton(onClick = { onCheckIn(energy, i) }, modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp),
                                border = BorderStroke(1.dp, if (selected) C.rose else C.border),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) C.prune else Color.Transparent
                                )) {
                                Text(label, style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1, color = if (selected) C.text else C.textSecondary)
                            }
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                SectionTitle("Habitudes du jour", "${done.size}/3", onPractice)
                TintedCard {
                    Column {
                        val habitNames = listOf(
                            "Bouger régulièrement",
                            if (recovery.level == RecoveryLevel.RED) "Mobilité douce ou repos" else "Séance ou mobilité",
                            "Préparer un coucher régulier"
                        )
                        habitNames.forEachIndexed { index, label ->
                            if (index != 0) HorizontalDivider(color = C.border.copy(alpha = 0.5f))
                            Row(modifier = Modifier.fillMaxWidth().clickable { onToggleHabit(index) }
                                .defaultMinSize(minHeight = 54.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = index in done, onCheckedChange = { onToggleHabit(index) })
                                Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                SectionTitle("Mes tendances", "Voir tout", onTracking)
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    TinyTrend("Sommeil", sleepText(days.take(7).mapNotNull { it.sleepMinutes }.takeIf { it.isNotEmpty() }?.average()?.toLong()),
                        Icons.Rounded.Bedtime, C.rose, days, { it.sleepMinutes }, Modifier.weight(1f), onTracking)
                    TinyTrend("Pas", days.take(7).mapNotNull { it.steps }.takeIf { it.isNotEmpty() }?.average()?.roundToInt()?.toString() ?: "—",
                        Icons.Rounded.DirectionsWalk, C.sage, days, { it.steps }, Modifier.weight(1f), onTracking)
                }
            }
        }
        item {
            Text(
                "Le score Tempo est une estimation de préparation, pas un diagnostic. Les valeurs absentes ne sont pas remplacées par des données fictives.",
                style = MaterialTheme.typography.labelSmall, color = C.textSecondary
            )
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
    LazyColumn(modifier = Modifier.fillMaxSize().background(C.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Headline("Pratiquer", "Détente, mobilité et mouvement") }
        item {
            TintedCard(colorStart = Color(0xFF563552), colorEnd = Color(0xFF302B39)) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Ma séance conseillée", color = C.rose, fontWeight = FontWeight.Medium)
                    Text(plan.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${plan.estimatedMinutes} min • ${plan.items.size} exercices • intensité ${plan.intensity}/10",
                        style = MaterialTheme.typography.bodySmall, color = C.textSecondary)
                    PrimaryAction("Commencer", Icons.Rounded.PlayArrow, onStart)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Choisir une activité")
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    PracticeTile("Respiration", "2 à 4 min", Icons.Rounded.Air, C.rose, Modifier.weight(1f), onBreathing)
                    PracticeTile("Mobilité", "Bouger en douceur", Icons.Rounded.SelfImprovement, C.sage,
                        Modifier.weight(1f), { onExplore(ExerciseCategory.MOBILITY) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    PracticeTile("Étirements", "Relâcher le corps", Icons.Rounded.Spa, C.rose,
                        Modifier.weight(1f), { onExplore(ExerciseCategory.STRETCHING) })
                    PracticeTile("Renforcement", "Stabilité et force", Icons.Rounded.FitnessCenter, C.sage,
                        Modifier.weight(1f), { onExplore(ExerciseCategory.FULL_BODY) })
                }
            }
        }
        item {
            Column {
                SectionTitle("Mes outils")
                TintedCard {
                    Column {
                        ListLink(Icons.Rounded.Book, "Mon programme", "Planifier et adapter ma semaine", onProgram)
                        HorizontalDivider(color = C.border)
                        ListLink(Icons.Rounded.Timer, "Minuteur libre", "Compte à rebours personnalisé", onTimer)
                        HorizontalDivider(color = C.border)
                        ListLink(Icons.Rounded.Tune, "Mes exercices", "Personnaliser la bibliothèque", onManage)
                    }
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
    TintedCard(modifier, C.surface, C.surfaceAlt, onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallIcon(icon, color)
            Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = C.textSecondary,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    val completed = history.count { it.completed }
    LazyColumn(modifier = Modifier.fillMaxSize().background(C.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)) {
        item { Headline("Suivi", "Comprendre ce qui évolue") }
        item {
            TintedCard {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Cette semaine", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricStat("Sommeil moyen",
                            sleepText(days.take(7).mapNotNull { it.sleepMinutes }.takeIf { it.isNotEmpty() }?.average()?.toLong()),
                            Modifier.weight(1f))
                        MetricStat("Séances terminées", completed.toString(), Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                SectionTitle("Aperçu des 7 jours")
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    TinyTrend("Sommeil", sleepText(days.firstOrNull { it.sleepMinutes != null }?.sleepMinutes),
                        Icons.Rounded.Bedtime, C.rose, days, { it.sleepMinutes }, Modifier.weight(1f), onHealth)
                    TinyTrend("Pas", days.firstOrNull()?.steps?.toString() ?: "—",
                        Icons.Rounded.DirectionsWalk, C.sage, days, { it.steps }, Modifier.weight(1f), onHealth)
                }
            }
        }
        item {
            Column {
                SectionTitle("Explorer mes données")
                TintedCard {
                    Column {
                        ListLink(Icons.Rounded.Favorite, "Santé", "Sommeil, FC repos, VFC et respiration", onHealth)
                        HorizontalDivider(color = C.border)
                        ListLink(Icons.Rounded.DirectionsWalk, "Activité", "Pas, distance et minutes d'exercice", onHealth)
                        HorizontalDivider(color = C.border)
                        ListLink(Icons.Rounded.BarChart, "Séances", "Fréquence, durée et progression", onProgress)
                    }
                }
            }
        }
        item {
            Text("Les graphiques montrent uniquement les mesures disponibles. Les jours manquants restent visibles.",
                style = MaterialTheme.typography.labelSmall, color = C.textSecondary)
        }
    }
}

@Composable
private fun MetricStat(title: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = C.textSecondary)
        Spacer(Modifier.height(5.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
        item { Headline("Moi", "Mon espace personnel, sans compte imposé") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle("Personnalisation")
                TintedCard {
                    Column {
                        ListLink(Icons.Rounded.Edit, "Mes exercices", "Créer, modifier et organiser", onExercises)
                        HorizontalDivider(color = C.border)
                        ListLink(Icons.Rounded.Spa, "Mes catégories", "Activer, personnaliser les icônes", onCategories)
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
                        ListLink(Icons.Rounded.Favorite, "Santé Connect", "Synchronisation, autorisations et mesures", onHealth)
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
                        ListLink(Icons.Rounded.Update, "Vérifier les mises à jour", "Rechercher une nouvelle version", onUpdates)
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
        item { Headline("Exercices", "Choisis librement une pratique adaptée à ton confort") }
        if (exercises.isEmpty()) {
            item { Text("Aucun exercice disponible dans cette catégorie.",
                color = C.textSecondary) }
        }
        items(exercises, key = { it.id }) { exercise ->
            TintedCard(onClick = { onStart(exercise) }) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallIcon(Icons.Rounded.SelfImprovement, C.rose)
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
