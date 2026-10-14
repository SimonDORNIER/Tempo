package fr.tempo.health.domain

data class BreathingGuide(val exerciseId: String, val title: String, val description: String, val inhaleSeconds: Int, val exhaleSeconds: Int, val durationSeconds: Int)

object BreathingGuides {
    val all = listOf(
        BreathingGuide("breathing-360", "Respiration abdominale", "Se recentrer, relâcher les épaules", 4, 6, 180),
        BreathingGuide("breath-calm-46", "Expiration douce", "Ralentir le rythme sans forcer", 4, 6, 180),
        BreathingGuide("breath-balanced-44", "Respiration équilibrée", "Pause attentive au rythme régulier", 4, 4, 120),
        BreathingGuide("breath-light-35", "Retour au calme", "Rythme confortable et progressif", 3, 5, 120),
        BreathingGuide("breath-night-46", "Pause du soir", "Installer une transition tranquille", 4, 6, 240)
    )
}
