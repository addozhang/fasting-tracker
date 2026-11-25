package com.fastingtracker.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fastingtracker.domain.coordinator.FastingSessionCoordinator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Lightweight 15-minute WorkManager job that keeps the Android 16 Live Update
 * Super Island pill ("13.5h" fasting or "3.5h" eating), ProgressStyle bar,
 * and Glance widgets fresh, and auto-dismisses the eating window island on expiry.
 */
@HiltWorker
class LiveUpdateRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val coordinator: FastingSessionCoordinator
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        coordinator.syncActiveSurfaces(rescheduleAlarms = false)
        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "fasting_live_update_refresh"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<LiveUpdateRefreshWorker>(
                15, TimeUnit.MINUTES
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
