package com.fastingtracker.domain.usecase.fasting

import com.fastingtracker.domain.model.FastingPhase
import com.fastingtracker.domain.model.FastingSession
import com.fastingtracker.domain.repository.FastingSessionRepository
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

enum class SessionValidationError {
    END_BEFORE_START,
    FUTURE_TIME,
    DURATION_OUT_OF_RANGE,
    OVERLAPS_EXISTING
}

class SessionValidationException(val error: SessionValidationError) : IllegalArgumentException(error.name)

object SessionTimeValidator {
    const val MIN_DURATION_SECONDS = 3600L       // 1 hour
    const val MAX_DURATION_SECONDS = 168L * 3600 // 7 days (168 hours)

    fun validateCompleted(
        startTime: Instant,
        endTime: Instant,
        existingSessions: List<FastingSession>,
        excludeSessionId: Long? = null,
        now: Instant = Instant.now()
    ): SessionValidationError? {
        if (!endTime.isAfter(startTime)) return SessionValidationError.END_BEFORE_START
        if (endTime.isAfter(now.plusSeconds(60))) return SessionValidationError.FUTURE_TIME

        val durationSec = Duration.between(startTime, endTime).seconds
        if (durationSec < MIN_DURATION_SECONDS || durationSec > MAX_DURATION_SECONDS) {
            return SessionValidationError.DURATION_OUT_OF_RANGE
        }

        val overlaps = existingSessions
            .filter { it.id != excludeSessionId }
            .any { other ->
                val otherEnd = other.endTime ?: now
                startTime.isBefore(otherEnd) && endTime.isAfter(other.startTime)
            }
        if (overlaps) return SessionValidationError.OVERLAPS_EXISTING

        return null
    }
}

/**
 * Updates the start time of the currently active fasting session.
 */
class UpdateActiveStartTimeUseCase @Inject constructor(
    private val sessionRepository: FastingSessionRepository
) {
    suspend operator fun invoke(newStartTime: Instant, now: Instant = Instant.now()): Result<FastingSession> {
        val active = sessionRepository.getActiveSession()
            ?: return Result.failure(IllegalStateException("No active session"))
        if (newStartTime.isAfter(now.plusSeconds(60))) {
            return Result.failure(SessionValidationException(SessionValidationError.FUTURE_TIME))
        }
        val durationSec = Duration.between(newStartTime, now).seconds
        if (durationSec > SessionTimeValidator.MAX_DURATION_SECONDS) {
            return Result.failure(SessionValidationException(SessionValidationError.DURATION_OUT_OF_RANGE))
        }
        val updated = active.copy(startTime = newStartTime, updatedAt = now)
        sessionRepository.updateSession(updated)
        return Result.success(updated)
    }
}

/**
 * Creates a manual past fasting record or edits an existing completed fasting record.
 * Enforces 1h..168h duration bounds and forbids overlap with any other session.
 */
class SaveSessionRecordUseCase @Inject constructor(
    private val sessionRepository: FastingSessionRepository
) {
    suspend operator fun invoke(
        sessionId: Long?,
        planId: Long,
        startTime: Instant,
        endTime: Instant,
        mood: Int?,
        notes: String?,
        now: Instant = Instant.now()
    ): Result<Long> {
        val allSessions = sessionRepository.getAllSessions()
        val error = SessionTimeValidator.validateCompleted(
            startTime = startTime,
            endTime = endTime,
            existingSessions = allSessions,
            excludeSessionId = sessionId,
            now = now
        )
        if (error != null) {
            return Result.failure(SessionValidationException(error))
        }

        val cleanNotes = notes?.trim()?.takeIf { it.isNotEmpty() }
        return try {
            if (sessionId != null) {
                val existing = sessionRepository.getSessionById(sessionId)
                    ?: return Result.failure(IllegalArgumentException("Session not found"))
                sessionRepository.updateSession(
                    existing.copy(
                        planId = planId,
                        startTime = startTime,
                        endTime = endTime,
                        mood = mood,
                        notes = cleanNotes,
                        updatedAt = now
                    )
                )
                Result.success(sessionId)
            } else {
                val newId = sessionRepository.startFasting(
                    FastingSession(
                        id = null,
                        startTime = startTime,
                        endTime = endTime,
                        planId = planId,
                        initialPhase = FastingPhase.FASTING,
                        mood = mood,
                        hunger = null,
                        waterMl = null,
                        notes = cleanNotes,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                Result.success(newId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Deletes a completed fasting session by ID.
 */
class DeleteSessionUseCase @Inject constructor(
    private val sessionRepository: FastingSessionRepository
) {
    suspend operator fun invoke(sessionId: Long): Result<Unit> {
        return try {
            sessionRepository.deleteSession(sessionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
