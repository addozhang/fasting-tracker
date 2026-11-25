package com.fastingtracker.data.repository

import com.fastingtracker.data.local.dao.FastingSessionDao
import com.fastingtracker.domain.model.FastingPhase
import com.fastingtracker.data.local.entity.FastingSessionEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

class FastingSessionRepositoryImplTest {

    private val dao = mockk<FastingSessionDao>()
    private lateinit var repository: FastingSessionRepositoryImpl

    @Before
    fun setUp() {
        repository = FastingSessionRepositoryImpl(dao)
    }

    // ─── getActiveSession ────────────────────────────────────────────────────

    @Test
    fun `getActiveSession returns null when no active session`() = runTest {
        coEvery { dao.getActiveSession() } returns null
        assertNull(repository.getActiveSession())
    }

    @Test
    fun `getActiveSession returns domain model when active session exists`() = runTest {
        val now = Instant.now()
        coEvery { dao.getActiveSession() } returns makeEntity(startTime = now)
        val result = repository.getActiveSession()
        assertNotNull(result)
        assertEquals(now, result!!.startTime)
        assertNull(result.endTime)
    }

    // ─── startFasting ────────────────────────────────────────────────────────

    @Test
    fun `startFasting inserts session and returns generated id`() = runTest {
        coEvery { dao.insert(any()) } returns 42L
        val session = makeDomainSession()
        val id = repository.startFasting(session)
        assertEquals(42L, id)
        coVerify(exactly = 1) { dao.insert(any()) }
    }

    // ─── endFasting ──────────────────────────────────────────────────────────

    @Test
    fun `endFasting updates entity with end time`() = runTest {
        val now = Instant.now()
        val entity = makeEntity(id = 1L)
        val endTime = now.plusSeconds(3600)
        val updatedSlot = slot<FastingSessionEntity>()

        coEvery { dao.getById(1L) } returns entity
        coEvery { dao.update(capture(updatedSlot)) } returns Unit

        repository.endFasting(1L, endTime)

        assertEquals(endTime, updatedSlot.captured.endTime)
    }

    @Test
    fun `endFasting does nothing if session not found`() = runTest {
        coEvery { dao.getById(999L) } returns null
        // Should not throw
        repository.endFasting(999L, Instant.now())
        coVerify(exactly = 0) { dao.update(any()) }
    }

    // ─── getAllSessions ───────────────────────────────────────────────────────

    @Test
    fun `getAllSessions returns empty list when no sessions`() = runTest {
        coEvery { dao.getAll() } returns emptyList()
        assertEquals(emptyList<Any>(), repository.getAllSessions())
    }

    @Test
    fun `getAllSessions maps all entities to domain models`() = runTest {
        coEvery { dao.getAll() } returns listOf(makeEntity(id = 1L), makeEntity(id = 2L))
        val result = repository.getAllSessions()
        assertEquals(2, result.size)
    }

    // ─── getSessionsCount ────────────────────────────────────────────────────

    @Test
    fun `getSessionsCount returns dao count`() = runTest {
        coEvery { dao.getSessionsCount() } returns 7
        assertEquals(7, repository.getSessionsCount())
    }

    // ─── observeActiveSession ────────────────────────────────────────────────

    @Test
    fun `observeActiveSession emits null when no active session`() = runTest {
        coEvery { dao.observeActiveSession() } returns flowOf(null)
        val flow = repository.observeActiveSession()
        // Collect one value
        var emitted: Any? = "not_set"
        flow.collect { emitted = it }
        assertNull(emitted)
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun makeEntity(
        id: Long? = 1L,
        startTime: Instant = Instant.now(),
        endTime: Instant? = null,
        planId: Long = 1L
    ) = FastingSessionEntity(
        id = id,
        startTime = startTime,
        endTime = endTime,
        planId = planId,
        initialPhase = FastingPhase.FASTING,
        mood = null,
        hunger = null,
        waterMl = null,
        notes = null,
        createdAt = startTime,
        updatedAt = startTime
    )

    private fun makeDomainSession(
        startTime: Instant = Instant.now(),
        planId: Long = 1L
    ) = com.fastingtracker.domain.model.FastingSession(
        id = null,
        startTime = startTime,
        endTime = null,
        planId = planId,
        initialPhase = FastingPhase.FASTING,
        mood = null,
        hunger = null,
        waterMl = null,
        notes = null,
        createdAt = startTime,
        updatedAt = startTime
    )
}
