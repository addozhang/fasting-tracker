package com.fastingtracker.domain.model

import com.fastingtracker.domain.model.NotificationType
import java.time.Instant

/**
 * Domain model for notification settings
 * Pure Kotlin class with no Room annotations
 */
data class NotificationSetting(
    val id: Long? = null,
    val type: NotificationType,
    val title: String,
    val message: String,
    val advanceMinutes: Int,
    val isEnabled: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
