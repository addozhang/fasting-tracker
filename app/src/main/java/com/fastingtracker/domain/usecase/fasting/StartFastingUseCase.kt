package com.fastingtracker.domain.usecase.fasting

import com.fastingtracker.domain.model.FastingPhase
import com.fastingtracker.domain.model.FastingSession
import com.fastingtracker.domain.repository.FastingSessionRepository
import java.time.Instant
import javax.inject.Inject

/**
 * Use case to start a new fasting session
 * Automatically ends any active session before starting a new one
 */
class StartFastingUseCase @Inject constructor(
    private val sessionRepository: FastingSessionRepository
) {
    /**
     * Start a new fasting session
     * @param planId ID of the fasting plan to use
     * @param initialPhase The starting phase (FASTING or EATING)
     * @return Result with session ID on success or error on failure
     */
    suspend operator fun invoke(
        planId: Long,
        initialPhase: FastingPhase = FastingPhase.FASTING,
        startOffsetMinutes: Long = 0L
    ): Result<Long> {
        return try {
            // End any active session first
            val activeSession = sessionRepository.getActiveSession()
            if (activeSession != null) {
                sessionRepository.endFasting(
                    sessionId = activeSession.id!!,
                    endTime = Instant.now()
                )
            }

            // Create new session — startTime may be in the past if user specifies an offset
            val now = Instant.now()
            val startTime = now.minusSeconds(startOffsetMinutes * 60L)
            val newSession = FastingSession(
                id = null,
                startTime = startTime,
                endTime = null,
                planId = planId,
                initialPhase = initialPhase,
                mood = null,
                hunger = null,
                waterMl = null,
                notes = null,
                createdAt = now,
                updatedAt = now
            )

            val sessionId = sessionRepository.startFasting(newSession)
            Result.success(sessionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
