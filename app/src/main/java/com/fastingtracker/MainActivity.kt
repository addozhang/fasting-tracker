package com.fastingtracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fastingtracker.domain.model.AppLanguage
import com.fastingtracker.navigation.NavGraph
import com.fastingtracker.navigation.SettingsRoute
import com.fastingtracker.navigation.StatsRoute
import com.fastingtracker.navigation.TimerRoute
import com.fastingtracker.notification.NotificationHelper
import com.fastingtracker.presentation.AppViewModel
import com.fastingtracker.ui.theme.FastingTrackerTheme
import com.fastingtracker.ui.theme.LocalFastingColors
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val appViewModel: AppViewModel by viewModels()
    private var endFastTriggerCount by mutableIntStateOf(0)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = getSystemService(android.content.pm.ShortcutManager::class.java)
            val shortcut = android.content.pm.ShortcutInfo.Builder(this, "start_fasting")
                .setShortLabel(getString(R.string.shortcut_start_fast_short))
                .setLongLabel(getString(R.string.shortcut_start_fast_long))
                .setIcon(android.graphics.drawable.Icon.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(Intent(this, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                })
                .build()
            shortcutManager?.dynamicShortcuts = listOf(shortcut)
        }

        setContent {
            val appTheme by appViewModel.appTheme.collectAsStateWithLifecycle()
            val dynamicColor by appViewModel.dynamicColor.collectAsStateWithLifecycle()
            val appLanguage by appViewModel.appLanguage.collectAsStateWithLifecycle()
            val showDisclaimer by appViewModel.showDisclaimer.collectAsStateWithLifecycle()

            LaunchedEffect(appLanguage) {
                applyAppLocale(appLanguage)
            }

            FastingTrackerTheme(appTheme = appTheme, dynamicColor = dynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        endFastTriggerCount = endFastTriggerCount,
                        onEndFastTriggerConsumed = { endFastTriggerCount = 0 }
                    )

                    if (showDisclaimer) {
                        AlertDialog(
                            onDismissRequest = { /* non-dismissable */ },
                            title = {
                                Text(
                                    text = stringResource(R.string.disclaimer_title),
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Text(
                                    text = stringResource(R.string.disclaimer_message),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = { appViewModel.acceptDisclaimer() }) {
                                    Text(stringResource(R.string.disclaimer_accept))
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appViewModel.onAppResumed()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == NotificationHelper.ACTION_END_FAST) {
            endFastTriggerCount++
            intent.action = null
        }
    }

    private fun applyAppLocale(language: AppLanguage) {
        val targetTags = when (language) {
            AppLanguage.SYSTEM -> ""
            AppLanguage.ENGLISH -> "en"
            AppLanguage.CHINESE -> "zh"
        }
        val currentTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (currentTags != targetTags) {
            val locales = if (targetTags.isEmpty()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(targetTags)
            }
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }
}

private data class TopLevelDestination(
    val route: Any,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun MainScreen(
    endFastTriggerCount: Int = 0,
    onEndFastTriggerConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val fastingColors = LocalFastingColors.current

    val destinations = listOf(
        TopLevelDestination(TimerRoute, R.string.nav_timer, Icons.Rounded.Timer, Icons.Outlined.Timer),
        TopLevelDestination(StatsRoute, R.string.nav_stats, Icons.Rounded.BarChart, Icons.Outlined.BarChart),
        TopLevelDestination(SettingsRoute, R.string.nav_settings, Icons.Rounded.Settings, Icons.Outlined.Settings)
    )

    // If user tapped "End Fast" from Super Island / notification, jump to Timer tab
    LaunchedEffect(endFastTriggerCount) {
        if (endFastTriggerCount > 0) {
            navController.navigate(TimerRoute) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                HorizontalDivider(thickness = 1.dp, color = fastingColors.cardBorder)
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    destinations.forEach { dest ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.hasRoute(dest.route::class)
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = stringResource(dest.labelRes)
                                )
                            },
                            label = { Text(stringResource(dest.labelRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = fastingColors.fastSoftBg,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            innerPadding = innerPadding,
            endFastTriggerCount = endFastTriggerCount,
            onEndFastTriggerConsumed = onEndFastTriggerConsumed
        )
    }
}
