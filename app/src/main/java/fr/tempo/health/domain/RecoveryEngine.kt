package fr.tempo.health.domain

import fr.tempo.health.data.DailyCheckInEntity
import fr.tempo.health.data.DailyHealthEntity
import kotlin.math.abs
import kotlin.math.roundToInt

enum class RecoveryLevel { GREEN, ORANGE, RED, UNKNOWN }
enum class RecoveryFactorState { POSITIVE, NEUTRAL, NEGATIVE, MISSING }

data class RecoveryFactor(
    val label: String,
    val value: String,
    val reference: String,
    val state: RecoveryFactorState,
    val impact: Int
)

data class RecoveryResult(
    val score: Int?,
    val level: RecoveryLevel,
    val reasons: List<String>,
    val sessionTitle: String,
    val sessionMinutes: Int,
    val intensity: Int,
    val confidence: Int = 0,
    val baselineDays: Int = 0,
    val factors: List<RecoveryFactor> = emptyList()
)

object RecoveryEngine {
    fun calculate(
        days: List<DailyHealthEntity>,
        checkIn: DailyCheckInEntity?
    ): RecoveryResult {
        val latest = days.firstOrNull()
            ?: return RecoveryResult(
                score = null,
                level = RecoveryLevel.UNKNOWN,
                reasons = listOf("Synchronise Santé Connect pour calculer ta récupération."),
                sessionTitle = "Mobilité douce",
                sessionMinutes = 10,
                intensity = 2
            )

        val baseline = days.drop(1).take(27)
        var score = 70
        val reasons = mutableListOf<String>()
        val factors = mutableListOf<RecoveryFactor>()
        var availableSignals = 0

        val sleepBase = baseline
            .mapNotNull { it.sleepMinutes?.toDouble() }
            .averageOrNull()

        compareHigherIsBetter(
            current = latest.sleepMinutes?.toDouble(),
            baseline = sleepBase,
            label = "Sommeil",
            lowPenalty = 15,
            highBonus = 5,
            format = { value -> formatDuration(value.roundToInt()) }
        ).let { result ->
            score += result.impact
            factors += result.factor
            result.reason?.let(reasons::add)
            if (result.factor.state != RecoveryFactorState.MISSING) availableSignals++
        }

        val hrvBase = baseline.mapNotNull { it.hrvRmssdMs }.averageOrNull()
        compareHigherIsBetter(
            current = latest.hrvRmssdMs,
            baseline = hrvBase,
            label = "HRV",
            lowPenalty = 15,
            highBonus = 5,
            format = { value -> value.roundToInt().toString() + " ms" }
        ).let { result ->
            score += result.impact
            factors += result.factor
            result.reason?.let(reasons::add)
            if (result.factor.state != RecoveryFactorState.MISSING) availableSignals++
        }

        val rhrBase = baseline.mapNotNull { it.restingHeartRate }.averageOrNull()
        val rhr = latest.restingHeartRate
        if (rhr != null && rhrBase != null) {
            val diff = rhr - rhrBase
            val impact = when {
                diff >= 8 -> -15
                diff >= 4 -> -8
                diff <= -3 -> 4
                else -> 0
            }
            val state = when {
                impact > 0 -> RecoveryFactorState.POSITIVE
                impact < 0 -> RecoveryFactorState.NEGATIVE
                else -> RecoveryFactorState.NEUTRAL
            }
            score += impact
            availableSignals++
            factors += RecoveryFactor(
                label = "FC repos",
                value = rhr.roundToInt().toString() + " bpm",
                reference = rhrBase.roundToInt().toString() + " bpm",
                state = state,
                impact = impact
            )
            when {
                diff >= 8 -> reasons += "FC repos nettement au-dessus de ta référence."
                diff >= 4 -> reasons += "FC repos un peu élevée."
                diff <= -3 -> reasons += "FC repos favorable par rapport à ta référence."
            }
        } else {
            factors += RecoveryFactor(
                label = "FC repos",
                value = "—",
                reference = "Référence insuffisante",
                state = RecoveryFactorState.MISSING,
                impact = 0
            )
        }

        val recentExercise = days.take(3).sumOf { it.exerciseMinutes ?: 0L }
        val loadImpact = when {
            recentExercise >= 150 -> -10
            recentExercise >= 90 -> -5
            else -> 0
        }
        score += loadImpact
        availableSignals++
        factors += RecoveryFactor(
            label = "Charge 3 jours",
            value = recentExercise.toString() + " min",
            reference = "< 90 min = faible",
            state = if (loadImpact < 0) {
                RecoveryFactorState.NEGATIVE
            } else {
                RecoveryFactorState.NEUTRAL
            },
            impact = loadImpact
        )
        if (recentExercise >= 150) {
            reasons += "Charge sportive élevée sur les 3 derniers jours."
        } else if (recentExercise >= 90) {
            reasons += "Charge sportive récente modérée."
        }

        if (checkIn != null) {
            val energyImpact = (checkIn.energy - 3) * 5
            score += energyImpact
            availableSignals++
            factors += RecoveryFactor(
                label = "Énergie",
                value = checkIn.energy.toString() + "/5",
                reference = "3/5",
                state = when {
                    checkIn.energy >= 4 -> RecoveryFactorState.POSITIVE
                    checkIn.energy <= 2 -> RecoveryFactorState.NEGATIVE
                    else -> RecoveryFactorState.NEUTRAL
                },
                impact = energyImpact
            )

            val painImpact = when (checkIn.pain) {
                1 -> -5
                2 -> -15
                3 -> -30
                else -> 0
            }
            score += painImpact
            availableSignals++
            factors += RecoveryFactor(
                label = "Douleurs",
                value = when (checkIn.pain) {
                    0 -> "Aucune"
                    1 -> "Légères"
                    2 -> "Moyennes"
                    else -> "Importantes"
                },
                reference = "Aucune",
                state = if (painImpact < 0) {
                    RecoveryFactorState.NEGATIVE
                } else {
                    RecoveryFactorState.NEUTRAL
                },
                impact = painImpact
            )

            when (checkIn.pain) {
                1 -> reasons += "Douleurs légères signalées."
                2 -> reasons += "Douleurs moyennes : intensité réduite."
                3 -> reasons += "Douleurs importantes : priorité à la récupération."
            }
            if (checkIn.energy <= 2) reasons += "Énergie ressentie basse."
            if (checkIn.energy >= 4) reasons += "Bonne énergie ressentie."
        } else {
            factors += RecoveryFactor(
                label = "Ressenti",
                value = "Non renseigné",
                reference = "Check-in du jour",
                state = RecoveryFactorState.MISSING,
                impact = 0
            )
            reasons += "Ajoute ton ressenti pour affiner le score."
        }

        score = score.coerceIn(0, 100)

        val level = when {
            score >= 70 -> RecoveryLevel.GREEN
            score >= 45 -> RecoveryLevel.ORANGE
            else -> RecoveryLevel.RED
        }

        val recommendation = when (level) {
            RecoveryLevel.GREEN -> Triple("Renforcement + mobilité", 24, 6)
            RecoveryLevel.ORANGE -> Triple("Full body léger + mobilité", 18, 4)
            RecoveryLevel.RED -> Triple("Mobilité + récupération", 12, 2)
            RecoveryLevel.UNKNOWN -> Triple("Mobilité douce", 10, 2)
        }

        val baselineDays = baseline.count { day ->
            day.sleepMinutes != null ||
                day.hrvRmssdMs != null ||
                day.restingHeartRate != null
        }

        val confidence = (
            (availableSignals / 6.0 * 65.0) +
                (baselineDays.coerceAtMost(14) / 14.0 * 35.0)
            ).roundToInt().coerceIn(0, 100)

        return RecoveryResult(
            score = score,
            level = level,
            reasons = reasons.distinct().take(4),
            sessionTitle = recommendation.first,
            sessionMinutes = recommendation.second,
            intensity = recommendation.third,
            confidence = confidence,
            baselineDays = baselineDays,
            factors = factors
        )
    }

    private data class ComparisonResult(
        val impact: Int,
        val factor: RecoveryFactor,
        val reason: String?
    )

    private fun compareHigherIsBetter(
        current: Double?,
        baseline: Double?,
        label: String,
        lowPenalty: Int,
        highBonus: Int,
        format: (Double) -> String
    ): ComparisonResult {
        if (current == null || baseline == null || baseline <= 0.0) {
            return ComparisonResult(
                impact = 0,
                factor = RecoveryFactor(
                    label = label,
                    value = current?.let(format) ?: "—",
                    reference = "Référence insuffisante",
                    state = RecoveryFactorState.MISSING,
                    impact = 0
                ),
                reason = null
            )
        }

        val ratio = current / baseline
        val impact: Int
        val state: RecoveryFactorState
        val reason: String?

        when {
            ratio < 0.80 -> {
                impact = -lowPenalty
                state = RecoveryFactorState.NEGATIVE
                reason = "$label nettement sous ta référence."
            }
            ratio < 0.92 -> {
                impact = -(lowPenalty / 2)
                state = RecoveryFactorState.NEGATIVE
                reason = "$label légèrement sous ta référence."
            }
            ratio > 1.08 -> {
                impact = highBonus
                state = RecoveryFactorState.POSITIVE
                reason = "$label au-dessus de ta référence."
            }
            else -> {
                impact = 0
                state = RecoveryFactorState.NEUTRAL
                reason = null
            }
        }

        return ComparisonResult(
            impact = impact,
            factor = RecoveryFactor(
                label = label,
                value = format(current),
                reference = format(baseline),
                state = state,
                impact = impact
            ),
            reason = reason
        )
    }

    private fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val rest = minutes % 60
        return if (hours > 0) {
            hours.toString() + " h " + rest.toString().padStart(2, '0')
        } else {
            rest.toString() + " min"
        }
    }

    private fun List<Double>.averageOrNull(): Double? =
        if (isEmpty()) null else average()
}
