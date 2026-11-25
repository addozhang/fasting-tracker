package com.fastingtracker.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
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
class NavigationIntegrationTest {

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
    }

    @Test
    fun navigate_through_all_3_tabs_sequentially() {
        // Timer -> Stats
        composeRule.onAllNodesWithText(s(R.string.nav_stats)).onFirst().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(s(R.string.total_fasts)).assertIsDisplayed()

        // Stats -> Settings
        composeRule.onAllNodesWithText(s(R.string.nav_settings)).onFirst().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(s(R.string.settings_appearance)).assertIsDisplayed()

        // Settings -> Timer
        composeRule.onAllNodesWithText(s(R.string.nav_timer)).onFirst().performClick()
        composeRule.waitForStartFast()
        composeRule.onNodeWithText(s(R.string.start_fasting), useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
