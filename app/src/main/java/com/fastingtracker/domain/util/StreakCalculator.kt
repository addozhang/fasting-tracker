package com.fastingtracker.domain.util

import com.fastingtracker.domain.model.FastingSession
import java.time.LocalDate
import java.time.ZoneId

object StreakCalculator {

    fun calculate(
        sessions: List<FastingSession>,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Int {
        val completedDates = sessions
            .asSequence()
            .filter { it.endTime != null }
            .map { it.startTime.atZone(zone).toLocalDate() }
            .distinct()
            .sortedDescending()
            .toList()

        if (completedDates.isEmpty()) return 0

        val mostRecent = completedDates.first()
        val yesterday = today.minusDays(1)
        if (mostRecent != today && mostRecent != yesterday) return 0

        var streak = 0
        var expected = mostRecent
        for (date in completedDates) {
            if (date == expected) {
                streak++
                expected = expected.minusDays(1)
            } else if (date < expected) {
                break
            }
        }
        return streak
    }
}
