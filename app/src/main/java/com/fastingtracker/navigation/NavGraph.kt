package com.fastingtracker.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.fastingtracker.presentation.home.HomeScreen
import com.fastingtracker.presentation.progress.ProgressScreen
import com.fastingtracker.presentation.settings.SettingsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues = PaddingValues(),
    endFastTriggerCount: Int = 0,
    onEndFastTriggerConsumed: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = TimerRoute,
        modifier = Modifier.padding(innerPadding),
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
    ) {
        composable<TimerRoute> {
            HomeScreen(
                endFastTriggerCount = endFastTriggerCount,
                onEndFastTriggerConsumed = onEndFastTriggerConsumed
            )
        }
        composable<StatsRoute> {
            ProgressScreen()
        }
        composable<SettingsRoute> {
            SettingsScreen()
        }
    }
}
