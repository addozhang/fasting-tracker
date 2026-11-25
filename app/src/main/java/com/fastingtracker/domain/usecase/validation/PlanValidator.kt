package com.fastingtracker.domain.usecase.validation

import javax.inject.Inject

/**
 * Validator for plan-related input validation
 * Based on Flutter's ValidationMixin
 */
class PlanValidator @Inject constructor() {
    
    /**
     * Validate fasting hours (1-72 hours)
     */
    fun isValidFastingHours(hours: Int): Boolean {
        return hours in 1..72
    }

    /**
     * Validate eating hours (0-24 hours)
     */
    fun isValidEatingHours(hours: Int): Boolean {
        return hours in 0..24
    }

    /**
     * Validate plan name (not empty, reasonable length)
     */
    fun isValidPlanName(name: String): Boolean {
        val trimmed = name.trim()
        return trimmed.isNotEmpty() && trimmed.length <= 50
    }
}
