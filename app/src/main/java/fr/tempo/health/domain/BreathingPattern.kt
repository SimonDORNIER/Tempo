package fr.tempo.health.domain

enum class BreathPhase {
    INHALE,
    HOLD_AFTER_INHALE,
    EXHALE,
    HOLD_AFTER_EXHALE
}

data class BreathPhaseState(
    val phase: BreathPhase,
    val phaseDurationSeconds: Int,
    val remainingSeconds: Int,
    val cycleIndex: Int,
    val stepIndex: Int
)

/** 0 secondes désactive complètement la phase correspondante. */
data class BreathingPattern(
    val inhaleSeconds: Int = 4,
    val holdAfterInhaleSeconds: Int = 0,
    val exhaleSeconds: Int = 6,
    val holdAfterExhaleSeconds: Int = 0
) {
    val cycleSeconds: Int
        get() = inhaleSeconds + holdAfterInhaleSeconds + exhaleSeconds + holdAfterExhaleSeconds

    val canStart: Boolean
        get() = inhaleSeconds > 0 || exhaleSeconds > 0

    fun locate(elapsedSeconds: Int): BreathPhaseState? {
        if (!canStart || cycleSeconds <= 0) return null
        val seconds = elapsedSeconds.coerceAtLeast(0)
        val cycleIndex = seconds / cycleSeconds
        var offset = seconds % cycleSeconds
        val phases = listOf(
            BreathPhase.INHALE to inhaleSeconds,
            BreathPhase.HOLD_AFTER_INHALE to holdAfterInhaleSeconds,
            BreathPhase.EXHALE to exhaleSeconds,
            BreathPhase.HOLD_AFTER_EXHALE to holdAfterExhaleSeconds
        )
        phases.forEachIndexed { index, (phase, duration) ->
            if (duration > 0) {
                if (offset < duration) {
                    return BreathPhaseState(
                        phase = phase,
                        phaseDurationSeconds = duration,
                        remainingSeconds = duration - offset,
                        cycleIndex = cycleIndex,
                        stepIndex = cycleIndex * 4 + index
                    )
                }
                offset -= duration
            }
        }
        return null
    }
}
