package com.fastingtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fastingtracker.data.local.entity.ReminderSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderSettingDao {
    @Query("SELECT * FROM reminder_settings")
    fun observeAll(): Flow<List<ReminderSettingEntity>>

    @Query("SELECT * FROM reminder_settings WHERE type = :type")
    suspend fun getByType(type: String): ReminderSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setting: ReminderSettingEntity)
}
