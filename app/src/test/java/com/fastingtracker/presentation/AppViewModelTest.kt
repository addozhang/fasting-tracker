package com.fastingtracker.presentation

import com.fastingtracker.domain.coordinator.FastingSessionCoordinator
import com.fastingtracker.domain.model.AppLanguage
import com.fastingtracker.domain.model.AppTheme
import com.fastingtracker.domain.repository.SettingsRepository
import com.fastingtracker.domain.usecase.settings.GetLanguageUseCase
import com.fastingtracker.domain.usecase.settings.GetThemeUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

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
    fun `initializes disclaimer and theme state from repositories`() = runTest {
        val getTheme = mockk<GetThemeUseCase>()
        val getLang = mockk<GetLanguageUseCase>()
        val settingsRepo = mockk<SettingsRepository>(relaxed = true)
        val coordinator = mockk<FastingSessionCoordinator>(relaxed = true)

        every { getTheme() } returns flowOf(AppTheme.DARK)
        every { getLang() } returns flowOf(AppLanguage.CHINESE)
        every { settingsRepo.observeDynamicColor() } returns flowOf(false)
        coEvery { settingsRepo.hasAcceptedDisclaimer() } returns false

        val vm = AppViewModel(getTheme, getLang, settingsRepo, coordinator)
        advanceUntilIdle()

        assertEquals(AppTheme.DARK, vm.appTheme.value)
        assertEquals(AppLanguage.CHINESE, vm.appLanguage.value)
        assertFalse(vm.dynamicColor.value)
        assertTrue(vm.showDisclaimer.value)

        vm.acceptDisclaimer()
        advanceUntilIdle()
        assertFalse(vm.showDisclaimer.value)
        coVerify(exactly = 1) { settingsRepo.setDisclaimerAccepted() }
    }
}
