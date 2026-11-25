package com.fastingtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.fastingtracker.domain.model.NotificationType
import java.time.Instant

@Entity(tableName = "notification_settings")
@TypeConverters(Converters::class)
data class NotificationSettingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val type: NotificationType,
    val title: String,
    val message: String,
    val advanceMinutes: Int,
    val isEnabled: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
