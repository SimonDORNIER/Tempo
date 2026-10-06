package fr.tempo.health.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "daily_health")
data class DailyHealthEntity(
    @PrimaryKey val date: String,
    val sleepStartEpochMs: Long? = null,
    val sleepEndEpochMs: Long? = null,
    val sleepMinutes: Long? = null,
    val lightMinutes: Long? = null,
    val deepMinutes: Long? = null,
    val remMinutes: Long? = null,
    val awakeMinutes: Long? = null,
    val overnightHeartRate: Double? = null,
    val restingHeartRate: Double? = null,
    val hrvRmssdMs: Double? = null,
    val respiratoryRate: Double? = null,
    val steps: Long? = null,
    val distanceMeters: Double? = null,
    val caloriesKcal: Double? = null,
    val exerciseMinutes: Long? = null,
    val weightKg: Double? = null,
    val vo2Max: Double? = null,
    val syncedAtEpochMs: Long
)

@Dao
interface DailyHealthDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(day: DailyHealthEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(days: List<DailyHealthEntity>)

    @Query("SELECT * FROM daily_health ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int = 28): Flow<List<DailyHealthEntity>>

    @Query("SELECT * FROM daily_health WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): DailyHealthEntity?
}

@Database(
    entities = [DailyHealthEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TempoHealthDatabase : RoomDatabase() {
    abstract fun dailyHealthDao(): DailyHealthDao

    companion object {
        @Volatile private var INSTANCE: TempoHealthDatabase? = null

        fun get(context: Context): TempoHealthDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TempoHealthDatabase::class.java,
                    "tempo-health.db"
                ).build().also { INSTANCE = it }
            }
    }
}
