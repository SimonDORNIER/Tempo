package fr.tempo.health.domain

import fr.tempo.health.data.DailyCheckInEntity
import fr.tempo.health.data.DailyHealthEntity

enum class RecoveryLevel { GREEN, ORANGE, RED, UNKNOWN }

data class RecoveryResult(
    val score: Int?,
    val level: RecoveryLevel,
    val reasons: List<String>,
    val sessionTitle: String,
    val sessionMinutes: Int,
    val intensity: Int
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

        compareHigherIsBetter(
            current = latest.sleepMinutes?.toDouble(),
            baseline = baseline.mapNotNull { it.sleepMinutes?.toDouble() }.averageOrNull(),
            label = "sommeil",
            lowPenalty = 15,
            highBonus = 5
        )?.let { (delta, reason) ->
            score += delta
            reasons += reason
        }

        compareHigherIsBetter(
            current = latest.hrvRmssdMs,
            baseline = baseline.mapNotNull { it.hrvRmssdMs }.averageOrNull(),
            label = "HRV",
            lowPenalty = 15,
            highBonus = 5
        )?.let { (delta, reason) ->
            score += delta
            reasons += reason
        }

        val rhrBase = baseline.mapNotNull { it.restingHeartRate }.averageOrNull()
        if (latest.restingHeartRate != null && rhrBase != null) {
            val diff = latest.restingHeartRate - rhrBase
            when {
                diff >= 8 -> {
                    score -= 15
                    reasons += "FC repos nettement au-dessus de ta référence."
                }
                diff >= 4 -> {
                    score -= 8
                    reasons += "FC repos un peu élevée."
                }
                diff <= -3 -> {
                    score += 4
                    reasons += "FC repos favorable par rapport à ta référence."
                }
            }
        }

        val recentExercise = days.take(3).sumOf { it.exerciseMinutes ?: 0L }
        if (recentExercise >= 150) {
            score -= 10
            reasons += "Charge sportive élevée sur les 3 derniers jours."
        } else if (recentExercise >= 90) {
            score -= 5
            reasons += "Charge sportive récente modérée."
        }

        if (checkIn != null) {
            score += (checkIn.energy - 3) * 5
            when (checkIn.pain) {
                1 -> {
                    score -= 5
                    reasons += "Douleurs légères signalées."
                }
                2 -> {
                    score -= 15
                    reasons += "Douleurs moyennes : intensité réduite."
                }
                3 -> {
                    score -= 30
                    reasons += "Douleurs importantes : priorité à la récupération."
                }
            }
            if (checkIn.energy <= 2) reasons += "Énergie ressentie basse."
            if (checkIn.energy >= 4) reasons += "Bonne énergie ressentie."
        } else {
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

        return RecoveryResult(
            score = score,
            level = level,
            reasons = reasons.take(3),
            sessionTitle = recommendation.first,
            sessionMinutes = recommendation.second,
            intensity = recommendation.third
        )
    }

    private fun compareHigherIsBetter(
        current: Double?,
        baseline: Double?,
        label: String,
        lowPenalty: Int,
        highBonus: Int
    ): Pair<Int, String>? {
        if (current == null || baseline == null || baseline <= 0.0) return null
        val ratio = current / baseline
        return when {
            ratio < 0.80 -> -lowPenalty to "$label nettement sous ta référence."
            ratio < 0.92 -> -(lowPenalty / 2) to "$label légèrement sous ta référence."
            ratio > 1.08 -> highBonus to "$label au-dessus de ta référence."
            else -> 0 to "$label proche de ta référence."
        }
    }

    private fun List<Double>.averageOrNull(): Double? =
        if (isEmpty()) null else average()
}
