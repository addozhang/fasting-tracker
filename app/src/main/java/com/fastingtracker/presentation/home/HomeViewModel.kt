package com.fastingtracker.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastingtracker.R
import com.fastingtracker.domain.coordinator.FastingSessionCoordinator
import com.fastingtracker.domain.model.Plan
import com.fastingtracker.domain.model.PlanType
import com.fastingtracker.domain.repository.FastingSessionRepository
import com.fastingtracker.domain.repository.PlanRepository
import com.fastingtracker.domain.repository.SettingsRepository
import com.fastingtracker.domain.usecase.fasting.EndFastOutcome
import com.fastingtracker.domain.usecase.plan.CreatePlanUseCase
import com.fastingtracker.domain.usecase.plan.DeletePlanUseCase
import com.fastingtracker.domain.usecase.statistics.GetCurrentFastingProgressUseCase
import com.fastingtracker.domain.util.StreakCalculator
import com.fastingtracker.util.TickerProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val coordinator: FastingSessionCoordinator,
    private val sessionRepository: FastingSessionRepository,
    private val planRepository: PlanRepository,
    private val settingsRepository: SettingsRepository,
    private val getCurrentFastingProgress: GetCurrentFastingProgressUseCase,
    private val createPlanUseCase: CreatePlanUseCase,
    private val deletePlanUseCase: DeletePlanUseCase,
    tickerProvider: TickerProvider
) : ViewModel() {

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        sessionRepository.observeActiveSession(),
        planRepository.observeAllPlans(),
        sessionRepository.observeAllSessions(),
        settingsRepository.observeSelectedPlanId(),
        tickerProvider.ticker(1000L).onStart { emit(Unit) }
    ) { activeSession, plans, allSessions, savedPlanId, _ ->
        val now = Instant.now()
        val completedSessions = allSessions.filter { it.endTime != null }
        val streak = StreakCalculator.calculate(completedSessions)
        val defaultPlan = plans.firstOrNull { it.id == savedPlanId }
            ?: plans.firstOrNull { it.fastingHours == 16 && it.eatingHours == 8 }
            ?: plans.firstOrNull()

        if (activeSession != null) {
            val activePlan = plans.firstOrNull { it.id == activeSession.planId } ?: defaultPlan
            if (activePlan != null) {
                val progress = getCurrentFastingProgress.calculate(activeSession, activePlan, now)
                HomeUiState.ActiveFasting(
                    progress = progress,
                    selectedPlan = activePlan,
                    plans = plans,
                    currentStreak = streak
                )
            } else {
                HomeUiState.Idle(
                    selectedPlan = defaultPlan,
                    plans = plans,
                    lastEndedTime = null,
                    eatingElapsedSeconds = 0L,
                    eatingTargetSeconds = null,
                    currentStreak = streak
                )
            }
        } else {
            val lastCompleted = completedSessions.maxByOrNull { it.endTime!! }
            val lastEnded = lastCompleted?.endTime
            val eatingElapsed = if (lastEnded != null) {
                Duration.between(lastEnded, now).seconds.coerceAtLeast(0L)
            } else 0L
            val eatingTarget = defaultPlan?.eatingHours?.let { it * 3600L }

            HomeUiState.Idle(
                selectedPlan = defaultPlan,
                plans = plans,
                lastEndedTime = lastEnded,
                eatingElapsedSeconds = eatingElapsed,
                eatingTargetSeconds = eatingTarget,
                currentStreak = streak
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = HomeUiState.Loading
    )

    fun onStartFasting(planId: Long, startOffsetMinutes: Long = 0L) {
        viewModelScope.launch {
            val result = coordinator.startFast(planId = planId, startOffsetMinutes = startOffsetMinutes)
            result.onFailure { e ->
                _effects.send(HomeEffect.ShowError(e.message ?: "Failed to start fast"))
            }
        }
    }

    fun onStartFastingAt(planId: Long, startTime: Instant) {
        val offsetMins = Duration.between(startTime, Instant.now()).toMinutes().coerceAtLeast(0L)
        onStartFasting(planId = planId, startOffsetMinutes = offsetMins)
    }

    fun onUpdateActiveStartTime(newStartTime: Instant) {
        viewModelScope.launch {
            coordinator.updateActiveStartTime(newStartTime).onFailure { e ->
                _effects.send(HomeEffect.ShowError(e.message ?: "Invalid start time"))
            }
        }
    }

    fun onEndFasting(
        endTime: Instant = Instant.now(),
        mood: Int? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
            val result = coordinator.endFast(endTime = endTime, mood = mood, notes = notes)
            result.onSuccess { outcome ->
                when (outcome) {
                    is EndFastOutcome.Saved -> _effects.send(HomeEffect.ShowMessage(R.string.fast_saved_toast))
                    is EndFastOutcome.DiscardedShort -> _effects.send(HomeEffect.ShowMessage(R.string.fast_discarded_toast))
                }
            }.onFailure { e ->
                _effects.send(HomeEffect.ShowError(e.message ?: "Failed to end fast"))
            }
        }
    }

    fun onSelectPlan(plan: Plan) {
        val id = plan.id ?: return
        viewModelScope.launch {
            settingsRepository.setSelectedPlanId(id)
        }
    }

    fun onCreatePlan(name: String, fastingHours: Int, eatingHours: Int?) {
        viewModelScope.launch {
            val now = Instant.now()
            val planType = if (eatingHours == null) PlanType.TARGET else PlanType.DAILY
            val plan = Plan(
                id = null,
                name = name.trim(),
                planType = planType,
                fastingHours = fastingHours,
                eatingHours = eatingHours,
                isDefault = false,
                createdAt = now,
                updatedAt = now
            )
            val result = createPlanUseCase(plan)
            result.onSuccess { newId ->
                settingsRepository.setSelectedPlanId(newId)
            }.onFailure { e ->
                _effects.send(HomeEffect.ShowError(e.message ?: "Failed to create plan"))
            }
        }
    }

    fun onDeletePlan(planId: Long) {
        viewModelScope.launch {
            val result = deletePlanUseCase(planId)
            result.onSuccess {
                _effects.send(HomeEffect.ShowMessage(R.string.plan_deleted_toast))
            }.onFailure { e ->
                _effects.send(HomeEffect.ShowError(e.message ?: "Cannot delete plan"))
            }
        }
    }
}
