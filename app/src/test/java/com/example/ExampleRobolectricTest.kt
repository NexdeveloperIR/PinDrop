package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.PinDropTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PIN DROP", appName)
    }

    @Test
    fun `splash screen navigates to main menu and starts gameplay`() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            PinDropTheme {
                PinDropApp()
            }
        }
        composeTestRule.mainClock.advanceTimeBy(100)
        composeTestRule.onNodeWithTag("splash_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("splash_screen").performClick()

        composeTestRule.mainClock.advanceTimeBy(350)
        composeTestRule.onNodeWithTag("main_menu_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("play_button").performClick()

        composeTestRule.mainClock.advanceTimeBy(350)
        composeTestRule.onNodeWithTag("gameplay_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gameplay_stage_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gameplay_speed_text").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pause_button").performClick()

        composeTestRule.mainClock.advanceTimeBy(250)
        composeTestRule.onNodeWithTag("pause_overlay").assertIsDisplayed()
    }

    @Test
    fun `main activity launches without crashing`() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        org.junit.Assert.assertNotNull(activity)
    }

    @Test
    fun `sound manager initializes and settings sound toggle works`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val soundManager = com.example.audio.SoundManager(context)
        soundManager.playClick()
        soundManager.playCollision()
        soundManager.release()

        var toggledTo: Boolean? = null
        composeTestRule.setContent {
            PinDropTheme {
                com.example.ui.screens.SettingsScreen(
                    stats = com.example.model.PlayerStats(soundEnabled = true),
                    onToggleSound = { toggledTo = it },
                    onToggleHaptics = {},
                    onResetStats = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_sound_switch").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_sound_switch").performClick()
        composeTestRule.waitForIdle()
        assertEquals(false, toggledTo)
    }
}
