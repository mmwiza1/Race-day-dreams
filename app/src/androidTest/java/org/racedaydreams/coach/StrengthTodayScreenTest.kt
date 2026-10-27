package org.racedaydreams.coach

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A Strength-only installation remains useful without running setup or a race goal. */
@RunWith(AndroidJUnit4::class)
class StrengthTodayScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun standaloneEmptyStateHasWorkingProgrammeAndIndependentLoggingActions() {
        var destination: String? = null
        compose.setContent {
            MaterialTheme { StrengthTodayScreen(CoachUiState(loading = false), { destination = it }) }
        }
        compose.onNodeWithText("Your strength training").assertIsDisplayed()
        compose.onNodeWithText("Create a gym programme").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("gym", destination) }
        compose.onNodeWithText("Log a spontaneous workout").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("strength/log", destination) }
        compose.onAllNodesWithText("Run now").assertCountEquals(0)
        compose.onAllNodesWithText("Review running baseline").assertCountEquals(0)
    }

    @Test
    fun largeTextDarkModeRetainsIndependentTrainingAndProgressActions() {
        var now = 0
        var destination: String? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    StrengthTodayScreen(CoachUiState(loading = false), { destination = it }, { now++ })
                }
            }
        }
        // The first occurrence is the card heading; the second is its accessible button label.
        compose.onAllNodesWithText("Gym now")[1].performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, now) }
        compose.onNodeWithText("PBs & progression").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("strength/pbs", destination) }
        compose.onNodeWithText("Exercises & technique").performScrollTo().assertIsDisplayed()
    }
}
