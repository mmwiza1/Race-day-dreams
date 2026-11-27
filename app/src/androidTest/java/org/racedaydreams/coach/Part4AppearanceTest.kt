package org.racedaydreams.coach

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.racedaydreams.coach.theme.DisciplineAppearance
import org.racedaydreams.coach.theme.DisciplineColourSettings
import org.racedaydreams.coach.theme.RaceDayTheme
import org.racedaydreams.domain.settings.AccentColour
import org.racedaydreams.domain.settings.ColourDiscipline

class Part4AppearanceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun colourChoicesStayIndependentAndExposeSelection() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val colours = mutableStateOf(emptyMap<ColourDiscipline, AccentColour>())
        val dark = InstrumentationRegistry.getArguments().getString("part2Evidence") == "dark"
        compose.setContent {
            RaceDayTheme(darkTheme = dark, dynamicColour = false) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DisciplineAppearance(colours.value, ColourDiscipline.SETTINGS) {
                        CoachPage {
                            DisciplineColourSettings(colours.value, false) { discipline, colour ->
                                colours.value =
                                    if (colour == null) colours.value - discipline
                                    else colours.value + (discipline to colour)
                            }
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Default").assertIsSelected()
        capturePart2(context, "accent-default")
        compose.onNodeWithText("Teal").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Gym / Strength").performScrollTo().performClick()
        compose.onNodeWithText("Plum").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Bouldering").performScrollTo().performClick()
        compose.onNodeWithText("Amber").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Settings").performScrollTo().performClick()
        compose.onNodeWithText("Default (green)").performScrollTo().assertIsSelected()
        capturePart2(context, "accent-settings-default")
        compose.onNodeWithText("Violet").performScrollTo().performClick().assertIsSelected()
        capturePart2(context, "accent-selected")
        compose
            .onNodeWithText("Default (green)")
            .performScrollTo()
            .performClick()
            .assertIsSelected()
        compose.onNodeWithText("Running").performScrollTo().performClick()
        compose.onNodeWithText("Teal").performScrollTo().assertIsSelected()
        assertEquals(3, colours.value.size)
    }

    @Test
    fun curatedPairsKeepTextAndControlsReadableInBothThemes() {
        val dark = mutableStateOf(false)
        val colour = mutableStateOf<AccentColour?>(null)
        lateinit var base: ColorScheme
        lateinit var applied: ColorScheme
        var running = Color.Unspecified
        compose.setContent {
            RaceDayTheme(darkTheme = dark.value, dynamicColour = false) {
                val default = MaterialTheme.colorScheme
                DisciplineAppearance(
                    colour.value?.let { mapOf(ColourDiscipline.SETTINGS to it) }.orEmpty(),
                    ColourDiscipline.SETTINGS,
                ) {
                    val current = MaterialTheme.colorScheme
                    val runningAccent = Discipline.RUNNING.accent()
                    SideEffect {
                        base = default
                        applied = current
                        running = runningAccent
                    }
                }
            }
        }
        for (useDark in listOf(false, true)) {
            for (option in listOf(null) + AccentColour.entries) {
                compose.runOnIdle {
                    dark.value = useDark
                    colour.value = option
                }
                compose.runOnIdle {
                    assertEquals(base.primary, running)
                    assertEquals(base.surface, applied.surface)
                    assertTrue(
                        "accent against surface",
                        contrast(applied.primary, applied.surface) >= 4.5,
                    )
                    assertTrue(
                        "label against accent",
                        contrast(applied.onPrimary, applied.primary) >= 4.5,
                    )
                    assertTrue(
                        "label against container",
                        contrast(applied.onPrimaryContainer, applied.primaryContainer) >= 4.5,
                    )
                }
            }
        }
    }

    @Test
    fun diagnosticSummaryCanRevealAndExportFullJson() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dark = InstrumentationRegistry.getArguments().getString("part2Evidence") == "dark"
        val exported = mutableStateOf(false)
        val report =
            """{"recording":{"lastCheckpointState":"saved","delivery":{"callbacks":36,
            "longestCallbackGapMillis":1654,"fixesOverTenSecondsLate":0},
            "routeEvidence":{"acceptedOnDelivery":35,"rejectedOrDeferred":{"paused":1},"flushFailures":0}}}"""
        val reviewing = mutableStateOf(false)
        compose.setContent {
            RaceDayTheme(darkTheme = dark, dynamicColour = false) {
                Surface(Modifier.fillMaxSize()) {
                    CoachPage { CoachButton("Review recording") { reviewing.value = true } }
                    if (reviewing.value) {
                        DiagnosticReview(report, dismiss = { reviewing.value = false }) {
                            exported.value = true
                            reviewing.value = false
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Review recording").performClick()
        compose.onNodeWithText("Last recording state").assertIsDisplayed()
        compose.onNodeWithText("Saved").assertIsDisplayed()
        compose.onNodeWithText("Longest callback gap").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("paused: 1").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Point save failures").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Saved").performScrollTo()
        capturePart2(context, "diagnostic-summary")
        compose.onNodeWithText("View full JSON").performScrollTo().performClick()
        compose.onNodeWithText("Show summary").assertIsDisplayed()
        compose.onNodeWithText(report).assertExists()
        compose.onNodeWithText("Export JSON").performClick()
        assertTrue(exported.value)
    }

    private fun contrast(first: Color, second: Color): Double {
        val high = maxOf(first.luminance(), second.luminance())
        val low = minOf(first.luminance(), second.luminance())
        return (high + 0.05) / (low + 0.05)
    }
}
