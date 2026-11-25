package com.fastingtracker.presentation.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastingtracker.R
import com.fastingtracker.domain.repository.FastingSessionRepository
import com.fastingtracker.domain.repository.PlanRepository
import com.fastingtracker.domain.usecase.fasting.DeleteSessionUseCase
import com.fastingtracker.domain.usecase.fasting.SaveSessionRecordUseCase
import com.fastingtracker.domain.usecase.fasting.SessionValidationError
import com.fastingtracker.domain.usecase.fasting.SessionValidationException
import com.fastingtracker.domain.usecase.statistics.GetStatisticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class ProgressViewModel @Inject constructor(
    sessionRepository: FastingSessionRepository,
    planRepository: PlanRepository,
    private val getStatisticsUseCase: GetStatisticsUseCase,
    private val saveSessionRecordUseCase: SaveSessionRecordUseCase,
    private val deleteSessionUseCase: DeleteSessionUseCase
) : ViewModel() {

    private val _effects = Channel<ProgressEffect>(Channel.BUFFERED)
    val effects: Flow<ProgressEffect> = _effects.receiveAsFlow()

    val uiState: StateFlow<ProgressUiState> = combine(
        sessionRepository.observeAllSessions(),
        planRepository.observeAllPlans()
    ) { sessions, plans ->
        val stats = getStatisticsUseCase.calculate(sessions, plans)
        ProgressUiState(
            statistics = stats,
            sessionHistory = sessions.filter { it.endTime != null },
            plans = plans,
            plansById = plans.mapNotNull { p -> p.id?.let { it to p } }.toMap(),
            isLoading = false,
            errorMessage = null
        )
    }
        .catch { e ->
            emit(ProgressUiState(isLoading = false, errorMessage = e.message ?: "Failed to load statistics"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ProgressUiState(isLoading = true)
        )

    fun saveSession(
        sessionId: Long?,
        planId: Long,
        startTime: Instant,
        endTime: Instant,
        mood: Int?,
        notes: String?,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = saveSessionRecordUseCase(
                sessionId = sessionId,
                planId = planId,
                startTime = startTime,
                endTime = endTime,
                mood = mood,
                notes = notes
            )
            result.onSuccess {
                onSaved()
                _effects.send(ProgressEffect.ShowMessage(R.string.record_updated_toast))
            }.onFailure { err ->
                if (err is SessionValidationException) {
                    val res = when (err.error) {
                        SessionValidationError.END_BEFORE_START -> R.string.error_end_before_start
                        SessionValidationError.FUTURE_TIME -> R.string.error_future_time
                        SessionValidationError.DURATION_OUT_OF_RANGE -> R.string.error_duration_range
                        SessionValidationError.OVERLAPS_EXISTING -> R.string.error_session_overlap
                    }
                    _effects.send(ProgressEffect.ShowValidationError(res))
                } else {
                    _effects.send(ProgressEffect.ShowError(err.message ?: "Failed to save record"))
                }
            }
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            deleteSessionUseCase(sessionId)
                .onSuccess { _effects.send(ProgressEffect.ShowMessage(R.string.record_deleted_toast)) }
                .onFailure { e -> _effects.send(ProgressEffect.ShowError(e.message ?: "Failed to delete")) }
        }
    }
}
