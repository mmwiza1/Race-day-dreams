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
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.theme.RaceDayTheme
import org.racedaydreams.domain.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.coaching.*

@RunWith(AndroidJUnit4::class)
class SharedRunFeedbackCardTest {
    @get:Rule val compose = createComposeRule()

    private fun feedback(rpe: Double?) = RunScoreEngine.analyse(
        PlannedWorkout("w", LocalDate.of(2026, 11, 15), WorkoutCategory.EASY_RUN, 5.0, Duration.ofSeconds(1800), emptyList(), "original"),
        ActivityRecord("a", SourceIdentity("native", "a", null), Instant.EPOCH, Instant.EPOCH.plusSeconds(1800), null, null, 5000.0, null, null, null, null, null, rpe, "", emptyList(), emptyList(), null, null), null,
    )

    private fun show(result: RunFeedback, dark: Boolean = false, large: Boolean = false) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (large) 2f else 1f)) {
                RaceDayTheme(darkTheme = dark, dynamicColour = false) {
                    CoachPage { SharedRunFeedbackCard(result) }
                }
            }
        }
    }

    @Test fun missingEffortRemainsUnavailableAndCannotBecomeAnExecutionScore() {
        show(feedback(null))
        compose.onNodeWithText("Execution evidence incomplete").assertIsDisplayed()
        compose.onNodeWithText("10/10", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Effort match: Unavailable", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Score components").performScrollTo().performClick()
        compose.onNodeWithText("Effort match: Unavailable", substring = true).assertExists()
        compose.onNodeWithText("Hide details")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))
            .performScrollTo().performClick()
        compose.onNodeWithText("Score components")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed"))
        compose.onNodeWithText("Effort match: Unavailable", substring = true).assertDoesNotExist()
    }

    @Test fun recordedExecutionDisplaysTheExistingScoreWithItsLimitationsBehindDisclosure() {
        show(feedback(3.0), dark = true)
        compose.onNodeWithText("10/10 — Good execution").assertIsDisplayed()
        compose.onNodeWithText("What went well").assertExists()
        compose.onNodeWithText("How was this calculated?").performScrollTo().performClick()
        compose.onNodeWithText("rule version 3", substring = true).assertExists()
        compose.onNodeWithText("not scientifically validated weights", substring = true).assertExists()
        compose.onNodeWithText("Completion alone does not prove fitness improved", substring = true).assertExists()
    }

    @Test fun largeTextKeepsAthleteChoiceAndEvidenceExplanationScrollable() {
        show(feedback(8.0), dark = true, large = true)
        compose.onNodeWithText("What differed from the plan").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("What we learned").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Any meaningful plan adjustment remains your choice", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("How was this calculated?").performScrollTo().performClick()
        compose.onNodeWithText("Missing evidence is excluded", substring = true).performScrollTo().assertIsDisplayed()
    }
}
