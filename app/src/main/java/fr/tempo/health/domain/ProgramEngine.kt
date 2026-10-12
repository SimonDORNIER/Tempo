package fr.tempo.health.domain

import fr.tempo.health.data.WorkoutHistoryEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class ProgramDayKind {
    SESSION,
    MOBILITY,
    RECOVERY
}

data class ProgramDay(
    val date: LocalDate,
    val label: String,
    val title: String,
    val kind: ProgramDayKind,
    val minutes: Int,
    val completed: Boolean,
    val missed: Boolean
)

data class WeeklyProgram(
    val goal: TrainingGoal,
    val weekNumber: Int,
    val level: Int,
    val targetSessions: Int,
    val completedThisWeek: Int,
    val missedThisWeek: Int,
    val todayGuidance: String,
    val days: List<ProgramDay>
)

object ProgramEngine {
    fun build(
        goal: TrainingGoal,
        history: List<WorkoutHistoryEntity>,
        preferredMinutes: Int,
        recovery: RecoveryResult
    ): WeeklyProgram {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val monday = today.with(
            TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        )

        val templates = templates(goal)
        val completedDates = history
            .filter { it.completed }
            .map {
                Instant.ofEpochMilli(it.startedAtEpochMs)
                    .atZone(zone)
                    .toLocalDate()
            }
            .toSet()

        val targetSessions = templates.count {
            it.kind == ProgramDayKind.SESSION
        }.coerceAtLeast(1)

        val fourWeeksAgo = today.minusDays(27)
        val recentCompleted = history.filter { item ->
            item.completed &&
                Instant.ofEpochMilli(item.startedAtEpochMs)
                    .atZone(zone)
                    .toLocalDate() >= fourWeeksAgo
        }

        val difficulty = recentCompleted
            .mapNotNull { it.perceivedDifficulty }
            .takeIf { it.isNotEmpty() }
            ?.average()

        val expectedFourWeeks = targetSessions * 4.0
        val adherence = if (expectedFourWeeks > 0) {
            recentCompleted.size / expectedFourWeeks
        } else {
            0.0
        }

        val level = when {
            difficulty != null && difficulty >= 4.2 -> 1
            adherence < 0.50 -> 1
            adherence < 0.85 -> 2
            adherence >= 1.05 && (difficulty == null || difficulty <= 3.2) -> 4
            else -> 3
        }

        val durationScale = when (level) {
            1 -> 0.80
            2 -> 0.90
            4 -> 1.10
            else -> 1.0
        }

        val days = templates.map { template ->
            val date = monday.plusDays((template.day.value - 1).toLong())
            val completed = date in completedDates
            val missed = date.isBefore(today) &&
                !completed &&
                template.kind == ProgramDayKind.SESSION

            ProgramDay(
                date = date,
                label = dayLabel(template.day),
                title = template.title,
                kind = template.kind,
                minutes = when (template.kind) {
                    ProgramDayKind.SESSION ->
                        (preferredMinutes * durationScale)
                            .toInt()
                            .coerceIn(10, 40)
                    ProgramDayKind.MOBILITY ->
                        (10 * durationScale).toInt().coerceIn(8, 15)
                    ProgramDayKind.RECOVERY -> 8
                },
                completed = completed,
                missed = missed
            )
        }

        val completedThisWeek = days.count {
            it.kind == ProgramDayKind.SESSION && it.completed
        }
        val missedThisWeek = days.count { it.missed }
        val todayProgram = days.firstOrNull { it.date == today }

        val todayGuidance = when {
            recovery.level == RecoveryLevel.RED ->
                "Récupération prioritaire : remplace le programme par 8 à 12 min de mobilité douce ou du repos."

            todayProgram?.kind == ProgramDayKind.SESSION ->
                todayProgram.title + " • environ " + todayProgram.minutes +
                    " min. La séance Tempo s'adapte à ta récupération."

            todayProgram?.kind == ProgramDayKind.MOBILITY ->
                todayProgram.title + " • " + todayProgram.minutes +
                    " min suffisent aujourd'hui."

            missedThisWeek > 0 ->
                "Une séance a été manquée : fais une version courte de 10 à 15 min aujourd'hui si tu as l'énergie, sinon continue le planning sans doubler demain."

            else ->
                "Jour léger : marche, bouge régulièrement et garde de l'énergie pour la prochaine séance."
        }

        val weekNumber = (
            history.count { it.completed } / targetSessions
            ) + 1

        return WeeklyProgram(
            goal = goal,
            weekNumber = weekNumber.coerceAtLeast(1),
            level = level,
            targetSessions = targetSessions,
            completedThisWeek = completedThisWeek,
            missedThisWeek = missedThisWeek,
            todayGuidance = todayGuidance,
            days = days
        )
    }

    private data class Template(
        val day: DayOfWeek,
        val title: String,
        val kind: ProgramDayKind
    )

    private fun templates(goal: TrainingGoal): List<Template> =
        when (goal) {
            TrainingGoal.PAIN_FREE -> listOf(
                Template(DayOfWeek.MONDAY, "Contrôle + mobilité", ProgramDayKind.SESSION),
                Template(DayOfWeek.TUESDAY, "Mobilité douce", ProgramDayKind.MOBILITY),
                Template(DayOfWeek.THURSDAY, "Renforcement contrôlé", ProgramDayKind.SESSION),
                Template(DayOfWeek.SATURDAY, "Mobilité complète", ProgramDayKind.MOBILITY)
            )

            TrainingGoal.MOBILITY -> listOf(
                Template(DayOfWeek.MONDAY, "Mobilité complète", ProgramDayKind.SESSION),
                Template(DayOfWeek.TUESDAY, "Mobilité courte", ProgramDayKind.MOBILITY),
                Template(DayOfWeek.THURSDAY, "Mobilité complète", ProgramDayKind.SESSION),
                Template(DayOfWeek.SATURDAY, "Mobilité active", ProgramDayKind.SESSION),
                Template(DayOfWeek.SUNDAY, "Relâchement", ProgramDayKind.MOBILITY)
            )

            TrainingGoal.FITNESS -> listOf(
                Template(DayOfWeek.MONDAY, "Full body", ProgramDayKind.SESSION),
                Template(DayOfWeek.WEDNESDAY, "Renforcement + mobilité", ProgramDayKind.SESSION),
                Template(DayOfWeek.FRIDAY, "Full body", ProgramDayKind.SESSION),
                Template(DayOfWeek.SUNDAY, "Mobilité légère", ProgramDayKind.MOBILITY)
            )

            TrainingGoal.STRENGTH -> listOf(
                Template(DayOfWeek.MONDAY, "Renforcement A", ProgramDayKind.SESSION),
                Template(DayOfWeek.WEDNESDAY, "Renforcement B", ProgramDayKind.SESSION),
                Template(DayOfWeek.FRIDAY, "Renforcement C", ProgramDayKind.SESSION),
                Template(DayOfWeek.SUNDAY, "Mobilité récupération", ProgramDayKind.MOBILITY)
            )

            TrainingGoal.RELAXATION -> listOf(
                Template(DayOfWeek.MONDAY, "Détente + mobilité", ProgramDayKind.MOBILITY),
                Template(DayOfWeek.WEDNESDAY, "Relâchement", ProgramDayKind.MOBILITY),
                Template(DayOfWeek.FRIDAY, "Détente + mobilité", ProgramDayKind.MOBILITY),
                Template(DayOfWeek.SUNDAY, "Retour au calme", ProgramDayKind.MOBILITY)
            )
        }

    private fun dayLabel(day: DayOfWeek): String =
        when (day) {
            DayOfWeek.MONDAY -> "Lun"
            DayOfWeek.TUESDAY -> "Mar"
            DayOfWeek.WEDNESDAY -> "Mer"
            DayOfWeek.THURSDAY -> "Jeu"
            DayOfWeek.FRIDAY -> "Ven"
            DayOfWeek.SATURDAY -> "Sam"
            DayOfWeek.SUNDAY -> "Dim"
        }
}
