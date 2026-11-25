package com.fastingtracker.domain.coordinator

import android.content.Context
import android.util.Log
import com.fastingtracker.domain.model.FastingPhase
import com.fastingtracker.domain.repository.FastingSessionRepository
import com.fastingtracker.domain.repository.PlanRepository
import com.fastingtracker.domain.repository.SettingsRepository
import com.fastingtracker.domain.usecase.fasting.EndFastOutcome
import com.fastingtracker.domain.usecase.fasting.EndFastingUseCase
import com.fastingtracker.domain.usecase.fasting.StartFastingUseCase
import com.fastingtracker.domain.usecase.fasting.UpdateActiveStartTimeUseCase
import com.fastingtracker.domain.usecase.statistics.GetCurrentFastingProgressUseCase
import com.fastingtracker.notification.FastingAlarmScheduler
import com.fastingtracker.notification.LiveUpdateRefreshWorker
import com.fastingtracker.notification.NotificationHelper
import com.fastingtracker.widget.FastingWidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates fasting & eating-window lifecycle side effects across Room DB,
 * Android 16 Live Updates (HyperOS 4 Super Island), AlarmManager alarms,
 * 15-min WorkManager refresh, and Glance widgets.
 */
@Singleton
class FastingSessionCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val startFastingUseCase: StartFastingUseCase,
    private val endFastingUseCase: EndFastingUseCase,
    private val updateActiveStartTimeUseCase: UpdateActiveStartTimeUseCase,
    private val getCurrentFastingProgress: GetCurrentFastingProgressUseCase,
    private val sessionRepository: FastingSessionRepository,
    private val planRepository: PlanRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: NotificationHelper,
    private val alarmScheduler: FastingAlarmScheduler,
    private val widgetUpdater: FastingWidgetUpdater
) {
    companion object {
        private const val TAG = "FastingCoordinator"
        private const val EATING_WINDOW_GRACE_HOURS = 2L
    }

    suspend fun startFast(planId: Long, startOffsetMinutes: Long = 0L): Result<Long> {
        settingsRepository.setSelectedPlanId(planId)
        val result = startFastingUseCase(
            planId = planId,
            initialPhase = FastingPhase.FASTING,
            startOffsetMinutes = startOffsetMinutes
        )
        if (result.isSuccess) {
            syncActiveSurfaces(rescheduleAlarms = true)
        }
        return result
    }

    suspend fun updateActiveStartTime(newStartTime: Instant): Result<Unit> {
        val previous = sessionRepository.getActiveSession()
        val result = updateActiveStartTimeUseCase(newStartTime)
        if (result.isSuccess) {
            previous?.id?.let { alarmScheduler.cancelAllAlarmsForSession(it) }
            syncActiveSurfaces(rescheduleAlarms = true)
            return Result.success(Unit)
        }
        return Result.failure(result.exceptionOrNull() ?: IllegalStateException("Update failed"))
    }

    suspend fun endFast(
        endTime: Instant = Instant.now(),
        mood: Int? = null,
        notes: String? = null
    ): Result<EndFastOutcome> {
        val activeSessionId = sessionRepository.getActiveSession()?.id
        val result = endFastingUseCase(endTime = endTime, mood = mood, notes = notes)
        if (result.isSuccess) {
            activeSessionId?.let { alarmScheduler.cancelAllAlarmsForSession(it) }
            syncActiveSurfaces(rescheduleAlarms = false)
        }
        return result
    }

    /**
     * Synchronizes ongoing Live Update notification (Fasting OR active Eating Window),
     * alarms (optional), 15-min refresh worker, and Glance widgets with DB state.
     * Eating window Super Island automatically expires after (eatingHours + 2h grace).
     */
    suspend fun syncActiveSurfaces(
        rescheduleAlarms: Boolean = false,
        now: Instant = Instant.now()
    ) {
        val progress = getCurrentFastingProgress(now)
        if (progress != null) {
            runCatching { notificationHelper.showOngoingLiveUpdate(progress) }
                .onFailure { Log.w(TAG, "Failed to update live notification", it) }
            if (rescheduleAlarms) {
                runCatching {
                    alarmScheduler.scheduleSessionAlarms(
                        sessionId = progress.sessionId,
                        startTime = progress.startTime,
                        fastingHours = progress.fastingHours
                    )
                }.onFailure { Log.w(TAG, "Failed to schedule alarms", it) }
            }
            runCatching { LiveUpdateRefreshWorker.enqueue(context) }
                .onFailure { Log.w(TAG, "Failed to enqueue refresh worker", it) }
        } else {
            val showingEatingIsland = tryShowEatingWindowIsland(now)
            if (showingEatingIsland) {
                runCatching { LiveUpdateRefreshWorker.enqueue(context) }
                    .onFailure { Log.w(TAG, "Failed to enqueue eating refresh worker", it) }
            } else {
                notificationHelper.cancelOngoingNotification()
                runCatching { LiveUpdateRefreshWorker.cancel(context) }
            }
        }
        runCatching { widgetUpdater.updateAll() }
            .onFailure { Log.w(TAG, "Failed to refresh widgets", it) }
    }

    private suspend fun tryShowEatingWindowIsland(now: Instant): Boolean {
        val lastCompleted = sessionRepository.getAllSessions()
            .filter { it.endTime != null }
            .maxByOrNull { it.endTime!! } ?: return false
        val lastEnded = lastCompleted.endTime ?: return false

        val plans = planRepository.getAllPlans()
        val selectedPlanId = settingsRepository.getSelectedPlanId()
        val plan = plans.firstOrNull { it.id == selectedPlanId }
            ?: plans.firstOrNull { it.id == lastCompleted.planId }
            ?: return false
        val eatingHours = plan.eatingHours ?: return false

        val elapsedSeconds = Duration.between(lastEnded, now).seconds
        val maxWindowSeconds = (eatingHours + EATING_WINDOW_GRACE_HOURS) * 3600L
        if (elapsedSeconds < 0L || elapsedSeconds > maxWindowSeconds) {
            return false
        }

        runCatching {
            notificationHelper.showEatingWindowLiveUpdate(
                planName = plan.name,
                lastEndedTime = lastEnded,
                eatingHours = eatingHours,
                now = now
            )
        }.onFailure { Log.w(TAG, "Failed to show eating window live update", it) }
        return true
    }
}
