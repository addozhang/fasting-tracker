package com.fastingtracker.domain.usecase.statistics

import com.fastingtracker.domain.model.PlanType
import com.fastingtracker.domain.model.FastingSession
import com.fastingtracker.domain.model.Plan
import java.time.Duration
import java.time.Instant

/**
 * Extension functions for calculating fasting session durations
 */

/**
 * Get pure fasting duration (without eating time for daily plans)
 * Matches Flutter's getPureFastingDuration logic
 */
fun FastingSession.getPureFastingDuration(plan: Plan): Duration {
    val actualDuration = if (endTime != null) {
        Duration.between(startTime, endTime)
    } else {
        Duration.between(startTime, Instant.now())
    }

    return when (plan.planType) {
        PlanType.TARGET -> {
            // Target plans only have fasting time
            actualDuration
        }
        PlanType.DAILY -> {
            // Daily plans: return minimum of actual duration and fasting hours
            val fastingDuration = Duration.ofHours(plan.fastingHours.toLong())
            if (actualDuration > fastingDuration) fastingDuration else actualDuration
        }
    }
}

/**
 * Check if this fasting session is a valid completion based on plan rules
 */
fun FastingSession.isValidFastingCompletion(plan: Plan): Boolean {
    if (endTime == null) return false // Not completed yet

    val actualDuration = Duration.between(startTime, endTime)
    val fastingTarget = Duration.ofHours(plan.fastingHours.toLong())

    return when (plan.planType) {
        PlanType.TARGET -> {
            // Target plans: must complete 100% of target time
            actualDuration >= fastingTarget
        }
        PlanType.DAILY -> {
            // Daily plans: check phase at completion
            val currentPhaseAtEnd = getCurrentPhaseAtEnd(plan)
            if (currentPhaseAtEnd == initialPhase) {
                // Ended during fasting phase - need 100% completion
                actualDuration >= fastingTarget
            } else {
                // Ended during eating phase - valid completion
                true
            }
        }
    }
}

/**
 * Get the fasting phase at the time of completion
 */
private fun FastingSession.getCurrentPhaseAtEnd(plan: Plan): com.fastingtracker.domain.model.FastingPhase {
    val actualDuration = Duration.between(startTime, endTime!!)
    val fastingDuration = Duration.ofHours(plan.fastingHours.toLong())

    return when (plan.planType) {
        PlanType.TARGET -> {
            if (actualDuration < fastingDuration) {
                com.fastingtracker.domain.model.FastingPhase.FASTING
            } else {
                com.fastingtracker.domain.model.FastingPhase.EATING
            }
        }
        PlanType.DAILY -> {
            // Daily plans: calculate position in cycle
            val cycleDuration = fastingDuration.plusHours((plan.eatingHours ?: 0).toLong())
            val positionInCycleHours = actualDuration.toHours() % cycleDuration.toHours()
            
            if (positionInCycleHours < fastingDuration.toHours()) {
                com.fastingtracker.domain.model.FastingPhase.FASTING
            } else {
                com.fastingtracker.domain.model.FastingPhase.EATING
            }
        }
    }
}
