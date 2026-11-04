package fr.tempo.health.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * WorkManager conserve la programmation après la fermeture de l'application et le redémarrage.
 * Android peut différer l'exécution (Doze, économie d'énergie). Aucune heure exacte garantie.
 */
object HealthSyncScheduler {
    const val INTERVAL_HOURS = 2L
    private const val UNIQUE_WORK_NAME = "tempo-health-connect-background-sync"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<HealthSyncWorker>(
            INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

class HealthSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val repository = HealthRepository(applicationContext)

        // Sans prise en charge ou sans permission explicite, aucune lecture arrière-plan.
        if (repository.availability() != HealthConnectAvailability.AVAILABLE ||
            !repository.backgroundReadSupported()
        ) {
            return Result.success()
        }

        return try {
            val granted = repository.grantedPermissions()
            if (repository.backgroundReadPermission !in granted ||
                granted.intersect(repository.requiredPermissions).isEmpty()
            ) {
                return Result.success()
            }

            // Aujourd'hui et les deux jours précédents : rapide et suffisant pour la nuit.
            // L'écriture se fait dans la base Room déjà observée par l'interface.
            repository.syncInBackgroundIfDue()
            Result.success()
        } catch (_: SecurityException) {
            // Permission révoquée : ne jamais boucler en erreur ni effacer le cache.
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 2) Result.retry() else Result.success()
        }
    }
}
