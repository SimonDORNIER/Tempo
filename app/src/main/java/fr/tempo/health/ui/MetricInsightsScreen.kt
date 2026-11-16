package fr.tempo.health.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.tempo.health.data.DailyHealthEntity
import fr.tempo.health.ui.theme.TempoColors as C
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Fiches ouvertes directement depuis une statistique de l'accueil ou du Suivi.
 * Seules les valeurs réellement stockées au jour sont utilisées : pas de courbe horaire inventée.
 */
enum class HealthMetric(val route: String, val title: String, val unit: String,
                        val kind: TempoMetricKind, val bars: Boolean = false) {
    SLEEP("sleep", "Sommeil", "min", TempoMetricKind.SLEEP, true),
    STEPS("steps", "Pas", "pas", TempoMetricKind.STEPS, true),
    RESTING_PULSE("resting-pulse", "FC au repos", "bpm", TempoMetricKind.RESTING_PULSE),
    HRV("hrv", "Variabilité cardiaque", "ms", TempoMetricKind.HRV),
    RESPIRATION("respiration", "Respiration", "resp/min", TempoMetricKind.RESPIRATION),
    OXYGEN("oxygen", "Oxygénation", "%", TempoMetricKind.OXYGEN_SATURATION),
    ACTIVITY("activity", "Minutes d'activité", "min", TempoMetricKind.ACTIVE_MINUTES, true),
    DISTANCE("distance", "Distance", "km", TempoMetricKind.OTHER, true);

    fun read(day: DailyHealthEntity): Double? = when (this) {
        SLEEP -> day.sleepMinutes?.toDouble()
        STEPS -> day.steps?.toDouble()
        RESTING_PULSE -> day.restingHeartRate
        HRV -> day.hrvRmssdMs
        RESPIRATION -> day.respiratoryRate
        OXYGEN -> day.oxygenSaturationPct
        ACTIVITY -> day.exerciseMinutes?.toDouble()
        DISTANCE -> day.distanceMeters?.div(1000.0)
    }

    fun display(value: Double?): String {
        if (value == null || !value.isFinite()) return "—"
        return when (this) {
            SLEEP -> {
                val minutes = value.roundToInt()
                "${minutes / 60} h ${(minutes % 60).toString().padStart(2, '0')}"
            }
            STEPS -> String.format(Locale.FRANCE, "%,d pas", value.roundToInt())
            RESTING_PULSE -> "${value.roundToInt()} bpm"
            HRV -> "${value.roundToInt()} ms"
            RESPIRATION -> String.format(Locale.FRANCE, "%.1f resp/min", value)
            OXYGEN -> String.format(Locale.FRANCE, "%.1f %%", value)
            ACTIVITY -> "${value.roundToInt()} min"
            DISTANCE -> String.format(Locale.FRANCE, "%.2f km", value)
        }
    }

    companion object {
        fun fromRoute(route: String?): HealthMetric? = entries.firstOrNull { it.route == route }
    }
}

@Composable
fun MetricInsightsScreen(
    days: List<DailyHealthEntity>,
    metric: HealthMetric,
    onAllMetrics: () -> Unit,
    hasHealthPermissions: Boolean = true,
    missingPermissionLabels: List<String> = emptyList(),
    syncing: Boolean = false,
    syncMessage: String? = null,
    onSync: () -> Unit = {},
    onPermissions: () -> Unit = {}
) {
    var period by remember(metric) { mutableIntStateOf(1) }
    val today = LocalDate.now()
    val indexed = days.associateBy { it.date }
    // La vue du jour a un seul relevé : la courbe retrace donc les 7 derniers jours,
    // sans prétendre fournir une évolution horaire.
    val chartDays = if (period == 28) 28 else 7
    val chart = metricByCalendarDay(days, chartDays) { metric.read(it) }
    val visible = (if (period == 1) 0..0 else 0 until period).map { offset ->
        today.minusDays(offset.toLong()) to
            indexed[today.minusDays(offset.toLong()).toString()]?.let(metric::read)
    }
    val recorded = visible.mapNotNull { it.second?.takeIf(Double::isFinite) }
    val displayValue = when {
        period == 1 -> recorded.firstOrNull()
        metric == HealthMetric.STEPS || metric == HealthMetric.ACTIVITY ||
            metric == HealthMetric.DISTANCE -> recorded.sum().takeIf { recorded.isNotEmpty() }
        else -> recorded.average().takeIf { recorded.isNotEmpty() }
    }
    val comparisonValue = if (period == 1) {
        visible.firstOrNull()?.second
    } else {
        recorded.average().takeIf { recorded.isNotEmpty() }
    }
    val reference = TempoHealthSignals.reference(days.drop(7).mapNotNull(metric::read))
    val isCurrentActivity = period == 1 &&
        (metric == HealthMetric.STEPS || metric == HealthMetric.ACTIVITY)
    val signal = TempoHealthSignals.assessDaily(
        metric.kind, comparisonValue, reference, isCurrentActivity
    )
    val color = TempoHealthSignals.color(signal)
    val chartKind = if (metric.bars) TempoChartKind.BARS else TempoChartKind.LINE
    val hrvUnavailable = metric == HealthMetric.HRV && days.none { it.hrvRmssdMs != null }
    val hrvPermissionMissing = !hasHealthPermissions || "VFC / HRV" in missingPermissionLabels
    val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRANCE)
    val title = when (period) {
        1 -> "Aujourd'hui"
        7 -> "7 derniers jours"
        else -> "28 derniers jours"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(C.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(metric.title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = C.text)
                Text("Mesures enregistrées", fontSize = 13.sp, color = C.textSecondary)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to "Jour", 7 to "7 jours", 28 to "28 jours").forEach { (p, name) ->
                    FilterChip(selected = period == p, onClick = { period = p },
                        label = { Text(name) })
                }
            }
        }
        if (hrvUnavailable) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = C.surface),
                    border = BorderStroke(1.dp, C.amber.copy(alpha = 0.45f))
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            if (hrvPermissionMissing) "Accès VFC manquant"
                            else "Aucune VFC importée",
                            color = C.text, fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (hrvPermissionMissing)
                                "Autorise Tempo Coach à lire la VFC dans Santé Connect."
                            else
                                "Aucune mesure trouvée sur les 28 derniers jours. Vérifie que Google Health partage la VFC avec Santé Connect.",
                            color = C.textSecondary, fontSize = 13.sp
                        )
                        if (syncMessage?.contains("impossible", ignoreCase = true) == true ||
                            syncMessage?.contains("erreur", ignoreCase = true) == true) {
                            Text(syncMessage, color = C.amber, fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = if (hrvPermissionMissing) onPermissions else onSync,
                            enabled = !syncing,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (syncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp, color = C.action
                                )
                            } else {
                                Text(if (hrvPermissionMissing)
                                    "Autoriser la VFC" else "Resynchroniser les 28 jours")
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = C.surface),
                border = BorderStroke(1.dp, color.copy(alpha = 0.55f))
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, color = C.textSecondary, fontSize = 13.sp)
                    Text(metric.display(displayValue), color = if (metric.kind == TempoMetricKind.OTHER) C.text else color,
                        fontSize = 31.sp, fontWeight = FontWeight.Bold)
                    Text(
                        when {
                            period == 1 -> today.format(dateFormatter).replaceFirstChar { it.uppercase() }
                            metric == HealthMetric.STEPS || metric == HealthMetric.ACTIVITY ||
                                metric == HealthMetric.DISTANCE -> "Total sur la période"
                            else -> "Moyenne sur la période"
                        },
                        color = C.textSecondary, fontSize = 12.sp
                    )
                    if (period == 1 && isCurrentActivity) {
                        Text("Journée en cours", color = C.textSecondary, fontSize = 12.sp)
                    } else if (comparisonValue != null && reference != null &&
                        metric.kind != TempoMetricKind.OTHER) {
                        Text(
                            TempoHealthSignals.comparisonLabel(metric.kind, comparisonValue, reference),
                            color = color, fontSize = 13.sp
                        )
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = C.surface)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Évolution • ${chartDays} jours", fontWeight = FontWeight.SemiBold,
                        color = C.text)
                    TempoTrendGraph(
                        values = chart, metric = metric.kind, usual = reference,
                        kind = chartKind, height = 145.dp
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${chartDays - 1} j avant", color = C.textSecondary, fontSize = 11.sp)
                        Text("Aujourd'hui", color = C.textSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
        if (period != 1) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = C.surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Statistiques", fontWeight = FontWeight.SemiBold, color = C.text)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PeriodMetricStat("Moy. / jour", metric.display(recorded.average().takeIf {
                                recorded.isNotEmpty()
                            }), Modifier.weight(1f))
                            PeriodMetricStat("Minimum", metric.display(recorded.minOrNull()),
                                Modifier.weight(1f))
                            PeriodMetricStat("Maximum", metric.display(recorded.maxOrNull()),
                                Modifier.weight(1f))
                        }
                        Text("${recorded.size} jour(s) renseigné(s) sur ${period}",
                            color = C.textSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            Text(if (period == 1) "Mesure du jour" else "Valeurs par jour",
                fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = C.text)
        }
        items(visible) { (date, value) ->
            val daySignal = TempoHealthSignals.assessDaily(
                metric.kind, value, reference,
                date == today && (metric == HealthMetric.STEPS ||
                    metric == HealthMetric.ACTIVITY)
            )
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(date.format(dateFormatter).replaceFirstChar { it.uppercase() },
                    color = C.textSecondary, fontSize = 14.sp)
                Text(metric.display(value),
                    fontWeight = FontWeight.SemiBold,
                    color = if (value == null || metric.kind == TempoMetricKind.OTHER)
                        C.text else TempoHealthSignals.color(daySignal), fontSize = 14.sp)
            }
            HorizontalDivider(color = C.border.copy(alpha = 0.35f))
        }
        item {
            Spacer(Modifier.height(4.dp))
            OutlinedButton(onClick = onAllMetrics, modifier = Modifier.fillMaxWidth()) {
                Text("Toutes mes mesures détaillées")
            }
        }
    }
}

@Composable
private fun PeriodMetricStat(title: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = C.textSecondary, fontSize = 11.sp)
        Text(value, fontSize = 13.sp, color = C.text, fontWeight = FontWeight.Bold)
    }
}
