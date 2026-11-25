package com.fastingtracker.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import com.fastingtracker.MainActivity
import com.fastingtracker.R
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SettingsScreenIntegrationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 2)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.POST_NOTIFICATIONS
    )

    private fun s(resId: Int) = composeRule.activity.getString(resId)

    @Before
    fun setup() {
        hiltRule.inject()
        composeRule.dismissDisclaimerIfShown()
        composeRule.waitForStartFast()
        composeRule.onAllNodesWithText(s(R.string.nav_settings)).onFirst().performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun settings_displays_segmented_theme_and_language_sheet() {
        composeRule.onNodeWithText(s(R.string.settings_appearance)).assertIsDisplayed()
        composeRule.onNodeWithText(s(R.string.theme_system)).assertIsDisplayed()
        composeRule.onNodeWithText(s(R.string.theme_light)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(s(R.string.theme_system)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(s(R.string.settings_language)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(s(R.string.lang_english)).assertIsDisplayed()
        composeRule.onAllNodesWithText(s(R.string.lang_system)).onLast().performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun settings_displays_notifications_section() {
        composeRule.onNodeWithText(s(R.string.settings_notifications)).assertIsDisplayed()
        composeRule.onNodeWithText(s(R.string.notif_ongoing_title)).assertIsDisplayed()
    }
}
