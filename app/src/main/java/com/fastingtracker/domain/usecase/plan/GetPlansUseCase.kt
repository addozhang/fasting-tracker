package com.fastingtracker.domain.usecase.plan

import com.fastingtracker.domain.model.Plan
import com.fastingtracker.domain.repository.PlanRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to retrieve all fasting plans
 */
class GetPlansUseCase @Inject constructor(
    private val planRepository: PlanRepository
) {
    operator fun invoke(): Flow<List<Plan>> {
        return planRepository.observeAllPlans()
    }
}
