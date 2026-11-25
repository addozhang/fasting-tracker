package com.fastingtracker.data.repository

import com.fastingtracker.data.local.dao.PlanDao
import com.fastingtracker.domain.model.Plan
import com.fastingtracker.domain.model.toDomain
import com.fastingtracker.domain.model.toEntity
import com.fastingtracker.domain.repository.PlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanRepositoryImpl @Inject constructor(
    private val planDao: PlanDao
) : PlanRepository {

    override fun observeAllPlans(): Flow<List<Plan>> {
        return planDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getPlanById(id: Long): Plan? {
        return planDao.getById(id)?.toDomain()
    }

    override suspend fun getAllPlans(): List<Plan> {
        return planDao.getAll().map { it.toDomain() }
    }

    override suspend fun createPlan(plan: Plan): Long {
        return planDao.insert(plan.toEntity())
    }

    override suspend fun updatePlan(plan: Plan) {
        planDao.update(plan.toEntity())
    }

    override suspend fun deletePlan(plan: Plan) {
        planDao.delete(plan.toEntity())
    }
}
