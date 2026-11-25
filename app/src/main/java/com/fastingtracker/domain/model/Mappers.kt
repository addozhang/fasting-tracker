package com.fastingtracker.domain.model

import com.fastingtracker.data.local.entity.FastingSessionEntity
import com.fastingtracker.data.local.entity.NotificationSettingEntity
import com.fastingtracker.data.local.entity.PlanEntity
import com.fastingtracker.data.local.entity.ReminderSettingEntity

/**
 * Extension functions to map between Entity and Domain models
 */

// Plan mappings
fun PlanEntity.toDomain() = Plan(
    id = id,
    name = name,
    planType = planType,
    fastingHours = fastingHours,
    eatingHours = eatingHours,
    isDefault = isDefault,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Plan.toEntity() = PlanEntity(
    id = id,
    name = name,
    planType = planType,
    fastingHours = fastingHours,
    eatingHours = eatingHours,
    isDefault = isDefault,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// FastingSession mappings
fun FastingSessionEntity.toDomain() = FastingSession(
    id = id,
    startTime = startTime,
    endTime = endTime,
    planId = planId,
    initialPhase = initialPhase,
    mood = mood,
    hunger = hunger,
    waterMl = waterMl,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun FastingSession.toEntity() = FastingSessionEntity(
    id = id,
    startTime = startTime,
    endTime = endTime,
    planId = planId,
    initialPhase = initialPhase,
    mood = mood,
    hunger = hunger,
    waterMl = waterMl,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// NotificationSetting mappings
fun NotificationSettingEntity.toDomain() = NotificationSetting(
    id = id,
    type = type,
    title = title,
    message = message,
    advanceMinutes = advanceMinutes,
    isEnabled = isEnabled,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun NotificationSetting.toEntity() = NotificationSettingEntity(
    id = id,
    type = type,
    title = title,
    message = message,
    advanceMinutes = advanceMinutes,
    isEnabled = isEnabled,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// ReminderSetting mappings
fun ReminderSettingEntity.toDomain() = ReminderSetting(
    id = id,
    type = type,
    intervalMinutes = intervalMinutes,
    isEnabled = isEnabled,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ReminderSetting.toEntity() = ReminderSettingEntity(
    id = id,
    type = type,
    intervalMinutes = intervalMinutes,
    isEnabled = isEnabled,
    createdAt = createdAt,
    updatedAt = updatedAt
)
