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
class StatsScreenIntegrationTest {

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
        composeRule.onAllNodesWithText(s(R.string.nav_stats)).onFirst().performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun stats_screen_displays_summary_chart_and_heatmap() {
        composeRule.onNodeWithText(s(R.string.total_fasts)).assertIsDisplayed()
        composeRule.onNodeWithText(s(R.string.weekly_overview)).assertIsDisplayed()
        composeRule.onNodeWithText(s(R.string.monthly_heatmap)).assertIsDisplayed()
    }

    @Test
    fun manual_log_past_fast_opens_editor_and_saves() {
        composeRule.onNodeWithText(s(R.string.log_past_fast)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(s(R.string.add_record_title)).assertIsDisplayed()
        composeRule.onNodeWithText("16h 0m").assertIsDisplayed()

        composeRule.onNodeWithText(s(R.string.save)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(s(R.string.total_fasts)).assertIsDisplayed()
    }
}
