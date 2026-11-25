package com.fastingtracker.domain.usecase.settings

import com.fastingtracker.domain.model.AppLanguage
import com.fastingtracker.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case to set the language preference
 */
class SetLanguageUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    /**
     * Set the language preference
     * @param language The language to set
     */
    suspend operator fun invoke(language: AppLanguage) {
        settingsRepository.setLanguage(language)
    }
}
