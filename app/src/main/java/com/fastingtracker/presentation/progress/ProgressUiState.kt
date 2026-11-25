package com.fastingtracker.presentation.progress

import androidx.annotation.StringRes
import com.fastingtracker.domain.model.FastingSession
import com.fastingtracker.domain.model.Plan
import com.fastingtracker.domain.usecase.statistics.Statistics

data class ProgressUiState(
    val statistics: Statistics = Statistics(
        totalSessions = 0,
        totalFastingTimeHours = 0.0,
        averageFastingTimeHours = 0.0,
        longestFastingTimeHours = 0.0,
        averageMood = null,
        averageHunger = null
    ),
    val sessionHistory: List<FastingSession> = emptyList(),
    val plans: List<Plan> = emptyList(),
    val plansById: Map<Long, Plan> = emptyMap(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface ProgressEffect {
    data class ShowMessage(@StringRes val messageRes: Int) : ProgressEffect
    data class ShowValidationError(@StringRes val errorRes: Int) : ProgressEffect
    data class ShowError(val message: String) : ProgressEffect
}
