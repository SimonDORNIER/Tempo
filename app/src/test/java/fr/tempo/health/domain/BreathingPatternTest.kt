package fr.tempo.health.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BreathingPatternTest {
    @Test fun fullCycleWithTwoPauses() {
        val pattern = BreathingPattern(4, 2, 6, 3)
        assertEquals(15, pattern.cycleSeconds)
        assertEquals(BreathPhase.INHALE, pattern.locate(0)?.phase)
        assertEquals(4, pattern.locate(0)?.remainingSeconds)
        assertEquals(BreathPhase.HOLD_AFTER_INHALE, pattern.locate(4)?.phase)
        assertEquals(2, pattern.locate(4)?.remainingSeconds)
        assertEquals(BreathPhase.EXHALE, pattern.locate(6)?.phase)
        assertEquals(6, pattern.locate(6)?.remainingSeconds)
        assertEquals(BreathPhase.HOLD_AFTER_EXHALE, pattern.locate(12)?.phase)
        assertEquals(3, pattern.locate(12)?.remainingSeconds)
        assertEquals(BreathPhase.INHALE, pattern.locate(15)?.phase)
        assertEquals(1, pattern.locate(15)?.cycleIndex)
        assertEquals(4, pattern.locate(15)?.stepIndex)
    }

    @Test fun phasesAtZeroAreSkipped() {
        val pattern = BreathingPattern(4, 0, 6, 0)
        assertEquals(BreathPhase.EXHALE, pattern.locate(4)?.phase)
        assertEquals(6, pattern.locate(4)?.remainingSeconds)
        assertEquals(BreathPhase.INHALE, pattern.locate(10)?.phase)
        assertEquals(4, pattern.locate(10)?.stepIndex)
        assertTrue(pattern.canStart)
    }

    @Test fun inspirationCanBeSkipped() {
        val pattern = BreathingPattern(0, 0, 5, 0)
        assertEquals(BreathPhase.EXHALE, pattern.locate(0)?.phase)
        assertEquals(BreathPhase.EXHALE, pattern.locate(5)?.phase)
        assertEquals(1, pattern.locate(5)?.cycleIndex)
    }

    @Test fun invalidWithoutAnyBreathingPhase() {
        val pattern = BreathingPattern(0, 3, 0, 2)
        assertFalse(pattern.canStart)
        assertNull(pattern.locate(0))
        assertNull(BreathingPattern(0, 0, 0, 0).locate(20))
    }
}
