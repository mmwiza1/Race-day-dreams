package org.racedaydreams.coach

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
        compose.onNodeWithText("Progress").performScrollTo().performClick()
        compose.onNodeWithText("Default (green)").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Blue").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Settings").performScrollTo().performClick()
        compose.onNodeWithText("Default (amber)").performScrollTo().assertIsSelected()
        capturePart2(context, "accent-settings-default")
        compose.onNodeWithText("Violet").performScrollTo().performClick().assertIsSelected()
        capturePart2(context, "accent-selected")
        compose
            .onNodeWithText("Default (amber)")
            .performScrollTo()
            .performClick()
            .assertIsSelected()
        compose.onNodeWithText("Running").performScrollTo().performClick()
        compose.onNodeWithText("Teal").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Progress").performScrollTo().performClick()
        compose.onNodeWithText("Blue").performScrollTo().assertIsSelected()
        assertEquals(4, colours.value.size)
    }

    @Test
    fun curatedPairsKeepTextAndControlsReadableInBothThemes() {
        val dark = mutableStateOf(false)
        val colour = mutableStateOf<AccentColour?>(null)
        val page = mutableStateOf(ColourDiscipline.SETTINGS)
        lateinit var base: ColorScheme
        lateinit var applied: ColorScheme
        var running = Color.Unspecified
        var distance = Color.Unspecified
        var warning = Color.Unspecified
        compose.setContent {
            RaceDayTheme(darkTheme = dark.value, dynamicColour = false) {
                val default = MaterialTheme.colorScheme
                DisciplineAppearance(
                    colour.value?.let { mapOf(page.value to it) }.orEmpty(),
                    page.value,
                ) {
                    val current = MaterialTheme.colorScheme
                    val runningAccent = Discipline.RUNNING.accent()
                    val distanceAccent = chartAccent(ChartHue.BLUE).strong
                    val warningColour = tileColour(Tile.Warning)
                    SideEffect {
                        base = default
                        applied = current
                        running = runningAccent
                        distance = distanceAccent
                        warning = warningColour
                    }
                }
            }
        }
        val defaults = mapOf(
            ColourDiscipline.STRENGTH to AccentColour.VIOLET,
            ColourDiscipline.BOULDERING to AccentColour.PLUM,
            ColourDiscipline.PROGRESS to AccentColour.GREEN,
            ColourDiscipline.SETTINGS to AccentColour.AMBER,
        )
        for (useDark in listOf(false, true)) {
            for (discipline in ColourDiscipline.entries) {
                compose.runOnIdle {
                    dark.value = useDark
                    page.value = discipline
                    colour.value = null
                }
                val defaultPrimary = compose.runOnIdle { applied.primary }
                for (option in AccentColour.entries) {
                    compose.runOnIdle { colour.value = option }
                    compose.runOnIdle {
                        if (discipline != ColourDiscipline.RUNNING) assertEquals(base.primary, running)
                        if (option == defaults[discipline]) assertEquals(defaultPrimary, applied.primary)
                        assertEquals(base.surface, applied.surface)
                        assertEquals(base.error, applied.error)
                        assertEquals(base.errorContainer, warning)
                        assertEquals(ChartHue.BLUE.strong(useDark), distance)
                        assertTrue("accent against surface", contrast(applied.primary, applied.surface) >= 4.5)
                        assertTrue("label against accent", contrast(applied.onPrimary, applied.primary) >= 4.5)
                        assertTrue("label against container", contrast(applied.onPrimaryContainer, applied.primaryContainer) >= 4.5)
                    }
                }
            }
        }
    }

    @Test
    fun sharedComponentsReadAndRespondAtCompactWidths() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dark = InstrumentationRegistry.getArguments().getString("part2Evidence") == "dark"
        val saved = mutableStateOf(false)
        compose.setContent {
            RaceDayTheme(darkTheme = dark, dynamicColour = false) {
                DisciplineAppearance(emptyMap(), ColourDiscipline.PROGRESS) {
                    Surface(Modifier.fillMaxSize()) {
                        CoachPage {
                            ScreenHeader(Discipline.PROGRESS, Glyph.TREND, "Training summary", "Progress")
                            SectionTitle("This week", "Selected period · Four weeks")
                            Panel {
                                StatRow(listOf(Stat("Distance", "12.8", "km"), Stat("Elapsed time", "1:26:34"), Stat("Average pace", "6:46", "/km")))
                            }
                            var notes by androidx.compose.runtime.remember { mutableStateOf("") }
                            ExpandableSection("Advanced training preferences", "Optional workout notes", "benchmark-advanced") {
                                CoachTextInput("Workout notes", notes, { notes = it })
                            }
                            HeroSecondaryActions(listOf("Review history" to { saved.value = false }))
                            CoachButton("Save session") { saved.value = true }
                            if (saved.value) {
                                SemanticCard(Tile.Warning) { androidx.compose.material3.Text("Review the missing effort data") }
                                Panel { SectionTitle("No comparison recorded yet") }
                            } else CoachLoading("Loading comparisons…")
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Progress").assertIsDisplayed()
        compose.onNodeWithText("Distance").assertIsDisplayed()
        capturePart2(context, "piece2-progress-components")
        compose.onNodeWithText("Workout notes").assertDoesNotExist()
        compose.onNodeWithTag("benchmark-advanced").performScrollTo().performClick()
        compose.onNodeWithText("Workout notes").performScrollTo().performTextInput("Easy effort")
        compose.onNodeWithText("Review history").performScrollTo().assertHasClickAction()
        compose.onNodeWithText("Save session").performScrollTo().performClick()
        compose.onNodeWithText("Review the missing effort data").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("No comparison recorded yet").performScrollTo().assertIsDisplayed()
        capturePart2(context, "piece2-action-feedback")
        compose.onNodeWithTag("benchmark-advanced").performScrollTo().performClick()
        compose.onNodeWithText("Workout notes").assertDoesNotExist()
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
