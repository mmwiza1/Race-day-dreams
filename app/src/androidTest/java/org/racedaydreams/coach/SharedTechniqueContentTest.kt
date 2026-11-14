package org.racedaydreams.coach

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import org.junit.Rule
import org.junit.Test
import org.racedaydreams.coach.theme.RaceDayTheme
import org.racedaydreams.domain.strength.TechniqueIllustrations

/** The production Android wrapper renders shared guidance with the actual licensed local image. */
class SharedTechniqueContentTest {
    @get:Rule val compose = createComposeRule()

    private fun show(dark: Boolean = false, large: Boolean = false, id: String = "bench-press") {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (large) 2f else 1f)) {
                RaceDayTheme(darkTheme = dark, dynamicColour = false) {
                    CoachPage { TechniqueSection(id) }
                }
            }
        }
    }

    private fun verifyKnown() {
        val art = TechniqueIllustrations.forExercise("bench-press")!!
        compose.onNodeWithContentDescription(art.description()).assertExists()
        compose.onNodeWithText("How to do it").assertExists()
        compose.onNodeWithText("Key cues").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Common mistakes").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("About this guidance").performScrollTo().assertIsDisplayed()
        val explanation = "General technique guidance. Start conservatively and seek qualified in-person coaching if you are unsure about a movement. Illustrations are general guidance, not individual form assessment or an exact joint-angle prescription."
        compose.onNodeWithText(explanation).assertDoesNotExist()
        compose.onNodeWithText("About this guidance").performClick()
        compose.onNodeWithText(explanation).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Hide details").performScrollTo().performClick()
        compose.onNodeWithText(explanation).assertDoesNotExist()
        compose.onNodeWithText("About this guidance").assertIsDisplayed()
    }

    @Test fun actualSharedGuidanceAndLicensedImageInLightTheme() { show(); verifyKnown() }
    @Test fun actualSharedGuidanceAndLicensedImageInDarkTheme() { show(dark = true); verifyKnown() }
    @Test fun largeTextSharedGuidanceScrollsThroughStackedNotesAndDisclosure() { show(dark = true, large = true); verifyKnown() }
    @Test fun unknownCustomMovementDoesNotInventInstructionsOrIllustration() {
        show(id = "custom-unrecognised")
        compose.onNodeWithText("A visual guide for this exercise is not available yet. Choose a familiar movement or seek qualified coaching.").assertIsDisplayed()
        compose.onNodeWithText("How to do it").assertDoesNotExist()
        compose.onNodeWithText("Key cues").assertDoesNotExist()
    }
}
