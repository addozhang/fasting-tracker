package com.fastingtracker.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
class FastingFlowIntegrationTest {

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
    fun one_tap_start_and_short_fast_discard_flow() {
        composeRule.onNodeWithText(s(R.string.start_fasting), useUnmergedTree = true).performClick()

        composeRule.waitUntil(timeoutMillis = 8_000) { composeRule.hasText(s(R.string.end_fast)) }

        composeRule.onNodeWithText(s(R.string.end_fast), useUnmergedTree = true).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { composeRule.hasText(s(R.string.end_fast_short_title)) }
        composeRule.onNodeWithText(s(R.string.discard_fast)).performClick()

        composeRule.waitUntil(timeoutMillis = 8_000) { composeRule.hasText(s(R.string.start_fasting)) }
        composeRule.onNodeWithText(s(R.string.start_fasting), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun adjust_start_time_2h_ago_and_save_fast_flow() {
        composeRule.onNodeWithText(s(R.string.adjust_start_time)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(s(R.string.quick_2h_ago)).performClick()

        composeRule.waitUntil(timeoutMillis = 8_000) { composeRule.hasText(s(R.string.end_fast)) }

        composeRule.onNodeWithText(s(R.string.end_fast), useUnmergedTree = true).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { composeRule.hasText(s(R.string.save_fast)) }
        composeRule.onNodeWithText(s(R.string.save_fast)).performClick()

        composeRule.waitUntil(timeoutMillis = 8_000) { composeRule.hasText(s(R.string.start_fasting)) }
    }
}
