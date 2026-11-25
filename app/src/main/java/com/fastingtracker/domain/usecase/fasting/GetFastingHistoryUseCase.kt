package com.fastingtracker.domain.usecase.fasting

import com.fastingtracker.domain.model.FastingSession
import com.fastingtracker.domain.repository.FastingSessionRepository
import javax.inject.Inject

/**
 * Use case to retrieve fasting session history
 */
class GetFastingHistoryUseCase @Inject constructor(
    private val sessionRepository: FastingSessionRepository
) {
    /**
     * Get all fasting sessions ordered by start time (newest first)
     * @return List of fasting sessions
     */
    suspend operator fun invoke(): List<FastingSession> {
        return sessionRepository.getAllSessions()
    }

    /**
     * Get paginated fasting sessions
     * @param limit Number of sessions to return
     * @param offset Number of sessions to skip
     * @return Paginated list of fasting sessions
     */
    suspend fun getPaginated(limit: Int, offset: Int): List<FastingSession> {
        return sessionRepository.getSessionsPaginated(limit, offset)
    }

    /**
     * Get total count of fasting sessions
     * @return Total number of sessions
     */
    suspend fun getCount(): Int {
        return sessionRepository.getSessionsCount()
    }
}
