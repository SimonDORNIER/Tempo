package fr.tempo.health.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class HealthConnectAvailability {
    AVAILABLE,
    UPDATE_REQUIRED,
    UNAVAILABLE
}

class HealthRepository(
    private val context: Context
) {
    private val database = TempoHealthDatabase.get(context)
    private val dao = database.dailyHealthDao()
    private val checkInDao = database.dailyCheckInDao()

    private val sleepPermission =
        HealthPermission.getReadPermission(SleepSessionRecord::class)
    private val heartRatePermission =
        HealthPermission.getReadPermission(HeartRateRecord::class)
    private val restingHeartRatePermission =
        HealthPermission.getReadPermission(RestingHeartRateRecord::class)
    private val hrvPermission =
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class)
    private val respiratoryPermission =
        HealthPermission.getReadPermission(RespiratoryRateRecord::class)
    private val stepsPermission =
        HealthPermission.getReadPermission(StepsRecord::class)
    private val distancePermission =
        HealthPermission.getReadPermission(DistanceRecord::class)
    private val caloriesPermission =
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    private val exercisePermission =
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    private val weightPermission =
        HealthPermission.getReadPermission(WeightRecord::class)
    private val vo2Permission =
        HealthPermission.getReadPermission(Vo2MaxRecord::class)

    val requiredPermissions: Set<String> = setOf(
        sleepPermission,
        heartRatePermission,
        restingHeartRatePermission,
        hrvPermission,
        respiratoryPermission,
        stepsPermission,
        distancePermission,
        caloriesPermission,
        exercisePermission,
        weightPermission,
        vo2Permission
    )

    fun observeRecentDays(limit: Int = 28): Flow<List<DailyHealthEntity>> =
        dao.observeRecent(limit)

    fun observeTodayCheckIn(): Flow<DailyCheckInEntity?> =
        checkInDao.observeByDate(LocalDate.now(ZoneId.systemDefault()).toString())

    suspend fun saveTodayCheckIn(energy: Int, pain: Int) {
        checkInDao.upsert(
            DailyCheckInEntity(
                date = LocalDate.now(ZoneId.systemDefault()).toString(),
                energy = energy.coerceIn(1, 5),
                pain = pain.coerceIn(0, 3),
                updatedAtEpochMs = Instant.now().toEpochMilli()
            )
        )
    }

    fun availability(): HealthConnectAvailability =
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectAvailability.UPDATE_REQUIRED
            else -> HealthConnectAvailability.UNAVAILABLE
        }

    private fun client(): HealthConnectClient =
        HealthConnectClient.getOrCreate(context)

    suspend fun grantedPermissions(): Set<String> {
        if (availability() != HealthConnectAvailability.AVAILABLE) return emptySet()
        return client().permissionController.getGrantedPermissions()
    }

    suspend fun hasRequiredPermissions(): Boolean =
        grantedPermissions().containsAll(requiredPermissions)

    suspend fun hasAnyGrantedPermissions(): Boolean =
        grantedPermissions().any { it in requiredPermissions }

    suspend fun lastSyncEpochMs(): Long? =
        dao.getLastSyncEpochMs()

    suspend fun shouldAutoSync(maxAgeMinutes: Long = 30): Boolean {
        val last = lastSyncEpochMs() ?: return true
        val age = Instant.now().toEpochMilli() - last
        return age >= Duration.ofMinutes(maxAgeMinutes).toMillis()
    }

    suspend fun syncRecentDays(dayCount: Int = 3) {
        syncDays(dayCount.coerceIn(1, 28))
    }

    suspend fun syncLast28Days() {
        syncDays(28)
    }

    private suspend fun syncDays(dayCount: Int) {
        val granted = grantedPermissions()
        if (granted.none { it in requiredPermissions }) {
            throw SecurityException("Aucune autorisation Santé Connect accordée")
        }

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val days = (0L until dayCount.toLong())
            .map { offset -> today.minusDays(offset) }
            .map { date -> readDay(date, zone, granted) }

        dao.upsertAll(days)
    }

    private suspend fun readDay(
        date: LocalDate,
        zone: ZoneId,
        granted: Set<String>
    ): DailyHealthEntity {
        val hc = client()
        val dayStart = date.atStartOfDay(zone).toInstant()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant()

        val aggregateMetrics = buildSet {
            if (stepsPermission in granted) add(StepsRecord.COUNT_TOTAL)
            if (distancePermission in granted) add(DistanceRecord.DISTANCE_TOTAL)
            if (caloriesPermission in granted) {
                add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
            }
            if (restingHeartRatePermission in granted) {
                add(RestingHeartRateRecord.BPM_AVG)
            }
            if (weightPermission in granted) add(WeightRecord.WEIGHT_AVG)
        }

        val aggregate = if (aggregateMetrics.isEmpty()) {
            null
        } else {
            hc.aggregate(
                AggregateRequest(
                    metrics = aggregateMetrics,
                    timeRangeFilter = TimeRangeFilter.between(dayStart, dayEnd)
                )
            )
        }

        val sleepRecords = if (sleepPermission in granted) {
            hc.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        dayStart.minus(Duration.ofHours(12)),
                        dayEnd
                    )
                )
            ).records
        } else {
            emptyList()
        }

        val mainSleep = sleepRecords
            .filter { record ->
                record.endTime.atZone(zone).toLocalDate() == date
            }
            .maxByOrNull { record ->
                Duration.between(record.startTime, record.endTime).toMinutes()
            }

        val sleepMinutes = mainSleep?.let {
            Duration.between(it.startTime, it.endTime).toMinutes()
        }

        fun stageMinutes(vararg stageTypes: Int): Long? {
            val sleep = mainSleep ?: return null
            val stages = sleep.stages.filter { it.stage in stageTypes }
            if (stages.isEmpty()) return null
            return stages.sumOf {
                Duration.between(it.startTime, it.endTime).toMinutes()
            }
        }

        val overnightHeartRate =
            if (mainSleep != null && heartRatePermission in granted) {
                hc.aggregate(
                    AggregateRequest(
                        metrics = setOf(HeartRateRecord.BPM_AVG),
                        timeRangeFilter = TimeRangeFilter.between(
                            mainSleep.startTime,
                            mainSleep.endTime
                        )
                    )
                )[HeartRateRecord.BPM_AVG]
            } else {
                null
            }

        val hrvWindowStart = mainSleep?.startTime ?: dayStart
        val hrvWindowEnd = mainSleep?.endTime ?: dayEnd

        val hrv = if (hrvPermission in granted) {
            hc.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateVariabilityRmssdRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        hrvWindowStart,
                        hrvWindowEnd
                    )
                )
            ).records
                .map { it.heartRateVariabilityMillis }
                .takeIf { it.isNotEmpty() }
                ?.average()
        } else {
            null
        }

        val respiratoryRate = if (respiratoryPermission in granted) {
            hc.readRecords(
                ReadRecordsRequest(
                    recordType = RespiratoryRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        hrvWindowStart,
                        hrvWindowEnd
                    )
                )
            ).records
                .map { it.rate }
                .takeIf { it.isNotEmpty() }
                ?.average()
        } else {
            null
        }

        val exerciseMinutes = if (exercisePermission in granted) {
            hc.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(dayStart, dayEnd)
                )
            ).records
                .sumOf {
                    Duration.between(it.startTime, it.endTime).toMinutes()
                }
                .takeIf { it > 0L }
        } else {
            null
        }

        val vo2Max = if (vo2Permission in granted) {
            hc.readRecords(
                ReadRecordsRequest(
                    recordType = Vo2MaxRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(dayStart, dayEnd)
                )
            ).records
                .map { it.vo2MillilitersPerMinuteKilogram }
                .takeIf { it.isNotEmpty() }
                ?.average()
        } else {
            null
        }

        return DailyHealthEntity(
            date = date.toString(),
            sleepStartEpochMs = mainSleep?.startTime?.toEpochMilli(),
            sleepEndEpochMs = mainSleep?.endTime?.toEpochMilli(),
            sleepMinutes = sleepMinutes,
            lightMinutes = stageMinutes(SleepSessionRecord.STAGE_TYPE_LIGHT),
            deepMinutes = stageMinutes(SleepSessionRecord.STAGE_TYPE_DEEP),
            remMinutes = stageMinutes(SleepSessionRecord.STAGE_TYPE_REM),
            awakeMinutes = stageMinutes(
                SleepSessionRecord.STAGE_TYPE_AWAKE,
                SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED,
                SleepSessionRecord.STAGE_TYPE_OUT_OF_BED
            ),
            overnightHeartRate = overnightHeartRate?.toDouble(),
            restingHeartRate =
                aggregate?.get(RestingHeartRateRecord.BPM_AVG)?.toDouble(),
            hrvRmssdMs = hrv,
            respiratoryRate = respiratoryRate,
            steps = aggregate?.get(StepsRecord.COUNT_TOTAL),
            distanceMeters =
                aggregate?.get(DistanceRecord.DISTANCE_TOTAL)?.inMeters,
            caloriesKcal =
                aggregate?.get(TotalCaloriesBurnedRecord.ENERGY_TOTAL)?.inKilocalories,
            exerciseMinutes = exerciseMinutes,
            weightKg = aggregate?.get(WeightRecord.WEIGHT_AVG)?.inKilograms,
            vo2Max = vo2Max,
            syncedAtEpochMs = Instant.now().toEpochMilli()
        )
    }
}
