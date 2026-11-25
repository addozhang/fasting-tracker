package com.fastingtracker.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.fastingtracker.MainActivity

/**
 * Dismiss the first-launch disclaimer dialog if it's showing.
 */
fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.dismissDisclaimerIfShown() {
    for (label in listOf("I Understand", "我已知晓")) {
        try {
            onNodeWithText(label, ignoreCase = true, substring = true).performClick()
            waitForIdle()
            return
        } catch (_: AssertionError) {
        }
    }
}

/**
 * Wait for the Timer screen to finish loading and ensure it is in Idle ("Start Fast") state.
 * If an active fast exists from a previous test, ends/discards it via the v2.0 End Fast sheet.
 */
fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.waitForStartFast() {
    waitUntil(timeoutMillis = 15_000) {
        hasText("Start Fast") || hasText("开始断食") || hasText("End Fast") || hasText("结束断食")
    }

    if (hasText("End Fast") || hasText("结束断食")) {
        clickAny("End Fast", "结束断食")
        waitForIdle()
        // End Fast confirmation sheet: <1h shows "Discard Fast", >=1h shows "Save Fast"
        waitUntil(timeoutMillis = 5_000) {
            hasText("Discard Fast") || hasText("放弃本次计时") || hasText("Save Fast") || hasText("保存本次记录")
        }
        clickAny("Discard Fast", "放弃本次计时", "Save Fast", "保存本次记录")
        waitForIdle()
        waitUntil(timeoutMillis = 10_000) {
            hasText("Start Fast") || hasText("开始断食")
        }
    }
}

fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.hasText(text: String): Boolean {
    return try {
        onNodeWithText(text, ignoreCase = true, substring = true, useUnmergedTree = true)
            .assertIsDisplayed()
        true
    } catch (_: AssertionError) {
        false
    }
}

fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.clickAny(vararg candidates: String) {
    for (label in candidates) {
        try {
            onNodeWithText(label, ignoreCase = true, substring = true, useUnmergedTree = true)
                .performClick()
            return
        } catch (_: AssertionError) {
        }
    }
}
