package com.fastingtracker.domain.usecase.settings

import com.fastingtracker.domain.model.AppLanguage
import com.fastingtracker.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe the current language preference
 */
class GetLanguageUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    /**
     * Observe the current language
     * @return Flow emitting the current AppLanguage
     */
    operator fun invoke(): Flow<AppLanguage> {
        return settingsRepository.observeLanguage()
    }
}
