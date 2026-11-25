package com.fastingtracker.domain.usecase

import com.fastingtracker.domain.model.FastingPhase
import com.fastingtracker.domain.model.FastingSession
import com.fastingtracker.domain.repository.FastingSessionRepository
import com.fastingtracker.domain.usecase.fasting.EndFastOutcome
import com.fastingtracker.domain.usecase.fasting.EndFastingUseCase
import com.fastingtracker.domain.usecase.fasting.SessionTimeValidator
import com.fastingtracker.domain.usecase.fasting.SessionValidationError
import com.fastingtracker.notification.NotificationHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class V2DomainRulesTest {

    @Test
    fun `ending fast under 1 hour discards session without saving`() = runTest {
        val repo = mockk<FastingSessionRepository>(relaxed = true)
        val now = Instant.parse("2026-11-25T10:00:00Z")
        val shortSession = FastingSession(
            id = 99L,
            startTime = now.minusSeconds(45L * 60),
            endTime = null,
            planId = 1L,
            initialPhase = FastingPhase.FASTING,
            mood = null, hunger = null, waterMl = null, notes = null,
            createdAt = now, updatedAt = now
        )
        coEvery { repo.getActiveSession() } returns shortSession

        val result = EndFastingUseCase(repo)(endTime = now)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() is EndFastOutcome.DiscardedShort)
        coVerify(exactly = 1) { repo.deleteSession(99L) }
        coVerify(exactly = 0) { repo.endFasting(any(), any(), any(), any()) }
    }

    @Test
    fun `ending fast at least 1 hour saves with mood and notes`() = runTest {
        val repo = mockk<FastingSessionRepository>(relaxed = true)
        val now = Instant.parse("2026-11-25T10:00:00Z")
        val validSession = FastingSession(
            id = 100L,
            startTime = now.minusSeconds(16L * 3600),
            endTime = null,
            planId = 1L,
            initialPhase = FastingPhase.FASTING,
            mood = null, hunger = null, waterMl = null, notes = null,
            createdAt = now, updatedAt = now
        )
        coEvery { repo.getActiveSession() } returns validSession

        val result = EndFastingUseCase(repo)(endTime = now, mood = 4, notes = "Felt clear")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() is EndFastOutcome.Saved)
        coVerify(exactly = 1) { repo.endFasting(100L, now, 4, "Felt clear") }
    }

    @Test
    fun `validator enforces 1h to 168h bounds and rejects overlaps`() {
        val now = Instant.parse("2026-11-25T12:00:00Z")
        val existing = FastingSession(
            id = 1L,
            startTime = now.minusSeconds(20L * 3600),
            endTime = now.minusSeconds(4L * 3600),
            planId = 1L,
            initialPhase = FastingPhase.FASTING,
            mood = null, hunger = null, waterMl = null, notes = null,
            createdAt = now, updatedAt = now
        )

        // Under 1h
        assertEquals(
            SessionValidationError.DURATION_OUT_OF_RANGE,
            SessionTimeValidator.validateCompleted(now.minusSeconds(30 * 60), now, emptyList(), now = now)
        )
        // Over 168h
        assertEquals(
            SessionValidationError.DURATION_OUT_OF_RANGE,
            SessionTimeValidator.validateCompleted(now.minusSeconds(170L * 3600), now, emptyList(), now = now)
        )
        // Overlaps existing
        assertEquals(
            SessionValidationError.OVERLAPS_EXISTING,
            SessionTimeValidator.validateCompleted(now.minusSeconds(10L * 3600), now.minusSeconds(2L * 3600), listOf(existing), now = now)
        )
        // Valid non-overlapping
        assertNull(
            SessionTimeValidator.validateCompleted(now.minusSeconds(3L * 3600), now.minusSeconds(1L * 3600), listOf(existing), now = now)
        )
    }

    @Test
    fun `super island compact elapsed text formats minutes under 1h and decimal hours above`() {
        assertEquals("0m", NotificationHelper.formatCompactElapsed(0L))
        assertEquals("45m", NotificationHelper.formatCompactElapsed(45L * 60))
        assertEquals("1.0h", NotificationHelper.formatCompactElapsed(3600L))
        assertEquals("13.5h", NotificationHelper.formatCompactElapsed(13L * 3600 + 30L * 60))
    }
}
