package com.fastingtracker.domain.usecase.statistics

/**
 * Represents a fasting achievement badge.
 * @param id Stable identifier used as a key in UI lists.
 * @param emoji Visual representation.
 * @param labelKey Key mapping to a localized label string.
 * @param unlocked Whether the user has earned this badge.
 */
data class Achievement(
    val id: String,
    val emoji: String,
    val labelKey: AchievementKey,
    val unlocked: Boolean
)

enum class AchievementKey {
    FIRST_FAST,
    CLUB_16H,
    CLUB_18H,
    WARRIOR_24H,
    STREAK_7_DAYS
}
