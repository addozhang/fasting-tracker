package com.fastingtracker.domain.usecase

import com.fastingtracker.domain.model.FastingPhase
import com.fastingtracker.domain.model.FastingSession
import com.fastingtracker.domain.model.Plan
import com.fastingtracker.domain.model.PlanType
import com.fastingtracker.domain.repository.FastingSessionRepository
import com.fastingtracker.domain.repository.PlanRepository
import com.fastingtracker.domain.usecase.plan.DeletePlanUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class DeletePlanUseCaseTest {

    private lateinit var planRepository: PlanRepository
    private lateinit var sessionRepository: FastingSessionRepository
    private lateinit var deletePlanUseCase: DeletePlanUseCase

    private val defaultPlan = Plan(
        id = 1L,
        name = "16:8",
        planType = PlanType.DAILY,
        fastingHours = 16,
        eatingHours = 8,
        isDefault = true,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )
    private val customPlan = Plan(
        id = 10L,
        name = "15:9 Custom",
        planType = PlanType.DAILY,
        fastingHours = 15,
        eatingHours = 9,
        isDefault = false,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    @Before
    fun setup() {
        planRepository = mockk(relaxed = true)
        sessionRepository = mockk(relaxed = true)
        deletePlanUseCase = DeletePlanUseCase(planRepository, sessionRepository)
    }

    @Test
    fun `cannot delete default plan`() = runTest {
        coEvery { planRepository.getPlanById(1L) } returns defaultPlan
        val result = deletePlanUseCase(1L)
        assertTrue(result.isFailure)
        coVerify(exactly = 0) { planRepository.deletePlan(any()) }
    }

    @Test
    fun `cannot delete custom plan with active session`() = runTest {
        coEvery { planRepository.getPlanById(10L) } returns customPlan
        coEvery { sessionRepository.getActiveSession() } returns FastingSession(
            id = 5L,
            startTime = Instant.now(),
            endTime = null,
            planId = 10L,
            initialPhase = FastingPhase.FASTING,
            mood = null,
            hunger = null,
            waterMl = null,
            notes = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val result = deletePlanUseCase(10L)
        assertTrue(result.isFailure)
        coVerify(exactly = 0) { planRepository.deletePlan(any()) }
    }

    @Test
    fun `deleting custom plan reassigns historical sessions to fallback default plan before delete`() = runTest {
        coEvery { planRepository.getPlanById(10L) } returns customPlan
        coEvery { sessionRepository.getActiveSession() } returns null
        coEvery { planRepository.getAllPlans() } returns listOf(defaultPlan, customPlan)

        val result = deletePlanUseCase(10L)
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { sessionRepository.reassignPlan(10L, 1L) }
        coVerify(exactly = 1) { planRepository.deletePlan(customPlan) }
    }
}
