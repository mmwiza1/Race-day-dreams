package org.racedaydreams.coach

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.theme.RaceDayTheme
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.SourceIdentity

@RunWith(AndroidJUnit4::class)
class RunSummaryPresentationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun elapsedPaceUsesChosenUnitsAndDoesNotSubstituteMovingTime() {
        val recorded = record(1000.0, 140.0, 160.0)
        assertEquals("10:00/km", recordedElapsedPace(recorded, false))
        assertEquals("16:06/mi", recordedElapsedPace(recorded, true))
        assertNull(recordedElapsedPace(record(null, null, null), false))
        assertNull(recordedElapsedPace(record(0.0, null, null), false))
        assertEquals(Duration.ofSeconds(500), recorded.movingTime())
        assertEquals(Duration.ofSeconds(600), recorded.elapsedTime())
    }

    @Test
    fun largeTextSummaryScrollsAndKeepsMeasuredAndCalculatedValuesDistinct() {
        val recorded = record(1000.0, 140.0, 160.0)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                RaceDayTheme(darkTheme = true, dynamicColour = false) {
                    CoachPage {
                        RecordedRunSummary(recorded, CoachUiState())
                        RecordedHeartRateSummary(recorded)
                    }
                }
            }
        }
        compose.onNodeWithText("Elapsed time").assertExists()
        compose.onNodeWithText("0:10:00").assertExists()
        compose.onNodeWithText("10:00/km").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Moving time: 0:08:20").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Average recorded HR").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("140.0 bpm").assertExists()
        compose.onNodeWithText("Maximum recorded HR").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("160.0 bpm").assertExists()
        assertEquals(140.0, recorded.averageHeartRate()!!, 0.0)
    }

    @Test
    fun missingMeasurementsHaveHonestEmptyStates() {
        compose.setContent {
            RaceDayTheme(dynamicColour = false) {
                CoachPage {
                    RecordedRunSummary(record(null, null, null), CoachUiState())
                    RecordedHeartRateSummary(record(null, null, null))
                }
            }
        }
        compose.onNodeWithText("Distance unavailable").assertExists()
        compose.onNodeWithText("Pace unavailable").assertExists()
        compose
            .onNodeWithText(
                "Average and maximum heart-rate summaries are unavailable for this run."
            )
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("0 bpm").assertDoesNotExist()
        compose.onNodeWithText("Maximum recorded HR").assertDoesNotExist()
    }

    private fun record(distance: Double?, average: Double?, maximum: Double?): ActivityRecord {
        val start = Instant.parse("2026-11-01T10:00:00Z")
        return ActivityRecord(
            "summary-test",
            SourceIdentity("rdd", "summary-test", null),
            start,
            start.plusSeconds(600),
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            distance,
            Duration.ofSeconds(500),
            average,
            maximum,
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
}
