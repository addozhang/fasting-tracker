package com.fastingtracker.domain.usecase.plan

import com.fastingtracker.domain.repository.FastingSessionRepository
import com.fastingtracker.domain.repository.PlanRepository
import javax.inject.Inject

/**
 * Use case to delete a custom fasting plan safely.
 * Reassigns any past sessions referencing this plan to a default fallback plan
 * before deleting, preventing SQLite ForeignKey.CASCADE from wiping user history.
 */
class DeletePlanUseCase @Inject constructor(
    private val planRepository: PlanRepository,
    private val sessionRepository: FastingSessionRepository
) {
    suspend operator fun invoke(planId: Long): Result<Unit> {
        val plan = planRepository.getPlanById(planId)
            ?: return Result.failure(IllegalArgumentException("Plan not found"))

        if (plan.isDefault) {
            return Result.failure(IllegalArgumentException("Cannot delete default plans"))
        }

        val activeSession = sessionRepository.getActiveSession()
        if (activeSession?.planId == planId) {
            return Result.failure(IllegalStateException("Cannot delete plan with active fasting session"))
        }

        val allPlans = planRepository.getAllPlans()
        val fallbackPlanId = allPlans.firstOrNull { it.isDefault && it.id != planId }?.id
            ?: allPlans.firstOrNull { it.id != planId }?.id
            ?: return Result.failure(IllegalStateException("No fallback plan available"))

        return try {
            // Rebind any historical sessions so ForeignKey.CASCADE never deletes history
            sessionRepository.reassignPlan(fromPlanId = planId, toPlanId = fallbackPlanId)
            planRepository.deletePlan(plan)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
