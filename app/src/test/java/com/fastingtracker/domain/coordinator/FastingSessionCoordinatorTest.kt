package com.fastingtracker.domain.coordinator

import android.content.Context
import com.fastingtracker.domain.model.FastingPhase
import com.fastingtracker.domain.model.FastingSession
import com.fastingtracker.domain.model.Plan
import com.fastingtracker.domain.model.PlanType
import com.fastingtracker.domain.repository.FastingSessionRepository
import com.fastingtracker.domain.repository.PlanRepository
import com.fastingtracker.domain.repository.SettingsRepository
import com.fastingtracker.domain.usecase.fasting.EndFastOutcome
import com.fastingtracker.domain.usecase.fasting.EndFastingUseCase
import com.fastingtracker.domain.usecase.fasting.StartFastingUseCase
import com.fastingtracker.domain.usecase.fasting.UpdateActiveStartTimeUseCase
import com.fastingtracker.domain.usecase.statistics.FastingProgress
import com.fastingtracker.domain.usecase.statistics.GetCurrentFastingProgressUseCase
import com.fastingtracker.notification.FastingAlarmScheduler
import com.fastingtracker.notification.NotificationHelper
import com.fastingtracker.widget.FastingWidgetUpdater
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class FastingSessionCoordinatorTest {

    private val context = mockk<Context>(relaxed = true)
    private val startUseCase = mockk<StartFastingUseCase>()
    private val endUseCase = mockk<EndFastingUseCase>()
    private val updateStartUseCase = mockk<UpdateActiveStartTimeUseCase>()
    private val getProgress = mockk<GetCurrentFastingProgressUseCase>()
    private val sessionRepo = mockk<FastingSessionRepository>(relaxed = true)
    private val planRepo = mockk<PlanRepository>(relaxed = true)
    private val settingsRepo = mockk<SettingsRepository>(relaxed = true)
    private val notifHelper = mockk<NotificationHelper>(relaxed = true)
    private val alarmScheduler = mockk<FastingAlarmScheduler>(relaxed = true)
    private val widgetUpdater = mockk<FastingWidgetUpdater>(relaxed = true)

    private lateinit var coordinator: FastingSessionCoordinator

    private val now = Instant.now()
    private val plan168 = Plan(1L, "16:8", PlanType.DAILY, 16, 8, true, now, now)
    private val sampleProgress = FastingProgress(
        progressPercent = 25,
        remainingTimeSeconds = 12L * 3600,
        elapsedTimeSeconds = 4L * 3600,
        currentPhase = FastingPhase.FASTING,
        targetMinutes = 16L * 60,
        planName = "16:8",
        phaseMilestone = null,
        sessionId = 77L,
        planId = 1L,
        fastingHours = 16,
        startTime = now.minusSeconds(4L * 3600),
        expectedEndTime = now.plusSeconds(12L * 3600),
        isGoalReached = false,
        overtimeSeconds = 0L
    )

    @Before
    fun setup() {
        coordinator = FastingSessionCoordinator(
            context = context,
            startFastingUseCase = startUseCase,
            endFastingUseCase = endUseCase,
            updateActiveStartTimeUseCase = updateStartUseCase,
            getCurrentFastingProgress = getProgress,
            sessionRepository = sessionRepo,
            planRepository = planRepo,
            settingsRepository = settingsRepo,
            notificationHelper = notifHelper,
            alarmScheduler = alarmScheduler,
            widgetUpdater = widgetUpdater
        )
    }

    @Test
    fun `startFast persists selected plan, posts live update, schedules alarms, and updates widgets`() = runTest {
        coEvery { startUseCase(1L, FastingPhase.FASTING, 30L) } returns Result.success(77L)
        coEvery { getProgress(any()) } returns sampleProgress

        val result = coordinator.startFast(planId = 1L, startOffsetMinutes = 30L)
        assertTrue(result.isSuccess)

        coVerify(exactly = 1) { settingsRepo.setSelectedPlanId(1L) }
        coVerify(exactly = 1) { notifHelper.showOngoingLiveUpdate(sampleProgress) }
        verify(exactly = 1) { alarmScheduler.scheduleSessionAlarms(77L, sampleProgress.startTime, 16, any()) }
        coVerify(exactly = 1) { widgetUpdater.updateAll() }
    }

    @Test
    fun `updateActiveStartTime cancels previous alarms and reschedules with new start time`() = runTest {
        val activeSession = FastingSession(
            id = 77L, startTime = now.minusSeconds(4L * 3600), endTime = null,
            planId = 1L, initialPhase = FastingPhase.FASTING,
            mood = null, hunger = null, waterMl = null, notes = null,
            createdAt = now, updatedAt = now
        )
        val newStart = now.minusSeconds(6L * 3600)
        val updatedProgress = sampleProgress.copy(startTime = newStart)

        coEvery { sessionRepo.getActiveSession() } returns activeSession
        coEvery { updateStartUseCase(newStart, any()) } returns Result.success(activeSession.copy(startTime = newStart))
        coEvery { getProgress(any()) } returns updatedProgress

        val result = coordinator.updateActiveStartTime(newStart)
        assertTrue(result.isSuccess)

        verify(exactly = 1) { alarmScheduler.cancelAllAlarmsForSession(77L) }
        verify(exactly = 1) { alarmScheduler.scheduleSessionAlarms(77L, newStart, 16, any()) }
        coVerify(exactly = 1) { notifHelper.showOngoingLiveUpdate(updatedProgress) }
        coVerify(exactly = 1) { widgetUpdater.updateAll() }
    }

    @Test
    fun `endFast cancels alarms for exact sessionId and transitions to eating window island`() = runTest {
        val activeSession = FastingSession(
            id = 77L, startTime = now.minusSeconds(16L * 3600), endTime = null,
            planId = 1L, initialPhase = FastingPhase.FASTING,
            mood = null, hunger = null, waterMl = null, notes = null,
            createdAt = now, updatedAt = now
        )
        val savedSession = activeSession.copy(endTime = now)
        coEvery { sessionRepo.getActiveSession() } returns activeSession
        coEvery { endUseCase(now, 4, "Good") } returns Result.success(EndFastOutcome.Saved(savedSession))
        coEvery { getProgress(any()) } returns null
        coEvery { sessionRepo.getAllSessions() } returns listOf(savedSession)
        coEvery { planRepo.getAllPlans() } returns listOf(plan168)
        coEvery { settingsRepo.getSelectedPlanId() } returns 1L

        val result = coordinator.endFast(endTime = now, mood = 4, notes = "Good")
        assertTrue(result.isSuccess)

        verify(exactly = 1) { alarmScheduler.cancelAllAlarmsForSession(77L) }
        coVerify(exactly = 1) { notifHelper.showEatingWindowLiveUpdate("16:8", now, 8, any()) }
        coVerify(exactly = 1) { widgetUpdater.updateAll() }
    }

    @Test
    fun `syncActiveSurfaces auto-cancels eating window island after window plus grace expires`() = runTest {
        val oldCompleted = FastingSession(
            id = 5L,
            startTime = now.minusSeconds(28L * 3600),
            endTime = now.minusSeconds(12L * 3600), // 12h ago > 8h + 2h grace
            planId = 1L, initialPhase = FastingPhase.FASTING,
            mood = null, hunger = null, waterMl = null, notes = null,
            createdAt = now, updatedAt = now
        )
        coEvery { getProgress(now) } returns null
        coEvery { sessionRepo.getAllSessions() } returns listOf(oldCompleted)
        coEvery { planRepo.getAllPlans() } returns listOf(plan168)
        coEvery { settingsRepo.getSelectedPlanId() } returns 1L

        coordinator.syncActiveSurfaces(rescheduleAlarms = false, now = now)

        verify(exactly = 1) { notifHelper.cancelOngoingNotification() }
        coVerify(exactly = 0) { notifHelper.showEatingWindowLiveUpdate(any(), any(), any(), any()) }
        coVerify(exactly = 1) { widgetUpdater.updateAll() }
    }
}
