package com.fastingtracker.data.repository

import com.fastingtracker.data.local.dao.NotificationSettingDao
import com.fastingtracker.domain.model.NotificationType
import com.fastingtracker.domain.model.NotificationSetting
import com.fastingtracker.domain.model.toDomain
import com.fastingtracker.domain.model.toEntity
import com.fastingtracker.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationSettingDao: NotificationSettingDao
) : NotificationRepository {

    override fun observeAllSettings(): Flow<List<NotificationSetting>> {
        return notificationSettingDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAllSettings(): List<NotificationSetting> {
        return notificationSettingDao.getAll().map { it.toDomain() }
    }

    override suspend fun getSettingByType(type: NotificationType): NotificationSetting? {
        return notificationSettingDao.getByType(type)?.toDomain()
    }

    override suspend fun upsertSetting(setting: NotificationSetting) {
        notificationSettingDao.upsert(setting.toEntity())
    }
}
