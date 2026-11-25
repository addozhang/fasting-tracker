package com.fastingtracker.domain.repository

import com.fastingtracker.domain.model.AppLanguage
import com.fastingtracker.domain.model.AppTheme
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for app settings using DataStore Preferences
 */
interface SettingsRepository {
    fun observeTheme(): Flow<AppTheme>
    suspend fun getTheme(): AppTheme
    suspend fun setTheme(theme: AppTheme)

    fun observeDynamicColor(): Flow<Boolean>
    suspend fun setDynamicColor(enabled: Boolean)

    fun observeLanguage(): Flow<AppLanguage>
    suspend fun getLanguage(): AppLanguage
    suspend fun setLanguage(language: AppLanguage)

    suspend fun hasAcceptedDisclaimer(): Boolean
    suspend fun setDisclaimerAccepted()

    fun observeSelectedPlanId(): Flow<Long?>
    suspend fun getSelectedPlanId(): Long?
    suspend fun setSelectedPlanId(planId: Long)

    fun observeSuperIslandEnabled(): Flow<Boolean>
    suspend fun isSuperIslandEnabled(): Boolean
    suspend fun setSuperIslandEnabled(enabled: Boolean)

    fun observeLockScreenVisible(): Flow<Boolean>
    suspend fun isLockScreenVisible(): Boolean
    suspend fun setLockScreenVisible(visible: Boolean)

    fun observeMinimalNotification(): Flow<Boolean>
    suspend fun isMinimalNotification(): Boolean
    suspend fun setMinimalNotification(minimal: Boolean)
}
