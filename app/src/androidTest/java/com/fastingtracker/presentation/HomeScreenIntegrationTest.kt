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
class HomeScreenIntegrationTest {

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
    fun app_launches_and_shows_timer_screen() {
        composeRule.onNodeWithText(s(R.string.start_fasting), useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun home_shows_3_tab_bottom_navigation() {
        composeRule.onNodeWithText(s(R.string.nav_timer)).assertIsDisplayed()
        composeRule.onNodeWithText(s(R.string.nav_stats)).assertIsDisplayed()
        composeRule.onNodeWithText(s(R.string.nav_settings)).assertIsDisplayed()
    }

    @Test
    fun tapping_plan_pill_opens_plan_management_sheet() {
        composeRule.onAllNodesWithText("16:8")[0].performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(s(R.string.plans_sheet_title)).assertIsDisplayed()
        composeRule.onAllNodesWithText("14:10").fetchSemanticsNodes().also { assert(it.isNotEmpty()) }
        composeRule.onAllNodesWithText("18:6").fetchSemanticsNodes().also { assert(it.isNotEmpty()) }
        composeRule.onAllNodesWithText("20:4").fetchSemanticsNodes().also { assert(it.isNotEmpty()) }
    }
}
