package org.racedaydreams.coach

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.theme.RaceDayTheme
import org.racedaydreams.domain.recording.RunMetricsEngine

@RunWith(AndroidJUnit4::class)
class ActiveRunPanelTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun finishRequiresExplicitConfirmationAndCanBeCancelled() {
        val commands = mutableListOf<String>()
        compose.setContent {
            RaceDayTheme(dynamicColour = false) {
                ActiveRunPanel(
                    RunMetricsEngine.Status(5000.0, 1800000, 1500000, 300.0, "GPS ready", false),
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    commands::add,
                )
            }
        }
        compose.onNodeWithText("Finish & save").performClick()
        compose.runOnIdle { assertTrue(commands.isEmpty()) }
        compose.onNodeWithText("Keep recording").performClick()
        compose.runOnIdle { assertTrue(commands.isEmpty()) }
        compose.onNodeWithText("Pause").performClick()
        compose.runOnIdle { assertEquals(listOf("PAUSE"), commands) }
        compose.onNodeWithText("Finish & save").performClick()
        compose.onNodeWithText("Save recorded run").performClick()
        compose.runOnIdle { assertEquals(listOf("PAUSE", "FINISH"), commands) }
    }

    @Test
    fun largeTextKeepsControlsVisibleAndDoesNotInventWeakSignalPace() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                RaceDayTheme(darkTheme = true, dynamicColour = false) {
                    ActiveRunPanel(
                        RunMetricsEngine.Status(0.0, 60000, 60000, 300.0, "GPS signal weak", true),
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        {},
                    )
                }
            }
        }
        compose.onNodeWithText("Resume").assertIsDisplayed()
        compose.onNodeWithText("Finish & save").assertIsDisplayed()
        compose.onNodeWithText("Pace unavailable").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Active average pace").assertDoesNotExist()
        compose.onNodeWithText("GPS signal weak").performScrollTo().assertIsDisplayed()
    }
}
