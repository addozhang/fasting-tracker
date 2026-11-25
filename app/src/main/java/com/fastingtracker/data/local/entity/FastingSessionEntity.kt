package com.fastingtracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.TypeConverters
import com.fastingtracker.domain.model.FastingPhase
import java.time.Instant

@Entity(
    tableName = "fasting_sessions",
    indices = [Index(value = ["startTime", "planId"])],
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@TypeConverters(Converters::class)
data class FastingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val startTime: Instant,
    val endTime: Instant?,
    val planId: Long,
    val initialPhase: FastingPhase,
    val mood: Int?,
    val hunger: Int?,
    val waterMl: Int?,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
