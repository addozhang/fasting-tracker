package com.fastingtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.time.Instant

@Entity(tableName = "reminder_settings")
@TypeConverters(Converters::class)
data class ReminderSettingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val type: String,
    val intervalMinutes: Int,
    val isEnabled: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
