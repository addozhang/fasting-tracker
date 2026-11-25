package com.fastingtracker.data.backup

import android.content.Context
import com.fastingtracker.domain.model.FastingPhase
import com.fastingtracker.domain.model.FastingSession
import com.fastingtracker.domain.model.Plan
import com.fastingtracker.domain.model.PlanType
import com.fastingtracker.domain.repository.FastingSessionRepository
import com.fastingtracker.domain.repository.PlanRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

class LocalBackupManagerTest {

    private val context = mockk<Context>(relaxed = true)
    private val planRepo = mockk<PlanRepository>(relaxed = true)
    private val sessionRepo = mockk<FastingSessionRepository>(relaxed = true)
    private lateinit var manager: LocalBackupManager

    private val now = Instant.parse("2026-11-25T10:00:00Z")
    private val plan168 = Plan(1L, "16:8", PlanType.DAILY, 16, 8, true, now, now)

    @Before
    fun setup() {
        manager = LocalBackupManager(context, planRepo, sessionRepo)
    }

    @Test
    fun `buildBackupPayload exports plans and only completed sessions`() = runTest {
        val completed = FastingSession(
            10L, now.minusSeconds(18L * 3600), now.minusSeconds(2L * 3600),
            1L, FastingPhase.FASTING, 4, null, null, "Great", now, now
        )
        val active = FastingSession(
            11L, now.minusSeconds(3600), null,
            1L, FastingPhase.FASTING, null, null, null, null, now, now
        )
        coEvery { planRepo.getAllPlans() } returns listOf(plan168)
        coEvery { sessionRepo.getAllSessions() } returns listOf(completed, active)

        val payload = manager.buildBackupPayload()
        assertEquals(1, payload.plans.size)
        assertEquals(1, payload.sessions.size)
        assertEquals("Great", payload.sessions.first().notes)
        assertEquals(4, payload.sessions.first().mood)
    }

    @Test
    fun `restorePayload deduplicates existing plans and sessions while importing new ones`() = runTest {
        val existingStart = now.minusSeconds(40L * 3600)
        val existingSession = FastingSession(
            1L, existingStart, existingStart.plusSeconds(16L * 3600),
            1L, FastingPhase.FASTING, null, null, null, null, now, now
        )
        coEvery { planRepo.getAllPlans() } returns listOf(plan168)
        coEvery { sessionRepo.getAllSessions() } returns listOf(existingSession)
        coEvery { planRepo.createPlan(any()) } returns 99L
        coEvery { sessionRepo.startFasting(any()) } returns 200L

        val newStart = now.minusSeconds(18L * 3600)
        val payload = BackupPayload(
            version = 1,
            exportedAtMillis = now.toEpochMilli(),
            plans = listOf(
                BackupPlan(1L, "16:8", "DAILY", 16, 8, true),
                BackupPlan(50L, "Custom 15:9", "DAILY", 15, 9, false)
            ),
            sessions = listOf(
                // Duplicate within 60s -> should be skipped
                BackupSession(existingStart.toEpochMilli() + 10_000L, existingStart.plusSeconds(16L * 3600).toEpochMilli(), 1L),
                // Brand new session referencing custom plan 50L -> remapped to 99L
                BackupSession(newStart.toEpochMilli(), newStart.plusSeconds(15L * 3600).toEpochMilli(), 50L, mood = 5, notes = "Restored")
            )
        )

        val res = manager.restorePayload(payload)
        assertEquals(1, res.plansImported)
        assertEquals(1, res.sessionsImported)
        coVerify(exactly = 1) { planRepo.createPlan(match { it.name == "Custom 15:9" && it.fastingHours == 15 }) }
        coVerify(exactly = 1) { sessionRepo.startFasting(match { it.planId == 99L && it.mood == 5 && it.notes == "Restored" }) }
    }
}
