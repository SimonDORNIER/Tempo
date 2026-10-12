package fr.tempo.health.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import fr.tempo.health.MainActivity
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

enum class ReminderType {
    MOVE,
    CHECKIN,
    MOBILITY,
    WORKOUT
}

data class ReminderSettings(
    val moveEnabled: Boolean = false,
    val moveIntervalHours: Int = 2,
    val checkInEnabled: Boolean = false,
    val checkInHour: Int = 9,
    val mobilityEnabled: Boolean = false,
    val mobilityHour: Int = 18,
    val workoutEnabled: Boolean = false,
    val workoutHour: Int = 19
)

class ReminderManager(
    private val context: Context
) {
    private val preferences = context.getSharedPreferences(
        "tempo-health-settings",
        Context.MODE_PRIVATE
    )

    fun load(): ReminderSettings =
        ReminderSettings(
            moveEnabled = preferences.getBoolean(KEY_MOVE_ENABLED, false),
            moveIntervalHours = preferences.getInt(KEY_MOVE_INTERVAL, 2)
                .coerceIn(1, 4),
            checkInEnabled = preferences.getBoolean(KEY_CHECKIN_ENABLED, false),
            checkInHour = preferences.getInt(KEY_CHECKIN_HOUR, 9)
                .coerceIn(6, 22),
            mobilityEnabled = preferences.getBoolean(KEY_MOBILITY_ENABLED, false),
            mobilityHour = preferences.getInt(KEY_MOBILITY_HOUR, 18)
                .coerceIn(6, 22),
            workoutEnabled = preferences.getBoolean(KEY_WORKOUT_ENABLED, false),
            workoutHour = preferences.getInt(KEY_WORKOUT_HOUR, 19)
                .coerceIn(6, 22)
        )

    fun save(settings: ReminderSettings) {
        preferences.edit()
            .putBoolean(KEY_MOVE_ENABLED, settings.moveEnabled)
            .putInt(KEY_MOVE_INTERVAL, settings.moveIntervalHours)
            .putBoolean(KEY_CHECKIN_ENABLED, settings.checkInEnabled)
            .putInt(KEY_CHECKIN_HOUR, settings.checkInHour)
            .putBoolean(KEY_MOBILITY_ENABLED, settings.mobilityEnabled)
            .putInt(KEY_MOBILITY_HOUR, settings.mobilityHour)
            .putBoolean(KEY_WORKOUT_ENABLED, settings.workoutEnabled)
            .putInt(KEY_WORKOUT_HOUR, settings.workoutHour)
            .apply()

        scheduleAll(settings)
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Rappels Tempo Health",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Bouger, check-in, mobilité et séances"
            }
        )
    }

    fun scheduleAll(settings: ReminderSettings = load()) {
        scheduleMove(settings)
        scheduleDaily(
            ReminderType.CHECKIN,
            settings.checkInEnabled,
            settings.checkInHour
        )
        scheduleDaily(
            ReminderType.MOBILITY,
            settings.mobilityEnabled,
            settings.mobilityHour
        )
        scheduleDaily(
            ReminderType.WORKOUT,
            settings.workoutEnabled,
            settings.workoutHour
        )
    }

    private fun scheduleMove(settings: ReminderSettings) {
        val workManager = WorkManager.getInstance(context)
        val name = workName(ReminderType.MOVE)

        if (!settings.moveEnabled) {
            workManager.cancelUniqueWork(name)
            return
        }

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(
            settings.moveIntervalHours.toLong(),
            TimeUnit.HOURS
        )
            .setInputData(
                workDataOf(INPUT_TYPE to ReminderType.MOVE.name)
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            name,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun scheduleDaily(
        type: ReminderType,
        enabled: Boolean,
        hour: Int
    ) {
        val workManager = WorkManager.getInstance(context)
        val name = workName(type)

        if (!enabled) {
            workManager.cancelUniqueWork(name)
            return
        }

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(
            24,
            TimeUnit.HOURS
        )
            .setInitialDelay(
                delayUntilHour(hour),
                TimeUnit.MILLISECONDS
            )
            .setInputData(
                workDataOf(INPUT_TYPE to type.name)
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            name,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun delayUntilHour(hour: Int): Long {
        val now = ZonedDateTime.now()
        var target = now
            .withHour(hour.coerceIn(0, 23))
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }

        return Duration.between(now, target).toMillis()
    }

    companion object {
        const val CHANNEL_ID = "tempo-health-reminders"
        const val INPUT_TYPE = "reminder-type"

        private const val KEY_MOVE_ENABLED = "reminder-move-enabled"
        private const val KEY_MOVE_INTERVAL = "reminder-move-interval-hours"
        private const val KEY_CHECKIN_ENABLED = "reminder-checkin-enabled"
        private const val KEY_CHECKIN_HOUR = "reminder-checkin-hour"
        private const val KEY_MOBILITY_ENABLED = "reminder-mobility-enabled"
        private const val KEY_MOBILITY_HOUR = "reminder-mobility-hour"
        private const val KEY_WORKOUT_ENABLED = "reminder-workout-enabled"
        private const val KEY_WORKOUT_HOUR = "reminder-workout-hour"

        fun workName(type: ReminderType): String =
            "tempo-reminder-" + type.name.lowercase()
    }
}

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val raw = inputData.getString(ReminderManager.INPUT_TYPE)
            ?: return Result.success()
        val type = runCatching { ReminderType.valueOf(raw) }
            .getOrNull()
            ?: return Result.success()

        val settings = ReminderManager(applicationContext).load()
        if (!isEnabled(type, settings)) return Result.success()

        if (type == ReminderType.MOVE) {
            val hour = ZonedDateTime.now().hour
            if (hour !in 8..21) return Result.success()
        }

        if (type == ReminderType.WORKOUT) {
            val day = ZonedDateTime.now().dayOfWeek
            if (day !in setOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.FRIDAY
                )
            ) {
                return Result.success()
            }
        }

        showNotification(type)
        return Result.success()
    }

    private fun isEnabled(
        type: ReminderType,
        settings: ReminderSettings
    ): Boolean =
        when (type) {
            ReminderType.MOVE -> settings.moveEnabled
            ReminderType.CHECKIN -> settings.checkInEnabled
            ReminderType.MOBILITY -> settings.mobilityEnabled
            ReminderType.WORKOUT -> settings.workoutEnabled
        }

    private fun showNotification(type: ReminderType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            type.ordinal + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (title, text) = when (type) {
            ReminderType.MOVE ->
                "Bouge 2 minutes" to
                    "Lève-toi, marche un peu et relâche épaules et hanches."
            ReminderType.CHECKIN ->
                "Comment tu te sens ?" to
                    "Renseigne énergie et douleurs pour améliorer les conseils du jour."
            ReminderType.MOBILITY ->
                "Mobilité du jour" to
                    "Quelques minutes suffisent pour entretenir ta mobilité."
            ReminderType.WORKOUT ->
                "Séance Tempo" to
                    "Regarde la séance adaptée à ta récupération du jour."
        }

        val notification = NotificationCompat.Builder(
            applicationContext,
            ReminderManager.CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(10_000 + type.ordinal, notification)
    }
}
