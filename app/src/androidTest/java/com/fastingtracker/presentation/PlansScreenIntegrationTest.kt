package com.fastingtracker.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
class PlansScreenIntegrationTest {

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
        composeRule.onAllNodesWithText("16:8")[0].performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun plan_sheet_shows_daily_and_extended_presets() {
        composeRule.onNodeWithText(s(R.string.plans_sheet_title)).assertIsDisplayed()
        composeRule.onAllNodesWithText("14:10").fetchSemanticsNodes().also { assert(it.isNotEmpty()) }
        composeRule.onAllNodesWithText("16:8").fetchSemanticsNodes().also { assert(it.isNotEmpty()) }
        composeRule.onAllNodesWithText("18:6").fetchSemanticsNodes().also { assert(it.isNotEmpty()) }
        composeRule.onAllNodesWithText("20:4").fetchSemanticsNodes().also { assert(it.isNotEmpty()) }
    }

    @Test
    fun plan_sheet_shows_new_plan_button() {
        composeRule.onNodeWithText(s(R.string.new_custom_plan)).assertIsDisplayed()
    }
}
