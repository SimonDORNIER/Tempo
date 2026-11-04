package fr.tempo.health.ui

import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.PI

/**
 * Enveloppes audio continues et sans angles vifs : pas de clic lors du
 * démarrage ou de l'arrêt. Aucune ressource externe, aucune permission.
 */
internal object BreathingSoundEnvelope {
    const val SHORT_DURATION_MS = 900
    const val HOLD_DURATION_MS = 850
    const val CONTINUOUS_PREVIEW_MS = 1700

    fun durationMs(
        mode: BreathingCueMode,
        isPause: Boolean,
        remainingSeconds: Int,
        preview: Boolean
    ): Int = when {
        isPause -> HOLD_DURATION_MS
        mode == BreathingCueMode.SHORT -> SHORT_DURATION_MS
        preview -> CONTINUOUS_PREVIEW_MS
        else -> remainingSeconds.coerceAtLeast(1) * 1000
    }

    /** Gain 0–1 ; très longue extinction pour les bips, et fade pour les nappes. */
    fun gain(elapsedMs: Double, durationMs: Int, mode: BreathingCueMode, isPause: Boolean): Double {
        if (durationMs <= 0 || elapsedMs <= 0.0 || elapsedMs >= durationMs) return 0.0
        return when {
            isPause -> {
                // Deux petites clochettes discrètes, séparées par un silence.
                val first = pulse(elapsedMs, 0.0, 360.0)
                val second = pulse(elapsedMs, 450.0, 340.0) * 0.72
                maxOf(first, second)
            }
            mode == BreathingCueMode.SHORT -> {
                val entry = smoothstep(elapsedMs / 165.0)
                val end = smoothstep((durationMs - elapsedMs) / 470.0)
                val decay = exp(-0.34 * elapsedMs / durationMs)
                entry * end * decay
            }
            else -> {
                val entry = smoothstep(elapsedMs / 380.0)
                val end = smoothstep((durationMs - elapsedMs) / 580.0)
                // Très lente respiration d'amplitude, à peine perceptible.
                val movement = 0.96 + 0.04 * sin(2.0 * PI * elapsedMs / 8500.0)
                entry * end * movement
            }
        }
    }

    /** Amplitude modérée même lorsque le volume Tempo est à 100 %. */
    fun peak(mode: BreathingCueMode, isPause: Boolean): Double = when {
        isPause -> 0.24
        mode == BreathingCueMode.SHORT -> 0.38
        else -> 0.24
    }

    private fun pulse(elapsedMs: Double, startMs: Double, lengthMs: Double): Double {
        val t = elapsedMs - startMs
        if (t <= 0.0 || t >= lengthMs) return 0.0
        return smoothstep(t / 105.0) * smoothstep((lengthMs - t) / 190.0)
    }

    private fun smoothstep(x: Double): Double {
        val t = x.coerceIn(0.0, 1.0)
        return t * t * (3.0 - 2.0 * t)
    }
}
