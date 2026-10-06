package fr.tempo.health

import android.app.Application
import fr.tempo.health.data.HealthRepository

class TempoHealthApplication : Application() {
    val healthRepository: HealthRepository by lazy {
        HealthRepository(applicationContext)
    }
}
