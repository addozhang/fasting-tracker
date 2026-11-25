package com.fastingtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fastingtracker.data.local.entity.FastingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FastingSessionDao {
    @Query("SELECT * FROM fasting_sessions ORDER BY startTime DESC LIMIT :limit OFFSET :offset")
    suspend fun getSessionsPaginated(limit: Int, offset: Int): List<FastingSessionEntity>

    @Query("SELECT * FROM fasting_sessions WHERE id = :id")
    suspend fun getById(id: Long): FastingSessionEntity?

    @Query("SELECT * FROM fasting_sessions WHERE endTime IS NULL LIMIT 1")
    suspend fun getActiveSession(): FastingSessionEntity?

    @Query("SELECT * FROM fasting_sessions WHERE endTime IS NULL LIMIT 1")
    fun observeActiveSession(): Flow<FastingSessionEntity?>

    @Query("SELECT * FROM fasting_sessions ORDER BY startTime DESC")
    fun observeAll(): Flow<List<FastingSessionEntity>>

    @Query("SELECT COUNT(*) FROM fasting_sessions")
    suspend fun getSessionsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: FastingSessionEntity): Long

    @Query("SELECT * FROM fasting_sessions ORDER BY startTime DESC")
    suspend fun getAll(): List<FastingSessionEntity>

    @Update
    suspend fun update(session: FastingSessionEntity)

    @Query("DELETE FROM fasting_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE fasting_sessions SET planId = :toPlanId WHERE planId = :fromPlanId")
    suspend fun reassignPlan(fromPlanId: Long, toPlanId: Long)
}
