package com.fastingtracker.presentation.settings

import android.app.Activity
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastingtracker.billing.DonationManager
import com.fastingtracker.billing.DonationState
import com.fastingtracker.data.backup.LocalBackupManager
import com.fastingtracker.domain.coordinator.FastingSessionCoordinator
import com.fastingtracker.domain.model.AppLanguage
import com.fastingtracker.domain.model.AppTheme
import com.fastingtracker.domain.model.NotificationSetting
import com.fastingtracker.domain.model.NotificationType
import com.fastingtracker.domain.repository.NotificationRepository
import com.fastingtracker.domain.repository.SettingsRepository
import com.fastingtracker.domain.usecase.settings.GetLanguageUseCase
import com.fastingtracker.domain.usecase.settings.GetThemeUseCase
import com.fastingtracker.domain.usecase.settings.SetLanguageUseCase
import com.fastingtracker.domain.usecase.settings.SetThemeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getThemeUseCase: GetThemeUseCase,
    private val setThemeUseCase: SetThemeUseCase,
    getLanguageUseCase: GetLanguageUseCase,
    private val setLanguageUseCase: SetLanguageUseCase,
    private val settingsRepository: SettingsRepository,
    private val notificationRepository: NotificationRepository,
    private val coordinator: FastingSessionCoordinator,
    private val localBackupManager: LocalBackupManager,
    private val donationManager: DonationManager
) : ViewModel() {

    val donationState: StateFlow<DonationState> = donationManager.state

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects: Flow<SettingsEffect> = _effects.receiveAsFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            getThemeUseCase(),
            settingsRepository.observeDynamicColor(),
            getLanguageUseCase()
        ) { theme, dynamicColor, language -> Triple(theme, dynamicColor, language) },
        combine(
            notificationRepository.observeAllSettings(),
            settingsRepository.observeSuperIslandEnabled(),
            settingsRepository.observeLockScreenVisible(),
            settingsRepository.observeMinimalNotification()
        ) { notifications, superIsland, lockScreen, minimal ->
            Quad(notifications, superIsland, lockScreen, minimal)
        }
    ) { (theme, dynamicColor, language), (notifications, superIsland, lockScreen, minimal) ->
        SettingsUiState(
            theme = theme,
            dynamicColor = dynamicColor,
            language = language,
            notificationSettings = notifications,
            superIslandEnabled = superIsland,
            lockScreenVisible = lockScreen,
            minimalNotification = minimal,
            isLoading = false,
            errorMessage = null
        )
    }
        .catch { e ->
            emit(SettingsUiState(isLoading = false, errorMessage = e.message ?: "Failed to load settings"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = SettingsUiState(isLoading = true)
        )

    fun onThemeChange(theme: AppTheme) {
        viewModelScope.launch { setThemeUseCase(theme) }
    }

    fun onDynamicColorToggle(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    }

    fun onSuperIslandToggle(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSuperIslandEnabled(enabled)
            coordinator.syncActiveSurfaces(rescheduleAlarms = false)
        }
    }

    fun onLockScreenToggle(visible: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLockScreenVisible(visible)
            coordinator.syncActiveSurfaces(rescheduleAlarms = false)
        }
    }

    fun onMinimalNotificationToggle(minimal: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMinimalNotification(minimal)
            coordinator.syncActiveSurfaces(rescheduleAlarms = false)
        }
    }

    fun onLanguageChange(language: AppLanguage) {
        viewModelScope.launch { setLanguageUseCase(language) }
    }

    fun onNotificationToggle(type: NotificationType, enabled: Boolean) {
        viewModelScope.launch {
            val current = uiState.value.notificationSettings.find { it.type == type }
            val now = Instant.now()
            val updated = current?.copy(isEnabled = enabled, updatedAt = now)
                ?: NotificationSetting(
                    id = null,
                    type = type,
                    title = type.name,
                    message = "",
                    advanceMinutes = 0,
                    isEnabled = enabled,
                    createdAt = now,
                    updatedAt = now
                )
            notificationRepository.upsertSetting(updated)
            coordinator.syncActiveSurfaces(rescheduleAlarms = false)
        }
    }

    fun onToggleAllNotifications(enabled: Boolean) {
        viewModelScope.launch {
            val now = Instant.now()
            val types = NotificationType.entries
            types.forEach { type ->
                val existing = uiState.value.notificationSettings.find { it.type == type }
                val updated = existing?.copy(isEnabled = enabled, updatedAt = now)
                    ?: NotificationSetting(
                        id = null,
                        type = type,
                        title = type.name,
                        message = "",
                        advanceMinutes = 0,
                        isEnabled = enabled,
                        createdAt = now,
                        updatedAt = now
                    )
                notificationRepository.upsertSetting(updated)
            }
            coordinator.syncActiveSurfaces(rescheduleAlarms = false)
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            localBackupManager.exportToUri(uri)
                .onSuccess { _effects.send(SettingsEffect.ExportSuccess) }
                .onFailure { e -> _effects.send(SettingsEffect.BackupError(e.message ?: "Export failed")) }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            localBackupManager.importFromUri(uri)
                .onSuccess { res ->
                    coordinator.syncActiveSurfaces(rescheduleAlarms = false)
                    _effects.send(SettingsEffect.ImportSuccess(res.plansImported, res.sessionsImported))
                }
                .onFailure { e -> _effects.send(SettingsEffect.BackupError(e.message ?: "Import failed")) }
        }
    }

    fun startDonation(activity: Activity) = donationManager.startDonation(activity)

    fun resetDonation() = donationManager.resetState()
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
