package com.fastingtracker.domain.usecase.settings

import com.fastingtracker.domain.model.AppTheme
import com.fastingtracker.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe the current theme preference
 */
class GetThemeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    /**
     * Observe the current theme
     * @return Flow emitting the current AppTheme
     */
    operator fun invoke(): Flow<AppTheme> {
        return settingsRepository.observeTheme()
    }
}
