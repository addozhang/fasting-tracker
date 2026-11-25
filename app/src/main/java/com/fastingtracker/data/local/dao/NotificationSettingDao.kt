package com.fastingtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fastingtracker.data.local.entity.NotificationSettingEntity
import com.fastingtracker.domain.model.NotificationType
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationSettingDao {
    @Query("SELECT * FROM notification_settings")
    fun observeAll(): Flow<List<NotificationSettingEntity>>

    @Query("SELECT * FROM notification_settings")
    suspend fun getAll(): List<NotificationSettingEntity>

    @Query("SELECT * FROM notification_settings WHERE type = :type")
    suspend fun getByType(type: NotificationType): NotificationSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setting: NotificationSettingEntity)
}
