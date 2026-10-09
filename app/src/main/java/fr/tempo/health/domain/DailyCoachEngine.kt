package fr.tempo.health.domain

import fr.tempo.health.data.DailyCheckInEntity
import fr.tempo.health.data.DailyHealthEntity

data class DailyCoachPlan(
    val emoji: String,
    val headline: String,
    val sessionGuidance: String,
    val reasons: List<String>,
    val actions: List<String>,
    val evening: String
)

object DailyCoachEngine {
    fun build(
        recovery: RecoveryResult,
        latest: DailyHealthEntity?,
        checkIn: DailyCheckInEntity?,
        workoutPlan: WorkoutPlan
    ): DailyCoachPlan {
        val actions = mutableListOf<String>()

        val headline: String
        val emoji: String
        val sessionGuidance: String

        when (recovery.level) {
            RecoveryLevel.GREEN -> {
                emoji = "🟢"
                headline = "Journée normale"
                sessionGuidance =
                    "Tu peux faire « " + workoutPlan.title + " » autour de " +
                        workoutPlan.estimatedMinutes + " min, intensité " +
                        workoutPlan.intensity + "/10."
                actions += "Fais la séance prévue si tu te sens toujours bien au démarrage."
            }

            RecoveryLevel.ORANGE -> {
                emoji = "🟠"
                headline = "Allège un peu"
                sessionGuidance =
                    "Privilégie « " + workoutPlan.title + " » sans chercher la performance."
                actions += "Garde une intensité confortable et remplace un exercice si nécessaire."
            }

            RecoveryLevel.RED -> {
                emoji = "🔴"
                headline = "Récupération prioritaire"
                sessionGuidance =
                    "Reste sur mobilité douce, marche facile ou repos selon ton ressenti."
                actions += "Évite aujourd'hui les efforts intenses ou prolongés."
            }

            RecoveryLevel.UNKNOWN -> {
                emoji = "⚪"
                headline = "Données à compléter"
                sessionGuidance =
                    "Tempo manque de données pour recommander une vraie intensité."
                actions += "Synchronise Santé Connect et renseigne ton ressenti du jour."
            }
        }

        val sleep = latest?.sleepMinutes
        if (sleep != null && sleep < 360) {
            actions += "Sommeil court : protège surtout ton énergie et évite de forcer."
        } else if (sleep != null && sleep >= 450) {
            actions += "Sommeil solide : profite-en pour bouger régulièrement dans la journée."
        }

        val steps = latest?.steps
        if (steps != null && steps < 3_000L) {
            actions += "Si possible, ajoute 10 à 15 min de marche tranquille."
        }

        if (checkIn?.pain != null && checkIn.pain >= 2) {
            actions += "Ne poursuis pas un mouvement qui augmente nettement une douleur."
        } else if (checkIn?.energy != null && checkIn.energy <= 2) {
            actions += "Énergie basse : fractionne l'activité en petits blocs faciles."
        }

        if (actions.size < 3) {
            actions += "Évite de rester assis trop longtemps : bouge 2 à 3 min régulièrement."
        }

        val reasons = recovery.reasons
            .filter { it.isNotBlank() }
            .take(3)
            .ifEmpty {
                listOf(
                    "La recommandation utilise tes données locales et ton historique récent."
                )
            }

        val evening = when (recovery.level) {
            RecoveryLevel.GREEN ->
                "Ce soir : note ton niveau de fatigue et garde 5 min de retour au calme si tu as fait la séance."
            RecoveryLevel.ORANGE ->
                "Ce soir : privilégie récupération, hydratation habituelle et coucher régulier."
            RecoveryLevel.RED ->
                "Ce soir : fais simple, relâche les tensions et vise une nuit complète."
            RecoveryLevel.UNKNOWN ->
                "Ce soir : complète les données manquantes pour améliorer la recommandation de demain."
        }

        return DailyCoachPlan(
            emoji = emoji,
            headline = headline,
            sessionGuidance = sessionGuidance,
            reasons = reasons,
            actions = actions.distinct().take(4),
            evening = evening
        )
    }
}
