package org.racedaydreams.coach

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.theme.RaceDayTheme
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.SourceIdentity
import org.racedaydreams.domain.strength.ExerciseCatalog
import org.racedaydreams.domain.strength.StrengthSession

/** W4a: recorded history and run summary keep their meaning in text, headings and one target. */
@RunWith(AndroidJUnit4::class)
class RecordedWorkoutPresentationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun historyGroupsRunsUnderDateHeadingsWithLabelledFiguresAndOneTapTarget() {
        val run = run("labelled-run", 7200.0)
        var opened: String? = null
        compose.setContent {
            RaceDayTheme(dynamicColour = false) {
                ActivityHistoryScreen(
                    CoachUiState(
                        loading = false,
                        activities = listOf(run),
                        strengthSessions = listOf(strength()),
                    ),
                    { opened = it },
                    loadSummaryPage = { _, _, _ -> listOf(run) },
                )
            }
        }
        compose.waitUntil(20000) {
            compose
                .onAllNodes(hasStateDescription("Recorded history ready"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNode(hasText("Strength history") and isHeading()).assertExists()
        compose.onNodeWithText("Strength workout").assertExists()
        compose.onNodeWithText("50 min · 1 exercise").assertExists()
        compose.onNodeWithText("Back Squat").assertExists()
        val recordedHeading = hasText("Recorded runs") and isHeading()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(recordedHeading)
        compose.onNode(recordedHeading).assertExists()
        val date = hasText("23 November 2026") and isHeading()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(date)
        compose.onNode(date).assertExists()
        // Distance, elapsed time and pace are figures with their measure named in words, inside
        // the single clickable row together with the source and plain status.
        val row =
            hasClickAction() and
                hasText("Recorded run") and
                hasText("7.2 km distance") and
                hasText("0:30:00 elapsed") and
                hasText("Completed · tap for details")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(row)
        compose.onNode(row).performClick()
        compose.runOnIdle { assertEquals(run.id(), opened) }
    }

    @Test
    fun runSummaryLeadsWithLabelledHeadingAndMergedFigures() {
        compose.setContent {
            MaterialTheme {
                CoachPage {
                    RecordedRunSummary(run("summary", 5000.0), CoachUiState())
                    RecordedHeartRateSummary(run("summary", 5000.0))
                }
            }
        }
        compose.onNode(hasText("Recorded summary") and isHeading()).assertIsDisplayed()
        // Each figure is read as one item: label and value together.
        compose
            .onNode(hasText("Elapsed time") and hasText("0:30:00"))
            .assertIsDisplayed()
        compose.onNode(hasText("Distance") and hasText("5.0 km")).assertIsDisplayed()
        compose.onNode(hasText("Elapsed average pace") and hasText("6:00/km")).assertExists()
        compose
            .onNodeWithText("Elapsed time includes pauses.", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNode(hasText("Recorded heart rate") and isHeading()).assertExists()
        compose
            .onNodeWithText("Average and maximum heart-rate summaries are unavailable for this run.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun run(id: String, metres: Double): ActivityRecord {
        val start = Instant.parse("2026-11-23T09:33:00Z")
        return ActivityRecord(
            id,
            SourceIdentity("manual", id, null),
            start,
            start.plusSeconds(1800),
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            metres,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            emptyList(),
            emptyList(),
            null,
            null,
        )
    }

    private fun strength(): StrengthSession {
        val exercise = requireNotNull(ExerciseCatalog.definition("back-squat")).exercise()
        return StrengthSession(
            "w4a-strength",
            Instant.parse("2026-11-22T08:00:00Z"),
            ZoneOffset.UTC,
            Duration.ofMinutes(50),
            7.0,
            null,
            listOf(
                StrengthSession.ExerciseSets(
                    exercise,
                    listOf(StrengthSession.SetEntry("w4a-set", 5, 80.0, 7.0, false)),
                )
            ),
        )
    }
}
