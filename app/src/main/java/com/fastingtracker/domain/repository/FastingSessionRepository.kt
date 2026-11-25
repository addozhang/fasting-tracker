package com.fastingtracker.domain.repository

import com.fastingtracker.domain.model.FastingSession
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Repository interface for FastingSession data operations
 */
interface FastingSessionRepository {
    suspend fun startFasting(session: FastingSession): Long

    suspend fun endFasting(
        sessionId: Long,
        endTime: Instant,
        mood: Int? = null,
        notes: String? = null
    )

    fun observeActiveSession(): Flow<FastingSession?>

    fun observeAllSessions(): Flow<List<FastingSession>>

    suspend fun getActiveSession(): FastingSession?

    suspend fun getAllSessions(): List<FastingSession>

    suspend fun getSessionById(sessionId: Long): FastingSession?

    suspend fun getSessionsPaginated(limit: Int, offset: Int): List<FastingSession>

    suspend fun getSessionsCount(): Int

    suspend fun updateSession(session: FastingSession)

    suspend fun deleteSession(sessionId: Long)

    suspend fun reassignPlan(fromPlanId: Long, toPlanId: Long)
}
