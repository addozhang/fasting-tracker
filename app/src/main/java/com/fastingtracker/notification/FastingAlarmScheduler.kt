package com.fastingtracker.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules and cancels all alarms (goal completion + metabolic milestones) for a fasting session.
 * Uses SCHEDULE_EXACT_ALARM when permitted and gracefully falls back to inexact alarms otherwise.
 */
@Singleton
class FastingAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "FastingAlarmScheduler"
        private const val REQUEST_CODE_END_BASE = 5000
        private const val REQUEST_CODE_MILESTONE_BASE = 6000
    }

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedules the goal completion alarm and all future metabolic milestone alarms for [sessionId].
     */
    fun scheduleSessionAlarms(
        sessionId: Long,
        startTime: Instant,
        fastingHours: Int,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val startMillis = startTime.toEpochMilli()

        // 1. Goal completion alarm
        val endTrigger = startMillis + fastingHours * 3600_000L
        if (endTrigger > nowMillis) {
            scheduleExactOrFallback(
                triggerAtMillis = endTrigger,
                pendingIntent = buildEndPendingIntent(sessionId, fastingHours)
            )
        }

        // 2. Metabolic milestone alarms (12h, 16h, 18h, 24h, 48h, 72h)
        FastingMilestone.entries.forEach { milestone ->
            val milestoneTrigger = startMillis + milestone.hours * 3600_000L
            if (milestoneTrigger > nowMillis) {
                scheduleExactOrFallback(
                    triggerAtMillis = milestoneTrigger,
                    pendingIntent = buildMilestonePendingIntent(sessionId, milestone.hours)
                )
            }
        }
    }

    /**
     * Cancels both the goal completion alarm and all milestone alarms for [sessionId].
     */
    fun cancelAllAlarmsForSession(sessionId: Long) {
        alarmManager.cancel(buildEndPendingIntent(sessionId, 0))
        FastingMilestone.entries.forEach { milestone ->
            alarmManager.cancel(buildMilestonePendingIntent(sessionId, milestone.hours))
        }
        Log.d(TAG, "Cancelled all alarms for session $sessionId")
    }

    private fun scheduleExactOrFallback(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm denied, falling back to inexact", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun buildEndPendingIntent(sessionId: Long, fastingHours: Int): PendingIntent {
        val intent = Intent(context, FastingAlarmReceiver::class.java).apply {
            action = FastingAlarmReceiver.ACTION_GOAL_COMPLETE
            putExtra(FastingAlarmReceiver.EXTRA_SESSION_ID, sessionId)
            putExtra(FastingAlarmReceiver.EXTRA_FASTING_HOURS, fastingHours)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_END_BASE + (sessionId.toInt() and 0x3FF),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildMilestonePendingIntent(sessionId: Long, milestoneHours: Int): PendingIntent {
        val intent = Intent(context, FastingAlarmReceiver::class.java).apply {
            action = FastingAlarmReceiver.ACTION_MILESTONE
            putExtra(FastingAlarmReceiver.EXTRA_SESSION_ID, sessionId)
            putExtra(FastingAlarmReceiver.EXTRA_MILESTONE_HOURS, milestoneHours)
        }
        val requestCode = REQUEST_CODE_MILESTONE_BASE + ((sessionId.toInt() and 0xFF) * 100) + milestoneHours
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
