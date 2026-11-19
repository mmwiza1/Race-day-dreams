package org.racedaydreams.coach

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.theme.RaceDayTheme
import org.racedaydreams.domain.activity.*

@RunWith(AndroidJUnit4::class)
class SharedRunComparisonTest {
    @get:Rule val compose = createComposeRule()

    private fun run(id: String, ago: Long, metres: Double = 5000.0, seconds: Long = 1500) =
        ActivityRecord(
            id,
            SourceIdentity("native", id, null),
            Instant.EPOCH.minusSeconds(ago),
            Instant.EPOCH.minusSeconds(ago).plusSeconds(seconds),
            null,
            null,
            metres,
            null,
            null,
            null,
            null,
            null,
            null,
            "",
            emptyList(),
            emptyList(),
            null,
            null,
        )

    @Test
    fun collapsedComparisonExpandsUsingActualComparableEvidenceAndCanClose() {
        val current = run("current", 0)
        val prior = run("prior", 86400)
        compose.setContent {
            RaceDayTheme(dynamicColour = false) {
                CoachPage {
                    SharedRunComparisonSection(
                        current,
                        listOf(current, prior),
                        emptyList(),
                        false,
                        { it.id() },
                        { "${decimal(it / 1000)} km" },
                        ::formatPaceSeconds,
                        ::decimal,
                    )
                }
            }
        }
        compose.onNodeWithText("Run comparison").assertDoesNotExist()
        compose.onNodeWithText("Compare this run").performClick()
        compose.onNodeWithText("Current · current").assertExists()
        compose.onNodeWithText("Previous · prior").assertExists()
        compose.onNodeWithText("Why these runs were compared").performScrollTo().performClick()
        compose.onNodeWithText("Route matching is not assumed.", substring = true).assertExists()
        compose.onNodeWithText("Compare this run").performScrollTo().performClick()
        compose.onNodeWithText("Run comparison").assertDoesNotExist()
    }

    @Test
    fun manualChoiceDoesNotPretendTheEngineEstablishedSimilarity() {
        val current = run("current", 0)
        val prior = run("far", 86400, 8000.0, 2800)
        compose.setContent {
            RaceDayTheme(darkTheme = true, dynamicColour = false) {
                CoachPage {
                    SharedRunComparisonSection(
                        current,
                        listOf(current, prior),
                        emptyList(),
                        false,
                        { it.id() },
                        { "${decimal(it / 1000)} km" },
                        ::formatPaceSeconds,
                        ::decimal,
                    )
                }
            }
        }
        compose.onNodeWithText("Compare this run").performClick()
        compose.onNodeWithText("No sufficiently comparable previous run found.").assertExists()
        compose.onNodeWithText("Choose another run").performScrollTo().performClick()
        compose.onNodeWithText("far · 8.0 km").performClick()
        compose.onNodeWithText("Previous · far").assertExists()
        compose.onNodeWithText("Why these runs were compared").performScrollTo().performClick()
        compose
            .onNodeWithText(
                "You chose this comparison manually. Similarity has not been established."
            )
            .assertExists()
    }

    @Test
    fun largeTextKeepsTheComparisonAndChoiceControlsScrollable() {
        val current = run("current", 0)
        val prior = run("prior", 86400)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                RaceDayTheme(darkTheme = true, dynamicColour = false) {
                    CoachPage {
                        SharedRunComparisonSection(
                            current,
                            listOf(current, prior),
                            emptyList(),
                            false,
                            { "15 November 2026 · ${it.id()}" },
                            { "${decimal(it / 1000)} km" },
                            ::formatPaceSeconds,
                            ::decimal,
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("Compare this run").performClick()
        compose
            .onNodeWithText("Previous · 15 November 2026 · prior")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Choose another run").performScrollTo().performClick()
        compose.onNodeWithText("Choose previous run").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Compare this run").performScrollTo().assertIsDisplayed()
    }
}
