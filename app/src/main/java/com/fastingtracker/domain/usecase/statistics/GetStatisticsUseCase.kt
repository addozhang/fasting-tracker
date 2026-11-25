package com.fastingtracker.domain.usecase.statistics

import com.fastingtracker.domain.model.FastingSession
import com.fastingtracker.domain.model.Plan
import com.fastingtracker.domain.repository.FastingSessionRepository
import com.fastingtracker.domain.repository.PlanRepository
import com.fastingtracker.domain.util.StreakCalculator
import java.time.Duration
import javax.inject.Inject

/**
 * Use case to calculate overall fasting statistics (single-fast model).
 */
class GetStatisticsUseCase @Inject constructor(
    private val sessionRepository: FastingSessionRepository,
    private val planRepository: PlanRepository
) {
    suspend operator fun invoke(): Statistics {
        val allSessions = sessionRepository.getAllSessions()
        val plans = planRepository.getAllPlans()
        return calculate(allSessions, plans)
    }

    fun calculate(
        allSessions: List<FastingSession>,
        @Suppress("UNUSED_PARAMETER") plans: List<Plan> = emptyList()
    ): Statistics {
        val completedSessions = allSessions.filter { it.endTime != null }

        if (completedSessions.isEmpty()) {
            return Statistics(
                totalSessions = 0,
                totalFastingTimeHours = 0.0,
                averageFastingTimeHours = 0.0,
                longestFastingTimeHours = 0.0,
                averageMood = null,
                averageHunger = null,
                currentStreak = 0,
                achievements = calculateAchievements(emptyList(), 0, 0)
            )
        }

        val durationsMinutes = completedSessions.map { session ->
            Duration.between(session.startTime, session.endTime!!).toMinutes().coerceAtLeast(0L)
        }
        val totalMinutes = durationsMinutes.sum()
        val avgMinutes = totalMinutes.toDouble() / completedSessions.size
        val longestMinutes = durationsMinutes.maxOrNull() ?: 0L

        val moods = completedSessions.mapNotNull { it.mood }
        val hungers = completedSessions.mapNotNull { it.hunger }
        val streak = StreakCalculator.calculate(completedSessions)

        return Statistics(
            totalSessions = completedSessions.size,
            totalFastingTimeHours = totalMinutes / 60.0,
            averageFastingTimeHours = avgMinutes / 60.0,
            longestFastingTimeHours = longestMinutes / 60.0,
            averageMood = if (moods.isNotEmpty()) moods.average() else null,
            averageHunger = if (hungers.isNotEmpty()) hungers.average() else null,
            currentStreak = streak,
            achievements = calculateAchievements(completedSessions, completedSessions.size, streak)
        )
    }

    private fun calculateAchievements(
        completedSessions: List<FastingSession>,
        totalCount: Int,
        streak: Int
    ): List<Achievement> {
        val longestHours = completedSessions.mapNotNull { s ->
            s.endTime?.let { Duration.between(s.startTime, it).toMinutes() / 60.0 }
        }.maxOrNull() ?: 0.0

        return listOf(
            Achievement("first_fast", "🥇", AchievementKey.FIRST_FAST, totalCount >= 1),
            Achievement("club_16h", "🔥", AchievementKey.CLUB_16H, longestHours >= 16.0),
            Achievement("club_18h", "⚡", AchievementKey.CLUB_18H, longestHours >= 18.0),
            Achievement("warrior_24h", "🏆", AchievementKey.WARRIOR_24H, longestHours >= 24.0),
            Achievement("streak_7d", "💎", AchievementKey.STREAK_7_DAYS, streak >= 7)
        )
    }
}
