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
 * Handles both goal-completion and metabolic-milestone alarms.
 * Verifies the session is still active, respects user notification toggles,
 * updates the ongoing Live Update notification, and refreshes Glance widgets.
 */
@AndroidEntryPoint
class FastingAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var getCurrentFastingProgress: GetCurrentFastingProgressUseCase
    @Inject lateinit var widgetUpdater: FastingWidgetUpdater

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val progress = getCurrentFastingProgress() ?: return@launch
                val expectedSessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
                if (expectedSessionId > 0L && progress.sessionId != expectedSessionId) {
                    return@launch
                }

                when (intent.action) {
                    ACTION_MILESTONE -> {
                        val hours = intent.getIntExtra(EXTRA_MILESTONE_HOURS, 0)
                        FastingMilestone.fromHours(hours)?.let { milestone ->
                            notificationHelper.showMilestoneNotification(milestone)
                        }
                    }
                    else -> {
                        val durationHours = intent.getIntExtra(EXTRA_FASTING_HOURS, progress.fastingHours)
                        notificationHelper.showFastingCompletedNotification(durationHours)
                    }
                }

                // Refresh Live Update pill/card and Glance home widgets
                notificationHelper.showOngoingLiveUpdate(progress)
                widgetUpdater.updateAll()
            } catch (e: Exception) {
                Log.w(TAG, "Failed handling fasting alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "FastingAlarmReceiver"
        const val ACTION_GOAL_COMPLETE = "com.fastingtracker.ALARM_GOAL_COMPLETE"
        const val ACTION_MILESTONE = "com.fastingtracker.ALARM_MILESTONE"
        const val EXTRA_SESSION_ID = "EXTRA_SESSION_ID"
        const val EXTRA_FASTING_HOURS = "EXTRA_FASTING_HOURS"
        const val EXTRA_MILESTONE_HOURS = "EXTRA_MILESTONE_HOURS"
    }
}
