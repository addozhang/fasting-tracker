package com.fastingtracker.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastingtracker.domain.coordinator.FastingSessionCoordinator
import com.fastingtracker.domain.model.AppLanguage
import com.fastingtracker.domain.model.AppTheme
import com.fastingtracker.domain.repository.SettingsRepository
import com.fastingtracker.domain.usecase.settings.GetLanguageUseCase
import com.fastingtracker.domain.usecase.settings.GetThemeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    getThemeUseCase: GetThemeUseCase,
    getLanguageUseCase: GetLanguageUseCase,
    private val settingsRepository: SettingsRepository,
    private val coordinator: FastingSessionCoordinator
) : ViewModel() {

    val appTheme: StateFlow<AppTheme> = getThemeUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppTheme.SYSTEM
        )

    val dynamicColor: StateFlow<Boolean> = settingsRepository.observeDynamicColor()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val appLanguage: StateFlow<AppLanguage> = getLanguageUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppLanguage.SYSTEM
        )

    private val _showDisclaimer = MutableStateFlow(false)
    val showDisclaimer: StateFlow<Boolean> = _showDisclaimer.asStateFlow()

    init {
        viewModelScope.launch {
            _showDisclaimer.value = !settingsRepository.hasAcceptedDisclaimer()
        }
    }

    fun onAppResumed() {
        viewModelScope.launch {
            coordinator.syncActiveSurfaces(rescheduleAlarms = false)
        }
    }

    fun acceptDisclaimer() {
        viewModelScope.launch {
            settingsRepository.setDisclaimerAccepted()
            _showDisclaimer.value = false
        }
    }
}
