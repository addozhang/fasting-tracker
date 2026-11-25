package com.fastingtracker.domain.usecase

import com.fastingtracker.domain.model.FastingSubPhase
import com.fastingtracker.domain.usecase.statistics.getPhaseMilestone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for getPhaseMilestone() pure function.
 * No mocks needed — purely functional logic.
 */
class GetPhaseMilestoneTest {

    @Test
    fun `returns null before 12 hours`() {
        assertNull(getPhaseMilestone(0))
        assertNull(getPhaseMilestone(11))
    }

    @Test
    fun `returns Fat Burning between 12 and 15 hours`() {
        assertEquals(FastingSubPhase.FAT_BURNING, getPhaseMilestone(12))
        assertEquals(FastingSubPhase.FAT_BURNING, getPhaseMilestone(15))
    }

    @Test
    fun `returns Ketosis at 16 hours`() {
        assertEquals(FastingSubPhase.KETOSIS, getPhaseMilestone(16))
        assertEquals(FastingSubPhase.KETOSIS, getPhaseMilestone(17))
    }

    @Test
    fun `returns Autophagy between 18 and 23 hours`() {
        assertEquals(FastingSubPhase.AUTOPHAGY, getPhaseMilestone(18))
        assertEquals(FastingSubPhase.AUTOPHAGY, getPhaseMilestone(23))
    }

    @Test
    fun `returns Deep Ketosis between 24 and 47 hours`() {
        assertEquals(FastingSubPhase.DEEP_KETOSIS, getPhaseMilestone(24))
        assertEquals(FastingSubPhase.DEEP_KETOSIS, getPhaseMilestone(47))
    }

    @Test
    fun `returns Extended Fasting between 48 and 71 hours`() {
        assertEquals(FastingSubPhase.EXTENDED_FASTING, getPhaseMilestone(48))
        assertEquals(FastingSubPhase.EXTENDED_FASTING, getPhaseMilestone(71))
    }

    @Test
    fun `returns Deep Autophagy at 72 hours and beyond`() {
        assertEquals(FastingSubPhase.DEEP_AUTOPHAGY, getPhaseMilestone(72))
        assertEquals(FastingSubPhase.DEEP_AUTOPHAGY, getPhaseMilestone(100))
    }
}
