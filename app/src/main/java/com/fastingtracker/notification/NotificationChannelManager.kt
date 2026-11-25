package com.fastingtracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.fastingtracker.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

const val CHANNEL_ONGOING = "fasting_ongoing_live"
const val CHANNEL_MILESTONE = "fasting_milestone"
const val CHANNEL_REMINDER = "fasting_reminder"

@Singleton
class NotificationChannelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // IMPORTANCE_DEFAULT (silent) is required for Android 16 Live Updates / Super Island promotion
            val ongoingChannel = NotificationChannel(
                CHANNEL_ONGOING,
                context.getString(R.string.channel_ongoing_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_ongoing_desc)
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }

            val milestoneChannel = NotificationChannel(
                CHANNEL_MILESTONE,
                context.getString(R.string.channel_milestone_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_milestone_desc)
                enableVibration(true)
                setShowBadge(true)
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDER,
                context.getString(R.string.channel_reminder_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_reminder_desc)
                enableVibration(true)
                setShowBadge(true)
            }

            nm.createNotificationChannels(listOf(ongoingChannel, milestoneChannel, reminderChannel))
        }
    }
}
