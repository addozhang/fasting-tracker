package com.fastingtracker.domain.usecase

import com.fastingtracker.domain.model.FastingPhase
import com.fastingtracker.domain.model.FastingSession
import com.fastingtracker.domain.model.FastingSubPhase
import com.fastingtracker.domain.model.Plan
import com.fastingtracker.domain.model.PlanType
import com.fastingtracker.domain.repository.FastingSessionRepository
import com.fastingtracker.domain.repository.PlanRepository
import com.fastingtracker.domain.usecase.statistics.GetCurrentFastingProgressUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class GetCurrentFastingProgressUseCaseTest {

    private lateinit var sessionRepository: FastingSessionRepository
    private lateinit var planRepository: PlanRepository
    private lateinit var useCase: GetCurrentFastingProgressUseCase

    private val plan168 = Plan(
        id = 1L,
        name = "16:8",
        planType = PlanType.DAILY,
        fastingHours = 16,
        eatingHours = 8,
        isDefault = true,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    @Before
    fun setup() {
        sessionRepository = mockk()
        planRepository = mockk()
        useCase = GetCurrentFastingProgressUseCase(sessionRepository, planRepository)
    }

    @Test
    fun `returns null when no active session`() = runTest {
        coEvery { sessionRepository.getActiveSession() } returns null
        assertNull(useCase())
    }

    @Test
    fun `calculates progress and fat burning sub-phase at 13h`() = runTest {
        val now = Instant.parse("2026-11-25T10:00:00Z")
        val session = FastingSession(
            id = 42L,
            startTime = now.minusSeconds(13L * 3600),
            endTime = null,
            planId = 1L,
            initialPhase = FastingPhase.FASTING,
            mood = null, hunger = null, waterMl = null, notes = null,
            createdAt = now, updatedAt = now
        )
        coEvery { sessionRepository.getActiveSession() } returns session
        coEvery { planRepository.getPlanById(1L) } returns plan168

        val progress = useCase(now)
        assertNotNull(progress)
        assertEquals(42L, progress!!.sessionId)
        assertEquals(81, progress.progressPercent)
        assertEquals(3L * 3600, progress.remainingTimeSeconds)
        assertEquals(FastingSubPhase.FAT_BURNING, progress.phaseMilestone)
        assertFalse(progress.isGoalReached)
    }

    @Test
    fun `enters overtime goal-reached state after target hours in single-fast model`() = runTest {
        val now = Instant.parse("2026-11-25T15:00:00Z")
        val session = FastingSession(
            id = 43L,
            startTime = now.minusSeconds(17L * 3600 + 30L * 60),
            endTime = null,
            planId = 1L,
            initialPhase = FastingPhase.FASTING,
            mood = null, hunger = null, waterMl = null, notes = null,
            createdAt = now, updatedAt = now
        )

        val progress = useCase.calculate(session, plan168, now)
        assertEquals(100, progress.progressPercent)
        assertEquals(0L, progress.remainingTimeSeconds)
        assertTrue(progress.isGoalReached)
        assertEquals(90L * 60, progress.overtimeSeconds)
        assertEquals(FastingSubPhase.KETOSIS, progress.phaseMilestone)
    }
}
