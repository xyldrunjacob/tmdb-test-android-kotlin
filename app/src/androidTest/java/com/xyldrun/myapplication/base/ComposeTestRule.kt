package com.xyldrun.myapplication.base

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule

open class ComposeTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    fun waitForIdle() {
        composeTestRule.waitForIdle()
    }
} 