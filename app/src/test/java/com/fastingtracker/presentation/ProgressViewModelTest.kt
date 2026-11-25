package com.fastingtracker.presentation

import app.cash.turbine.test
import com.fastingtracker.domain.model.FastingPhase
import com.fastingtracker.domain.model.FastingSession
import com.fastingtracker.domain.model.Plan
import com.fastingtracker.domain.model.PlanType
import com.fastingtracker.domain.repository.FastingSessionRepository
import com.fastingtracker.domain.repository.PlanRepository
import com.fastingtracker.domain.usecase.fasting.DeleteSessionUseCase
import com.fastingtracker.domain.usecase.fasting.SaveSessionRecordUseCase
import com.fastingtracker.domain.usecase.statistics.GetStatisticsUseCase
import com.fastingtracker.presentation.progress.ProgressViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `reactively computes statistics from observed sessions`() = runTest {
        val sessionRepo = mockk<FastingSessionRepository>(relaxed = true)
        val planRepo = mockk<PlanRepository>(relaxed = true)
        val now = Instant.now()
        val completed = FastingSession(
            id = 1L,
            startTime = now.minusSeconds(18L * 3600),
            endTime = now.minusSeconds(2L * 3600),
            planId = 1L,
            initialPhase = FastingPhase.FASTING,
            mood = 4, hunger = null, waterMl = null, notes = "Good",
            createdAt = now, updatedAt = now
        )
        val plan = Plan(1L, "16:8", PlanType.DAILY, 16, 8, true, now, now)

        every { sessionRepo.observeAllSessions() } returns flowOf(listOf(completed))
        every { planRepo.observeAllPlans() } returns flowOf(listOf(plan))

        val vm = ProgressViewModel(
            sessionRepository = sessionRepo,
            planRepository = planRepo,
            getStatisticsUseCase = GetStatisticsUseCase(sessionRepo, planRepo),
            saveSessionRecordUseCase = SaveSessionRecordUseCase(sessionRepo),
            deleteSessionUseCase = DeleteSessionUseCase(sessionRepo)
        )

        vm.uiState.test {
            skipItems(1) // initial loading
            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals(1, loaded.statistics.totalSessions)
            assertEquals(16.0, loaded.statistics.longestFastingTimeHours, 0.1)
            assertEquals(1, loaded.sessionHistory.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
