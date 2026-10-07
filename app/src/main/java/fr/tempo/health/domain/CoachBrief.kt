package fr.tempo.health.domain

import fr.tempo.health.data.DailyCheckInEntity
import fr.tempo.health.data.DailyHealthEntity
import fr.tempo.health.data.WorkoutHistoryEntity
import kotlin.math.abs
import kotlin.math.roundToInt

data class CoachBrief(
    val headline: String,
    val bullets: List<String>,
    val shareText: String
)

object CoachBriefBuilder {
    fun build(
        days: List<DailyHealthEntity>,
        checkIn: DailyCheckInEntity?,
        recovery: RecoveryResult,
        plan: WorkoutPlan,
        history: List<WorkoutHistoryEntity>
    ): CoachBrief {
        val latest = days.firstOrNull()
        val baseline = days.drop(1).take(27)

        val headline = when (recovery.level) {
            RecoveryLevel.GREEN -> "Bonne disponibilité aujourd'hui"
            RecoveryLevel.ORANGE -> "Journée à doser"
            RecoveryLevel.RED -> "Priorité à la récupération"
            RecoveryLevel.UNKNOWN -> "Données encore insuffisantes"
        }

        val bullets = mutableListOf<String>()

        recovery.score?.let {
            bullets += "Récupération " + it + "/100 • confiance " +
                recovery.confidence + "%"
        }

        if (latest != null) {
            compareMetric(
                label = "Sommeil",
                current = latest.sleepMinutes?.toDouble(),
                baseline = baseline.mapNotNull { it.sleepMinutes?.toDouble() }.averageOrNull(),
                format = { value -> formatMinutes(value.roundToInt()) }
            )?.let(bullets::add)

            compareMetric(
                label = "HRV",
                current = latest.hrvRmssdMs,
                baseline = baseline.mapNotNull { it.hrvRmssdMs }.averageOrNull(),
                format = { value -> value.roundToInt().toString() + " ms" }
            )?.let(bullets::add)

            compareMetric(
                label = "FC repos",
                current = latest.restingHeartRate,
                baseline = baseline.mapNotNull { it.restingHeartRate }.averageOrNull(),
                format = { value -> value.roundToInt().toString() + " bpm" },
                lowerIsBetter = true
            )?.let(bullets::add)

            latest.steps?.let {
                bullets += "Activité aujourd'hui : " + it + " pas"
            }
        }

        if (checkIn != null) {
            bullets += "Ressenti : énergie " + checkIn.energy + "/5 • douleurs " +
                when (checkIn.pain) {
                    0 -> "aucunes"
                    1 -> "légères"
                    2 -> "moyennes"
                    else -> "importantes"
                }
        } else {
            bullets += "Ressenti du jour non renseigné"
        }

        val weekCutoff = System.currentTimeMillis() -
            7L * 24L * 60L * 60L * 1000L
        val recentSessions = history.count {
            it.completed && it.startedAtEpochMs >= weekCutoff
        }
        bullets += "Entraînement : " + recentSessions +
            " séance(s) terminée(s) sur 7 jours"
        bullets += "Séance proposée : " + plan.title + " • " +
            plan.estimatedMinutes + " min • intensité " + plan.intensity + "/10"

        val shareText = buildString {
            appendLine("Analyse ce résumé Tempo Health.")
            appendLine("Je veux :")
            appendLine("1) un bilan court de récupération ;")
            appendLine("2) les variations importantes par rapport à mes références personnelles ;")
            appendLine("3) 2 à 4 conseils concrets pour aujourd'hui ;")
            appendLine("4) ton avis sur la séance proposée.")
            appendLine("Ne dramatise pas une variation isolée et signale clairement les limites des données.")
            appendLine()
            appendLine("Résumé :")
            bullets.forEach { appendLine("- " + it) }
            if (recovery.reasons.isNotEmpty()) {
                appendLine(
                    "- Facteurs du score : " +
                        recovery.reasons.joinToString(" • ")
                )
            }
        }.trim()

        return CoachBrief(
            headline = headline,
            bullets = bullets.take(7),
            shareText = shareText
        )
    }

    private fun compareMetric(
        label: String,
        current: Double?,
        baseline: Double?,
        format: (Double) -> String,
        lowerIsBetter: Boolean = false
    ): String? {
        if (current == null || baseline == null || baseline <= 0.0) return null

        val pct = ((current - baseline) / baseline) * 100.0
        val direction = when {
            abs(pct) < 3.0 -> "proche de ta référence"
            lowerIsBetter && pct < 0 -> "favorable"
            lowerIsBetter && pct > 0 -> "au-dessus de ta référence"
            !lowerIsBetter && pct > 0 -> "au-dessus de ta référence"
            else -> "sous ta référence"
        }

        return label + " : " + format(current) + " vs " +
            format(baseline) + " • " +
            (if (pct >= 0) "+" else "") + pct.roundToInt() +
            "% • " + direction
    }

    private fun formatMinutes(minutes: Int): String {
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
