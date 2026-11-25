package com.fastingtracker.data.backup

import android.content.Context
import android.net.Uri
import com.fastingtracker.domain.model.FastingPhase
import com.fastingtracker.domain.model.FastingSession
import com.fastingtracker.domain.model.Plan
import com.fastingtracker.domain.model.PlanType
import com.fastingtracker.domain.repository.FastingSessionRepository
import com.fastingtracker.domain.repository.PlanRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackupPayload(
    val version: Int = 1,
    val exportedAtMillis: Long,
    val plans: List<BackupPlan>,
    val sessions: List<BackupSession>
)

@Serializable
data class BackupPlan(
    val id: Long,
    val name: String,
    val planType: String,
    val fastingHours: Int,
    val eatingHours: Int?,
    val isDefault: Boolean
)

@Serializable
data class BackupSession(
    val startTimeMillis: Long,
    val endTimeMillis: Long?,
    val planId: Long,
    val mood: Int? = null,
    val notes: String? = null
)

data class ImportResult(val plansImported: Int, val sessionsImported: Int)

/**
 * Pure local JSON backup/restore via Android Storage Access Framework (SAF).
 * Zero network code, zero storage permissions required.
 */
@Singleton
class LocalBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val planRepository: PlanRepository,
    private val sessionRepository: FastingSessionRepository
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun exportToUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val payload = buildBackupPayload()
            val text = json.encodeToString(payload)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(text.toByteArray(Charsets.UTF_8))
            } ?: return@withContext Result.failure(IllegalStateException("Cannot open output stream"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromUri(uri: Uri): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return@withContext Result.failure(IllegalStateException("Cannot open input stream"))
            val payload = json.decodeFromString<BackupPayload>(text)
            Result.success(restorePayload(payload))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun buildBackupPayload(): BackupPayload {
        val plans = planRepository.getAllPlans().mapNotNull { plan ->
            val id = plan.id ?: return@mapNotNull null
            BackupPlan(
                id = id,
                name = plan.name,
                planType = plan.planType.name,
                fastingHours = plan.fastingHours,
                eatingHours = plan.eatingHours,
                isDefault = plan.isDefault
            )
        }
        val sessions = sessionRepository.getAllSessions()
            .filter { it.endTime != null }
            .map { s ->
                BackupSession(
                    startTimeMillis = s.startTime.toEpochMilli(),
                    endTimeMillis = s.endTime?.toEpochMilli(),
                    planId = s.planId,
                    mood = s.mood,
                    notes = s.notes
                )
            }
        return BackupPayload(
            exportedAtMillis = Instant.now().toEpochMilli(),
            plans = plans,
            sessions = sessions
        )
    }

    suspend fun restorePayload(payload: BackupPayload): ImportResult {
        val now = Instant.now()
        val existingPlans = planRepository.getAllPlans()
        val idMapping = mutableMapOf<Long, Long>()
        var importedPlans = 0

        for (bp in payload.plans) {
            val match = existingPlans.firstOrNull {
                it.fastingHours == bp.fastingHours && it.eatingHours == bp.eatingHours && it.name == bp.name
            }
            if (match?.id != null) {
                idMapping[bp.id] = match.id
            } else {
                val type = runCatching { PlanType.valueOf(bp.planType) }.getOrDefault(
                    if (bp.eatingHours == null) PlanType.TARGET else PlanType.DAILY
                )
                val newId = planRepository.createPlan(
                    Plan(
                        id = null,
                        name = bp.name,
                        planType = type,
                        fastingHours = bp.fastingHours,
                        eatingHours = bp.eatingHours,
                        isDefault = false,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                idMapping[bp.id] = newId
                importedPlans++
            }
        }

        val fallbackPlanId = existingPlans.firstOrNull()?.id ?: idMapping.values.firstOrNull() ?: 1L
        val existingSessions = sessionRepository.getAllSessions()
        var importedSessions = 0

        for (bs in payload.sessions) {
            val endMillis = bs.endTimeMillis ?: continue
            val start = Instant.ofEpochMilli(bs.startTimeMillis)
            val end = Instant.ofEpochMilli(endMillis)
            val duplicate = existingSessions.any {
                kotlin.math.abs(it.startTime.toEpochMilli() - bs.startTimeMillis) < 60_000L
            }
            if (!duplicate && end.isAfter(start)) {
                sessionRepository.startFasting(
                    FastingSession(
                        id = null,
                        startTime = start,
                        endTime = end,
                        planId = idMapping[bs.planId] ?: fallbackPlanId,
                        initialPhase = FastingPhase.FASTING,
                        mood = bs.mood,
                        hunger = null,
                        waterMl = null,
                        notes = bs.notes,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                importedSessions++
            }
        }

        return ImportResult(plansImported = importedPlans, sessionsImported = importedSessions)
    }
}
