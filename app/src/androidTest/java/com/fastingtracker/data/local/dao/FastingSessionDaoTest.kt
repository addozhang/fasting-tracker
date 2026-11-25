package com.fastingtracker.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fastingtracker.data.local.AppDatabase
import com.fastingtracker.domain.model.FastingPhase
import com.fastingtracker.data.local.entity.FastingSessionEntity
import com.fastingtracker.data.local.entity.PlanEntity
import com.fastingtracker.domain.model.PlanType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class FastingSessionDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var sessionDao: FastingSessionDao
    private lateinit var planDao: PlanDao

    private val now = Instant.now()
    private var testPlanId: Long = 0

    @Before
    fun setup() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        sessionDao = db.fastingSessionDao()
        planDao = db.planDao()

        // Insert a plan for FK reference
        testPlanId = planDao.insert(
            PlanEntity(
                name = "16:8",
                planType = PlanType.DAILY,
                fastingHours = 16,
                eatingHours = 8,
                isDefault = true,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun makeSession(
        endTime: Instant? = null,
        planId: Long = testPlanId,
        phase: FastingPhase = FastingPhase.FASTING
    ) = FastingSessionEntity(
        startTime = now,
        endTime = endTime,
        planId = planId,
        initialPhase = phase,
        mood = null,
        hunger = null,
        waterMl = null,
        notes = null,
        createdAt = now,
        updatedAt = now
    )

    @Test
    fun insertAndGetById() = runTest {
        val id = sessionDao.insert(makeSession())
        val session = sessionDao.getById(id)

        assertNotNull(session)
        assertEquals(testPlanId, session!!.planId)
        assertEquals(FastingPhase.FASTING, session.initialPhase)
        assertNull(session.endTime)
    }

    @Test
    fun getActiveSessionReturnsOnlyOpenSession() = runTest {
        sessionDao.insert(makeSession(endTime = now)) // completed
        sessionDao.insert(makeSession()) // active (no endTime)

        val active = sessionDao.getActiveSession()
        assertNotNull(active)
        assertNull(active!!.endTime)
    }

    @Test
    fun getActiveSessionReturnsNullWhenNone() = runTest {
        sessionDao.insert(makeSession(endTime = now))

        val active = sessionDao.getActiveSession()
        assertNull(active)
    }

    @Test
    fun updateSession() = runTest {
        val id = sessionDao.insert(makeSession())
        val session = sessionDao.getById(id)!!

        val endTime = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS)
        sessionDao.update(session.copy(endTime = endTime, mood = 5))

        val updated = sessionDao.getById(id)!!
        assertEquals(endTime, updated.endTime)
        assertEquals(5, updated.mood)
    }

    @Test
    fun getAllReturnsAllSessions() = runTest {
        sessionDao.insert(makeSession())
        sessionDao.insert(makeSession(endTime = now))
        sessionDao.insert(makeSession(phase = FastingPhase.EATING))

        val all = sessionDao.getAll()
        assertEquals(3, all.size)
    }

    @Test
    fun getSessionsPaginatedWorks() = runTest {
        repeat(5) { sessionDao.insert(makeSession(endTime = now)) }

        val page1 = sessionDao.getSessionsPaginated(limit = 2, offset = 0)
        val page2 = sessionDao.getSessionsPaginated(limit = 2, offset = 2)

        assertEquals(2, page1.size)
        assertEquals(2, page2.size)
    }

    @Test
    fun getSessionsCountReturnsCorrectCount() = runTest {
        assertEquals(0, sessionDao.getSessionsCount())

        sessionDao.insert(makeSession())
        sessionDao.insert(makeSession(endTime = now))

        assertEquals(2, sessionDao.getSessionsCount())
    }
}
