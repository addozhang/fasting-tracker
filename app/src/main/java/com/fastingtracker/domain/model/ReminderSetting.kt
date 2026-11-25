package com.fastingtracker.domain.model

import java.time.Instant

/**
 * Domain model for reminder settings
 * Pure Kotlin class with no Room annotations
 */
data class ReminderSetting(
    val id: Long? = null,
    val type: String,
    val intervalMinutes: Int,
    val isEnabled: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
