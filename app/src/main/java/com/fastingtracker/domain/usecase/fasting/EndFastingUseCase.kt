package com.fastingtracker.domain.usecase.fasting

import com.fastingtracker.domain.model.FastingSession
import com.fastingtracker.domain.repository.FastingSessionRepository
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

sealed interface EndFastOutcome {
    data class Saved(val session: FastingSession) : EndFastOutcome
    data class DiscardedShort(val elapsedMinutes: Long) : EndFastOutcome
}

/**
 * Use case to end the currently active fasting session.
 * Sessions shorter than 1 hour (< 3600s) are discarded and not saved to history.
 */
class EndFastingUseCase @Inject constructor(
    private val sessionRepository: FastingSessionRepository
) {
    companion object {
        const val MIN_SAVE_SECONDS = 3600L // 1 hour minimum
    }

    suspend operator fun invoke(
        endTime: Instant = Instant.now(),
        mood: Int? = null,
        notes: String? = null
    ): Result<EndFastOutcome> {
        return try {
            val activeSession = sessionRepository.getActiveSession()
                ?: return Result.failure(IllegalStateException("No active fasting session to end"))
            val sessionId = activeSession.id
                ?: return Result.failure(IllegalStateException("Active session missing ID"))

            val effectiveEnd = if (endTime.isBefore(activeSession.startTime)) Instant.now() else endTime
            val elapsedSeconds = Duration.between(activeSession.startTime, effectiveEnd).seconds.coerceAtLeast(0L)

            if (elapsedSeconds < MIN_SAVE_SECONDS) {
                sessionRepository.deleteSession(sessionId)
                return Result.success(EndFastOutcome.DiscardedShort(elapsedSeconds / 60L))
            }

            sessionRepository.endFasting(
                sessionId = sessionId,
                endTime = effectiveEnd,
                mood = mood,
                notes = notes
            )
            val saved = activeSession.copy(
                endTime = effectiveEnd,
                mood = mood,
                notes = notes?.takeIf { it.isNotBlank() },
                updatedAt = Instant.now()
            )
            Result.success(EndFastOutcome.Saved(saved))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
