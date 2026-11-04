package fr.tempo.health.ui

import fr.tempo.health.domain.BreathPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BreathingPauseSoundTest {
    @Test fun breathingCanBeContinuousWhilePausesStayShort() {
        val settings = BreathingSoundSettings(
            mode = BreathingCueMode.CONTINUOUS,
            volume = 70,
            inhalePitchHz = 460,
            exhalePitchHz = 330,
            pauseMode = BreathingCueMode.SHORT,
            pauseVolume = 40,
            pausePitchHz = 250,
            pauseTimbre = BreathingTimbre.CLEAR
        )
        val inhale = settings.profileFor(BreathPhase.INHALE)
        val exhale = settings.profileFor(BreathPhase.EXHALE)
        val firstPause = settings.profileFor(BreathPhase.HOLD_AFTER_INHALE)
        val secondPause = settings.profileFor(BreathPhase.HOLD_AFTER_EXHALE)

        assertEquals(BreathingCueMode.CONTINUOUS, inhale.mode)
        assertEquals(BreathingCueMode.CONTINUOUS, exhale.mode)
        assertEquals(BreathingCueMode.SHORT, firstPause.mode)
        assertEquals(firstPause, secondPause)
        assertEquals(70, inhale.volume)
        assertEquals(40, firstPause.volume)
        assertEquals(250, firstPause.pitchHz)
        assertEquals(BreathingTimbre.CLEAR, firstPause.timbre)
        assertEquals(460, inhale.pitchHz)
        assertEquals(330, exhale.pitchHz)
    }

    @Test fun pausesCanBeContinuousWithoutChangingBreathing() {
        val settings = BreathingSoundSettings(
            mode = BreathingCueMode.SHORT,
            pauseMode = BreathingCueMode.CONTINUOUS,
            pauseVolume = 65,
            pausePitchHz = 290
        )
        assertEquals(
            BreathingCueMode.SHORT,
            settings.profileFor(BreathPhase.INHALE).mode
        )
        assertEquals(
            BreathingCueMode.CONTINUOUS,
            settings.profileFor(BreathPhase.HOLD_AFTER_EXHALE).mode
        )
        assertEquals(65, settings.profileFor(BreathPhase.HOLD_AFTER_INHALE).volume)
    }

    @Test fun shortPauseSoundsOnlyAtBeginningWhileContinuousFillsPhase() {
        assertEquals(
            850,
            BreathingSoundEnvelope.durationMs(
                BreathingCueMode.SHORT, true, remainingSeconds = 7, preview = false
            )
        )
        assertEquals(
            7000,
            BreathingSoundEnvelope.durationMs(
                BreathingCueMode.CONTINUOUS, true, remainingSeconds = 7, preview = false
            )
        )
        assertEquals(
            1700,
            BreathingSoundEnvelope.durationMs(
                BreathingCueMode.CONTINUOUS, true, remainingSeconds = 7, preview = true
            )
        )
    }

    @Test fun continuousPauseHasNoGapsAndSmoothFades() {
        val duration = 5000
        val start = BreathingSoundEnvelope.gain(
            40.0, duration, BreathingCueMode.CONTINUOUS, true
        )
        val middle = BreathingSoundEnvelope.gain(
            2200.0, duration, BreathingCueMode.CONTINUOUS, true
        )
        val end = BreathingSoundEnvelope.gain(
            4920.0, duration, BreathingCueMode.CONTINUOUS, true
        )
        assertTrue(start > 0.0)
        assertTrue(middle > start)
        assertTrue(middle > end)
        assertTrue(end > 0.0)
        assertEquals(
            0.0,
            BreathingSoundEnvelope.gain(
                5000.0, duration, BreathingCueMode.CONTINUOUS, true
            ),
            0.0
        )
        assertTrue(
            BreathingSoundEnvelope.peak(BreathingCueMode.CONTINUOUS, true) <
                BreathingSoundEnvelope.peak(BreathingCueMode.SHORT, true)
        )
    }
}
