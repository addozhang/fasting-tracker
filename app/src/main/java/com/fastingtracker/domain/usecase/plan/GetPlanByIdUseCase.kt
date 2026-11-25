package com.fastingtracker.domain.usecase.plan

import com.fastingtracker.domain.model.Plan
import com.fastingtracker.domain.repository.PlanRepository
import javax.inject.Inject

/**
 * Use case to retrieve a plan by ID
 */
class GetPlanByIdUseCase @Inject constructor(
    private val planRepository: PlanRepository
) {
    suspend operator fun invoke(planId: Long): Plan? {
        return planRepository.getPlanById(planId)
    }
}
