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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

@Entity(tableName = "daily_checkin")
data class DailyCheckInEntity(
    @PrimaryKey val date: String,
    val energy: Int,
    val pain: Int,
    val updatedAtEpochMs: Long
)

@Dao
interface DailyHealthDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(days: List<DailyHealthEntity>)

    @Query("SELECT * FROM daily_health ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int = 28): Flow<List<DailyHealthEntity>>
}

@Dao
interface DailyCheckInDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(checkIn: DailyCheckInEntity)

    @Query("SELECT * FROM daily_checkin WHERE date = :date LIMIT 1")
    fun observeByDate(date: String): Flow<DailyCheckInEntity?>
}

@Database(
    entities = [DailyHealthEntity::class, DailyCheckInEntity::class],
    version = 2,
    exportSchema = false
)
abstract class TempoHealthDatabase : RoomDatabase() {
    abstract fun dailyHealthDao(): DailyHealthDao
    abstract fun dailyCheckInDao(): DailyCheckInDao

    companion object {
        @Volatile private var INSTANCE: TempoHealthDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS daily_checkin (
                        date TEXT NOT NULL,
                        energy INTEGER NOT NULL,
                        pain INTEGER NOT NULL,
                        updatedAtEpochMs INTEGER NOT NULL,
                        PRIMARY KEY(date)
                    )
                    """.trimIndent()
                )
            }
        }

        fun get(context: Context): TempoHealthDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TempoHealthDatabase::class.java,
                    "tempo-health.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
