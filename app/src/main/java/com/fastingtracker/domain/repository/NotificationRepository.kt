package com.fastingtracker.domain.repository

import com.fastingtracker.domain.model.NotificationType
import com.fastingtracker.domain.model.NotificationSetting
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for NotificationSetting data operations
 */
interface NotificationRepository {
    /**
     * Observe all notification settings
     */
    fun observeAllSettings(): Flow<List<NotificationSetting>>

    /**
     * Get all notification settings (one-shot, non-Flow)
     */
    suspend fun getAllSettings(): List<NotificationSetting>

    /**
     * Get a notification setting by type
     */
    suspend fun getSettingByType(type: NotificationType): NotificationSetting?

    /**
     * Upsert (insert or update) a notification setting
     */
    suspend fun upsertSetting(setting: NotificationSetting)
}
