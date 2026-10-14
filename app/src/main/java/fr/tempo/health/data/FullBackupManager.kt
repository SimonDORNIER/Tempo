package fr.tempo.health.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.room.withTransaction
import fr.tempo.health.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class BackupResult(
    val success: Boolean,
    val message: String,
    val timestampEpochMs: Long? = null
)

data class BackupStatus(
    val configured: Boolean,
    val accessible: Boolean,
    val lastBackupEpochMs: Long?,
    val stale: Boolean
)

data class BackupPreview(
    val createdAtEpochMs: Long?,
    val appVersion: String?,
    val healthDays: Int,
    val checkIns: Int,
    val workouts: Int
)

class FullBackupManager(
    private val context: Context
) {
    private val preferences = context.getSharedPreferences(
        "tempo-health-settings",
        Context.MODE_PRIVATE
    )
    private val database = TempoHealthDatabase.get(context)
    private val resolver = context.contentResolver
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var autoBackupJob: Job? = null

    fun hasDestination(): Boolean =
        !preferences.getString(PREF_BACKUP_URI, null).isNullOrBlank()

    fun lastBackupEpochMs(): Long? =
        preferences.getLong(PREF_LAST_BACKUP, 0L).takeIf { it > 0L }

    fun status(maxAgeDays: Long = 3): BackupStatus {
        val configured = hasDestination()
        val accessible = if (configured) canAccessDestination() else false
        val last = lastBackupEpochMs()
        val stale = last == null ||
            System.currentTimeMillis() - last >
                maxAgeDays.coerceAtLeast(1) * 24L * 60L * 60L * 1000L

        return BackupStatus(
            configured = configured,
            accessible = accessible,
            lastBackupEpochMs = last,
            stale = configured && stale
        )
    }

    fun canAccessDestination(): Boolean {
        val raw = preferences.getString(PREF_BACKUP_URI, null) ?: return false
        val uri = Uri.parse(raw)

        val persistedPermission = resolver.persistedUriPermissions
            .firstOrNull { permission -> permission.uri == uri }

        if (persistedPermission?.isReadPermission == true ||
            persistedPermission?.isWritePermission == true
        ) {
            return true
        }

        return runCatching {
            resolver.openFileDescriptor(uri, "r")?.use { true } ?: false
        }.getOrDefault(false)
    }

    fun rememberDestination(uri: Uri): Boolean {
        runCatching {
            resolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }

        return preferences.edit()
            .putString(PREF_BACKUP_URI, uri.toString())
            .commit()
    }

    fun forgetDestination() {
        preferences.edit()
            .remove(PREF_BACKUP_URI)
            .remove(PREF_LAST_BACKUP)
            .apply()
    }

    suspend fun backupToSavedLocation(): BackupResult {
        val raw = preferences.getString(PREF_BACKUP_URI, null)
            ?: return BackupResult(false, "Aucun emplacement de sauvegarde configuré.")

        return backupTo(Uri.parse(raw))
    }

    suspend fun backupIfConfigured(): BackupResult? =
        if (hasDestination()) backupToSavedLocation() else null

    fun requestAutoBackup(delayMillis: Long = 800L) {
        if (!hasDestination()) return

        autoBackupJob?.cancel()
        autoBackupJob = scope.launch {
            delay(delayMillis.coerceAtLeast(0L))
            backupToSavedLocation()
        }
    }

    suspend fun backupTo(uri: Uri): BackupResult =
        runCatching {
            val json = exportFullBackup()
            resolver.openOutputStream(uri, "wt")?.bufferedWriter().use { writer ->
                requireNotNull(writer) { "Impossible d'ouvrir le fichier de sauvegarde." }
                writer.write(json)
                writer.flush()
            }

            val now = System.currentTimeMillis()
            preferences.edit()
                .putLong(PREF_LAST_BACKUP, now)
                .apply()

            BackupResult(
                success = true,
                message = "Sauvegarde complète enregistrée.",
                timestampEpochMs = now
            )
        }.getOrElse { error ->
            BackupResult(
                success = false,
                message = "Sauvegarde impossible : " +
                    (error.message ?: "erreur inconnue")
            )
        }

    fun inspect(uri: Uri): BackupPreview? =
        runCatching {
            val raw = resolver.openInputStream(uri)?.bufferedReader().use { reader ->
                requireNotNull(reader) { "Impossible d'ouvrir la sauvegarde." }
                reader.readText()
            }
            val root = JSONObject(raw)
            require(root.optString("app") == "Tempo Health")

            BackupPreview(
                createdAtEpochMs = root.optLong("createdAtEpochMs", 0L)
                    .takeIf { it > 0L },
                appVersion = root.optString("appVersion").takeIf { it.isNotBlank() },
                healthDays = root.optJSONArray("dailyHealth")?.length() ?: 0,
                checkIns = root.optJSONArray("checkIns")?.length() ?: 0,
                workouts = root.optJSONArray("workoutHistory")?.length() ?: 0
            )
        }.getOrNull()

    suspend fun restoreFrom(uri: Uri): BackupResult =
        runCatching {
            val raw = resolver.openInputStream(uri)?.bufferedReader().use { reader ->
                requireNotNull(reader) { "Impossible d'ouvrir la sauvegarde." }
                reader.readText()
            }
            importFullBackup(raw)
            BackupResult(
                success = true,
                message = "Sauvegarde restaurée. Tes données Tempo sont à jour."
            )
        }.getOrElse { error ->
            BackupResult(
                success = false,
                message = "Restauration impossible : " +
                    (error.message ?: "sauvegarde invalide")
            )
        }

    private suspend fun exportFullBackup(): String {
        val settings = JSONObject()
        preferences.all.forEach { (key, value) ->
            if (key == PREF_BACKUP_URI || key == PREF_LAST_BACKUP) {
                return@forEach
            }

            when (value) {
                is Boolean -> settings.put(key, value)
                is Int -> settings.put(key, value)
                is Long -> settings.put(key, value)
                is Float -> settings.put(key, value.toDouble())
                is String -> settings.put(key, value)
                is Set<*> -> {
                    val values = JSONArray()
                    value.filterIsInstance<String>().forEach(values::put)
                    settings.put(key, values)
                }
            }
        }

        val health = database.dailyHealthDao().getAll()
        val checkIns = database.dailyCheckInDao().getAll()
        val history = database.workoutHistoryDao().getAll()

        return JSONObject()
            .put("schemaVersion", 2)
            .put("app", "Tempo Health")
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("createdAtEpochMs", System.currentTimeMillis())
            .put("settings", settings)
            .put("dailyHealth", JSONArray().apply {
                health.forEach { day -> put(dayToJson(day)) }
            })
            .put("checkIns", JSONArray().apply {
                checkIns.forEach { checkIn -> put(checkInToJson(checkIn)) }
            })
            .put("workoutHistory", JSONArray().apply {
                history.forEach { item -> put(historyToJson(item)) }
            })
            .toString()
    }

    private suspend fun importFullBackup(raw: String) {
        val root = JSONObject(raw)
        require(root.optString("app") == "Tempo Health") {
            "Ce fichier n'est pas une sauvegarde Tempo Health."
        }
        require(root.optInt("schemaVersion", 0) >= 2) {
            "Format de sauvegarde trop ancien."
        }

        val settings = root.optJSONObject("settings") ?: JSONObject()
        val health = parseHealth(root.optJSONArray("dailyHealth") ?: JSONArray())
        val checkIns = parseCheckIns(root.optJSONArray("checkIns") ?: JSONArray())
        val history = parseHistory(root.optJSONArray("workoutHistory") ?: JSONArray())

        val destination = preferences.getString(PREF_BACKUP_URI, null)
        val lastBackup = preferences.getLong(PREF_LAST_BACKUP, 0L)

        database.withTransaction {
            database.dailyHealthDao().clearAll()
            database.dailyCheckInDao().clearAll()
            database.workoutHistoryDao().clearAll()

            if (health.isNotEmpty()) {
                database.dailyHealthDao().upsertAll(health)
            }
            if (checkIns.isNotEmpty()) {
                database.dailyCheckInDao().upsertAll(checkIns)
            }
            if (history.isNotEmpty()) {
                database.workoutHistoryDao().upsertAll(history)
            }
        }

        val editor = preferences.edit().clear()
        val keys = settings.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = settings.get(key)
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Double -> {
                    if (value % 1.0 == 0.0 &&
                        value >= Int.MIN_VALUE &&
                        value <= Int.MAX_VALUE
                    ) {
                        editor.putInt(key, value.toInt())
                    } else {
                        editor.putFloat(key, value.toFloat())
                    }
                }
                is String -> editor.putString(key, value)
                is JSONArray -> {
                    val set = buildSet {
                        for (index in 0 until value.length()) {
                            add(value.optString(index))
                        }
                    }
                    editor.putStringSet(key, set)
                }
            }
        }

        destination?.let { editor.putString(PREF_BACKUP_URI, it) }
        if (lastBackup > 0L) {
            editor.putLong(PREF_LAST_BACKUP, lastBackup)
        }
        check(editor.commit()) { "Impossible d'appliquer les réglages restaurés." }
    }

    private fun dayToJson(day: DailyHealthEntity): JSONObject =
        JSONObject()
            .put("date", day.date)
            .putNullable("sleepStartEpochMs", day.sleepStartEpochMs)
            .putNullable("sleepEndEpochMs", day.sleepEndEpochMs)
            .putNullable("sleepMinutes", day.sleepMinutes)
            .putNullable("lightMinutes", day.lightMinutes)
            .putNullable("deepMinutes", day.deepMinutes)
            .putNullable("remMinutes", day.remMinutes)
            .putNullable("awakeMinutes", day.awakeMinutes)
            .putNullable("overnightHeartRate", day.overnightHeartRate)
            .putNullable("restingHeartRate", day.restingHeartRate)
            .putNullable("hrvRmssdMs", day.hrvRmssdMs)
            .putNullable("respiratoryRate", day.respiratoryRate)
            .putNullable("steps", day.steps)
            .putNullable("distanceMeters", day.distanceMeters)
            .putNullable("caloriesKcal", day.caloriesKcal)
            .putNullable("exerciseMinutes", day.exerciseMinutes)
            .putNullable("weightKg", day.weightKg)
            .putNullable("vo2Max", day.vo2Max)
            .put("syncedAtEpochMs", day.syncedAtEpochMs)

    private fun checkInToJson(item: DailyCheckInEntity): JSONObject =
        JSONObject()
            .put("date", item.date)
            .put("energy", item.energy)
            .put("pain", item.pain)
            .put("updatedAtEpochMs", item.updatedAtEpochMs)

    private fun historyToJson(item: WorkoutHistoryEntity): JSONObject =
        JSONObject()
            .put("id", item.id)
            .put("startedAtEpochMs", item.startedAtEpochMs)
            .put("endedAtEpochMs", item.endedAtEpochMs)
            .put("title", item.title)
            .put("intensity", item.intensity)
            .put("durationSeconds", item.durationSeconds)
            .put("plannedExercises", item.plannedExercises)
            .put("completedExercises", item.completedExercises)
            .put("completed", item.completed)
            .putNullable("perceivedDifficulty", item.perceivedDifficulty)
            .put("muscleGroupsCsv", item.muscleGroupsCsv)

    private fun parseHealth(array: JSONArray): List<DailyHealthEntity> =
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    DailyHealthEntity(
                        date = item.getString("date"),
                        sleepStartEpochMs = item.longOrNull("sleepStartEpochMs"),
                        sleepEndEpochMs = item.longOrNull("sleepEndEpochMs"),
                        sleepMinutes = item.longOrNull("sleepMinutes"),
                        lightMinutes = item.longOrNull("lightMinutes"),
                        deepMinutes = item.longOrNull("deepMinutes"),
                        remMinutes = item.longOrNull("remMinutes"),
                        awakeMinutes = item.longOrNull("awakeMinutes"),
                        overnightHeartRate = item.doubleOrNull("overnightHeartRate"),
                        restingHeartRate = item.doubleOrNull("restingHeartRate"),
                        hrvRmssdMs = item.doubleOrNull("hrvRmssdMs"),
                        respiratoryRate = item.doubleOrNull("respiratoryRate"),
                        steps = item.longOrNull("steps"),
                        distanceMeters = item.doubleOrNull("distanceMeters"),
                        caloriesKcal = item.doubleOrNull("caloriesKcal"),
                        exerciseMinutes = item.longOrNull("exerciseMinutes"),
                        weightKg = item.doubleOrNull("weightKg"),
                        vo2Max = item.doubleOrNull("vo2Max"),
                        syncedAtEpochMs = item.getLong("syncedAtEpochMs")
                    )
                )
            }
        }

    private fun parseCheckIns(array: JSONArray): List<DailyCheckInEntity> =
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    DailyCheckInEntity(
                        date = item.getString("date"),
                        energy = item.getInt("energy"),
                        pain = item.getInt("pain"),
                        updatedAtEpochMs = item.getLong("updatedAtEpochMs")
                    )
                )
            }
        }

    private fun parseHistory(array: JSONArray): List<WorkoutHistoryEntity> =
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    WorkoutHistoryEntity(
                        id = item.optLong("id", 0L),
                        startedAtEpochMs = item.getLong("startedAtEpochMs"),
                        endedAtEpochMs = item.getLong("endedAtEpochMs"),
                        title = item.getString("title"),
                        intensity = item.getInt("intensity"),
                        durationSeconds = item.getInt("durationSeconds"),
                        plannedExercises = item.getInt("plannedExercises"),
                        completedExercises = item.getInt("completedExercises"),
                        completed = item.getBoolean("completed"),
                        perceivedDifficulty = item.intOrNull("perceivedDifficulty"),
                        muscleGroupsCsv = item.optString("muscleGroupsCsv")
                    )
                )
            }
        }

    private fun JSONObject.putNullable(key: String, value: Any?): JSONObject =
        put(key, value ?: JSONObject.NULL)

    private fun JSONObject.longOrNull(key: String): Long? =
        if (isNull(key) || !has(key)) null else getLong(key)

    private fun JSONObject.doubleOrNull(key: String): Double? =
        if (isNull(key) || !has(key)) null else getDouble(key)

    private fun JSONObject.intOrNull(key: String): Int? =
        if (isNull(key) || !has(key)) null else getInt(key)

    companion object {
        const val DEFAULT_FILE_NAME = "TempoHealth-backup.json"
        private const val PREF_BACKUP_URI = "backup-document-uri"
        private const val PREF_LAST_BACKUP = "backup-last-success-epoch-ms"
    }
}
