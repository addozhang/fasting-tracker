package com.fastingtracker.data.repository

import com.fastingtracker.data.local.dao.FastingSessionDao
import com.fastingtracker.domain.model.FastingSession
import com.fastingtracker.domain.model.toDomain
import com.fastingtracker.domain.model.toEntity
import com.fastingtracker.domain.repository.FastingSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FastingSessionRepositoryImpl @Inject constructor(
    private val fastingSessionDao: FastingSessionDao
) : FastingSessionRepository {

    override suspend fun startFasting(session: FastingSession): Long {
        return fastingSessionDao.insert(session.toEntity())
    }

    override suspend fun endFasting(
        sessionId: Long,
        endTime: Instant,
        mood: Int?,
        notes: String?
    ) {
        val session = fastingSessionDao.getById(sessionId)
        session?.let {
            fastingSessionDao.update(
                it.copy(
                    endTime = endTime,
                    mood = mood ?: it.mood,
                    notes = notes?.takeIf { n -> n.isNotBlank() } ?: it.notes,
                    updatedAt = Instant.now()
                )
            )
        }
    }

    override fun observeActiveSession(): Flow<FastingSession?> {
        return fastingSessionDao.observeActiveSession().map { it?.toDomain() }
    }

    override fun observeAllSessions(): Flow<List<FastingSession>> {
        return fastingSessionDao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getActiveSession(): FastingSession? {
        return fastingSessionDao.getActiveSession()?.toDomain()
    }

    override suspend fun getAllSessions(): List<FastingSession> {
        return fastingSessionDao.getAll().map { it.toDomain() }
    }

    override suspend fun getSessionById(sessionId: Long): FastingSession? {
        return fastingSessionDao.getById(sessionId)?.toDomain()
    }

    override suspend fun getSessionsPaginated(limit: Int, offset: Int): List<FastingSession> {
        return fastingSessionDao.getSessionsPaginated(limit, offset).map { it.toDomain() }
    }

    override suspend fun getSessionsCount(): Int {
        return fastingSessionDao.getSessionsCount()
    }

    override suspend fun updateSession(session: FastingSession) {
        fastingSessionDao.update(session.toEntity())
    }

    override suspend fun deleteSession(sessionId: Long) {
        fastingSessionDao.deleteById(sessionId)
    }

    override suspend fun reassignPlan(fromPlanId: Long, toPlanId: Long) {
        fastingSessionDao.reassignPlan(fromPlanId, toPlanId)
    }
}
