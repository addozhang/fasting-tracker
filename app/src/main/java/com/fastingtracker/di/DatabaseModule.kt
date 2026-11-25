package com.fastingtracker.di

import android.content.Context
import com.fastingtracker.data.local.AppDatabase
import com.fastingtracker.data.local.dao.FastingSessionDao
import com.fastingtracker.data.local.dao.NotificationSettingDao
import com.fastingtracker.data.local.dao.PlanDao
import com.fastingtracker.data.local.dao.ReminderSettingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun providePlanDao(database: AppDatabase): PlanDao {
        return database.planDao()
    }

    @Provides
    @Singleton
    fun provideFastingSessionDao(database: AppDatabase): FastingSessionDao {
        return database.fastingSessionDao()
    }

    @Provides
    @Singleton
    fun provideNotificationSettingDao(database: AppDatabase): NotificationSettingDao {
        return database.notificationSettingDao()
    }

    @Provides
    @Singleton
    fun provideReminderSettingDao(database: AppDatabase): ReminderSettingDao {
        return database.reminderSettingDao()
    }
}
