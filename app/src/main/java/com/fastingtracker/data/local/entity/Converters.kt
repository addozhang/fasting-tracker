package com.fastingtracker.data.local.entity

import androidx.room.TypeConverter
import com.fastingtracker.domain.model.FastingPhase
import com.fastingtracker.domain.model.NotificationType
import com.fastingtracker.domain.model.PlanType
import java.time.Instant

class Converters {

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun toInstant(millis: Long?): Instant? = millis?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun fromPlanType(type: PlanType?): String? = type?.name

    @TypeConverter
    fun toPlanType(value: String?): PlanType? = value?.let { PlanType.valueOf(it) }

    @TypeConverter
    fun fromFastingPhase(phase: FastingPhase?): String? = phase?.name

    @TypeConverter
    fun toFastingPhase(value: String?): FastingPhase? = value?.let { FastingPhase.valueOf(it) }

    @TypeConverter
    fun fromNotificationType(type: NotificationType?): String? = type?.name

    @TypeConverter
    fun toNotificationType(value: String?): NotificationType? = value?.let { NotificationType.valueOf(it) }
}
