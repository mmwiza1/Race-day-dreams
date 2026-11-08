package org.racedaydreams.coach

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.theme.RaceDayTheme
import org.racedaydreams.domain.activity.*

@RunWith(AndroidJUnit4::class)
class RunningPerformanceUiTest {
    @get:Rule val compose = createComposeRule()

    private fun run(
        id: String,
        date: LocalDate,
        average: Double? = null,
        maximum: Double? = null,
    ): ActivityRecord {
        val start = date.atTime(12, 0).toInstant(ZoneOffset.UTC)
        return ActivityRecord(
            id,
            SourceIdentity("manual", id, null),
            start,
            start.plusSeconds(1800),
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            5000.0,
            null,
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

    @Test
    fun chartSupportsVisibleSelectionButtonsAndScreenReaderWeekActions() {
        val today = LocalDate.now()
        val runs = listOf(run("previous", today.minusWeeks(1)), run("current", today))
        val weeks = ActivityAnalytics.weekly(runs, today, ZoneId.systemDefault(), 4)
        compose.setContent {
            RaceDayTheme(dynamicColour = false) {
                CoachPage {
                    WeeklyRunningChart(weeks, runs, RunningTrendMetric.DISTANCE, CoachUiState())
                }
            }
        }
        val chart = compose.onNodeWithTag("running-trend-chart")
        val latestDescription =
            chart
                .fetchSemanticsNode()
                .config[androidx.compose.ui.semantics.SemanticsProperties.StateDescription]
        compose.onNodeWithText("Earlier week").performScrollTo().performClick()
        val earlierDescription =
            chart
                .fetchSemanticsNode()
                .config[androidx.compose.ui.semantics.SemanticsProperties.StateDescription]
        assertNotEquals(latestDescription, earlierDescription)
        compose.onNodeWithText("Later week").performClick()
        assertEquals(
            latestDescription,
            chart
                .fetchSemanticsNode()
                .config[androidx.compose.ui.semantics.SemanticsProperties.StateDescription],
        )
        val actions = chart.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals(4, actions.size)
        compose.runOnIdle { assertTrue(actions.first().action()) }
        assertTrue(
            chart
                .fetchSemanticsNode()
                .config[androidx.compose.ui.semantics.SemanticsProperties.StateDescription]
                .contains("No recorded runs")
        )
        compose.onNodeWithText("Earlier week").assertIsNotEnabled()
        compose.onNodeWithTag("selected-running-week").assertExists()
    }

    @Test
    fun largeTextHeartRateSelectionKeepsActualAndMissingMeasurementsDistinct() {
        val today = LocalDate.now()
        val runs = listOf(run("missing", today.minusDays(1)), run("measured", today, 152.0, 181.0))
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                RaceDayTheme(darkTheme = true, dynamicColour = false) {
                    CoachPage { RunningHeartRatePanel(runs, CoachUiState()) }
                }
            }
        }
        compose.onNodeWithText("152 bpm").assertExists()
        compose.onNodeWithText("181 bpm").assertExists()
        compose.onNodeWithText("View earlier run").performScrollTo().performClick()
        compose.onNodeWithText("No heart-rate measurements recorded for this run.").assertExists()
        compose.onAllNodesWithText("Unavailable").assertCountEquals(2)
        compose.onNodeWithText("0 bpm").assertDoesNotExist()
        compose.onNodeWithText("Heart-rate zones unavailable").assertExists()
        val later = compose.onNodeWithText("View later run").performScrollTo().assertIsDisplayed()
        val target = later.fetchSemanticsNode()
        assertEquals(target.size.width.toFloat(), target.boundsInRoot.width, 1f)
        later.performClick()
        compose.onNodeWithText("152 bpm").assertExists()
        compose.onNodeWithText("181 bpm").assertExists()
    }

    @Test
    fun noHeartRateEvidenceOffersAnEmptyStateWithoutInventedZoneCharts() {
        compose.setContent {
            RaceDayTheme(dynamicColour = false) {
                CoachPage { RunningHeartRatePanel(emptyList(), CoachUiState()) }
            }
        }
        compose.onNodeWithText("No heart-rate measurements recorded in this range.").assertExists()
        compose.onNodeWithText("Heart-rate zones unavailable").assertExists()
        compose.onNodeWithTag("running-heart-rate-chart").assertDoesNotExist()
        compose.onNodeWithText("Average heart rate").assertDoesNotExist()
        compose.onNodeWithText("Maximum heart rate").assertDoesNotExist()
    }
}
