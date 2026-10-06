package fr.tempo.health.data

import android.content.Context
import fr.tempo.health.domain.ExerciseCategory
import fr.tempo.health.domain.WorkoutPlan
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(context: Context) {
    private val dao = TempoHealthDatabase.get(context).workoutHistoryDao()

    fun observeRecent(limit: Int = 30): Flow<List<WorkoutHistoryEntity>> =
        dao.observeRecent(limit)

    suspend fun saveSession(
        plan: WorkoutPlan,
        startedAtEpochMs: Long,
        endedAtEpochMs: Long,
        durationSeconds: Int,
        completedExercises: Int,
        completed: Boolean
    ): Long {
        val groups = plan.items
            .map { it.exercise.category }
            .filter {
                it == ExerciseCategory.UPPER_BODY ||
                    it == ExerciseCategory.LOWER_BODY ||
                    it == ExerciseCategory.CORE
            }
            .distinct()
            .joinToString(",")

        return dao.insert(
            WorkoutHistoryEntity(
                startedAtEpochMs = startedAtEpochMs,
                endedAtEpochMs = endedAtEpochMs,
                title = plan.title,
                intensity = plan.intensity,
                durationSeconds = durationSeconds.coerceAtLeast(0),
                plannedExercises = plan.items.size,
                completedExercises = completedExercises.coerceIn(0, plan.items.size),
                completed = completed,
                muscleGroupsCsv = groups
            )
        )
    }

    suspend fun setDifficulty(id: Long, difficulty: Int) {
        dao.updateDifficulty(id, difficulty.coerceIn(1, 5))
    }
}
