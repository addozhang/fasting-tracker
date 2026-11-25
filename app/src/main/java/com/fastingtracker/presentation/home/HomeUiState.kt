package com.fastingtracker.presentation.home

import androidx.annotation.StringRes
import com.fastingtracker.domain.model.Plan
import com.fastingtracker.domain.usecase.statistics.FastingProgress
import java.time.Instant

sealed class HomeUiState {
    data object Loading : HomeUiState()

    data class Idle(
        val selectedPlan: Plan?,
        val plans: List<Plan>,
        val lastEndedTime: Instant?,
        val eatingElapsedSeconds: Long,
        val eatingTargetSeconds: Long?,
        val currentStreak: Int
    ) : HomeUiState()

    data class ActiveFasting(
        val progress: FastingProgress,
        val selectedPlan: Plan?,
        val plans: List<Plan>,
        val currentStreak: Int
    ) : HomeUiState()
}

sealed interface HomeEffect {
    data class ShowMessage(@StringRes val messageRes: Int) : HomeEffect
    data class ShowError(val message: String) : HomeEffect
}
