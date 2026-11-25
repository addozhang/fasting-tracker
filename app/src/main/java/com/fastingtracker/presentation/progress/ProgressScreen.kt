package com.fastingtracker.presentation.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fastingtracker.R
import com.fastingtracker.domain.model.FastingSession
import com.fastingtracker.domain.model.Plan
import com.fastingtracker.domain.usecase.statistics.AchievementKey
import com.fastingtracker.presentation.components.DateTimePickerFlow
import com.fastingtracker.ui.theme.LocalFastingColors
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max

private val HISTORY_MOODS = listOf("😫", "😐", "🙂", "😊", "✨")

@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val fastingColors = LocalFastingColors.current

    var editingSession by remember { mutableStateOf<FastingSession?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    var editorValidationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ProgressEffect.ShowMessage -> snackbarHostState.showSnackbar(context.getString(effect.messageRes))
                is ProgressEffect.ShowValidationError -> editorValidationError = context.getString(effect.errorRes)
                is ProgressEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header with + Manual Add
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.stats_title),
                                style = MaterialTheme.typography.headlineLarge
                            )
                            Surface(
                                onClick = {
                                    editorValidationError = null
                                    showAddSheet = true
                                },
                                shape = CircleShape,
                                color = fastingColors.fastSoftBg
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = stringResource(R.string.log_past_fast),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // 2x2 Organic Minimal Summary Cards
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MetricCard(
                                    title = stringResource(R.string.total_fasts),
                                    value = uiState.statistics.totalSessions.toString(),
                                    emoji = "✓",
                                    modifier = Modifier.weight(1f)
                                )
                                MetricCard(
                                    title = stringResource(R.string.current_streak),
                                    value = "${uiState.statistics.currentStreak}d",
                                    emoji = "🔥",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MetricCard(
                                    title = stringResource(R.string.longest_fast),
                                    value = String.format(Locale.US, "%.1fh", uiState.statistics.longestFastingTimeHours),
                                    emoji = "🏆",
                                    modifier = Modifier.weight(1f)
                                )
                                MetricCard(
                                    title = stringResource(R.string.average_fast),
                                    value = String.format(Locale.US, "%.1fh", uiState.statistics.averageFastingTimeHours),
                                    emoji = "◔",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Achievement Badges
                    if (uiState.statistics.achievements.isNotEmpty()) {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(uiState.statistics.achievements, key = { it.id }) { achievement ->
                                    val labelRes = when (achievement.labelKey) {
                                        AchievementKey.FIRST_FAST -> R.string.achievement_first_fast
                                        AchievementKey.CLUB_16H -> R.string.achievement_16h
                                        AchievementKey.CLUB_18H -> R.string.achievement_18h
                                        AchievementKey.WARRIOR_24H -> R.string.achievement_24h
                                        AchievementKey.STREAK_7_DAYS -> R.string.achievement_7d
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (achievement.unlocked) fastingColors.fastSoftBg else MaterialTheme.colorScheme.surfaceContainer,
                                        border = BorderStroke(
                                            1.dp,
                                            if (achievement.unlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else fastingColors.cardBorder
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = achievement.emoji,
                                                fontSize = 15.sp,
                                                color = if (achievement.unlocked) Color.Unspecified else Color.Unspecified.copy(alpha = 0.35f)
                                            )
                                            Text(
                                                text = stringResource(labelRes),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (achievement.unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Upgraded Weekly Rounded Bar Chart
                    item {
                        WeeklyChartCard(sessions = uiState.sessionHistory)
                    }

                    // Monthly Rhythm Heatmap
                    item {
                        MonthlyHeatmapCard(sessions = uiState.sessionHistory)
                    }

                    // History Section Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.fasting_history),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = stringResource(R.string.tap_to_edit_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (uiState.sessionHistory.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, fastingColors.cardBorder)
                            ) {
                                Text(
                                    text = stringResource(R.string.no_history_yet),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(28.dp)
                                )
                            }
                        }
                    } else {
                        items(uiState.sessionHistory, key = { it.id ?: it.startTime.toEpochMilli() }) { session ->
                            HistoryRowCard(
                                session = session,
                                planName = uiState.plansById[session.planId]?.name ?: "16:8",
                                onClick = {
                                    editorValidationError = null
                                    editingSession = session
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet || editingSession != null) {
        SessionEditorSheet(
            existingSession = editingSession,
            plans = uiState.plans,
            validationError = editorValidationError,
            onClearError = { editorValidationError = null },
            onSave = { planId, start, end, mood, notes ->
                viewModel.saveSession(
                    sessionId = editingSession?.id,
                    planId = planId,
                    startTime = start,
                    endTime = end,
                    mood = mood,
                    notes = notes,
                    onSaved = {
                        showAddSheet = false
                        editingSession = null
                        editorValidationError = null
                    }
                )
            },
            onDelete = editingSession?.id?.let { id ->
                {
                    viewModel.deleteSession(id)
                    editingSession = null
                }
            },
            onDismiss = {
                showAddSheet = false
                editingSession = null
                editorValidationError = null
            }
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    emoji: String,
    modifier: Modifier = Modifier
) {
    val fastingColors = LocalFastingColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, fastingColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = emoji, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun WeeklyChartCard(sessions: List<FastingSession>) {
    val fastingColors = LocalFastingColors.current
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val last7Days = remember(today) { (6 downTo 0).map { today.minusDays(it.toLong()) } }

    val hoursPerDay = remember(sessions, last7Days) {
        last7Days.map { date ->
            sessions
                .filter { s -> s.endTime != null && s.startTime.atZone(zone).toLocalDate() == date }
                .sumOf { s -> Duration.between(s.startTime, s.endTime!!).toMinutes() / 60.0 }
        }
    }
    var selectedIdx by remember { mutableIntStateOf(6) }
    val maxHours = max(hoursPerDay.maxOrNull() ?: 0.0, 24.0)
    val goalHours = 16.0

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, fastingColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.weekly_overview),
                    style = MaterialTheme.typography.titleMedium
                )
                val selDate = last7Days[selectedIdx]
                val selDayName = selDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                Text(
                    text = "$selDayName · ${String.format(Locale.US, "%.1fh", hoursPerDay[selectedIdx])}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(144.dp)
            ) {
                // Dashed 16h Goal Reference Line
                val goalFraction = (1f - (goalHours / maxHours).toFloat()).coerceIn(0.05f, 0.95f)
                val primaryColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val y = size.height * goalFraction
                    drawLine(
                        color = primaryColor.copy(alpha = 0.4f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    )
                }

                // Rounded Bars
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    last7Days.forEachIndexed { idx, date ->
                        val hrs = hoursPerDay[idx]
                        val isSelected = idx == selectedIdx
                        val heightFraction = if (hrs <= 0.0) 0.04f else (hrs / maxHours).toFloat().coerceIn(0.06f, 1f)

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedIdx = idx },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                val barModifier = Modifier
                                    .width(22.dp)
                                    .height((112f * heightFraction).dp)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                                if (hrs <= 0.0) {
                                    Box(modifier = barModifier.background(fastingColors.ringTrack))
                                } else if (isSelected) {
                                    Box(
                                        modifier = barModifier.background(
                                            Brush.verticalGradient(
                                                listOf(fastingColors.fastGradientStart, fastingColors.fastGradientEnd)
                                            )
                                        )
                                    )
                                } else {
                                    Box(modifier = barModifier.background(fastingColors.fastSoftBg))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.weekly_target_label, goalHours.toInt()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonthlyHeatmapCard(sessions: List<FastingSession>) {
    val fastingColors = LocalFastingColors.current
    val zone = ZoneId.systemDefault()
    val currentMonth = remember { YearMonth.now(zone) }
    var displayedMonth by remember { mutableStateOf(currentMonth) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    val dailyHours = remember(sessions, displayedMonth) {
        sessions
            .filter { it.endTime != null }
            .groupBy { it.startTime.atZone(zone).toLocalDate() }
            .filterKeys { YearMonth.from(it) == displayedMonth }
            .mapValues { (_, list) ->
                list.sumOf { Duration.between(it.startTime, it.endTime!!).toMinutes() / 60.0 }
            }
    }

    val daysInMonth = displayedMonth.lengthOfMonth()
    val firstDayOffset = displayedMonth.atDay(1).dayOfWeek.value - 1 // Mon = 0
    val monthFormatter = remember { DateTimeFormatter.ofPattern("yyyy / MM") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, fastingColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.monthly_heatmap),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            displayedMonth = displayedMonth.minusMonths(1)
                            selectedDate = null
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = displayedMonth.format(monthFormatter),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = {
                            if (displayedMonth < currentMonth) {
                                displayedMonth = displayedMonth.plusMonths(1)
                                selectedDate = null
                            }
                        },
                        enabled = displayedMonth < currentMonth,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val totalCells = firstDayOffset + daysInMonth
            val rows = (totalCells + 6) / 7
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val dayNum = cellIndex - firstDayOffset + 1
                            if (dayNum in 1..daysInMonth) {
                                val date = displayedMonth.atDay(dayNum)
                                val hrs = dailyHours[date] ?: 0.0
                                val bg = when {
                                    hrs >= 16.0 -> fastingColors.heatmap3
                                    hrs >= 12.0 -> fastingColors.heatmap2
                                    hrs > 0.0 -> fastingColors.heatmap1
                                    else -> fastingColors.heatmap0
                                }
                                val isSel = selectedDate == date
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bg)
                                        .then(
                                            if (isSel) Modifier.border(
                                                2.dp,
                                                MaterialTheme.colorScheme.onSurface,
                                                RoundedCornerShape(8.dp)
                                            ) else Modifier
                                        )
                                        .clickable { selectedDate = if (isSel) null else date },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNum.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (hrs >= 12.0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                            }
                        }
                    }
                }
            }

            selectedDate?.let { date ->
                val hrs = dailyHours[date] ?: 0.0
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (hrs > 0.0) String.format(Locale.US, "%.1fh", hrs) else stringResource(R.string.heatmap_no_record),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hrs > 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRowCard(
    session: FastingSession,
    planName: String,
    onClick: () -> Unit
) {
    val fastingColors = LocalFastingColors.current
    val zone = ZoneId.systemDefault()
    val startZdt = session.startTime.atZone(zone)
    val endZdt = (session.endTime ?: Instant.now()).atZone(zone)
    val duration = Duration.between(session.startTime, session.endTime ?: Instant.now())
    val hours = duration.toHours()
    val mins = duration.toMinutes() % 60
    val dateStr = startZdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val timeRange = "${startZdt.format(DateTimeFormatter.ofPattern("HH:mm"))} → ${endZdt.format(DateTimeFormatter.ofPattern("HH:mm"))}"
    val moodEmoji = session.mood?.let { HISTORY_MOODS.getOrNull(it - 1) }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, fastingColors.cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = dateStr, style = MaterialTheme.typography.titleSmall)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Text(
                            text = planName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (moodEmoji != null) {
                        Text(text = moodEmoji, fontSize = 14.sp)
                    }
                }
                Text(
                    text = buildString {
                        append(timeRange)
                        if (!session.notes.isNullOrBlank()) {
                            append(" · ")
                            append(session.notes)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${hours}h ${mins}m",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (hours >= 16) {
                    Text(
                        text = "★ ${hours}h",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionEditorSheet(
    existingSession: FastingSession?,
    plans: List<Plan>,
    validationError: String?,
    onClearError: () -> Unit,
    onSave: (planId: Long, start: Instant, end: Instant, mood: Int?, notes: String?) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val fastingColors = LocalFastingColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val defaultEnd = remember { Instant.now().minusSeconds(60) }
    val defaultStart = remember { defaultEnd.minusSeconds(16L * 3600) }

    var startTime by remember(existingSession) { mutableStateOf(existingSession?.startTime ?: defaultStart) }
    var endTime by remember(existingSession) { mutableStateOf(existingSession?.endTime ?: defaultEnd) }
    var selectedPlanId by remember(existingSession, plans) {
        mutableStateOf(existingSession?.planId ?: plans.firstOrNull()?.id ?: 1L)
    }
    var selectedMood by remember(existingSession) { mutableStateOf(existingSession?.mood) }
    var notes by remember(existingSession) { mutableStateOf(existingSession?.notes ?: "") }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val fmt = remember { DateTimeFormatter.ofPattern("MM-dd HH:mm") }
    val zone = ZoneId.systemDefault()
    val durationMins = Duration.between(startTime, endTime).toMinutes()
    val durationText = if (durationMins > 0) "${durationMins / 60}h ${durationMins % 60}m" else "—"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(if (existingSession != null) R.string.edit_record_title else R.string.add_record_title),
                    style = MaterialTheme.typography.titleLarge
                )
                if (onDelete != null) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Start & End Pickers
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    onClick = { onClearError(); pickingStart = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.start_date_time), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(startTime.atZone(zone).format(fmt), style = MaterialTheme.typography.titleSmall)
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Surface(
                    onClick = { onClearError(); pickingEnd = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.end_date_time), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(endTime.atZone(zone).format(fmt), style = MaterialTheme.typography.titleSmall)
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Calculated Duration Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = fastingColors.fastSoftBg
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.calculated_duration), style = MaterialTheme.typography.labelMedium)
                    Text(durationText, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            // Plan Chips
            if (plans.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(plans, key = { it.id ?: 0L }) { plan ->
                        FilterChip(
                            selected = plan.id == selectedPlanId,
                            onClick = { plan.id?.let { selectedPlanId = it } },
                            label = { Text(plan.name) }
                        )
                    }
                }
            }

            // Optional Mood
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HISTORY_MOODS.forEachIndexed { idx, emoji ->
                    val moodVal = idx + 1
                    val selected = selectedMood == moodVal
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) fastingColors.fastSoftBg else MaterialTheme.colorScheme.surfaceContainer)
                            .border(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedMood = if (selected) null else moodVal },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 19.sp)
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = { Text(stringResource(R.string.note_optional_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (validationError != null) {
                Text(
                    text = validationError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

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
                    onClick = { onSave(selectedPlanId, startTime, endTime, selectedMood, notes.ifBlank { null }) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }

    if (pickingStart) {
        DateTimePickerFlow(
            initialInstant = startTime,
            onConfirm = { startTime = it; pickingStart = false },
            onDismiss = { pickingStart = false }
        )
    }
    if (pickingEnd) {
        DateTimePickerFlow(
            initialInstant = endTime,
            onConfirm = { endTime = it; pickingEnd = false },
            onDismiss = { pickingEnd = false }
        )
    }
    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            text = { Text(stringResource(R.string.delete_record_confirm)) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
