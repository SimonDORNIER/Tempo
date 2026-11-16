package fr.tempo.health.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class HrvReadWindowsTest {
    private val dayStart = Instant.parse("2026-11-16T00:00:00Z")
    private val dayEnd = Instant.parse("2026-11-17T00:00:00Z")

    @Test fun sleepWindowIsTriedBeforeWholeCalendarDay() {
        val sleepStart = Instant.parse("2026-11-15T22:45:00Z")
        val sleepEnd = Instant.parse("2026-11-16T06:30:00Z")
        assertEquals(
            listOf(sleepStart to sleepEnd, dayStart to dayEnd),
            hrvReadWindows(dayStart, dayEnd, sleepStart, sleepEnd)
        )
    }

    @Test fun dayIsCheckedWithoutRecordedSleep() {
        assertEquals(
            listOf(dayStart to dayEnd),
            hrvReadWindows(dayStart, dayEnd, null, null)
        )
    }

    @Test fun invalidSleepPeriodDoesNotPreventDaytimeImport() {
        val wrong = Instant.parse("2026-11-16T09:00:00Z")
        assertEquals(
            listOf(dayStart to dayEnd),
            hrvReadWindows(dayStart, dayEnd, wrong, wrong)
        )
    }

    @Test fun sameWindowIsNotReadTwice() {
        assertEquals(
            listOf(dayStart to dayEnd),
            hrvReadWindows(dayStart, dayEnd, dayStart, dayEnd)
        )
    }
}
