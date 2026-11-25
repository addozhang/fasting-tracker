package com.fastingtracker.domain.usecase.settings

import com.fastingtracker.domain.model.AppTheme
import com.fastingtracker.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case to set the theme preference
 */
class SetThemeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    /**
     * Set the theme preference
     * @param theme The theme to set
     */
    suspend operator fun invoke(theme: AppTheme) {
        settingsRepository.setTheme(theme)
    }
}
