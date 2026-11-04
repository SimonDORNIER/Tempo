package fr.tempo.health.ui

import fr.tempo.health.domain.BreathPhase

/**
 * Les pauses utilisent leur propre profil, distinct de celui des inspirations
 * et des expirations. Même profil pour les deux pauses du cycle.
 */
internal data class BreathingPhaseSoundProfile(
    val mode: BreathingCueMode,
    val volume: Int,
    val pitchHz: Int,
    val timbre: BreathingTimbre,
    val isPause: Boolean
)

internal fun BreathingSoundSettings.profileFor(
    phase: BreathPhase
): BreathingPhaseSoundProfile = when (phase) {
    BreathPhase.INHALE -> BreathingPhaseSoundProfile(
        mode, volume, inhalePitchHz, inhaleTimbre, false
    )
    BreathPhase.EXHALE -> BreathingPhaseSoundProfile(
        mode, volume, exhalePitchHz, exhaleTimbre, false
    )
    BreathPhase.HOLD_AFTER_INHALE,
    BreathPhase.HOLD_AFTER_EXHALE -> BreathingPhaseSoundProfile(
        pauseMode, pauseVolume, pausePitchHz, pauseTimbre, true
    )
}
