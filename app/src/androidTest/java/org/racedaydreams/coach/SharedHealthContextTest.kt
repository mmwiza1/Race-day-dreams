package org.racedaydreams.coach

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.theme.RaceDayTheme
import org.racedaydreams.domain.health.*

@RunWith(AndroidJUnit4::class)
class SharedHealthContextTest {
    @get:Rule val compose=createComposeRule()
    private val actions=mutableListOf<String>()
    private fun review()=RecoveryCoaching.Context(listOf("Comparable-source sleep and effort support reviewing today's demand."),listOf("Keep planned workout","Choose easy instead"),true)
    private fun showRecovery(large:Boolean=false) {
        compose.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,if(large)2f else 1f)) {
                RaceDayTheme(darkTheme=true,dynamicColour=false) {CoachPage {SharedRecoveryContextCard(review()){actions.add(it)}}}
            }
        }
    }
    @Test fun missingHealthStaysUnavailableWithSourceDetailsCollapsed() {
        val missing=HealthIntelligence.Trend(HealthIntelligence.Metric.RESTING_HR,"Unavailable",null,null,0,0,"Insufficient evidence","Optional data is missing.")
        compose.setContent {RaceDayTheme(dynamicColour=false){CoachPage{SharedHealthTrendCard(missing){it.toString()}}}}
        compose.onNodeWithText("Resting heart rate").assertIsDisplayed()
        compose.onNodeWithText("Not available").assertIsDisplayed()
        compose.onNodeWithText("Recent daily average").assertDoesNotExist()
        compose.onNodeWithText("View evidence & sources").assertIsDisplayed().performClick()
        compose.onNodeWithText("Optional data is missing.",substring=true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Hide details").performScrollTo().performClick()
        compose.onNodeWithText("Optional data is missing.",substring=true).assertDoesNotExist()
    }
    @Test fun recoveryPreviewCanBeKeptWithoutChangingTheWorkout() {
        showRecovery()
        compose.runOnIdle {assertEquals(emptyList<String>(),actions)}
        compose.onNodeWithText("Preview easy alternative").performScrollTo().performClick()
        compose.onNodeWithText("Replace only this session with easy running?").assertIsDisplayed()
        compose.onNodeWithText("Keep original").performClick()
        compose.onNodeWithText("Replace only this session with easy running?").assertDoesNotExist()
        compose.runOnIdle {assertEquals(emptyList<String>(),actions)}
    }
    @Test fun recoveryAt200PercentRequiresExplicitConfirmationAndKeepsTheReason() {
        showRecovery(large=true)
        compose.onNodeWithText("Keep the plan, run by effort, move or rest. Your goal stays unchanged.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Preview easy alternative").performScrollTo().performClick()
        compose.runOnIdle {assertEquals(emptyList<String>(),actions)}
        compose.onNodeWithText("Choose easy").performClick()
        compose.runOnIdle {assertEquals(review().evidence(),actions)}
        compose.onNodeWithText("Replace only this session with easy running?").assertDoesNotExist()
    }
}
