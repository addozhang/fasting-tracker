package com.fastingtracker.domain.usecase.statistics

import com.fastingtracker.domain.model.FastingPhase
import com.fastingtracker.domain.model.FastingSubPhase
import java.time.Instant

/**
 * Data class representing current fasting progress (single-fast model).
 */
data class FastingProgress(
    val progressPercent: Int, // 0-100
    val remainingTimeSeconds: Long,
    val elapsedTimeSeconds: Long,
    val currentPhase: FastingPhase,
    val targetMinutes: Long,
    val planName: String,
    val phaseMilestone: FastingSubPhase?,
    val sessionId: Long = 0L,
    val planId: Long = 0L,
    val fastingHours: Int = (targetMinutes / 60).toInt().coerceAtLeast(1),
    val startTime: Instant = Instant.now().minusSeconds(elapsedTimeSeconds),
    val expectedEndTime: Instant = startTime.plusSeconds(targetMinutes * 60L),
    val isGoalReached: Boolean = elapsedTimeSeconds >= targetMinutes * 60L,
    val overtimeSeconds: Long = (elapsedTimeSeconds - targetMinutes * 60L).coerceAtLeast(0L)
)

/**
 * Get fasting sub-phase based on elapsed fasting hours.
 * Returns null if less than 12 hours (basic fasting).
 */
fun getPhaseMilestone(elapsedHours: Long): FastingSubPhase? {
    return when {
        elapsedHours < 12 -> null
        else -> FastingSubPhase.fromElapsedHours(elapsedHours)
    }
}
