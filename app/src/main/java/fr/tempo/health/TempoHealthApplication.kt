package fr.tempo.health

import android.app.Application
import fr.tempo.health.data.HealthRepository
import fr.tempo.health.data.WorkoutRepository

class TempoHealthApplication : Application() {
    val healthRepository: HealthRepository by lazy {
        HealthRepository(applicationContext)
    }

    val workoutRepository: WorkoutRepository by lazy {
        WorkoutRepository(applicationContext)
    }
}
