package com.fastingtracker.domain.repository

import com.fastingtracker.domain.model.ReminderSetting
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for ReminderSetting data operations
 */
interface ReminderRepository {
    /**
     * Observe all reminder settings
     */
    fun observeAllSettings(): Flow<List<ReminderSetting>>

    /**
     * Get a reminder setting by type
     */
    suspend fun getSettingByType(type: String): ReminderSetting?

    /**
     * Upsert (insert or update) a reminder setting
     */
    suspend fun upsertSetting(setting: ReminderSetting)
}
