package com.fastingtracker.di

import com.fastingtracker.data.repository.FastingSessionRepositoryImpl
import com.fastingtracker.data.repository.NotificationRepositoryImpl
import com.fastingtracker.data.repository.PlanRepositoryImpl
import com.fastingtracker.data.repository.ReminderRepositoryImpl
import com.fastingtracker.data.repository.SettingsRepositoryImpl
import com.fastingtracker.domain.repository.FastingSessionRepository
import com.fastingtracker.domain.repository.NotificationRepository
import com.fastingtracker.domain.repository.PlanRepository
import com.fastingtracker.domain.repository.ReminderRepository
import com.fastingtracker.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPlanRepository(
        impl: PlanRepositoryImpl
    ): PlanRepository

    @Binds
    @Singleton
    abstract fun bindFastingSessionRepository(
        impl: FastingSessionRepositoryImpl
    ): FastingSessionRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: NotificationRepositoryImpl
    ): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindReminderRepository(
        impl: ReminderRepositoryImpl
    ): ReminderRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository
}
