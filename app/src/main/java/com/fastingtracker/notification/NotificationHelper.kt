package com.fastingtracker.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.fastingtracker.MainActivity
import com.fastingtracker.R
import com.fastingtracker.domain.model.FastingSubPhase
import com.fastingtracker.domain.model.NotificationType
import com.fastingtracker.domain.repository.NotificationRepository
import com.fastingtracker.domain.repository.SettingsRepository
import com.fastingtracker.domain.usecase.statistics.FastingProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationRepository: NotificationRepository,
    private val settingsRepository: SettingsRepository
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val ACTION_END_FAST = "com.fastingtracker.ACTION_END_FAST"
        const val NOTIFICATION_ID_ONGOING = 1001
        const val NOTIFICATION_ID_MILESTONE = 2001
        const val NOTIFICATION_ID_COMPLETED = 2002

        /**
         * Compact elapsed text optimized for Android 16 / HyperOS 4 Super Island pill width:
         * - Under 1 hour: "45m"
         * - 1h or more: "13.5h" (natural progression with 15-min background refresh)
         */
        fun formatCompactElapsed(elapsedSeconds: Long): String {
            val clamped = elapsedSeconds.coerceAtLeast(0L)
            return if (clamped < 3600L) {
                "${clamped / 60L}m"
            } else {
                val hours = clamped / 3600.0
                String.format(Locale.US, "%.1fh", hours)
            }
        }
    }

    private fun hasPostPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private suspend fun isTypeEnabled(type: NotificationType): Boolean {
        val setting = notificationRepository.getSettingByType(type)
        return setting?.isEnabled ?: true
    }

    fun canPostPromotedLiveUpdates(): Boolean {
        return if (Build.VERSION.SDK_INT >= 36) {
            notificationManager.canPostPromotedNotifications()
        } else false
    }

    /**
     * Shows or updates the ongoing fasting notification.
     * Uses Android 16+ Live Updates (ProgressStyle + compact ShortCriticalText + Chronometer)
     * which natively projects onto HyperOS 4 Super Island, and gracefully falls back to
     * a standard Chronometer ongoing notification on Android 8–15.
     */
    suspend fun showOngoingLiveUpdate(progress: FastingProgress) {
        if (!hasPostPermission()) return
        if (!isTypeEnabled(NotificationType.FASTING_START)) {
            cancelOngoingNotification()
            return
        }

        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val zone = ZoneId.systemDefault()
        val startStr = progress.startTime.atZone(zone).format(timeFormatter)
        val targetStr = progress.expectedEndTime.atZone(zone).format(timeFormatter)

        val phaseLabel = subPhaseLabel(progress.phaseMilestone)
        val title = if (progress.isGoalReached) {
            context.getString(R.string.notif_ongoing_goal_reached, progress.planName)
        } else {
            context.getString(R.string.notif_ongoing_title_format, progress.planName, phaseLabel)
        }
        val body = context.getString(R.string.notif_ongoing_body_format, startStr, targetStr)

        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val endFastIntent = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java).apply {
                action = ACTION_END_FAST
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val superIslandEnabled = settingsRepository.isSuperIslandEnabled()
        val lockScreenVisible = settingsRepository.isLockScreenVisible()
        val minimalStyle = settingsRepository.isMinimalNotification()
        val compactElapsed = formatCompactElapsed(progress.elapsedTimeSeconds)
        val effectiveTitle = if (minimalStyle) {
            if (progress.isGoalReached) {
                "${context.getString(R.string.notif_complete_title)} · $compactElapsed"
            } else {
                "$phaseLabel · $compactElapsed"
            }
        } else title
        val effectiveBody = if (minimalStyle) null else body

        val iconRes = phaseIconRes(progress)
        val notification = if (Build.VERSION.SDK_INT >= 36) {
            buildAndroid16LiveUpdate(
                progress = progress,
                iconRes = iconRes,
                title = effectiveTitle,
                body = effectiveBody,
                openIntent = openIntent,
                endFastIntent = endFastIntent,
                promoteToSuperIsland = superIslandEnabled,
                lockScreenVisible = lockScreenVisible,
                minimalStyle = minimalStyle
            )
        } else {
            val compatBuilder = NotificationCompat.Builder(context, CHANNEL_ONGOING)
                .setSmallIcon(iconRes)
                .setContentTitle(effectiveTitle)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setVisibility(
                    if (lockScreenVisible) NotificationCompat.VISIBILITY_PUBLIC
                    else NotificationCompat.VISIBILITY_SECRET
                )
                .setWhen(progress.startTime.toEpochMilli())
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setContentIntent(openIntent)
            if (effectiveBody != null) compatBuilder.setContentText(effectiveBody)
            if (!minimalStyle) {
                compatBuilder.addAction(
                    R.drawable.ic_notification_complete,
                    context.getString(R.string.end_fast),
                    endFastIntent
                )
            }
            compatBuilder.build()
        }

        notificationManager.notify(NOTIFICATION_ID_ONGOING, notification)
    }

    private fun phaseIconRes(progress: FastingProgress): Int {
        if (progress.isGoalReached) return R.drawable.ic_phase_goal_reached
        return when (progress.phaseMilestone) {
            FastingSubPhase.FAT_BURNING -> R.drawable.ic_phase_fat_burning
            FastingSubPhase.KETOSIS,
            FastingSubPhase.DEEP_KETOSIS -> R.drawable.ic_phase_ketosis
            FastingSubPhase.AUTOPHAGY,
            FastingSubPhase.EXTENDED_FASTING,
            FastingSubPhase.DEEP_AUTOPHAGY -> R.drawable.ic_phase_autophagy
            else -> R.drawable.ic_phase_settling
        }
    }

    private fun buildAndroid16LiveUpdate(
        progress: FastingProgress,
        iconRes: Int,
        title: String,
        body: String?,
        openIntent: PendingIntent,
        endFastIntent: PendingIntent,
        promoteToSuperIsland: Boolean,
        lockScreenVisible: Boolean,
        minimalStyle: Boolean
    ): Notification {
        val totalHours = progress.fastingHours.coerceAtLeast(1)
        val firstSeg = minOf(12, totalHours)
        val secondSeg = (totalHours - firstSeg).coerceAtLeast(0)

        val segments = buildList {
            add(Notification.ProgressStyle.Segment(firstSeg * 10).setColor(Color.parseColor("#E06D3B")))
            if (secondSeg > 0) {
                add(Notification.ProgressStyle.Segment(secondSeg * 10).setColor(Color.parseColor("#F2A65A")))
            }
        }
        val points = buildList {
            if (totalHours > 12) {
                add(Notification.ProgressStyle.Point(120).setColor(Color.parseColor("#C88A2B")))
            }
            if (totalHours > 16) {
                add(Notification.ProgressStyle.Point(160).setColor(Color.parseColor("#C88A2B")))
            }
            if (totalHours > 24) {
                add(Notification.ProgressStyle.Point(240).setColor(Color.parseColor("#C88A2B")))
            }
        }

        val currentProgressUnits = ((progress.elapsedTimeSeconds.toDouble() / 3600.0) * 10.0)
            .toInt()
            .coerceIn(0, totalHours * 10)

        val progressStyle = Notification.ProgressStyle()
            .setProgress(currentProgressUnits)
            .setProgressSegments(segments)
            .setProgressTrackerIcon(Icon.createWithResource(context, iconRes))
            .apply {
                if (points.isNotEmpty()) setProgressPoints(points)
            }

        val builder = Notification.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(
                if (lockScreenVisible) Notification.VISIBILITY_PUBLIC
                else Notification.VISIBILITY_SECRET
            )
            .setStyle(progressStyle)
            .setContentIntent(openIntent)

        if (body != null) {
            builder.setContentText(body)
        }
        if (!minimalStyle) {
            val endAction = Notification.Action.Builder(
                Icon.createWithResource(context, R.drawable.ic_notification_complete),
                context.getString(R.string.end_fast),
                endFastIntent
            ).build()
            builder.addAction(endAction)
        }
        if (promoteToSuperIsland) {
            builder
                .setShortCriticalText(formatCompactElapsed(progress.elapsedTimeSeconds))
                .addExtras(Bundle().apply { putBoolean("android.requestPromotedOngoing", true) })
        }

        return builder.build()
    }

    fun cancelOngoingNotification() {
        notificationManager.cancel(NOTIFICATION_ID_ONGOING)
    }

    /**
     * Shows or updates the Eating Window Live Update / Super Island notification
     * (sage green ProgressStyle + utensils icon) while within the natural eating window.
     */
    suspend fun showEatingWindowLiveUpdate(
        planName: String,
        lastEndedTime: java.time.Instant,
        eatingHours: Int,
        now: java.time.Instant = java.time.Instant.now()
    ) {
        if (!hasPostPermission()) return
        if (!isTypeEnabled(NotificationType.FASTING_START)) {
            cancelOngoingNotification()
            return
        }

        val elapsedSeconds = java.time.Duration.between(lastEndedTime, now).seconds.coerceAtLeast(0L)
        val zone = ZoneId.systemDefault()
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val endedStr = lastEndedTime.atZone(zone).format(timeFormatter)
        val nextFastStr = lastEndedTime.plusSeconds(eatingHours * 3600L).atZone(zone).format(timeFormatter)

        val title = context.getString(R.string.notif_eating_title_format, planName)
        val body = context.getString(R.string.notif_eating_body_format, endedStr, nextFastStr)

        val openIntent = PendingIntent.getActivity(
            context,
            2,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val superIslandEnabled = settingsRepository.isSuperIslandEnabled()
        val lockScreenVisible = settingsRepository.isLockScreenVisible()
        val minimalStyle = settingsRepository.isMinimalNotification()
        val compactElapsed = formatCompactElapsed(elapsedSeconds)
        val effectiveTitle = if (minimalStyle) {
            "${context.getString(R.string.eating_window_label)} · $compactElapsed"
        } else title
        val effectiveBody = if (minimalStyle) null else body

        val iconRes = R.drawable.ic_phase_eating
        val notification = if (Build.VERSION.SDK_INT >= 36) {
            val totalUnits = (eatingHours.coerceAtLeast(1)) * 10
            val currentUnits = ((elapsedSeconds.toDouble() / 3600.0) * 10.0)
                .toInt()
                .coerceIn(0, totalUnits)
            val progressStyle = Notification.ProgressStyle()
                .setProgress(currentUnits)
                .setProgressSegments(
                    listOf(
                        Notification.ProgressStyle.Segment(totalUnits).setColor(Color.parseColor("#4F826A"))
                    )
                )
                .setProgressTrackerIcon(Icon.createWithResource(context, iconRes))

            val builder = Notification.Builder(context, CHANNEL_ONGOING)
                .setSmallIcon(iconRes)
                .setContentTitle(effectiveTitle)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(
                    if (lockScreenVisible) Notification.VISIBILITY_PUBLIC
                    else Notification.VISIBILITY_SECRET
                )
                .setStyle(progressStyle)
                .setContentIntent(openIntent)

            if (effectiveBody != null) builder.setContentText(effectiveBody)
            if (!minimalStyle) {
                val startAction = Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_phase_settling),
                    context.getString(R.string.start_fasting),
                    openIntent
                ).build()
                builder.addAction(startAction)
            }
            if (superIslandEnabled) {
                builder
                    .setShortCriticalText(compactElapsed)
                    .addExtras(Bundle().apply { putBoolean("android.requestPromotedOngoing", true) })
            }
            builder.build()
        } else {
            val compatBuilder = NotificationCompat.Builder(context, CHANNEL_ONGOING)
                .setSmallIcon(iconRes)
                .setContentTitle(effectiveTitle)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setVisibility(
                    if (lockScreenVisible) NotificationCompat.VISIBILITY_PUBLIC
                    else NotificationCompat.VISIBILITY_SECRET
                )
                .setWhen(lastEndedTime.toEpochMilli())
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setContentIntent(openIntent)
            if (effectiveBody != null) compatBuilder.setContentText(effectiveBody)
            if (!minimalStyle) {
                compatBuilder.addAction(
                    R.drawable.ic_phase_settling,
                    context.getString(R.string.start_fasting),
                    openIntent
                )
            }
            compatBuilder.build()
        }

        notificationManager.notify(NOTIFICATION_ID_ONGOING, notification)
    }

    suspend fun showMilestoneNotification(milestone: FastingMilestone) {
        if (!hasPostPermission()) return
        if (!isTypeEnabled(NotificationType.PHASE_MILESTONE)) return

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.notif_milestone_title_format, milestoneLabel(milestone))
        val desc = milestoneDescription(milestone)

        val notification = NotificationCompat.Builder(context, CHANNEL_MILESTONE)
            .setSmallIcon(R.drawable.ic_notification_milestone)
            .setContentTitle(title)
            .setContentText(desc)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_MILESTONE + milestone.hours, notification)
    }

    suspend fun showFastingCompletedNotification(durationHours: Int) {
        if (!hasPostPermission()) return
        if (!isTypeEnabled(NotificationType.FASTING_END)) return

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notification_complete)
            .setContentTitle(context.getString(R.string.notif_complete_title))
            .setContentText(context.getString(R.string.notif_complete_body, durationHours))
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_COMPLETED, notification)
    }

    private fun subPhaseLabel(subPhase: FastingSubPhase?): String = when (subPhase) {
        FastingSubPhase.FAT_BURNING -> context.getString(R.string.phase_fat_burning)
        FastingSubPhase.KETOSIS -> context.getString(R.string.phase_ketosis)
        FastingSubPhase.DEEP_KETOSIS -> context.getString(R.string.phase_deep_ketosis)
        FastingSubPhase.AUTOPHAGY -> context.getString(R.string.phase_autophagy)
        FastingSubPhase.EXTENDED_FASTING -> context.getString(R.string.phase_deep_autophagy)
        FastingSubPhase.DEEP_AUTOPHAGY -> context.getString(R.string.phase_extended_reset)
        else -> context.getString(R.string.phase_settling)
    }

    private fun milestoneLabel(milestone: FastingMilestone): String = when (milestone) {
        FastingMilestone.FAT_BURNING -> context.getString(R.string.phase_fat_burning)
        FastingMilestone.KETOSIS -> context.getString(R.string.phase_ketosis)
        FastingMilestone.DEEP_KETOSIS -> context.getString(R.string.phase_deep_ketosis)
        FastingMilestone.AUTOPHAGY -> context.getString(R.string.phase_autophagy)
        FastingMilestone.EXTENDED_48H -> context.getString(R.string.phase_deep_autophagy)
        FastingMilestone.EXTENDED_72H -> context.getString(R.string.phase_extended_reset)
    }

    private fun milestoneDescription(milestone: FastingMilestone): String = when (milestone) {
        FastingMilestone.FAT_BURNING -> context.getString(R.string.milestone_12h_desc)
        FastingMilestone.KETOSIS -> context.getString(R.string.milestone_16h_desc)
        FastingMilestone.DEEP_KETOSIS -> context.getString(R.string.milestone_18h_desc)
        FastingMilestone.AUTOPHAGY -> context.getString(R.string.milestone_24h_desc)
        FastingMilestone.EXTENDED_48H -> context.getString(R.string.milestone_48h_desc)
        FastingMilestone.EXTENDED_72H -> context.getString(R.string.milestone_72h_desc)
    }
}
