package com.fastingtracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.fastingtracker.MainActivity
import com.fastingtracker.R
import com.fastingtracker.domain.model.FastingSubPhase
import com.fastingtracker.domain.repository.PlanRepository
import com.fastingtracker.domain.repository.SettingsRepository
import com.fastingtracker.domain.usecase.statistics.FastingProgress
import com.fastingtracker.domain.usecase.statistics.GetCurrentFastingProgressUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private data class WidgetSnapshot(
    val isFasting: Boolean,
    val planName: String,
    val phaseLabel: String,
    val elapsedText: String,
    val progressFraction: Float,
    val percentText: String,
    val startClock: String,
    val targetClock: String
)

class FastingGlanceWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun getCurrentFastingProgress(): GetCurrentFastingProgressUseCase
        fun planRepository(): PlanRepository
        fun settingsRepository(): SettingsRepository
    }

    companion object {
        private val COMPACT_SIZE = DpSize(120.dp, 120.dp)
        private val WIDE_SIZE = DpSize(240.dp, 120.dp)
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(COMPACT_SIZE, WIDE_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadSnapshot(context)
        provideContent {
            FastingWidgetContent(snapshot = snapshot)
        }
    }

    private suspend fun loadSnapshot(context: Context): WidgetSnapshot {
        val ep = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val progress: FastingProgress? = runCatching { ep.getCurrentFastingProgress().invoke() }.getOrNull()

        if (progress != null) {
            val zone = ZoneId.systemDefault()
            val fmt = DateTimeFormatter.ofPattern("HH:mm")
            val hours = progress.elapsedTimeSeconds / 3600L
            val mins = (progress.elapsedTimeSeconds % 3600L) / 60L
            val phaseLabel = when (progress.phaseMilestone) {
                FastingSubPhase.FAT_BURNING -> context.getString(R.string.phase_fat_burning)
                FastingSubPhase.KETOSIS -> context.getString(R.string.phase_ketosis)
                FastingSubPhase.DEEP_KETOSIS -> context.getString(R.string.phase_deep_ketosis)
                FastingSubPhase.AUTOPHAGY -> context.getString(R.string.phase_autophagy)
                FastingSubPhase.EXTENDED_FASTING -> context.getString(R.string.phase_deep_autophagy)
                FastingSubPhase.DEEP_AUTOPHAGY -> context.getString(R.string.phase_extended_reset)
                else -> context.getString(R.string.phase_settling)
            }
            return WidgetSnapshot(
                isFasting = true,
                planName = progress.planName,
                phaseLabel = phaseLabel,
                elapsedText = "${hours}h ${mins}m",
                progressFraction = (progress.progressPercent / 100f).coerceIn(0f, 1f),
                percentText = "${progress.progressPercent}%",
                startClock = progress.startTime.atZone(zone).format(fmt),
                targetClock = progress.expectedEndTime.atZone(zone).format(fmt)
            )
        }

        val selectedId = runCatching { ep.settingsRepository().getSelectedPlanId() }.getOrNull()
        val plans = runCatching { ep.planRepository().getAllPlans() }.getOrDefault(emptyList())
        val planName = plans.firstOrNull { it.id == selectedId }?.name
            ?: plans.firstOrNull()?.name
            ?: "16:8"

        return WidgetSnapshot(
            isFasting = false,
            planName = planName,
            phaseLabel = context.getString(R.string.eating_window_label),
            elapsedText = context.getString(R.string.widget_ready),
            progressFraction = 0f,
            percentText = "",
            startClock = "",
            targetClock = ""
        )
    }
}

@Composable
private fun FastingWidgetContent(snapshot: WidgetSnapshot) {
    val size = LocalSize.current
    val isWide = size.width >= 210.dp

    val surfaceBg = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF1D1C1A))
    val textPrimary = ColorProvider(day = Color(0xFF1F1D1A), night = Color(0xFFF2EFEA))
    val textMuted = ColorProvider(day = Color(0xFF706B63), night = Color(0xFF9E988E))
    val accent = if (snapshot.isFasting) {
        ColorProvider(day = Color(0xFFE06D3B), night = Color(0xFFEC7D4B))
    } else {
        ColorProvider(day = Color(0xFF4F826A), night = Color(0xFF69A086))
    }
    val track = ColorProvider(day = Color(0xFFEAE4DA), night = Color(0xFF2B2825))

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(surfaceBg)
            .clickable(actionStartActivity<MainActivity>())
            .padding(16.dp)
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = snapshot.planName,
                    style = TextStyle(color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                )
                if (isWide && snapshot.percentText.isNotEmpty()) {
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = snapshot.percentText,
                        style = TextStyle(color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            Text(
                text = snapshot.elapsedText,
                style = TextStyle(
                    color = textPrimary,
                    fontSize = if (isWide) 24.sp else 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                text = snapshot.phaseLabel,
                style = TextStyle(color = textMuted, fontSize = 12.sp)
            )

            if (snapshot.isFasting) {
                Spacer(modifier = GlanceModifier.height(10.dp))
                LinearProgressIndicator(
                    progress = snapshot.progressFraction,
                    modifier = GlanceModifier.fillMaxWidth().height(6.dp).cornerRadius(3.dp),
                    color = accent,
                    backgroundColor = track
                )
                if (isWide && snapshot.startClock.isNotEmpty()) {
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        Text(
                            text = snapshot.startClock,
                            style = TextStyle(color = textMuted, fontSize = 11.sp)
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = snapshot.targetClock,
                            style = TextStyle(color = textMuted, fontSize = 11.sp)
                        )
                    }
                }
            }
        }
    }
}
