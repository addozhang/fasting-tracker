package com.fastingtracker.presentation.settings

import com.fastingtracker.domain.model.AppLanguage
import com.fastingtracker.domain.model.AppTheme
import com.fastingtracker.domain.model.NotificationSetting

data class SettingsUiState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val dynamicColor: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val notificationSettings: List<NotificationSetting> = emptyList(),
    val superIslandEnabled: Boolean = true,
    val lockScreenVisible: Boolean = true,
    val minimalNotification: Boolean = true,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface SettingsEffect {
    data object ExportSuccess : SettingsEffect
    data class ImportSuccess(val plans: Int, val sessions: Int) : SettingsEffect
    data class BackupError(val reason: String) : SettingsEffect
}
