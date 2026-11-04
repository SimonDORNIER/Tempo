package fr.tempo.health.ui

import android.content.Context
import fr.tempo.health.domain.BreathingPattern

/** Préférences de respiration également exportées avec le backup Tempo existant. */
object BreathingExercisePreferences {
    private const val FILE = "tempo-health-settings"
    private const val PREFIX = "breathing-exercise-"

    fun durationMinutes(context: Context, exerciseId: String, defaultMinutes: Int): Int =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getInt(PREFIX + "duration-" + exerciseId, defaultMinutes)
            .coerceIn(1, 60)

    fun saveDuration(context: Context, exerciseId: String, minutes: Int) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putInt(PREFIX + "duration-" + exerciseId, minutes.coerceIn(1, 60))
            .apply()
    }

    fun customPattern(context: Context): BreathingPattern {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return BreathingPattern(
            inhaleSeconds = prefs.getInt(PREFIX + "inhale", 4).coerceIn(0, 60),
            holdAfterInhaleSeconds = prefs.getInt(PREFIX + "hold-in", 0).coerceIn(0, 60),
            exhaleSeconds = prefs.getInt(PREFIX + "exhale", 6).coerceIn(0, 60),
            holdAfterExhaleSeconds = prefs.getInt(PREFIX + "hold-out", 0).coerceIn(0, 60)
        )
    }

    fun saveCustomPattern(context: Context, pattern: BreathingPattern) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putInt(PREFIX + "inhale", pattern.inhaleSeconds.coerceIn(0, 60))
            .putInt(PREFIX + "hold-in", pattern.holdAfterInhaleSeconds.coerceIn(0, 60))
            .putInt(PREFIX + "exhale", pattern.exhaleSeconds.coerceIn(0, 60))
            .putInt(PREFIX + "hold-out", pattern.holdAfterExhaleSeconds.coerceIn(0, 60))
            .apply()
    }
}
