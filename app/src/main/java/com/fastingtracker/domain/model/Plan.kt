package com.fastingtracker.domain.model

import com.fastingtracker.domain.model.PlanType
import java.time.Instant

/**
 * Domain model for fasting plans
 * Pure Kotlin class with no Room annotations
 *
 * id semantics:
 *   null  → new plan not yet persisted (Room will auto-generate id on insert)
 *   > 0   → existing plan with a real database id
 *   0     → INVALID — Room treats 0 as a real id and will REPLACE any existing row with id=0
 */
data class Plan(
    val id: Long? = null,
    val name: String,
    val planType: PlanType,
    val fastingHours: Int,
    val eatingHours: Int?,
    val isDefault: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    init {
        require(id == null || id > 0L) {
            "Plan.id must be null (new, unsaved) or a positive number (existing). " +
            "Got id=$id — passing 0 is invalid: Room will treat it as a real row id."
        }
    }
}
