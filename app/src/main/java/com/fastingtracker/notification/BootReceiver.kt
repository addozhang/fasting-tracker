package com.fastingtracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.fastingtracker.domain.usecase.statistics.GetCurrentFastingProgressUseCase
import com.fastingtracker.widget.FastingWidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Restores ongoing Live Update notification, reschedules all alarms,
 * re-enqueues the 15-min refresh worker, and updates Glance widgets after reboot.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var getCurrentFastingProgress: GetCurrentFastingProgressUseCase
    @Inject lateinit var alarmScheduler: FastingAlarmScheduler
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var widgetUpdater: FastingWidgetUpdater

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val progress = getCurrentFastingProgress()
                if (progress != null) {
                    notificationHelper.showOngoingLiveUpdate(progress)
                    alarmScheduler.scheduleSessionAlarms(
                        sessionId = progress.sessionId,
                        startTime = progress.startTime,
                        fastingHours = progress.fastingHours
                    )
                    LiveUpdateRefreshWorker.enqueue(context)
                    Log.d(TAG, "Restored active fast ${progress.sessionId} after reboot")
                }
                widgetUpdater.updateAll()
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring fasting session after boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
