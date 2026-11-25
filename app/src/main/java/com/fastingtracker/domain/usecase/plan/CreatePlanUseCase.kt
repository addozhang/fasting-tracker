package com.fastingtracker.domain.usecase.plan

import com.fastingtracker.domain.model.Plan
import com.fastingtracker.domain.repository.PlanRepository
import com.fastingtracker.domain.usecase.validation.PlanValidator
import javax.inject.Inject

/**
 * Use case to create a new fasting plan with validation
 */
class CreatePlanUseCase @Inject constructor(
    private val planRepository: PlanRepository,
    private val validator: PlanValidator
) {
    /**
     * Create a new plan after validation
     * @return Result with plan ID on success or error message on failure
     */
    suspend operator fun invoke(plan: Plan): Result<Long> {
        // Validate plan name
        if (!validator.isValidPlanName(plan.name)) {
            return Result.failure(IllegalArgumentException("Plan name must be between 1-50 characters"))
        }

        // Validate fasting hours
        if (!validator.isValidFastingHours(plan.fastingHours)) {
            return Result.failure(IllegalArgumentException("Fasting hours must be between 1-72 hours"))
        }

        // Validate eating hours if provided
        plan.eatingHours?.let { hours ->
            if (!validator.isValidEatingHours(hours)) {
                return Result.failure(IllegalArgumentException("Eating hours must be between 0-24 hours"))
            }
        }

        // Create plan
        return try {
            val planId = planRepository.createPlan(plan)
            Result.success(planId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
