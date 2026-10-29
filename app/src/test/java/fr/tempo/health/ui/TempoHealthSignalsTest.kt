package fr.tempo.health.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TempoHealthSignalsTest {
    @Test fun minimumFiveDaysForBaseline() {
        assertEquals(null, TempoHealthSignals.reference(listOf(7.0, 8.0, 7.5, 8.2)))
        assertEquals(8.0, TempoHealthSignals.reference(listOf(7.0, 8.0, 9.0, 7.5, 8.2))!!, 0.001)
    }

    @Test fun sleepThresholds() {
        assertEquals(TempoSignal.NORMAL, TempoHealthSignals.assess(TempoMetricKind.SLEEP, 450.0, 450.0))
        assertEquals(TempoSignal.CAUTION, TempoHealthSignals.assess(TempoMetricKind.SLEEP, 410.0, 450.0))
        assertEquals(TempoSignal.LOW, TempoHealthSignals.assess(TempoMetricKind.SLEEP, 360.0, 450.0))
    }

    @Test fun heartRateLowerIsNotBad() {
        assertEquals(TempoSignal.NORMAL, TempoHealthSignals.assess(TempoMetricKind.RESTING_PULSE, 58.0, 63.0))
        assertEquals(TempoSignal.CAUTION, TempoHealthSignals.assess(TempoMetricKind.RESTING_PULSE, 67.0, 63.0))
        assertEquals(TempoSignal.LOW, TempoHealthSignals.assess(TempoMetricKind.RESTING_PULSE, 71.0, 63.0))
    }

    @Test fun hrvHigherIsNotBad() {
        assertEquals(TempoSignal.NORMAL, TempoHealthSignals.assess(TempoMetricKind.HRV, 35.0, 29.0))
        assertEquals(TempoSignal.CAUTION, TempoHealthSignals.assess(TempoMetricKind.HRV, 26.0, 29.0))
        assertEquals(TempoSignal.LOW, TempoHealthSignals.assess(TempoMetricKind.HRV, 23.0, 29.0))
    }

    @Test fun activityAndRespirationUseTheSameStatusSemantics() {
        assertEquals(TempoSignal.NORMAL, TempoHealthSignals.assess(TempoMetricKind.STEPS, 11000.0, 8000.0))
        assertEquals(TempoSignal.CAUTION, TempoHealthSignals.assess(TempoMetricKind.STEPS, 5200.0, 8000.0))
        assertEquals(TempoSignal.LOW, TempoHealthSignals.assess(TempoMetricKind.STEPS, 3200.0, 8000.0))
        assertEquals(TempoSignal.NORMAL, TempoHealthSignals.assess(TempoMetricKind.RESPIRATION, 15.0, 15.0))
        assertEquals(TempoSignal.CAUTION, TempoHealthSignals.assess(TempoMetricKind.RESPIRATION, 17.0, 15.0))
        assertEquals(TempoSignal.LOW, TempoHealthSignals.assess(TempoMetricKind.RESPIRATION, 18.0, 15.0))
    }

    @Test fun noDiagnosisWithoutReference() {
        assertEquals(TempoSignal.UNKNOWN, TempoHealthSignals.assess(TempoMetricKind.HRV, 29.0, null))
        assertEquals(TempoSignal.UNKNOWN, TempoHealthSignals.assess(TempoMetricKind.OTHER, 65.0, 60.0))
        assertTrue(TempoHealthSignals.comparisonLabel(TempoMetricKind.HRV, 25.0, 29.0).contains("En dessous"))
    }
}
