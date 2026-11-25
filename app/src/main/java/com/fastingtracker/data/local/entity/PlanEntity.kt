package com.fastingtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.fastingtracker.domain.model.PlanType
import java.time.Instant

@Entity(tableName = "plans")
@TypeConverters(Converters::class)
data class PlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val name: String,
    val planType: PlanType,
    val fastingHours: Int,
    val eatingHours: Int?,
    val isDefault: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant
)
