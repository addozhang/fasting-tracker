package com.fastingtracker.presentation

import app.cash.turbine.test
import com.fastingtracker.domain.coordinator.FastingSessionCoordinator
import com.fastingtracker.domain.model.FastingPhase
import com.fastingtracker.domain.model.FastingSession
import com.fastingtracker.domain.model.Plan
import com.fastingtracker.domain.model.PlanType
import com.fastingtracker.domain.repository.FastingSessionRepository
import com.fastingtracker.domain.repository.PlanRepository
import com.fastingtracker.domain.repository.SettingsRepository
import com.fastingtracker.domain.usecase.fasting.EndFastOutcome
import com.fastingtracker.domain.usecase.plan.CreatePlanUseCase
import com.fastingtracker.domain.usecase.plan.DeletePlanUseCase
import com.fastingtracker.domain.usecase.statistics.GetCurrentFastingProgressUseCase
import com.fastingtracker.presentation.home.HomeUiState
import com.fastingtracker.presentation.home.HomeViewModel
import com.fastingtracker.util.TickerProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val plan168 = Plan(1L, "16:8", PlanType.DAILY, 16, 8, true, Instant.now(), Instant.now())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `emits Idle when no active session and ActiveFasting when active`() = runTest {
        val coordinator = mockk<FastingSessionCoordinator>(relaxed = true)
        val sessionRepo = mockk<FastingSessionRepository>()
        val planRepo = mockk<PlanRepository>()
        val settingsRepo = mockk<SettingsRepository>(relaxed = true)
        val progressUseCase = GetCurrentFastingProgressUseCase(sessionRepo, planRepo)
        val createPlan = mockk<CreatePlanUseCase>(relaxed = true)
        val deletePlan = mockk<DeletePlanUseCase>(relaxed = true)
        val ticker = mockk<TickerProvider>()

        every { sessionRepo.observeActiveSession() } returns flowOf(null)
        every { planRepo.observeAllPlans() } returns flowOf(listOf(plan168))
        every { sessionRepo.observeAllSessions() } returns flowOf(emptyList())
        every { settingsRepo.observeSelectedPlanId() } returns flowOf(1L)
        every { ticker.ticker(any()) } returns emptyFlow()

        val vm = HomeViewModel(
            coordinator, sessionRepo, planRepo, settingsRepo, progressUseCase, createPlan, deletePlan, ticker
        )

        vm.uiState.test {
            assertTrue(awaitItem() is HomeUiState.Loading)
            val idle = awaitItem() as HomeUiState.Idle
            assertEquals("16:8", idle.selectedPlan?.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `start and end fast delegate to coordinator`() = runTest {
        val coordinator = mockk<FastingSessionCoordinator>(relaxed = true)
        val sessionRepo = mockk<FastingSessionRepository>()
        val planRepo = mockk<PlanRepository>()
        val settingsRepo = mockk<SettingsRepository>(relaxed = true)
        val progressUseCase = GetCurrentFastingProgressUseCase(sessionRepo, planRepo)
        val createPlan = mockk<CreatePlanUseCase>(relaxed = true)
        val deletePlan = mockk<DeletePlanUseCase>(relaxed = true)
        val ticker = mockk<TickerProvider>()

        val active = FastingSession(7L, Instant.now().minusSeconds(7200), null, 1L, FastingPhase.FASTING, null, null, null, null, Instant.now(), Instant.now())
        every { sessionRepo.observeActiveSession() } returns flowOf(active)
        every { planRepo.observeAllPlans() } returns flowOf(listOf(plan168))
        every { sessionRepo.observeAllSessions() } returns flowOf(listOf(active))
        every { settingsRepo.observeSelectedPlanId() } returns flowOf(1L)
        every { ticker.ticker(any()) } returns emptyFlow()
        coEvery { coordinator.startFast(1L, 0L) } returns Result.success(7L)
        coEvery { coordinator.endFast(any(), any(), any()) } returns Result.success(EndFastOutcome.Saved(active))

        val vm = HomeViewModel(
            coordinator, sessionRepo, planRepo, settingsRepo, progressUseCase, createPlan, deletePlan, ticker
        )
        vm.onStartFasting(1L, 0L)
        vm.onEndFasting(mood = 4, notes = "Great")
        advanceUntilIdle()

        coVerify(exactly = 1) { coordinator.startFast(1L, 0L) }
        coVerify(exactly = 1) { coordinator.endFast(any(), 4, "Great") }
    }
}
