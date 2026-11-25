package com.fastingtracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fastingtracker.data.local.dao.FastingSessionDao
import com.fastingtracker.data.local.dao.NotificationSettingDao
import com.fastingtracker.data.local.dao.PlanDao
import com.fastingtracker.data.local.dao.ReminderSettingDao
import com.fastingtracker.data.local.entity.Converters
import com.fastingtracker.data.local.entity.FastingSessionEntity
import com.fastingtracker.data.local.entity.NotificationSettingEntity
import com.fastingtracker.data.local.entity.PlanEntity
import com.fastingtracker.data.local.entity.ReminderSettingEntity
import com.fastingtracker.domain.model.NotificationType
import com.fastingtracker.domain.model.PlanType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

@Database(
    entities = [
        PlanEntity::class,
        FastingSessionEntity::class,
        NotificationSettingEntity::class,
        ReminderSettingEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun planDao(): PlanDao
    abstract fun fastingSessionDao(): FastingSessionDao
    abstract fun notificationSettingDao(): NotificationSettingDao
    abstract fun reminderSettingDao(): ReminderSettingDao

    companion object {
        private const val DATABASE_NAME = "fasting_tracker.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null
        private val seedMutex = Mutex()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plans ADD COLUMN isDefault INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE plans SET isDefault = 1 WHERE id IN (1, 2, 3)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS progress_records")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                val types = listOf(
                    Triple("FASTING_START", "Ongoing Timer", "Live fasting progress notification"),
                    Triple("FASTING_END", "Fasting Complete", "You have reached your fasting goal"),
                    Triple("PHASE_MILESTONE", "Milestone Reached", "You have entered a new fasting phase")
                )
                types.forEach { (type, title, message) ->
                    db.execSQL(
                        "INSERT OR IGNORE INTO notification_settings " +
                            "(type, title, message, advanceMinutes, isEnabled, createdAt, updatedAt) " +
                            "VALUES ('$type', '$title', '$message', 0, 1, $now, $now)"
                    )
                }
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            getInstance(context).prepopulateDatabase()
                        }
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // Ensure v2.0 presets (14:10, 24h, 48h, 72h) exist for upgraded installs
                        CoroutineScope(Dispatchers.IO).launch {
                            getInstance(context).ensureV2DefaultPresets()
                        }
                    }
                })
                .build()
        }
    }

    private suspend fun prepopulateDatabase() = seedMutex.withLock {
        if (planDao().getAll().isNotEmpty()) return@withLock
        val now = Instant.now()
        val defaultPlans = listOf(
            PlanEntity(id = 1L, name = "16:8", planType = PlanType.DAILY, fastingHours = 16, eatingHours = 8, isDefault = true, createdAt = now, updatedAt = now),
            PlanEntity(id = 2L, name = "18:6", planType = PlanType.DAILY, fastingHours = 18, eatingHours = 6, isDefault = true, createdAt = now, updatedAt = now),
            PlanEntity(id = 3L, name = "20:4", planType = PlanType.DAILY, fastingHours = 20, eatingHours = 4, isDefault = true, createdAt = now, updatedAt = now),
            PlanEntity(id = 4L, name = "14:10", planType = PlanType.DAILY, fastingHours = 14, eatingHours = 10, isDefault = true, createdAt = now, updatedAt = now),
            PlanEntity(id = 5L, name = "24h", planType = PlanType.TARGET, fastingHours = 24, eatingHours = null, isDefault = true, createdAt = now, updatedAt = now),
            PlanEntity(id = 6L, name = "48h", planType = PlanType.TARGET, fastingHours = 48, eatingHours = null, isDefault = true, createdAt = now, updatedAt = now),
            PlanEntity(id = 7L, name = "72h", planType = PlanType.TARGET, fastingHours = 72, eatingHours = null, isDefault = true, createdAt = now, updatedAt = now)
        )
        planDao().insertAll(defaultPlans)

        val defaultNotifications = listOf(
            NotificationSettingEntity(type = NotificationType.FASTING_START, title = "Ongoing Timer", message = "Live fasting progress notification", advanceMinutes = 0, isEnabled = true, createdAt = now, updatedAt = now),
            NotificationSettingEntity(type = NotificationType.FASTING_END, title = "Fasting Complete", message = "You have reached your fasting goal", advanceMinutes = 0, isEnabled = true, createdAt = now, updatedAt = now),
            NotificationSettingEntity(type = NotificationType.PHASE_MILESTONE, title = "Milestone Reached", message = "You have entered a new fasting phase", advanceMinutes = 0, isEnabled = true, createdAt = now, updatedAt = now)
        )
        defaultNotifications.forEach { notificationSettingDao().upsert(it) }
    }

    private suspend fun ensureV2DefaultPresets() = seedMutex.withLock {
        val existing = planDao().getAll()
        if (existing.isEmpty()) return@withLock // onCreate handles brand-new DBs
        val now = Instant.now()
        // Normalize legacy "24h OMAD" preset name to clean "24h"
        existing.filter { it.isDefault && it.name == "24h OMAD" }.forEach { old ->
            planDao().update(old.copy(name = "24h", updatedAt = now))
        }
        val desired = listOf(
            PlanEntity(id = null, name = "14:10", planType = PlanType.DAILY, fastingHours = 14, eatingHours = 10, isDefault = true, createdAt = now, updatedAt = now),
            PlanEntity(id = null, name = "24h", planType = PlanType.TARGET, fastingHours = 24, eatingHours = null, isDefault = true, createdAt = now, updatedAt = now),
            PlanEntity(id = null, name = "48h", planType = PlanType.TARGET, fastingHours = 48, eatingHours = null, isDefault = true, createdAt = now, updatedAt = now),
            PlanEntity(id = null, name = "72h", planType = PlanType.TARGET, fastingHours = 72, eatingHours = null, isDefault = true, createdAt = now, updatedAt = now)
        )
        val missing = desired.filter { preset ->
            existing.none { it.isDefault && it.fastingHours == preset.fastingHours && it.eatingHours == preset.eatingHours }
        }
        if (missing.isNotEmpty()) {
            planDao().insertAll(missing)
        }
    }
}
