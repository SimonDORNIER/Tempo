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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
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
    val dayDate = LocalDate.now().minusDays(offset.toLong())
    val day = days.firstOrNull { it.date == dayDate.toString() }
    val dateLabel = dayDate.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Mes mesures en détail", fontWeight = FontWeight.Bold, fontSize = 19.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { offset = (offset + 1).coerceAtMost(27) },
                enabled = offset < 27, modifier = Modifier.weight(1f)) { Text("‹ Veille") }
            Column(modifier = Modifier.weight(1.4f).padding(top = 8.dp)) {
                Text(dateLabel.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            OutlinedButton(onClick = { offset = (offset - 1).coerceAtLeast(0) },
                enabled = offset > 0, modifier = Modifier.weight(1f)) { Text("Jour ›") }
        }

        DataDetailCard("Sommeil et qualité", C.violetGlow) {
            if (day?.sleepStartEpochMs == null || day.sleepEndEpochMs == null) {
                Text("Pas de nuit mesurée pour ce jour.", color = C.textSecondary)
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
                Text(
                    "Sommeil : " + minutesLabel(day.sleepMinutes) +
                        "  •  Au lit : " + minutesLabel(bedMinutes),
                    fontWeight = FontWeight.SemiBold
                )
                Text("De " + timeLabel(day.sleepStartEpochMs) + " à " + timeLabel(day.sleepEndEpochMs),
                    color = C.textSecondary, fontSize = 13.sp)
                MetricPair("Qualité — efficacité du sommeil",
                    efficiency?.let { "$it %" } ?: "Non mesurable",
                    "Temps endormi / temps au lit, si les phases sont suffisamment renseignées.")
                if (phases.isNotEmpty()) {
                    SleepPhasesBar(phases, day.sleepStartEpochMs, day.sleepEndEpochMs)
                    Text("Éveil • Léger • Profond • Paradoxal",
                        color = C.textSecondary, fontSize = 12.sp)
                }
                SleepStageRow("Profond", day.deepMinutes, day.sleepMinutes, C.violetGlow)
                SleepStageRow("Léger", day.lightMinutes, day.sleepMinutes, C.textSecondary)
                SleepStageRow("Paradoxal", day.remMinutes, day.sleepMinutes, C.greenGlow)
                SleepStageRow("Éveillé", day.awakeMinutes, bedMinutes, C.rose)
                Text("L'efficacité n'est pas un score Fitbit ni une évaluation médicale. Les phases dépendent des données de la montre.",
                    color = C.textSecondary, fontSize = 12.sp)
            }
        }

        DataDetailCard("Cœur et récupération", C.rose) {
            MetricPair("Pouls du jour • moyenne", numberLabel(day?.pulseAverageBpm, " bpm"),
                "Minimum : " + numberLabel(day?.pulseMinBpm, " bpm") +
                    "   Maximum : " + numberLabel(day?.pulseMaxBpm, " bpm"))
            MetricPair("Pouls au repos", numberLabel(day?.restingHeartRate, " bpm"),
                "Valeur agrégée sur la journée")
            MetricPair("Pouls pendant le sommeil", numberLabel(day?.overnightHeartRate, " bpm"),
                "Moyenne sur la session de sommeil")
            MetricPair("Variabilité cardiaque (VFC)", numberLabel(day?.hrvRmssdMs, " ms"),
                "RMSSD : mesure moyenne disponible, à interpréter selon tes habitudes")
        }

        DataDetailCard("Respiration et oxygénation", C.greenGlow) {
            MetricPair("Fréquence respiratoire", decimalLabel(day?.respiratoryRate, " resp/min"),
                "Moyenne nocturne si une nuit est mesurée, sinon moyenne du jour")
            MetricPair("Saturation en oxygène", decimalLabel(day?.oxygenSaturationPct, " %"),
                "Moyenne des relevés reçus ; pas une mesure continue")
            MetricPair("Écart de température cutanée", signedLabel(day?.skinTemperatureDeltaC),
                "Écart par rapport à la référence du capteur, pas la température du corps")
        }

        DataDetailCard("Activité", C.greenGlow) {
            MetricPair("Pas", day?.steps?.toString() ?: "Pas de mesure", "Sur la journée")
            MetricPair("Distance", decimalLabel(day?.distanceMeters?.div(1000.0), " km"), "Sur la journée")
            MetricPair("Minutes de séances enregistrées", day?.exerciseMinutes?.let { "$it min" } ?: "Pas de mesure",
                "Ne représente pas toutes les minutes d'activité douce")
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
            DetailTrend("Respiration", days, period, TempoMetricKind.OTHER) { it.respiratoryRate }
            Text("Les jours sans données restent vides. Ces tendances n'établissent pas de diagnostic.",
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
private fun MetricPair(title: String, value: String, caption: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = C.textSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(value, color = C.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Text(caption, color = C.textSecondary, fontSize = 11.sp)
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
    kind: TempoMetricKind, value: (DailyHealthEntity) -> Double?
) {
    val points = metricByCalendarDay(days, period, value)
    val populated = points.filterNotNull()
    Text(title + " • " + populated.size + "/" + period + " jours",
        color = C.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    TempoTrendGraph(values = points, metric = kind, usual = null,
        kind = TempoChartKind.LINE, height = 75.dp)
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
