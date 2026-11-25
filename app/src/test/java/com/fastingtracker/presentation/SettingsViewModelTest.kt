package com.fastingtracker.presentation

import app.cash.turbine.test
import com.fastingtracker.billing.DonationManager
import com.fastingtracker.billing.DonationState
import com.fastingtracker.data.backup.LocalBackupManager
import com.fastingtracker.domain.coordinator.FastingSessionCoordinator
import com.fastingtracker.domain.model.AppLanguage
import com.fastingtracker.domain.model.AppTheme
import com.fastingtracker.domain.model.NotificationSetting
import com.fastingtracker.domain.model.NotificationType
import com.fastingtracker.domain.repository.NotificationRepository
import com.fastingtracker.domain.repository.SettingsRepository
import com.fastingtracker.domain.usecase.settings.GetLanguageUseCase
import com.fastingtracker.domain.usecase.settings.GetThemeUseCase
import com.fastingtracker.domain.usecase.settings.SetLanguageUseCase
import com.fastingtracker.domain.usecase.settings.SetThemeUseCase
import com.fastingtracker.presentation.settings.SettingsViewModel
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `combines settings flows and syncs surfaces when toggling notification`() = runTest {
        val getTheme = mockk<GetThemeUseCase>()
        val setTheme = mockk<SetThemeUseCase>(relaxed = true)
        val getLang = mockk<GetLanguageUseCase>()
        val setLang = mockk<SetLanguageUseCase>(relaxed = true)
        val settingsRepo = mockk<SettingsRepository>(relaxed = true)
        val notifRepo = mockk<NotificationRepository>(relaxed = true)
        val coordinator = mockk<FastingSessionCoordinator>(relaxed = true)
        val backup = mockk<LocalBackupManager>(relaxed = true)
        val donation = mockk<DonationManager>(relaxed = true)

        val setting = NotificationSetting(1L, NotificationType.FASTING_START, "Ongoing", "", 0, true, Instant.now(), Instant.now())
        every { getTheme() } returns flowOf(AppTheme.LIGHT)
        every { settingsRepo.observeDynamicColor() } returns flowOf(false)
        every { settingsRepo.observeSuperIslandEnabled() } returns flowOf(true)
        every { settingsRepo.observeLockScreenVisible() } returns flowOf(false)
        every { settingsRepo.observeMinimalNotification() } returns flowOf(true)
        every { getLang() } returns flowOf(AppLanguage.ENGLISH)
        every { notifRepo.observeAllSettings() } returns flowOf(listOf(setting))
        every { donation.state } returns MutableStateFlow(DonationState.Idle)

        val vm = SettingsViewModel(
            getTheme, setTheme, getLang, setLang, settingsRepo, notifRepo, coordinator, backup, donation
        )

        vm.uiState.test {
            skipItems(1)
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(AppTheme.LIGHT, state.theme)
            assertFalse(state.dynamicColor)
            cancelAndIgnoreRemainingEvents()
        }

        vm.onNotificationToggle(NotificationType.FASTING_START, false)
        advanceUntilIdle()
        coVerify(exactly = 1) { notifRepo.upsertSetting(match { it.type == NotificationType.FASTING_START && !it.isEnabled }) }
        coVerify(exactly = 1) { coordinator.syncActiveSurfaces(false, any()) }
    }
}
