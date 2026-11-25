package com.fastingtracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.fastingtracker.domain.model.AppTheme

private val OrganicLightColorScheme = lightColorScheme(
    primary = AmberCoralLight,
    onPrimary = Color.White,
    primaryContainer = AmberContainerLight,
    onPrimaryContainer = OnAmberContainerLight,
    secondary = SageGreenLight,
    onSecondary = Color.White,
    secondaryContainer = SageContainerLight,
    onSecondaryContainer = OnSageContainerLight,
    tertiary = MilestoneGoldLight,
    onTertiary = Color.White,
    tertiaryContainer = MilestoneContainerLight,
    onTertiaryContainer = OnMilestoneContainerLight,
    error = DangerLight,
    onError = Color.White,
    errorContainer = DangerContainerLight,
    onErrorContainer = OnDangerContainerLight,
    background = WarmAlabaster,
    onBackground = InkCharcoal,
    surface = WarmAlabaster,
    onSurface = InkCharcoal,
    surfaceVariant = SandSurfaceLow,
    onSurfaceVariant = WarmMutedText,
    surfaceContainerLowest = PureWarmWhite,
    surfaceContainerLow = PureWarmWhite,
    surfaceContainer = SandSurfaceLow,
    surfaceContainerHigh = SandSurfaceHigh,
    outlineVariant = SubtleBorderLight
)

private val OrganicDarkColorScheme = darkColorScheme(
    primary = AmberCoralDark,
    onPrimary = Color(0xFF1F120A),
    primaryContainer = AmberContainerDark,
    onPrimaryContainer = OnAmberContainerDark,
    secondary = SageGreenDark,
    onSecondary = Color(0xFF0D2118),
    secondaryContainer = SageContainerDark,
    onSecondaryContainer = OnSageContainerDark,
    tertiary = MilestoneGoldDark,
    onTertiary = Color(0xFF281C06),
    tertiaryContainer = MilestoneContainerDark,
    onTertiaryContainer = OnMilestoneContainerDark,
    error = DangerDark,
    onError = Color(0xFF2D0D0A),
    errorContainer = DangerContainerDark,
    onErrorContainer = OnDangerContainerDark,
    background = DeepCharcoal,
    onBackground = WarmIvory,
    surface = DeepCharcoal,
    onSurface = WarmIvory,
    surfaceVariant = CharcoalSurfaceLow,
    onSurfaceVariant = MutedStoneDark,
    surfaceContainerLowest = Color(0xFF181715),
    surfaceContainerLow = CharcoalSurface,
    surfaceContainer = CharcoalSurfaceLow,
    surfaceContainerHigh = CharcoalSurfaceHigh,
    outlineVariant = SubtleBorderDark
)

@Composable
fun FastingTrackerTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> OrganicDarkColorScheme
        else -> OrganicLightColorScheme
    }

    val extendedColors = if (darkTheme) DarkFastingColors else LightFastingColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalFastingColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
