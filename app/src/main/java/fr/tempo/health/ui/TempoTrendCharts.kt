package fr.tempo.health.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.tempo.health.data.DailyHealthEntity
import fr.tempo.health.domain.RecoveryLevel
import fr.tempo.health.ui.theme.TempoColors as C
import java.time.LocalDate
import kotlin.math.max

/** Statut indicatif par rapport aux mesures habituelles, jamais un diagnostic. */
enum class TempoMetricKind { SLEEP, HRV, RESTING_PULSE, STEPS, ACTIVE_MINUTES, OTHER }
enum class TempoSignal { NORMAL, CAUTION, LOW, UNKNOWN }
enum class TempoChartKind { LINE, BARS }

/** Une date manquante reste null (aucune interpolation). */
fun metricByCalendarDay(
    days: List<DailyHealthEntity>,
    period: Int,
    metric: (DailyHealthEntity) -> Double?
): List<Double?> {
    val indexed = days.associateBy { it.date }
    val today = LocalDate.now()
    return (period - 1 downTo 0).map { offset ->
        indexed[today.minusDays(offset.toLong()).toString()]?.let(metric)?.takeIf {
            it.isFinite() && it >= 0.0
        }
    }
}

object TempoHealthSignals {
    /** Minimum de cinq jours de comparaison distincts pour colorer une tendance personnelle. */
    fun reference(values: List<Double>): Double? {
        val cleaned = values.filter { it.isFinite() && it > 0.0 }.sorted()
        if (cleaned.size < 5) return null
        val middle = cleaned.size / 2
        return if (cleaned.size % 2 == 0) {
            (cleaned[middle - 1] + cleaned[middle]) / 2.0
        } else cleaned[middle]
    }

    fun assess(kind: TempoMetricKind, value: Double?, usual: Double?): TempoSignal {
        if (value == null || !value.isFinite()) return TempoSignal.UNKNOWN
        if (usual == null || usual <= 0 || !usual.isFinite()) return TempoSignal.UNKNOWN
        return when (kind) {
            TempoMetricKind.SLEEP -> when {
                value <= usual - 90.0 -> TempoSignal.LOW
                value <= usual - 40.0 -> TempoSignal.CAUTION
                else -> TempoSignal.NORMAL
            }
            TempoMetricKind.HRV -> when {
                value <= usual * 0.80 -> TempoSignal.LOW
                value <= usual * 0.90 -> TempoSignal.CAUTION
                else -> TempoSignal.NORMAL
            }
            TempoMetricKind.RESTING_PULSE -> when {
                value >= usual + 8.0 -> TempoSignal.LOW
                value >= usual + 4.0 -> TempoSignal.CAUTION
                else -> TempoSignal.NORMAL
            }
            // On ne déduit pas l'état de fatigue de l'activité, de la respiration
            // ou des pas. Ces couleurs restent celles de leur catégorie.
            TempoMetricKind.STEPS, TempoMetricKind.ACTIVE_MINUTES, TempoMetricKind.OTHER ->
                TempoSignal.UNKNOWN
        }
    }

    fun color(signal: TempoSignal): Color = when (signal) {
        TempoSignal.NORMAL -> C.good
        TempoSignal.CAUTION -> C.amber
        TempoSignal.LOW -> C.redAlert
        TempoSignal.UNKNOWN -> C.unknown
    }

    /** Couleurs d'identité constantes quel que soit le résultat. */
    fun categoryColor(kind: TempoMetricKind): Color = when (kind) {
        TempoMetricKind.SLEEP -> C.sleep
        TempoMetricKind.HRV, TempoMetricKind.RESTING_PULSE -> C.heart
        TempoMetricKind.STEPS, TempoMetricKind.ACTIVE_MINUTES -> C.activity
        TempoMetricKind.OTHER -> C.calm
    }

    fun displayColor(kind: TempoMetricKind, signal: TempoSignal): Color =
        categoryColor(kind)

    fun label(kind: TempoMetricKind, signal: TempoSignal): String =
        when (kind) {
            TempoMetricKind.SLEEP -> when (signal) {
                TempoSignal.NORMAL -> "Durée habituelle"
                TempoSignal.CAUTION -> "Nuit plus courte"
                TempoSignal.LOW -> "Sommeil nettement réduit"
                TempoSignal.UNKNOWN -> "Référence à construire"
            }
            TempoMetricKind.HRV, TempoMetricKind.RESTING_PULSE -> when (signal) {
                TempoSignal.NORMAL -> "Dans ma plage habituelle"
                TempoSignal.CAUTION -> "Écart modéré"
                TempoSignal.LOW -> "Écart important"
                TempoSignal.UNKNOWN -> "Référence à construire"
            }
            TempoMetricKind.STEPS, TempoMetricKind.ACTIVE_MINUTES -> "Volume d'activité"
            TempoMetricKind.OTHER -> "Mesures disponibles"
        }

    fun recoveryColor(level: RecoveryLevel): Color = when (level) {
        RecoveryLevel.GREEN -> C.good
        RecoveryLevel.ORANGE -> C.amber
        RecoveryLevel.RED -> C.redAlert
        RecoveryLevel.UNKNOWN -> C.unknown
    }
}

/**
 * Courbe native Compose à points (physiologie) ou barres (volumes).
 * Les séries sont de taille fixe 7/28 jours ; aucun segment ne traverse un trou de données.
 * Chaque point de physiologie est coloré selon sa comparaison à la référence individuelle.
 */
@Composable
fun TempoTrendGraph(
    values: List<Double?>,
    metric: TempoMetricKind,
    usual: Double?,
    kind: TempoChartKind,
    modifier: Modifier = Modifier,
    height: Dp = 96.dp
) {
    val valid = values.filterNotNull().filter { it.isFinite() && it >= 0.0 }
    val minValue = valid.minOrNull() ?: 0.0
    val maxValue = valid.maxOrNull() ?: 1.0
    val range = (maxValue - minValue).takeIf { it > 0.00001 } ?: 1.0
    val count = values.size.coerceAtLeast(1)
    val fallback = TempoHealthSignals.categoryColor(metric)

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val pad = 6.dp.toPx()
        val chartHeight = size.height - pad * 2f
        val usableWidth = (size.width - pad * 2f).coerceAtLeast(1f)
        val space = if (count == 1) 0f else usableWidth / (count - 1)
        val pointRadius = if (count > 14) 2.8.dp.toPx() else 4.0.dp.toPx()
        val strokeWidth = if (count > 14) 1.8.dp.toPx() else 2.6.dp.toPx()
        val referenceY = size.height - pad

        drawLine(
            color = C.border.copy(alpha = 0.60f),
            start = Offset(pad, referenceY),
            end = Offset(size.width - pad, referenceY),
            strokeWidth = 1.dp.toPx()
        )
        if (valid.isEmpty()) return@Canvas

        fun xAt(index: Int) = if (count == 1) size.width / 2f else pad + space * index
        fun yAt(value: Double): Float {
            val normalized = (value - minValue) / range
            return (pad + chartHeight * (0.83f - normalized.toFloat() * 0.68f))
                .coerceIn(pad, size.height - pad)
        }
        fun paint(value: Double): Color {
            // Le domaine reste stable : seuls les écarts défavorables
            // deviennent ambre ou rouge sur les points physiologiques.
            val state = TempoHealthSignals.assess(metric, value, usual)
            if (kind == TempoChartKind.BARS) return fallback
            return when (state) {
                TempoSignal.CAUTION -> C.amber
                TempoSignal.LOW -> C.redAlert
                else -> fallback
            }
        }

        when (kind) {
            TempoChartKind.LINE -> {
                // Les traits relient uniquement des dates consécutives renseignées.
                values.forEachIndexed { index, value ->
                    if (index == 0 || value == null || !value.isFinite()) return@forEachIndexed
                    val previous = values[index - 1]
                    if (previous == null || !previous.isFinite()) return@forEachIndexed
                    drawLine(
                        color = paint(value),
                        start = Offset(xAt(index - 1), yAt(previous)),
                        end = Offset(xAt(index), yAt(value)),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
                values.forEachIndexed { index, value ->
                    if (value == null || !value.isFinite()) return@forEachIndexed
                    val center = Offset(xAt(index), yAt(value))
                    drawCircle(color = C.background, radius = pointRadius + 1.7.dp.toPx(), center = center)
                    drawCircle(color = paint(value), radius = pointRadius, center = center)
                }
            }
            TempoChartKind.BARS -> {
                val width = (usableWidth / count * 0.65f).coerceAtLeast(2.5.dp.toPx())
                val ceiling = max(maxValue, 1.0)
                values.forEachIndexed { index, value ->
                    if (value == null || !value.isFinite()) return@forEachIndexed
                    val h = max(3.dp.toPx(), (value / ceiling).toFloat() * chartHeight * 0.9f)
                    val top = size.height - pad - h
                    val color = paint(value)
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(color, color.copy(alpha = 0.57f)),
                            startY = top,
                            endY = size.height - pad
                        ),
                        topLeft = Offset(xAt(index) - width / 2f, top),
                        size = Size(width, h),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
            }
        }
    }
}
