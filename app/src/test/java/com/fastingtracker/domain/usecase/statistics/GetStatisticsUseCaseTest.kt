package com.fastingtracker.domain.usecase.statistics

import com.fastingtracker.domain.model.FastingPhase
import com.fastingtracker.domain.model.FastingSession
import com.fastingtracker.domain.model.Plan
import com.fastingtracker.domain.model.PlanType
import com.fastingtracker.domain.repository.FastingSessionRepository
import com.fastingtracker.domain.repository.PlanRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant

class GetStatisticsUseCaseTest {

    private val sessionRepo = mockk<FastingSessionRepository>()
    private val planRepo = mockk<PlanRepository>()
    private lateinit var useCase: GetStatisticsUseCase

    private val daily16_8 = Plan(
        id = 1L, name = "16:8", planType = PlanType.DAILY,
        fastingHours = 16, eatingHours = 8, isDefault = true,
        createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH
    )
    private val daily20_4 = Plan(
        id = 2L, name = "20:4", planType = PlanType.DAILY,
        fastingHours = 20, eatingHours = 4, isDefault = true,
        createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH
    )
    private val target24 = Plan(
        id = 3L, name = "24h", planType = PlanType.TARGET,
        fastingHours = 24, eatingHours = null, isDefault = true,
        createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH
    )

    @Before
    fun setUp() {
        useCase = GetStatisticsUseCase(sessionRepo, planRepo)
        coEvery { planRepo.getAllPlans() } returns listOf(daily16_8, daily20_4, target24)
    }

    private fun session(
        planId: Long,
        durationHours: Long,
        completed: Boolean = true,
        mood: Int? = null,
        hunger: Int? = null
    ): FastingSession {
        val start = Instant.parse("2025-01-01T08:00:00Z")
        return FastingSession(
            id = null,
            startTime = start,
            endTime = if (completed) start.plus(Duration.ofHours(durationHours)) else null,
            planId = planId,
            initialPhase = FastingPhase.FASTING,
            mood = mood,
            hunger = hunger,
            waterMl = null,
            notes = null,
            createdAt = start,
            updatedAt = start
        )
    }

    @Test
    fun `empty sessions returns zero statistics`() = runTest {
        coEvery { sessionRepo.getAllSessions() } returns emptyList()
        val stats = useCase()
        assertEquals(0, stats.totalSessions)
        assertEquals(0.0, stats.totalFastingTimeHours, 0.001)
        assertEquals(0.0, stats.averageFastingTimeHours, 0.001)
        assertEquals(0.0, stats.longestFastingTimeHours, 0.001)
        assertNull(stats.averageMood)
        assertNull(stats.averageHunger)
    }

    @Test
    fun `incomplete sessions are excluded`() = runTest {
        coEvery { sessionRepo.getAllSessions() } returns listOf(
            session(planId = 1L, durationHours = 0, completed = false)
        )
        val stats = useCase()
        assertEquals(0, stats.totalSessions)
    }

    @Test
    fun `single completed session counts actual elapsed hours`() = runTest {
        coEvery { sessionRepo.getAllSessions() } returns listOf(
            session(planId = 1L, durationHours = 16)
        )

        val stats = useCase()
        assertEquals(1, stats.totalSessions)
        assertEquals(16.0, stats.totalFastingTimeHours, 0.001)
        assertEquals(16.0, stats.averageFastingTimeHours, 0.001)
        assertEquals(16.0, stats.longestFastingTimeHours, 0.001)
    }

    @Test
    fun `single-fast model counts overtime and partial completed sessions honestly`() = runTest {
        coEvery { sessionRepo.getAllSessions() } returns listOf(
            session(planId = 1L, durationHours = 10),
            session(planId = 1L, durationHours = 18),
            session(planId = 2L, durationHours = 20),
            session(planId = 3L, durationHours = 24)
        )

        val stats = useCase()
        assertEquals(4, stats.totalSessions)
        assertEquals(72.0, stats.totalFastingTimeHours, 0.001)
        assertEquals(24.0, stats.longestFastingTimeHours, 0.001)
        assertEquals(18.0, stats.averageFastingTimeHours, 0.001)
    }

    @Test
    fun `mood and hunger averaged across all completed sessions`() = runTest {
        coEvery { sessionRepo.getAllSessions() } returns listOf(
            session(planId = 1L, durationHours = 16, mood = 4, hunger = 2),
            session(planId = 1L, durationHours = 16, mood = 5, hunger = 3),
            session(planId = 1L, durationHours = 16, mood = null, hunger = null)
        )

        val stats = useCase()
        assertEquals(4.5, stats.averageMood!!, 0.001)
        assertEquals(2.5, stats.averageHunger!!, 0.001)
    }

    @Test
    fun `N+1 fixed - getAllPlans called once regardless of session count`() = runTest {
        coEvery { sessionRepo.getAllSessions() } returns listOf(
            session(planId = 1L, durationHours = 16),
            session(planId = 2L, durationHours = 20),
            session(planId = 3L, durationHours = 24)
        )

        useCase()

        coVerify(exactly = 1) { planRepo.getAllPlans() }
        coVerify(exactly = 0) { planRepo.getPlanById(any()) }
    }
}
