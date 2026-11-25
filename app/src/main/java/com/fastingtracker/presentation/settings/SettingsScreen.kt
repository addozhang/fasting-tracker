package com.fastingtracker.presentation.settings

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fastingtracker.BuildConfig
import com.fastingtracker.R
import com.fastingtracker.billing.DonationState
import com.fastingtracker.domain.model.AppLanguage
import com.fastingtracker.domain.model.AppTheme
import com.fastingtracker.domain.model.NotificationType
import com.fastingtracker.ui.theme.LocalFastingColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val donationState by viewModel.donationState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val fastingColors = LocalFastingColors.current

    var showLanguageSheet by remember { mutableStateOf(false) }
    var showDonationDialog by remember { mutableStateOf(false) }
    var showDisclaimerDialog by remember { mutableStateOf(false) }

    var hasNotifPermission by remember { mutableStateOf(checkNotifPermission(context)) }
    var canPostLiveUpdates by remember { mutableStateOf(checkLiveUpdatePermission(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotifPermission = checkNotifPermission(context)
                canPostLiveUpdates = checkLiveUpdatePermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportBackup(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importBackup(it) } }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsEffect.ExportSuccess ->
                    snackbarHostState.showSnackbar(context.getString(R.string.export_success))
                is SettingsEffect.ImportSuccess ->
                    snackbarHostState.showSnackbar(context.getString(R.string.import_success, effect.plans, effect.sessions))
                is SettingsEffect.BackupError ->
                    snackbarHostState.showSnackbar(context.getString(R.string.backup_error, effect.reason))
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineLarge
                    )
                }

                // Appearance Section
                item {
                    SettingsCard(title = stringResource(R.string.settings_appearance)) {
                        val themes = listOf(
                            AppTheme.SYSTEM to R.string.theme_system,
                            AppTheme.LIGHT to R.string.theme_light,
                            AppTheme.DARK to R.string.theme_dark
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            themes.forEachIndexed { index, (theme, labelRes) ->
                                SegmentedButton(
                                    selected = uiState.theme == theme,
                                    onClick = { viewModel.onThemeChange(theme) },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = themes.size)
                                ) {
                                    Text(stringResource(labelRes), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Spacer(modifier = Modifier.height(14.dp))
                            ToggleRow(
                                title = stringResource(R.string.dynamic_color_title),
                                description = stringResource(R.string.dynamic_color_desc),
                                checked = uiState.dynamicColor,
                                onCheckedChange = viewModel::onDynamicColorToggle
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = fastingColors.cardBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLanguageSheet = true }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.settings_language),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = when (uiState.language) {
                                        AppLanguage.SYSTEM -> stringResource(R.string.lang_system)
                                        AppLanguage.ENGLISH -> stringResource(R.string.lang_english)
                                        AppLanguage.CHINESE -> stringResource(R.string.lang_chinese)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Notifications Section
                item {
                    val allEnabled = uiState.notificationSettings.isNotEmpty() &&
                        uiState.notificationSettings.all { it.isEnabled }
                    val isOngoingEnabled = uiState.notificationSettings
                        .find { it.type == NotificationType.FASTING_START }?.isEnabled ?: true
                    val isMilestoneEnabled = uiState.notificationSettings
                        .find { it.type == NotificationType.PHASE_MILESTONE }?.isEnabled ?: true
                    val isCompleteEnabled = uiState.notificationSettings
                        .find { it.type == NotificationType.FASTING_END }?.isEnabled ?: true

                    SettingsCard(title = stringResource(R.string.settings_notifications)) {
                        if (!hasNotifPermission) {
                            PermissionPromptBanner(
                                title = stringResource(R.string.notif_permission_required),
                                message = stringResource(R.string.notif_permission_message),
                                onOpen = { openNotificationSettings(context, promoted = false) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        } else if (Build.VERSION.SDK_INT >= 36 && !canPostLiveUpdates) {
                            PermissionPromptBanner(
                                title = stringResource(R.string.live_update_hint_title),
                                message = stringResource(R.string.live_update_hint_desc),
                                onOpen = { openNotificationSettings(context, promoted = true) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        ToggleRow(
                            title = stringResource(R.string.notif_all_title),
                            description = null,
                            checked = allEnabled && hasNotifPermission,
                            enabled = hasNotifPermission,
                            onCheckedChange = viewModel::onToggleAllNotifications
                        )

                        if (hasNotifPermission) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = fastingColors.cardBorder)
                            Spacer(modifier = Modifier.height(10.dp))

                            ToggleRow(
                                title = stringResource(R.string.notif_ongoing_title),
                                description = stringResource(R.string.notif_ongoing_desc),
                                checked = isOngoingEnabled,
                                onCheckedChange = { viewModel.onNotificationToggle(NotificationType.FASTING_START, it) }
                            )
                            if (isOngoingEnabled) {
                                Column(
                                    modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (Build.VERSION.SDK_INT >= 36) {
                                        ToggleRow(
                                            title = stringResource(R.string.notif_super_island_title),
                                            description = stringResource(R.string.notif_super_island_desc),
                                            checked = uiState.superIslandEnabled,
                                            onCheckedChange = viewModel::onSuperIslandToggle
                                        )
                                    }
                                    ToggleRow(
                                        title = stringResource(R.string.notif_lock_screen_title),
                                        description = stringResource(R.string.notif_lock_screen_desc),
                                        checked = uiState.lockScreenVisible,
                                        onCheckedChange = viewModel::onLockScreenToggle
                                    )
                                    ToggleRow(
                                        title = stringResource(R.string.notif_minimal_title),
                                        description = stringResource(R.string.notif_minimal_desc),
                                        checked = uiState.minimalNotification,
                                        onCheckedChange = viewModel::onMinimalNotificationToggle
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            ToggleRow(
                                title = stringResource(R.string.notif_milestone_title),
                                description = stringResource(R.string.notif_milestone_desc),
                                checked = isMilestoneEnabled,
                                onCheckedChange = { viewModel.onNotificationToggle(NotificationType.PHASE_MILESTONE, it) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            ToggleRow(
                                title = stringResource(R.string.notif_complete_title),
                                description = stringResource(R.string.notif_complete_desc),
                                checked = isCompleteEnabled,
                                onCheckedChange = { viewModel.onNotificationToggle(NotificationType.FASTING_END, it) }
                            )
                        }
                    }
                }

                // Local Data Backup (SAF JSON)
                item {
                    SettingsCard(title = stringResource(R.string.settings_data)) {
                        Text(
                            text = stringResource(R.string.backup_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val stamp = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                                    exportLauncher.launch("fasting_backup_$stamp.json")
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.export_backup), style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.import_backup), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // Support Developer & About
                item {
                    SettingsCard(title = stringResource(R.string.settings_about)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.support_developer),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = stringResource(R.string.support_developer_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Surface(
                                onClick = { showDonationDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                color = fastingColors.fastSoftBg
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Favorite,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = stringResource(R.string.support_now),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = fastingColors.cardBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { openExternalUrl(context, "https://solocraft.app") }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.about_solocraft),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = stringResource(R.string.about_solocraft_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { openExternalUrl(context, "https://github.com/addozhang/fasting-tracker") }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.about_source_code),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = stringResource(R.string.about_source_code_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = fastingColors.cardBorder)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val flavorTag = if (BuildConfig.IS_FOSS) "FOSS" else "Play"
                            Text(
                                text = "${stringResource(R.string.app_version)} ${BuildConfig.VERSION_NAME} ($flavorTag)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { showDisclaimerDialog = true }) {
                                Text(stringResource(R.string.view_disclaimer), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    // Language BottomSheet
    if (showLanguageSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLanguageSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_language),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                val langs = listOf(
                    AppLanguage.SYSTEM to R.string.lang_system,
                    AppLanguage.ENGLISH to R.string.lang_english,
                    AppLanguage.CHINESE to R.string.lang_chinese
                )
                langs.forEach { (lang, labelRes) ->
                    val selected = uiState.language == lang
                    Surface(
                        onClick = {
                            viewModel.onLanguageChange(lang)
                            showLanguageSheet = false
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) fastingColors.fastSoftBg else MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(labelRes), style = MaterialTheme.typography.bodyLarge)
                            if (selected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDonationDialog) {
        AlertDialog(
            onDismissRequest = { showDonationDialog = false },
            title = { Text(stringResource(R.string.donation_dialog_title)) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.donation_dialog_message),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (BuildConfig.IS_FOSS) {
                        Text(
                            text = stringResource(R.string.donation_dialog_foss_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.donation_price),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDonationDialog = false
                    (context as? Activity)?.let { viewModel.startDonation(it) }
                }) {
                    Text(stringResource(R.string.support_now))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDonationDialog = false }) {
                    Text(stringResource(R.string.maybe_later))
                }
            }
        )
    }

    when (donationState) {
        is DonationState.Processing, is DonationState.Loading -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.donation_processing)) },
                text = {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                },
                confirmButton = {}
            )
        }
        is DonationState.Success -> {
            AlertDialog(
                onDismissRequest = viewModel::resetDonation,
                title = { Text(stringResource(R.string.donation_success_title)) },
                text = { Text(stringResource(R.string.donation_success_message)) },
                confirmButton = {
                    TextButton(onClick = viewModel::resetDonation) { Text(stringResource(R.string.ok)) }
                }
            )
        }
        is DonationState.Error -> {
            AlertDialog(
                onDismissRequest = viewModel::resetDonation,
                title = { Text(stringResource(R.string.donation_error_title)) },
                text = { Text(stringResource(R.string.donation_error_message)) },
                confirmButton = {
                    TextButton(onClick = viewModel::resetDonation) { Text(stringResource(R.string.ok)) }
                }
            )
        }
        is DonationState.Cancelled -> {
            LaunchedEffect(donationState) { viewModel.resetDonation() }
        }
        else -> Unit
    }

    if (showDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showDisclaimerDialog = false },
            title = { Text(stringResource(R.string.disclaimer_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.disclaimer_message),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = stringResource(R.string.privacy_first_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = stringResource(R.string.privacy_first_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDisclaimerDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    val fastingColors = LocalFastingColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, fastingColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    description: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun PermissionPromptBanner(
    title: String,
    message: String,
    onOpen: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.Warning, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
            }
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onOpen, shape = RoundedCornerShape(10.dp)) {
                Text(stringResource(R.string.enable_permission), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun checkNotifPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else true
}

private fun checkLiveUpdatePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= 36) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.canPostPromotedNotifications()
    } else true
}

private fun openNotificationSettings(context: Context, promoted: Boolean) {
    val intent = if (promoted && Build.VERSION.SDK_INT >= 36) {
        Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    } else {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    }
    runCatching { context.startActivity(intent) }.onFailure {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        )
    }
}

private fun openExternalUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        // Ignore if device has no browser installed
    }
}
