package com.fastingtracker.domain.util

import com.fastingtracker.domain.model.FastingPhase
import com.fastingtracker.domain.model.FastingSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class StreakCalculatorTest {

    private val zone: ZoneId = ZoneId.systemDefault()

    private fun sessionEndingOn(date: LocalDate, completed: Boolean = true): FastingSession {
        val start = date.atStartOfDay(zone).toInstant()
        return FastingSession(
            id = null,
            startTime = start,
            endTime = if (completed) start.plusSeconds(16 * 3600) else null,
            planId = 1L,
            initialPhase = FastingPhase.FASTING,
            mood = null,
            hunger = null,
            waterMl = null,
            notes = null,
            createdAt = start,
            updatedAt = start
        )
    }

    @Test
    fun `empty list returns 0`() {
        assertEquals(0, StreakCalculator.calculate(emptyList()))
    }

    @Test
    fun `only incomplete sessions return 0`() {
        val today = LocalDate.now()
        val sessions = listOf(sessionEndingOn(today, completed = false))
        assertEquals(0, StreakCalculator.calculate(sessions))
    }

    @Test
    fun `single completed session today returns 1`() {
        val sessions = listOf(sessionEndingOn(LocalDate.now()))
        assertEquals(1, StreakCalculator.calculate(sessions))
    }

    @Test
    fun `single completed session yesterday returns 1 (grace period)`() {
        val sessions = listOf(sessionEndingOn(LocalDate.now().minusDays(1)))
        assertEquals(1, StreakCalculator.calculate(sessions))
    }

    @Test
    fun `last fast 2 days ago returns 0 (broken streak)`() {
        val sessions = listOf(sessionEndingOn(LocalDate.now().minusDays(2)))
        assertEquals(0, StreakCalculator.calculate(sessions))
    }

    @Test
    fun `three consecutive days ending today returns 3`() {
        val today = LocalDate.now()
        val sessions = listOf(
            sessionEndingOn(today),
            sessionEndingOn(today.minusDays(1)),
            sessionEndingOn(today.minusDays(2))
        )
        assertEquals(3, StreakCalculator.calculate(sessions))
    }

    @Test
    fun `streak breaks at gap`() {
        val today = LocalDate.now()
        val sessions = listOf(
            sessionEndingOn(today),
            sessionEndingOn(today.minusDays(1)),
            sessionEndingOn(today.minusDays(3)),
            sessionEndingOn(today.minusDays(4))
        )
        assertEquals(2, StreakCalculator.calculate(sessions))
    }

    @Test
    fun `multiple sessions same day count as one`() {
        val today = LocalDate.now()
        val sessions = listOf(
            sessionEndingOn(today),
            sessionEndingOn(today),
            sessionEndingOn(today.minusDays(1))
        )
        assertEquals(2, StreakCalculator.calculate(sessions))
    }

    @Test
    fun `unsorted input still produces correct streak`() {
        val today = LocalDate.now()
        val sessions = listOf(
            sessionEndingOn(today.minusDays(2)),
            sessionEndingOn(today),
            sessionEndingOn(today.minusDays(1))
        )
        assertEquals(3, StreakCalculator.calculate(sessions))
    }

    @Test
    fun `incomplete sessions are filtered out`() {
        val today = LocalDate.now()
        val sessions = listOf(
            sessionEndingOn(today),
            sessionEndingOn(today.minusDays(1), completed = false),
            sessionEndingOn(today.minusDays(1))
        )
        assertEquals(2, StreakCalculator.calculate(sessions))
    }
}
