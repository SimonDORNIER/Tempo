package fr.tempo.health.ui

import fr.tempo.health.domain.BreathingPattern
import org.junit.Assert.assertEquals
import org.junit.Test

class BreathingBubbleTest {
    private val pattern = BreathingPattern(4, 2, 6, 3)
    private fun fraction(elapsed: Int, nextSecond: Boolean = false): Float =
        breathingBubbleFraction(pattern, pattern.locate(elapsed)!!, nextSecond)

    @Test fun expandsOverEntireInhalation() {
        assertEquals(0f, fraction(0), 0.001f)
        assertEquals(0.5f, fraction(2), 0.001f)
        assertEquals(1f, fraction(3, true), 0.001f)
    }

    @Test fun staysExpandedThroughoutFirstHold() {
        assertEquals(1f, fraction(4), 0.001f)
        assertEquals(1f, fraction(5, true), 0.001f)
    }

    @Test fun contractsOverEntireExhalation() {
        assertEquals(1f, fraction(6), 0.001f)
        assertEquals(0.5f, fraction(9), 0.001f)
        assertEquals(0f, fraction(11, true), 0.001f)
    }

    @Test fun staysContractedThroughoutSecondHold() {
        assertEquals(0f, fraction(12), 0.001f)
        assertEquals(0f, fraction(14, true), 0.001f)
        assertEquals(0f, fraction(15), 0.001f)
    }

    @Test fun supportsImmediateTransitionsAndOneSecondPhases() {
        val quick = BreathingPattern(1, 0, 1, 0)
        assertEquals(1f, breathingBubbleFraction(quick, quick.locate(0)!!, true), 0.001f)
        assertEquals(0f, breathingBubbleFraction(quick, quick.locate(1)!!, true), 0.001f)
    }

    @Test fun holdsRemainStillWhenPreviousBreathIsOmitted() {
        val exhaleOnly = BreathingPattern(0, 2, 5, 2)
        assertEquals(0f, breathingBubbleFraction(exhaleOnly, exhaleOnly.locate(0)!!), 0.001f)
        assertEquals(0f, breathingBubbleFraction(exhaleOnly, exhaleOnly.locate(1)!!, true), 0.001f)
        val inhaleOnly = BreathingPattern(5, 2, 0, 2)
        assertEquals(1f, breathingBubbleFraction(inhaleOnly, inhaleOnly.locate(7)!!), 0.001f)
        assertEquals(1f, breathingBubbleFraction(inhaleOnly, inhaleOnly.locate(8)!!, true), 0.001f)
    }
}
