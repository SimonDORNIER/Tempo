package fr.tempo.health.data

import java.time.Instant

/**
 * Une VFC peut être horodatée en dehors du sommeil principal (résumé quotidien).
 * On tente d'abord la fenêtre nocturne, puis toute la journée locale sans
 * dupliquer les interrogations identiques.
 */
internal fun hrvReadWindows(
    dayStart: Instant,
    dayEnd: Instant,
    sleepStart: Instant?,
    sleepEnd: Instant?
): List<Pair<Instant, Instant>> {
    require(dayEnd > dayStart) { "La journée doit avoir une fin postérieure au début." }
    val sleep = if (sleepStart != null && sleepEnd != null && sleepEnd > sleepStart) {
        sleepStart to sleepEnd
    } else {
        null
    }
    return listOfNotNull(sleep, dayStart to dayEnd).distinct()
}
