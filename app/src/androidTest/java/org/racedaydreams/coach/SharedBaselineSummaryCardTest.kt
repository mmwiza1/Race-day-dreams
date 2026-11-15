package org.racedaydreams.coach

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.theme.RaceDayTheme
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.baseline.*

@RunWith(AndroidJUnit4::class)
class SharedBaselineSummaryCardTest {
    @get:Rule val compose = createComposeRule()
    private val from = Instant.parse("2026-09-01T00:00:00Z")
    private val to = Instant.parse("2026-11-15T00:00:00Z")
    private fun manual() = BaselineEngine.applyManualOverlay(RunnerBaseline.empty(from, to), ManualBaselineInput(30.0, 3.0, 10.0, Duration.ofSeconds(1500), null, null, null, 8), to)

    private fun show(baseline: RunnerBaseline, dark: Boolean = false, large: Boolean = false, imperial: Boolean = false) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (large) 2f else 1f)) {
                RaceDayTheme(darkTheme = dark, dynamicColour = false) {
                    CoachPage {
                        SharedBaselineSummaryCard(baseline, { "${decimal(it / if (imperial) 1609.344 else 1000.0)} ${if (imperial) "mi" else "km"}" }, ::decimal, PaceCalculator::formatTime, { shortDate(it.atOffset(ZoneOffset.UTC).toLocalDate()) })
                    }
                }
            }
        }
    }

    @Test fun absentHistoryShowsHonestEmptyStateWithoutInventingDistanceOrResults() {
        show(RunnerBaseline.empty(from, to))
        compose.onNodeWithText("No running history is available yet. You can continue without it.").assertIsDisplayed()
        compose.onNodeWithText("Average recorded weekly distance:", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Recent 5K result:", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Baseline confidence: Low").assertExists()
        compose.onNodeWithText("History window").performScrollTo().performClick()
        compose.onNodeWithText("UTC boundaries", substring = true).assertExists()
    }

    @Test fun manualAnswersStayLabelledAndDoNotBecomeRecordedHistoryOrConfidence() {
        show(manual(), dark = true)
        compose.onNodeWithText("No running history is available yet. You can continue without it.").assertExists()
        compose.onNodeWithText("Average recorded weekly distance: 30.0 km · manual").assertExists()
        compose.onNodeWithText("Recent 5K result: 0:25:00 · manual").assertExists()
        compose.onNodeWithText("Baseline confidence: Low").assertExists()
        compose.onNodeWithText("not a probability of race success", substring = true).assertDoesNotExist()
        compose.onNodeWithText("About confidence").performScrollTo().performClick()
        compose.onNodeWithText("not a probability of race success", substring = true).assertExists()
    }

    @Test fun largeTextKeepsUnitLabelsAndConfidenceDisclosureScrollable() {
        show(manual(), dark = true, large = true, imperial = true)
        compose.onNodeWithText("Average recorded weekly distance: 18.6 mi · manual").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Longest recent run: 6.2 mi · manual").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("About confidence").performScrollTo().performClick()
        compose.onNodeWithText("not a probability of race success", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Hide details")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))
            .performScrollTo().performClick()
        compose.onNodeWithText("About confidence")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed"))
        compose.onNodeWithText("not a probability of race success", substring = true).assertDoesNotExist()
    }
}
