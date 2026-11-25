package com.fastingtracker.domain.usecase.statistics

/**
 * Data class representing overall fasting statistics
 */
data class Statistics(
    val totalSessions: Int,
    val totalFastingTimeHours: Double,
    val averageFastingTimeHours: Double,
    val longestFastingTimeHours: Double,
    val averageMood: Double?,
    val averageHunger: Double?,
    val currentStreak: Int = 0,
    val achievements: List<Achievement> = emptyList()
)
