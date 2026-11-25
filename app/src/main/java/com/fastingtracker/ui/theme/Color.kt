package com.fastingtracker.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Organic Minimal — Light Palette
val WarmAlabaster = Color(0xFFFAF8F5)
val PureWarmWhite = Color(0xFFFFFFFF)
val SandSurfaceLow = Color(0xFFF3EFE8)
val SandSurfaceHigh = Color(0xFFEAE4DA)
val InkCharcoal = Color(0xFF1F1D1A)
val WarmMutedText = Color(0xFF706B63)
val SubtleBorderLight = Color(0x141F1D1A)

val AmberCoralLight = Color(0xFFE06D3B)
val AmberGoldLight = Color(0xFFF2A65A)
val AmberContainerLight = Color(0xFFFCE8DC)
val OnAmberContainerLight = Color(0xFF6B2A0F)

val SageGreenLight = Color(0xFF4F826A)
val SageEndLight = Color(0xFF82B29A)
val SageContainerLight = Color(0xFFDFECE5)
val OnSageContainerLight = Color(0xFF1C3F2F)

val MilestoneGoldLight = Color(0xFFC88A2B)
val MilestoneContainerLight = Color(0xFFF9EED7)
val OnMilestoneContainerLight = Color(0xFF5A3B08)

val DangerLight = Color(0xFFC2463A)
val DangerContainerLight = Color(0xFFFBE4E2)
val OnDangerContainerLight = Color(0xFF6D1B14)

// Organic Minimal — Dark Palette
val DeepCharcoal = Color(0xFF131211)
val CharcoalSurface = Color(0xFF1D1C1A)
val CharcoalSurfaceLow = Color(0xFF252320)
val CharcoalSurfaceHigh = Color(0xFF2F2C28)
val WarmIvory = Color(0xFFF2EFEA)
val MutedStoneDark = Color(0xFF9E988E)
val SubtleBorderDark = Color(0x17F2EFEA)

val AmberCoralDark = Color(0xFFEC7D4B)
val AmberGoldDark = Color(0xFFF5B46E)
val AmberContainerDark = Color(0xFF3A2318)
val OnAmberContainerDark = Color(0xFFF8C7AC)

val SageGreenDark = Color(0xFF69A086)
val SageEndDark = Color(0xFF95C4AD)
val SageContainerDark = Color(0xFF1E3228)
val OnSageContainerDark = Color(0xFFBBE1CD)

val MilestoneGoldDark = Color(0xFFE3AA4F)
val MilestoneContainerDark = Color(0xFF382A14)
val OnMilestoneContainerDark = Color(0xFFF5D8A5)

val DangerDark = Color(0xFFE06C60)
val DangerContainerDark = Color(0xFF3B1E1B)
val OnDangerContainerDark = Color(0xFFF6B7B0)

@Immutable
data class FastingExtendedColors(
    val fastGradientStart: Color,
    val fastGradientEnd: Color,
    val fastSoftBg: Color,
    val eatGradientStart: Color,
    val eatGradientEnd: Color,
    val eatSoftBg: Color,
    val ringTrack: Color,
    val cardBorder: Color,
    val heatmap0: Color,
    val heatmap1: Color,
    val heatmap2: Color,
    val heatmap3: Color
)

val LightFastingColors = FastingExtendedColors(
    fastGradientStart = AmberCoralLight,
    fastGradientEnd = AmberGoldLight,
    fastSoftBg = Color(0x1FE06D3B),
    eatGradientStart = SageGreenLight,
    eatGradientEnd = SageEndLight,
    eatSoftBg = Color(0x1F4F826A),
    ringTrack = SandSurfaceHigh,
    cardBorder = SubtleBorderLight,
    heatmap0 = Color(0xFFEFEAE1),
    heatmap1 = Color(0xFFF6D5B8),
    heatmap2 = Color(0xFFEDA36C),
    heatmap3 = Color(0xFFDC6A38)
)

val DarkFastingColors = FastingExtendedColors(
    fastGradientStart = AmberCoralDark,
    fastGradientEnd = AmberGoldDark,
    fastSoftBg = Color(0x29EC7D4B),
    eatGradientStart = SageGreenDark,
    eatGradientEnd = SageEndDark,
    eatSoftBg = Color(0x2969A086),
    ringTrack = Color(0xFF2B2825),
    cardBorder = SubtleBorderDark,
    heatmap0 = Color(0xFF262421),
    heatmap1 = Color(0xFF5A3A28),
    heatmap2 = Color(0xFFA25731),
    heatmap3 = Color(0xFFE67947)
)

val LocalFastingColors = staticCompositionLocalOf { LightFastingColors }
