package com.fastingtracker.domain.usecase.statistics

import com.fastingtracker.domain.model.FastingPhase
import com.fastingtracker.domain.model.FastingSession
import com.fastingtracker.domain.model.Plan
import com.fastingtracker.domain.repository.FastingSessionRepository
import com.fastingtracker.domain.repository.PlanRepository
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Use case to calculate current fasting progress under the v2.0 single-fast model.
 * Progress counts from 0% to 100% up to [Plan.fastingHours], then enters overtime
 * (goal reached) while continuing to track total elapsed time and metabolic milestones.
 */
class GetCurrentFastingProgressUseCase @Inject constructor(
    private val sessionRepository: FastingSessionRepository,
    private val planRepository: PlanRepository
) {
    suspend operator fun invoke(now: Instant = Instant.now()): FastingProgress? {
        val activeSession = sessionRepository.getActiveSession() ?: return null
        val plan = planRepository.getPlanById(activeSession.planId) ?: return null
        return calculate(activeSession, plan, now)
    }

    fun calculate(
        session: FastingSession,
        plan: Plan,
        now: Instant = Instant.now()
    ): FastingProgress {
        val elapsedSeconds = Duration.between(session.startTime, now).seconds.coerceAtLeast(0L)
        val elapsedHours = elapsedSeconds / 3600L
        val targetMinutes = plan.fastingHours * 60L
        val targetSeconds = plan.fastingHours * 3600L

        val progressPercent = if (targetSeconds > 0) {
            ((elapsedSeconds.toDouble() / targetSeconds) * 100.0).roundToInt().coerceIn(0, 100)
        } else 100

        val remainingSeconds = (targetSeconds - elapsedSeconds).coerceAtLeast(0L)
        val isGoalReached = elapsedSeconds >= targetSeconds
        val overtimeSeconds = (elapsedSeconds - targetSeconds).coerceAtLeast(0L)

        return FastingProgress(
            progressPercent = progressPercent,
            remainingTimeSeconds = remainingSeconds,
            elapsedTimeSeconds = elapsedSeconds,
            currentPhase = FastingPhase.FASTING,
            targetMinutes = targetMinutes,
            planName = plan.name,
            phaseMilestone = getPhaseMilestone(elapsedHours),
            sessionId = session.id ?: 0L,
            planId = plan.id ?: session.planId,
            fastingHours = plan.fastingHours,
            startTime = session.startTime,
            expectedEndTime = session.startTime.plusSeconds(targetSeconds),
            isGoalReached = isGoalReached,
            overtimeSeconds = overtimeSeconds
        )
    }
}
