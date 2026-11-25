package com.fastingtracker.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fastingtracker.R
import com.fastingtracker.domain.model.FastingSubPhase
import com.fastingtracker.domain.model.Plan
import com.fastingtracker.domain.usecase.fasting.EndFastingUseCase
import com.fastingtracker.presentation.components.DateTimePickerFlow
import com.fastingtracker.ui.theme.LocalFastingColors
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val MOOD_EMOJIS = listOf("😫", "😐", "🙂", "😊", "✨")

private data class MilestoneSpec(
    val hours: Int,
    val emoji: String,
    val titleRes: Int,
    val descRes: Int
)

private val MILESTONE_SPECS = listOf(
    MilestoneSpec(12, "🔥", R.string.phase_fat_burning, R.string.milestone_12h_desc),
    MilestoneSpec(16, "⚡", R.string.phase_ketosis, R.string.milestone_16h_desc),
    MilestoneSpec(24, "✨", R.string.phase_autophagy, R.string.milestone_24h_desc),
    MilestoneSpec(48, "🧬", R.string.phase_deep_autophagy, R.string.milestone_48h_desc),
    MilestoneSpec(72, "🌿", R.string.phase_extended_reset, R.string.milestone_72h_desc)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    endFastTriggerCount: Int = 0,
    onEndFastTriggerConsumed: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showPlanSheet by remember { mutableStateOf(false) }
    var showCreatePlanDialog by remember { mutableStateOf(false) }
    var pendingDeletePlan by remember { mutableStateOf<Plan?>(null) }
    var showStartTimeSheet by remember { mutableStateOf(false) }
    var isAdjustingNewFast by remember { mutableStateOf(false) }
    var showEndFastSheet by remember { mutableStateOf(false) }
    var showElapsedMode by remember { mutableStateOf(false) }
    var selectedMilestoneHours by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HomeEffect.ShowMessage -> snackbarHostState.showSnackbar(context.getString(effect.messageRes))
                is HomeEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    LaunchedEffect(endFastTriggerCount, uiState) {
        if (endFastTriggerCount > 0 && uiState is HomeUiState.ActiveFasting) {
            showEndFastSheet = true
            onEndFastTriggerConsumed()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is HomeUiState.Idle -> {
                TimerScreenBody(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    isFasting = false,
                    plan = state.selectedPlan,
                    currentStreak = state.currentStreak,
                    progressFraction = if (state.lastEndedTime != null && state.eatingTargetSeconds != null && state.eatingTargetSeconds > 0) {
                        (state.eatingElapsedSeconds.toFloat() / state.eatingTargetSeconds.toFloat()).coerceIn(0f, 1f)
                    } else 0f,
                    mainTimeText = if (state.lastEndedTime != null) formatHMS(state.eatingElapsedSeconds) else "00:00:00",
                    subLabel = if (state.lastEndedTime != null) stringResource(R.string.eating_elapsed_label) else stringResource(R.string.ready_to_fast),
                    phasePillText = stringResource(R.string.eating_window_label),
                    phaseEmoji = "🌿",
                    isGoalReached = false,
                    elapsedHours = 0.0,
                    leftCardLabel = stringResource(R.string.last_ended_label),
                    leftCardValue = state.lastEndedTime?.let { formatShortClock(it) } ?: "—",
                    leftCardEditable = false,
                    rightCardLabel = stringResource(R.string.suggested_next_label),
                    rightCardValue = if (state.lastEndedTime != null && state.eatingTargetSeconds != null) {
                        formatSmartClock(state.lastEndedTime.plusSeconds(state.eatingTargetSeconds))
                    } else "—",
                    selectedMilestoneHours = null,
                    onPlanPillClick = { showPlanSheet = true },
                    onRingTap = {},
                    onMilestoneTap = {},
                    onLeftCardClick = {},
                    onPrimaryAction = {
                        val id = state.selectedPlan?.id ?: state.plans.firstOrNull()?.id ?: 1L
                        viewModel.onStartFasting(id, 0L)
                    },
                    onSecondaryAction = {
                        isAdjustingNewFast = true
                        showStartTimeSheet = true
                    }
                )
            }
            is HomeUiState.ActiveFasting -> {
                val p = state.progress
                val displaySeconds = if (p.isGoalReached || showElapsedMode) {
                    p.elapsedTimeSeconds
                } else {
                    p.remainingTimeSeconds
                }
                val subLabel = when {
                    p.isGoalReached -> stringResource(R.string.goal_reached_badge, formatHM(p.overtimeSeconds))
                    showElapsedMode -> stringResource(R.string.elapsed_label)
                    else -> stringResource(R.string.remaining_label)
                }
                val (phaseEmoji, phaseRes) = subPhaseDisplay(p.phaseMilestone)

                TimerScreenBody(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    isFasting = true,
                    plan = state.selectedPlan,
                    currentStreak = state.currentStreak,
                    progressFraction = (p.progressPercent / 100f).coerceIn(0f, 1f),
                    mainTimeText = formatHMS(displaySeconds),
                    subLabel = subLabel,
                    phasePillText = stringResource(phaseRes),
                    phaseEmoji = phaseEmoji,
                    isGoalReached = p.isGoalReached,
                    elapsedHours = p.elapsedTimeSeconds / 3600.0,
                    leftCardLabel = stringResource(R.string.started_at_label),
                    leftCardValue = formatSmartClock(p.startTime),
                    leftCardEditable = true,
                    rightCardLabel = stringResource(R.string.target_end_label),
                    rightCardValue = formatSmartClock(p.expectedEndTime),
                    selectedMilestoneHours = selectedMilestoneHours,
                    onPlanPillClick = { showPlanSheet = true },
                    onRingTap = { showElapsedMode = !showElapsedMode },
                    onMilestoneTap = { hrs ->
                        selectedMilestoneHours = if (selectedMilestoneHours == hrs) null else hrs
                    },
                    onLeftCardClick = {
                        isAdjustingNewFast = false
                        showStartTimeSheet = true
                    },
                    onPrimaryAction = { showEndFastSheet = true },
                    onSecondaryAction = null
                )
            }
        }
    }

    // 1. Grouped Plan Management BottomSheet
    if (showPlanSheet) {
        val plans = when (val s = uiState) {
            is HomeUiState.Idle -> s.plans
            is HomeUiState.ActiveFasting -> s.plans
            else -> emptyList()
        }
        val selectedId = when (val s = uiState) {
            is HomeUiState.Idle -> s.selectedPlan?.id
            is HomeUiState.ActiveFasting -> s.selectedPlan?.id
            else -> null
        }
        val isActive = uiState is HomeUiState.ActiveFasting

        PlanManagementSheet(
            plans = plans,
            selectedPlanId = selectedId,
            allowSelect = !isActive,
            onSelectPlan = { plan ->
                viewModel.onSelectPlan(plan)
                showPlanSheet = false
            },
            onCreateClick = {
                showPlanSheet = false
                showCreatePlanDialog = true
            },
            onDeleteClick = { plan ->
                pendingDeletePlan = plan
            },
            onDismiss = { showPlanSheet = false }
        )
    }

    if (showCreatePlanDialog) {
        CreatePlanDialog(
            onDismiss = { showCreatePlanDialog = false },
            onCreate = { name, fastingHours, eatingHours ->
                viewModel.onCreatePlan(name, fastingHours, eatingHours)
                showCreatePlanDialog = false
            }
        )
    }

    pendingDeletePlan?.let { plan ->
        AlertDialog(
            onDismissRequest = { pendingDeletePlan = null },
            title = { Text(stringResource(R.string.delete_plan_title)) },
            text = { Text(stringResource(R.string.delete_plan_message, plan.name)) },
            confirmButton = {
                TextButton(onClick = {
                    plan.id?.let { viewModel.onDeletePlan(it) }
                    pendingDeletePlan = null
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletePlan = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 2. Start Time Adjust Sheet (Quick Chips + M3 Date/Time Picker)
    if (showStartTimeSheet) {
        val currentStart = (uiState as? HomeUiState.ActiveFasting)?.progress?.startTime ?: Instant.now()
        val selectedPlanId = when (val s = uiState) {
            is HomeUiState.Idle -> s.selectedPlan?.id ?: s.plans.firstOrNull()?.id ?: 1L
            is HomeUiState.ActiveFasting -> s.selectedPlan?.id ?: 1L
            else -> 1L
        }
        StartTimeAdjustSheet(
            initialInstant = currentStart,
            onQuickOffsetSelected = { offsetMinutes ->
                showStartTimeSheet = false
                if (isAdjustingNewFast) {
                    viewModel.onStartFasting(selectedPlanId, offsetMinutes)
                } else {
                    viewModel.onUpdateActiveStartTime(Instant.now().minusSeconds(offsetMinutes * 60L))
                }
            },
            onExactInstantSelected = { instant ->
                showStartTimeSheet = false
                if (isAdjustingNewFast) {
                    viewModel.onStartFastingAt(selectedPlanId, instant)
                } else {
                    viewModel.onUpdateActiveStartTime(instant)
                }
            },
            onDismiss = { showStartTimeSheet = false }
        )
    }

    // 3. End Fast Confirmation Sheet (<1h discard guard + optional Mood & Note)
    if (showEndFastSheet) {
        val activeState = uiState as? HomeUiState.ActiveFasting
        if (activeState != null) {
            EndFastConfirmationSheet(
                progress = activeState.progress,
                onConfirmEnd = { endTime, mood, notes ->
                    showEndFastSheet = false
                    viewModel.onEndFasting(endTime = endTime, mood = mood, notes = notes)
                },
                onDismiss = { showEndFastSheet = false }
            )
        } else {
            showEndFastSheet = false
        }
    }
}

@Composable
private fun TimerScreenBody(
    modifier: Modifier,
    isFasting: Boolean,
    plan: Plan?,
    currentStreak: Int,
    progressFraction: Float,
    mainTimeText: String,
    subLabel: String,
    phasePillText: String,
    phaseEmoji: String,
    isGoalReached: Boolean,
    elapsedHours: Double,
    leftCardLabel: String,
    leftCardValue: String,
    leftCardEditable: Boolean,
    rightCardLabel: String,
    rightCardValue: String,
    selectedMilestoneHours: Int?,
    onPlanPillClick: () -> Unit,
    onRingTap: () -> Unit,
    onMilestoneTap: (Int) -> Unit,
    onLeftCardClick: () -> Unit,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: (() -> Unit)?
) {
    val fastingColors = LocalFastingColors.current
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 600),
        label = "ring_progress"
    )

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Plan Pill + Streak
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onPlanPillClick,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, fastingColors.cardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isFasting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                    )
                    Text(
                        text = plan?.name ?: "16:8",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.streak_days_short, currentStreak),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Phase Status Pill
        Surface(
            shape = CircleShape,
            color = if (isFasting) fastingColors.fastSoftBg else fastingColors.eatSoftBg
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = phaseEmoji, fontSize = 13.sp)
                Text(
                    text = phasePillText,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isFasting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Hero Organic Minimal Ring with Milestone Nodes
        val targetHours = (plan?.fastingHours ?: 16).coerceAtLeast(1)
        val visibleMilestones = remember(isFasting, targetHours) {
            if (!isFasting) emptyList()
            else MILESTONE_SPECS.filter { it.hours <= maxOf(targetHours, 16) }
        }

        Box(
            modifier = Modifier
                .size(284.dp)
                .clip(CircleShape)
                .clickable(enabled = isFasting, onClick = onRingTap),
            contentAlignment = Alignment.Center
        ) {
            val startColor = if (isFasting) fastingColors.fastGradientStart else fastingColors.eatGradientStart
            val endColor = if (isFasting) fastingColors.fastGradientEnd else fastingColors.eatGradientEnd
            val trackColor = fastingColors.ringTrack

            Canvas(modifier = Modifier.size(252.dp)) {
                val strokeWidth = 18.dp.toPx()
                val inset = strokeWidth / 2f
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(inset, inset)

                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                if (animatedProgress > 0f) {
                    drawArc(
                        brush = Brush.sweepGradient(listOf(startColor, endColor, startColor)),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            // Clickable Metabolic Milestone Nodes on the ring circumference
            visibleMilestones.forEach { ms ->
                val fraction = (ms.hours.toFloat() / targetHours.toFloat()).coerceIn(0.05f, 0.98f)
                val angleRad = Math.toRadians((fraction * 360.0 - 90.0))
                val radiusDp = 117f
                val xDp = (radiusDp * cos(angleRad)).toFloat()
                val yDp = (radiusDp * sin(angleRad)).toFloat()
                val unlocked = elapsedHours >= ms.hours

                Surface(
                    onClick = { onMilestoneTap(ms.hours) },
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (xDp * density).roundToInt(),
                                y = (yDp * density).roundToInt()
                            )
                        }
                        .size(30.dp),
                    shape = CircleShape,
                    color = if (unlocked) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(
                        width = if (unlocked) 2.dp else 1.dp,
                        color = if (unlocked) MaterialTheme.colorScheme.primary else fastingColors.cardBorder
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = ms.emoji, fontSize = 13.sp)
                    }
                }
            }

            // Center Readout
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 36.dp)
            ) {
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isGoalReached) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = mainTimeText,
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${(animatedProgress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isFasting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                    if (isFasting) {
                        Text(
                            text = "· ${stringResource(R.string.tap_to_switch)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Expandable Metabolic Milestone Explanation Card
        val activeMs = MILESTONE_SPECS.firstOrNull { it.hours == selectedMilestoneHours }
        AnimatedVisibility(visible = activeMs != null && isFasting) {
            activeMs?.let { ms ->
                val unlocked = elapsedHours >= ms.hours
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, fastingColors.cardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = ms.emoji, fontSize = 20.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${ms.hours}h · ${stringResource(ms.titleRes)}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = if (unlocked) fastingColors.fastSoftBg else MaterialTheme.colorScheme.surfaceContainer
                                ) {
                                    Text(
                                        text = stringResource(if (unlocked) R.string.milestone_unlocked else R.string.milestone_locked),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(ms.descRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { onMilestoneTap(ms.hours) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.close),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Start / Target Time Info Pair
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(enabled = leftCardEditable, onClick = onLeftCardClick),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, fastingColors.cardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = leftCardLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (leftCardEditable) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = leftCardValue,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, fastingColors.cardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = rightCardLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = rightCardValue,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Centered Primary CTA
        if (isFasting) {
            Button(
                onClick = onPrimaryAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(
                    text = stringResource(R.string.end_fast),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        } else {
            Button(
                onClick = onPrimaryAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.start_fasting),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            onSecondaryAction?.let {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = it) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.adjust_start_time),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanManagementSheet(
    plans: List<Plan>,
    selectedPlanId: Long?,
    allowSelect: Boolean,
    onSelectPlan: (Plan) -> Unit,
    onCreateClick: () -> Unit,
    onDeleteClick: (Plan) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dailyPresets = plans.filter { it.isDefault && it.eatingHours != null }.sortedBy { it.fastingHours }
    val extendedPresets = plans.filter { it.isDefault && it.eatingHours == null }.sortedBy { it.fastingHours }
    val customPlans = plans.filter { !it.isDefault }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.plans_sheet_title),
                    style = MaterialTheme.typography.titleLarge
                )
                TextButton(onClick = onCreateClick) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.new_custom_plan))
                }
            }

            // Daily Intermittent (2-column grid)
            if (dailyPresets.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.plan_group_daily),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    dailyPresets.chunked(2).forEach { rowPlans ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowPlans.forEach { plan ->
                                PlanOptionCard(
                                    plan = plan,
                                    isSelected = plan.id == selectedPlanId,
                                    enabled = allowSelect,
                                    onSelect = { onSelectPlan(plan) },
                                    onDelete = null,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowPlans.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Extended Fasts (24h, 48h, 72h)
            if (extendedPresets.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.plan_group_extended),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    extendedPresets.forEach { plan ->
                        PlanOptionCard(
                            plan = plan,
                            isSelected = plan.id == selectedPlanId,
                            enabled = allowSelect,
                            onSelect = { onSelectPlan(plan) },
                            onDelete = null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Custom Plans
            if (customPlans.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.plan_group_custom),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    customPlans.forEach { plan ->
                        PlanOptionCard(
                            plan = plan,
                            isSelected = plan.id == selectedPlanId,
                            enabled = allowSelect,
                            onSelect = { onSelectPlan(plan) },
                            onDelete = { onDeleteClick(plan) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanOptionCard(
    plan: Plan,
    isSelected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val fastingColors = LocalFastingColors.current
    Surface(
        onClick = onSelect,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) fastingColors.fastSoftBg else MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else fastingColors.cardBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plan.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (plan.eatingHours != null) {
                        stringResource(R.string.plan_daily_desc, plan.fastingHours, plan.eatingHours)
                    } else {
                        stringResource(R.string.plan_extended_desc, plan.fastingHours)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartTimeAdjustSheet(
    initialInstant: Instant,
    onQuickOffsetSelected: (Long) -> Unit,
    onExactInstantSelected: (Instant) -> Unit,
    onDismiss: () -> Unit
) {
    var showDateTimePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.adjust_start_sheet_title),
                style = MaterialTheme.typography.titleLarge
            )

            val quickOptions = listOf(
                0L to R.string.quick_now,
                30L to R.string.quick_30m_ago,
                60L to R.string.quick_1h_ago,
                120L to R.string.quick_2h_ago
            )
            quickOptions.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { (mins, labelRes) ->
                        OutlinedButton(
                            onClick = { onQuickOffsetSelected(mins) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(stringResource(labelRes))
                        }
                    }
                }
            }

            Button(
                onClick = { showDateTimePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.pick_exact_time))
            }
        }
    }

    if (showDateTimePicker) {
        DateTimePickerFlow(
            initialInstant = initialInstant,
            onConfirm = { picked ->
                showDateTimePicker = false
                onExactInstantSelected(picked)
            },
            onDismiss = { showDateTimePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EndFastConfirmationSheet(
    progress: com.fastingtracker.domain.usecase.statistics.FastingProgress,
    onConfirmEnd: (Instant, Int?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    val fastingColors = LocalFastingColors.current
    var endTime by remember { mutableStateOf(Instant.now()) }
    var showEndPicker by remember { mutableStateOf(false) }
    var selectedMood by remember { mutableStateOf<Int?>(null) }
    var note by remember { mutableStateOf("") }

    val elapsedSeconds = Duration.between(progress.startTime, endTime).seconds.coerceAtLeast(0L)
    val isUnderOneHour = elapsedSeconds < EndFastingUseCase.MIN_SAVE_SECONDS

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isUnderOneHour) {
                Text(
                    text = stringResource(R.string.end_fast_short_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = stringResource(R.string.end_fast_short_desc, formatHM(elapsedSeconds)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(stringResource(R.string.keep_fasting))
                    }
                    Button(
                        onClick = { onConfirmEnd(endTime, null, null) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.discard_fast))
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.end_fast_confirm_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = formatHM(elapsedSeconds),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // Editable End Time Row
                Surface(
                    onClick = { showEndPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.end_time_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = formatSmartClock(endTime),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Optional 5 Mood Emojis
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.mood_optional_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MOOD_EMOJIS.forEachIndexed { idx, emoji ->
                            val moodValue = idx + 1
                            val selected = selectedMood == moodValue
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) fastingColors.fastSoftBg else MaterialTheme.colorScheme.surfaceContainer)
                                    .border(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedMood = if (selected) null else moodValue },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }

                // Optional 1-line note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text(stringResource(R.string.note_optional_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = { onConfirmEnd(endTime, selectedMood, note.ifBlank { null }) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(stringResource(R.string.save_fast))
                    }
                }
            }
        }
    }

    if (showEndPicker) {
        DateTimePickerFlow(
            initialInstant = endTime,
            onConfirm = { picked ->
                endTime = picked
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false }
        )
    }
}

@Composable
private fun CreatePlanDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, fastingHours: Int, eatingHours: Int?) -> Unit
) {
    var planName by remember { mutableStateOf("") }
    var fastingHours by remember { mutableStateOf("16") }
    var eatingHours by remember { mutableStateOf("8") }
    var isExtended by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_plan_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = planName,
                    onValueChange = { planName = it },
                    label = { Text(stringResource(R.string.plan_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fastingHours,
                    onValueChange = { fastingHours = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.fasting_hours_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isExtended) {
                    OutlinedTextField(
                        value = eatingHours,
                        onValueChange = { eatingHours = it.filter(Char::isDigit) },
                        label = { Text(stringResource(R.string.eating_hours_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isExtended = !isExtended }
                ) {
                    Checkbox(checked = isExtended, onCheckedChange = { isExtended = it })
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.extended_fast_checkbox), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            val fh = fastingHours.toIntOrNull()
            TextButton(
                onClick = {
                    val f = fh ?: 16
                    val e = if (isExtended) null else eatingHours.toIntOrNull()
                    onCreate(planName, f, e)
                },
                enabled = planName.isNotBlank() && fh != null && fh in 1..168
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

private fun subPhaseDisplay(subPhase: FastingSubPhase?): Pair<String, Int> = when (subPhase) {
    FastingSubPhase.FAT_BURNING -> "🔥" to R.string.phase_fat_burning
    FastingSubPhase.KETOSIS -> "⚡" to R.string.phase_ketosis
    FastingSubPhase.DEEP_KETOSIS -> "⚡" to R.string.phase_deep_ketosis
    FastingSubPhase.AUTOPHAGY -> "✨" to R.string.phase_autophagy
    FastingSubPhase.EXTENDED_FASTING -> "🧬" to R.string.phase_deep_autophagy
    FastingSubPhase.DEEP_AUTOPHAGY -> "🌿" to R.string.phase_extended_reset
    else -> "🌱" to R.string.phase_settling
}

private fun formatHMS(seconds: Long): String {
    val s = seconds.coerceAtLeast(0L)
    val h = s / 3600
    val m = (s % 3600) / 60
    val r = s % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, r)
}

private fun formatHM(seconds: Long): String {
    val s = seconds.coerceAtLeast(0L)
    val h = s / 3600
    val m = (s % 3600) / 60
    return "${h}h ${m}m"
}

private fun formatShortClock(instant: Instant): String {
    return instant.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
}

@Composable
private fun formatSmartClock(instant: Instant): String {
    val zone = ZoneId.systemDefault()
    val zdt = instant.atZone(zone)
    val today = LocalDate.now(zone)
    val clock = zdt.format(DateTimeFormatter.ofPattern("HH:mm"))
    return when (zdt.toLocalDate()) {
        today -> clock
        today.plusDays(1) -> stringResource(R.string.tomorrow_prefix, clock)
        today.minusDays(1) -> stringResource(R.string.yesterday_prefix, clock)
        else -> zdt.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))
    }
}
