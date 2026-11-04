package fr.tempo.health.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BreathingSoundEnvelopeTest {
    @Test fun longGentleChimesReplaceAbruptBeep() {
        val ms = BreathingSoundEnvelope.durationMs(
            BreathingCueMode.SHORT, isPause = false, remainingSeconds = 4, preview = false
        )
        assertEquals(900, ms)
        assertEquals(0.0, BreathingSoundEnvelope.gain(0.0, ms, BreathingCueMode.SHORT, false), 0.0)
        assertTrue(BreathingSoundEnvelope.gain(160.0, ms, BreathingCueMode.SHORT, false) > 0.15)
        assertTrue(BreathingSoundEnvelope.gain(810.0, ms, BreathingCueMode.SHORT, false) < 0.18)
        assertEquals(0.0, BreathingSoundEnvelope.gain(ms.toDouble(), ms, BreathingCueMode.SHORT, false), 0.0)
    }

    @Test fun continuousToneStartsAndStopsWithSoftFades() {
        val duration = BreathingSoundEnvelope.durationMs(
            BreathingCueMode.CONTINUOUS, false, 6, false
        )
        assertEquals(6000, duration)
        val start = BreathingSoundEnvelope.gain(60.0, duration, BreathingCueMode.CONTINUOUS, false)
        val middle = BreathingSoundEnvelope.gain(2500.0, duration, BreathingCueMode.CONTINUOUS, false)
        val end = BreathingSoundEnvelope.gain(5920.0, duration, BreathingCueMode.CONTINUOUS, false)
        assertTrue(start < middle)
        assertTrue(end < middle)
        assertEquals(1700, BreathingSoundEnvelope.durationMs(
            BreathingCueMode.CONTINUOUS, false, 5, true
        ))
        assertTrue(BreathingSoundEnvelope.peak(BreathingCueMode.CONTINUOUS, false) <
            BreathingSoundEnvelope.peak(BreathingCueMode.SHORT, false))
    }

    @Test fun theTwoHoldChimesAreSeparatedBySilence() {
        val duration = BreathingSoundEnvelope.durationMs(
            BreathingCueMode.CONTINUOUS, true, 2, false
        )
        assertEquals(850, duration)
        val first = BreathingSoundEnvelope.gain(200.0, duration, BreathingCueMode.CONTINUOUS, true)
        val silence = BreathingSoundEnvelope.gain(405.0, duration, BreathingCueMode.CONTINUOUS, true)
        val second = BreathingSoundEnvelope.gain(610.0, duration, BreathingCueMode.CONTINUOUS, true)
        assertTrue(first > 0.0)
        assertEquals(0.0, silence, 0.0)
        assertTrue(second > 0.0)
        assertTrue(second < first)
    }
}
