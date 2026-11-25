package com.fastingtracker.domain.model

import com.fastingtracker.domain.model.FastingPhase
import java.time.Instant

/**
 * Domain model for fasting sessions
 * Pure Kotlin class with no Room annotations
 */
data class FastingSession(
    val id: Long? = null,
    val startTime: Instant,
    val endTime: Instant?,
    val planId: Long,
    val initialPhase: FastingPhase,
    val mood: Int?,
    val hunger: Int?,
    val waterMl: Int?,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
