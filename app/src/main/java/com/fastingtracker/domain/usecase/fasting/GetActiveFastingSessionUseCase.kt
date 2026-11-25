package com.fastingtracker.domain.usecase.fasting

import com.fastingtracker.domain.model.FastingSession
import com.fastingtracker.domain.repository.FastingSessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe the currently active fasting session
 */
class GetActiveFastingSessionUseCase @Inject constructor(
    private val sessionRepository: FastingSessionRepository
) {
    /**
     * Observe the active fasting session (if any)
     * @return Flow that emits the active session or null
     */
    operator fun invoke(): Flow<FastingSession?> {
        return sessionRepository.observeActiveSession()
    }
}
