package org.racedaydreams.coach

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.theme.RaceDayTheme

@RunWith(AndroidJUnit4::class)
class MaterialThemeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun brandedAndDeviceSchemesRetainReadableTextPairs() {
        var dark by mutableStateOf(false)
        var dynamic by mutableStateOf(false)
        var observed: ColorScheme? = null
        compose.setContent {
            RaceDayTheme(dark, dynamic) {
                observed = MaterialTheme.colorScheme
                Text("Theme ready")
            }
        }
        for (d in listOf(false, true)) for (device in listOf(false, true)) {
            compose.runOnIdle {
                dark = d
                dynamic = device
            }
            compose.waitForIdle()
            compose.runOnIdle {
                val s = requireNotNull(observed)
                listOf(
                        s.primary to s.onPrimary,
                        s.primaryContainer to s.onPrimaryContainer,
                        s.secondaryContainer to s.onSecondaryContainer,
                        s.tertiaryContainer to s.onTertiaryContainer,
                        s.surface to s.onSurface,
                        s.surfaceContainerLow to s.onSurface,
                        s.surfaceContainerHigh to s.onSurface,
                        s.errorContainer to s.onErrorContainer,
                    )
                    .forEach { (bg, fg) ->
                        val a = bg.luminance()
                        val b = fg.luminance()
                        assertTrue(
                            "Text pair must reach WCAG 4.5:1 in dark=$d dynamic=$device",
                            (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05) >= 4.5,
                        )
                    }
            }
        }
    }

    @Test
    fun sharedDisclosureIsARealCollapsedAccessibleControl() {
        compose.setContent {
            RaceDayTheme(dynamicColour = false) {
                CoachPage {
                    WhyDetails(
                        "Evidence stays separate from the prescription.",
                        "Supporting evidence",
                    )
                }
            }
        }
        compose
            .onNodeWithText("Evidence stays separate from the prescription.")
            .assertDoesNotExist()
        compose
            .onNode(hasText("Supporting evidence") and hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed"))
            .performClick()
        compose.onNodeWithText("Evidence stays separate from the prescription.").assertIsDisplayed()
        compose
            .onNode(hasText("Hide details") and hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))
            .performClick()
        compose
            .onNodeWithText("Evidence stays separate from the prescription.")
            .assertDoesNotExist()
    }
}
