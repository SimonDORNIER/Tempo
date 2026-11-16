package fr.tempo.health.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.tempo.health.data.DailyHealthEntity
import fr.tempo.health.ui.theme.TempoColors as C
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Évolution des mesures importées de Santé Connect. Aucune valeur de remplissage. */
@Composable
fun DetailedHealthMetrics(days: List<DailyHealthEntity>) {
    var offset by remember { mutableIntStateOf(0) }
    var period by remember { mutableIntStateOf(7) }
    var moreTrends by remember { mutableStateOf(false) }
    val dayDate = LocalDate.now().minusDays(offset.toLong())
    val day = days.firstOrNull { it.date == dayDate.toString() }
    val dateLabel = dayDate.format(DateTimeFormatter.ofPattern("EEE d MMMM", Locale.FRANCE))
    fun baseline(get: (DailyHealthEntity) -> Double?): Double? =
        TempoHealthSignals.reference(days.drop(7).mapNotNull(get))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Mes mesures en détail", fontWeight = FontWeight.Bold, fontSize = 19.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = { offset = (offset + 1).coerceAtMost(27) },
                enabled = offset < 27) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Jour précédent")
            }
            Text(
                dateLabel.replaceFirstChar { it.uppercase() },
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp, maxLines = 1
            )
            IconButton(onClick = { offset = (offset - 1).coerceAtLeast(0) },
                enabled = offset > 0) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Jour suivant")
            }
        }

        DataDetailCard("Sommeil et qualité", C.violetGlow) {
            if (day?.sleepStartEpochMs == null || day.sleepEndEpochMs == null) {
                Text("Aucune nuit enregistrée", color = C.textSecondary)
            } else {
                val bedMinutes = ((day.sleepEndEpochMs - day.sleepStartEpochMs) / 60000L)
                    .coerceAtLeast(0)
                val phases = remember(day.sleepStageTimeline) { parseSleepPhases(day.sleepStageTimeline) }
                val classified = phases.filter { it.type != 0 }.sumOf {
                    (it.end - it.start).coerceAtLeast(0) / 60000L
                }
                val reliableStages = bedMinutes > 0 && classified >= bedMinutes * 0.7
                val efficiency = if (reliableStages && day.sleepMinutes != null && bedMinutes > 0) {
                    (100.0 * day.sleepMinutes / bedMinutes).coerceIn(0.0, 100.0).roundToInt()
                } else null
                val sleepValue = day.sleepMinutes?.toDouble()
                val sleepReference = baseline { it.sleepMinutes?.toDouble() }
                val sleepState = TempoHealthSignals.assess(TempoMetricKind.SLEEP,
                    sleepValue, sleepReference)
                Text(
                    (if (reliableStages) "Sommeil mesuré : " else "Session déclarée : ") +
                        minutesLabel(day.sleepMinutes) +
                        "  •  Au lit : " + minutesLabel(bedMinutes),
                    fontWeight = FontWeight.SemiBold,
                    color = TempoHealthSignals.color(sleepState)
                )
                Text(TempoHealthSignals.comparisonLabel(TempoMetricKind.SLEEP,
                    sleepValue, sleepReference),
                    color = TempoHealthSignals.color(sleepState), fontSize = 11.sp)
                Text("De " + timeLabel(day.sleepStartEpochMs) + " à " + timeLabel(day.sleepEndEpochMs),
                    color = C.textSecondary, fontSize = 13.sp)
                MetricPair("Qualité — efficacité du sommeil",
                    efficiency?.let { "$it %" } ?: "Non mesurable",
                    "")
                if (phases.isNotEmpty()) {
                    SleepPhasesBar(phases, day.sleepStartEpochMs, day.sleepEndEpochMs)
                    Text("Éveil • Léger • Profond • Paradoxal",
                        color = C.textSecondary, fontSize = 12.sp)
                }
                SleepStageRow("Profond", day.deepMinutes, day.sleepMinutes, C.violetGlow)
                SleepStageRow("Léger", day.lightMinutes, day.sleepMinutes, C.textSecondary)
                SleepStageRow("Paradoxal", day.remMinutes, day.sleepMinutes, C.greenGlow)
                SleepStageRow("Éveillé", day.awakeMinutes, bedMinutes, C.rose)
                if (phases.isNotEmpty()) {
                    val awakeTypes = setOf(1, 3, 7)
                    val interruptions = phases.indices.count { index ->
                        phases[index].type in awakeTypes &&
                            (index == 0 || phases[index - 1].type !in awakeTypes)
                    }
                    MetricPair("Périodes d'éveil repérées", interruptions.toString(),
                        "Nombre de séquences notées par la source, pas forcément tous les réveils")
                }
                Text("L'efficacité n'est pas un score Fitbit ni une évaluation médicale. Les phases dépendent des données de la montre.",
                    color = C.textSecondary, fontSize = 12.sp)
            }
        }

        DataDetailCard("Cœur et récupération", C.rose) {
            MetricPair("Pouls du jour • moyenne", numberLabel(day?.pulseAverageBpm, " bpm"),
                "Minimum : " + numberLabel(day?.pulseMinBpm, " bpm") +
                    "   Maximum : " + numberLabel(day?.pulseMaxBpm, " bpm"))
            MetricPair("Pouls au repos", numberLabel(day?.restingHeartRate, " bpm"),
                "",
                TempoMetricKind.RESTING_PULSE, day?.restingHeartRate,
                baseline { it.restingHeartRate })
            MetricPair("Pouls pendant le sommeil", numberLabel(day?.overnightHeartRate, " bpm"),
                "")
            MetricPair("Variabilité cardiaque (VFC)", numberLabel(day?.hrvRmssdMs, " ms"),
                "",
                TempoMetricKind.HRV, day?.hrvRmssdMs,
                baseline { it.hrvRmssdMs })
        }

        DataDetailCard("Respiration et oxygénation", C.greenGlow) {
            MetricPair("Fréquence respiratoire", decimalLabel(day?.respiratoryRate, " resp/min"),
                "",
                TempoMetricKind.RESPIRATION, day?.respiratoryRate,
                baseline { it.respiratoryRate })
            MetricPair("Saturation en oxygène", decimalLabel(day?.oxygenSaturationPct, " %"),
                "",
                TempoMetricKind.OXYGEN_SATURATION, day?.oxygenSaturationPct,
                baseline { it.oxygenSaturationPct })
            MetricPair("Écart de température cutanée", signedLabel(day?.skinTemperatureDeltaC),
                "")
        }

        DataDetailCard("Activité", C.greenGlow) {
            MetricPair("Pas", day?.steps?.toString() ?: "Pas de mesure",
                if (offset == 0) "Journée en cours" else "Sur la journée",
                TempoMetricKind.STEPS,
                if (offset == 0) null else day?.steps?.toDouble(), baseline { it.steps?.toDouble() })
            MetricPair("Distance", decimalLabel(day?.distanceMeters?.div(1000.0), " km"))
            MetricPair("Calories", numberLabel(day?.caloriesKcal, " kcal"))
            MetricPair("Minutes de séances enregistrées", day?.exerciseMinutes?.let { "$it min" } ?: "Pas de mesure",
                "Ne représente pas toutes les minutes d'activité douce",
                TempoMetricKind.ACTIVE_MINUTES,
                if (offset == 0) null else day?.exerciseMinutes?.toDouble(), baseline { it.exerciseMinutes?.toDouble() })
            MetricPair("Poids", decimalLabel(day?.weightKg, " kg"))
            MetricPair("VO₂ max", decimalLabel(day?.vo2Max, ""))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = period == 7, onClick = { period = 7 }, label = { Text("7 jours") })
            FilterChip(selected = period == 28, onClick = { period = 28 }, label = { Text("28 jours") })
        }
        DataDetailCard("Tendances cardio • $period jours", C.violetGlow) {
            DetailTrend("FC repos", days, period, TempoMetricKind.RESTING_PULSE) {
                it.restingHeartRate
            }
            DetailTrend("VFC", days, period, TempoMetricKind.HRV) { it.hrvRmssdMs }
            DetailTrend("Pouls moyen", days, period, TempoMetricKind.OTHER) { it.pulseAverageBpm }
            DetailTrend("Respiration", days, period, TempoMetricKind.RESPIRATION) { it.respiratoryRate }
            DetailTrend("SpO₂", days, period, TempoMetricKind.OXYGEN_SATURATION) { it.oxygenSaturationPct }
            DetailTrend("Sommeil", days, period, TempoMetricKind.SLEEP, TempoChartKind.BARS) {
                it.sleepMinutes?.toDouble()
            }
            Text(TempoHealthSignals.LEGEND,
                color = C.textSecondary, fontSize = 12.sp)
        }
        Text("Données locales issues de Santé Connect • dernière synchronisation : " +
            (day?.syncedAtEpochMs?.let(::timeLabel) ?: "indisponible"),
            color = C.textSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun DataDetailCard(title: String, accent: Color, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = C.surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.75f))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = accent, fontSize = 16.sp)
            content()
        }
    }
}

@Composable
private fun MetricPair(
    title: String, value: String, caption: String,
    metricKind: TempoMetricKind? = null, reading: Double? = null, usual: Double? = null
) {
    val signal = metricKind?.let { TempoHealthSignals.assess(it, reading, usual) }
        ?: TempoSignal.UNKNOWN
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = C.textSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(value, color = if (metricKind == null) C.text else TempoHealthSignals.color(signal),
                fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Text(caption, color = C.textSecondary, fontSize = 11.sp)
        if (metricKind != null && reading != null) {
            Text(TempoHealthSignals.comparisonLabel(metricKind, reading, usual),
                color = TempoHealthSignals.color(signal), fontSize = 11.sp)
        }
    }
}

@Composable
private fun SleepStageRow(name: String, minutes: Long?, total: Long?, tint: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name, color = tint, fontSize = 13.sp)
        val pct = if (minutes != null && total != null && total > 0) {
            " (" + (minutes * 100 / total) + " %)"
        } else ""
        Text((minutes?.let { "$it min" } ?: "—") + pct, fontSize = 13.sp)
    }
}

/** Efficacité = sommeil réellement classifié / durée de la session, uniquement avec stades fiables. */
fun sleepEfficiency(day: DailyHealthEntity?): Int? {
    if (day?.sleepStartEpochMs == null || day.sleepEndEpochMs == null ||
        day.sleepMinutes == null) return null
    val minutes = (day.sleepEndEpochMs - day.sleepStartEpochMs) / 60000L
    if (minutes <= 0L) return null
    val classified = parseSleepPhases(day.sleepStageTimeline)
        .filter { it.type != 0 }
        .sumOf { (it.end - it.start).coerceAtLeast(0L) / 60000L }
    if (classified < minutes * 0.7) return null
    return (100.0 * day.sleepMinutes / minutes).coerceIn(0.0, 100.0).roundToInt()
}

private data class SleepPhase(val type: Int, val start: Long, val end: Long)
private fun parseSleepPhases(raw: String?): List<SleepPhase> =
    raw?.split("|")?.mapNotNull { encoded ->
        val parts = encoded.split(",")
        if (parts.size != 3) null else runCatching {
            SleepPhase(parts[0].toInt(), parts[1].toLong(), parts[2].toLong())
        }.getOrNull()
    }?.filter { it.end > it.start } ?: emptyList()

@Composable
private fun SleepPhasesBar(phases: List<SleepPhase>, start: Long, end: Long) {
    val duration = (end - start).coerceAtLeast(1L)
    Canvas(Modifier.fillMaxWidth().height(34.dp).background(C.surfaceAlt, RoundedCornerShape(9.dp))) {
        phases.forEach { phase ->
            val from = ((phase.start - start).toFloat() / duration).coerceIn(0f, 1f)
            val to = ((phase.end - start).toFloat() / duration).coerceIn(0f, 1f)
            val accent = when (phase.type) {
                1, 3, 7 -> C.rose
                4 -> C.violetPale
                5 -> C.violetGlow
                6 -> C.greenGlow
                2 -> C.textSecondary
                else -> C.border
            }
            if (to > from) {
                drawRect(accent, topLeft = Offset(size.width * from, 0f),
                    size = Size(size.width * (to - from), size.height))
            }
        }
    }
}

@Composable
private fun DetailTrend(
    title: String, days: List<DailyHealthEntity>, period: Int,
    kind: TempoMetricKind, chart: TempoChartKind = TempoChartKind.LINE,
    value: (DailyHealthEntity) -> Double?
) {
    val points = metricByCalendarDay(days, period, value)
    val populated = points.filterNotNull()
    Text(title + " • " + populated.size + "/" + period + " jours",
        color = C.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    val usual = TempoHealthSignals.reference(days.drop(7).mapNotNull(value))
    TempoTrendGraph(values = points, metric = kind, usual = usual,
        kind = chart, height = 75.dp)
    val lastComplete = if (kind == TempoMetricKind.STEPS ||
        kind == TempoMetricKind.ACTIVE_MINUTES) points.dropLast(1).lastOrNull { it != null }
        else points.lastOrNull { it != null }
    val state = TempoHealthSignals.assess(kind, lastComplete, usual)
    Text(TempoHealthSignals.comparisonLabel(kind, lastComplete, usual),
        color = TempoHealthSignals.color(state), fontSize = 11.sp)
}

private fun minutesLabel(value: Long?): String =
    value?.let { (it / 60).toString() + " h " + (it % 60).toString().padStart(2, '0') } ?: "—"

private fun numberLabel(value: Double?, unit: String): String =
    value?.takeIf { it.isFinite() }?.roundToInt()?.toString()?.plus(unit) ?: "—"

private fun decimalLabel(value: Double?, unit: String): String =
    value?.takeIf { it.isFinite() }?.let {
        String.format(Locale.FRANCE, "%.1f", it) + unit
    } ?: "—"

private fun signedLabel(value: Double?): String =
    value?.takeIf { it.isFinite() }?.let {
        String.format(Locale.FRANCE, "%+.2f °C", it)
    } ?: "—"

private fun timeLabel(epoch: Long): String =
    Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm", Locale.FRANCE))
