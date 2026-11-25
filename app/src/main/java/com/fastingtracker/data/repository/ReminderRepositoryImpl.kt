package com.fastingtracker.data.repository

import com.fastingtracker.data.local.dao.ReminderSettingDao
import com.fastingtracker.domain.model.ReminderSetting
import com.fastingtracker.domain.model.toDomain
import com.fastingtracker.domain.model.toEntity
import com.fastingtracker.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val reminderSettingDao: ReminderSettingDao
) : ReminderRepository {

    override fun observeAllSettings(): Flow<List<ReminderSetting>> {
        return reminderSettingDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getSettingByType(type: String): ReminderSetting? {
        return reminderSettingDao.getByType(type)?.toDomain()
    }

    override suspend fun upsertSetting(setting: ReminderSetting) {
        reminderSettingDao.upsert(setting.toEntity())
    }
}
