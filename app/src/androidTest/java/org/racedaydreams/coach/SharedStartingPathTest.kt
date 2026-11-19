package org.racedaydreams.coach

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.theme.RaceDayTheme

@RunWith(AndroidJUnit4::class)
class SharedStartingPathTest {
    @get:Rule val compose = createComposeRule()
    private val actions = mutableListOf<String>()

    private fun show(dark: Boolean = false, large: Boolean = false) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, if (large) 2f else 1f)
            ) {
                RaceDayTheme(darkTheme = dark, dynamicColour = false) {
                    SharedStartingPath(
                        { actions.add("record") },
                        { actions.add("race") },
                        { actions.add("strength") },
                        { actions.add("hybrid") },
                    )
                }
            }
        }
    }

    @Test
    fun allOriginalChoicesHaveDistinctActionsAndDoNotActBeforeAthleteChooses() {
        show()
        compose.onNodeWithText("Make today yours").assertIsDisplayed()
        compose.runOnIdle { assertTrue(actions.isEmpty()) }
        for (label in
            listOf(
                "Record my runs",
                "Train for a race",
                "Focus on strength",
                "Running + strength",
            )) compose.onNodeWithText(label).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf("record", "race", "strength", "hybrid"), actions) }
    }

    @Test
    fun dark200PercentKeepsStandaloneStrengthExplanationAndAllActionsAccessible() {
        show(dark = true, large = true)
        compose
            .onNodeWithText(
                "Set up optional strength programming or log your own workouts. No race goal or running profile required."
            )
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Focus on strength").performScrollTo().performClick()
        compose.onNodeWithText("Running + strength").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Record my runs").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(listOf("strength"), actions) }
    }
}
