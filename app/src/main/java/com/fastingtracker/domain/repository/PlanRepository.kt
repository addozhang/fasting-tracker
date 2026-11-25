package com.fastingtracker.domain.repository

import com.fastingtracker.domain.model.Plan
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Plan data operations
 */
interface PlanRepository {
    /**
     * Observe all plans ordered by creation date
     */
    fun observeAllPlans(): Flow<List<Plan>>

    /**
     * Get a plan by ID
     */
    suspend fun getPlanById(id: Long): Plan?

    suspend fun getAllPlans(): List<Plan>

    /**
     * Create a new plan
     * @return ID of created plan
     */
    suspend fun createPlan(plan: Plan): Long

    /**
     * Update an existing plan
     */
    suspend fun updatePlan(plan: Plan)

    /**
     * Delete a plan
     */
    suspend fun deletePlan(plan: Plan)
}
