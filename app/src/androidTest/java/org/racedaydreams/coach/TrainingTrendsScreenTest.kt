package org.racedaydreams.coach

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.theme.RaceDayTheme
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.SourceIdentity

/** The shared chart-first Training trends screen with the real Android formatters. */
@RunWith(AndroidJUnit4::class)
class TrainingTrendsScreenTest {
    @get:Rule val compose = createComposeRule()
    private val today = LocalDate.now()
    private val zone = ZoneId.systemDefault()
    private val opened = CopyOnWriteArrayList<String>()
    private val dark = mutableStateOf(false)

    private fun run(
        id: String,
        date: LocalDate,
        metres: Double? = 5_000.0,
        seconds: Long = 1_800,
        average: Double? = null,
        maximum: Double? = null,
        hour: Int = 12,
    ): ActivityRecord {
        val start = date.atTime(hour, 0).atZone(zone).toInstant()
        return ActivityRecord(
            id,
            SourceIdentity("manual", id, null),
            start,
            start.plusSeconds(seconds),
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            metres,
            Duration.ofSeconds(seconds),
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

    private fun weekOf(date: LocalDate) = date.minusDays((date.dayOfWeek.value - 1).toLong())

    private fun show(activities: List<ActivityRecord>, fontScale: Float = 1f, width: Dp? = null) {
        val state = CoachUiState(loading = false, activities = activities)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                RaceDayTheme(darkTheme = dark.value, dynamicColour = false) {
                    Box(if (width == null) Modifier else Modifier.width(width)) {
                        CoachPage {
                            SharedTrainingTrends(
                                activities,
                                today,
                                { it.atZone(zone).toLocalDate() },
                                false,
                                { formatDistance(it, state) },
                                ::decimal,
                                ::formatPaceSeconds,
                                ::shortDate,
                                ::friendlyDate,
                                { opened.add(it) },
                                null,
                            )
                        }
                    }
                }
            }
        }
    }

    private val week = hasContentDescription("Week of ", substring = true)

    private fun weekNodes() = compose.onAllNodes(week).fetchSemanticsNodes().size

    @Test
    fun periodsShowTheirCalendarWeeksAndLongRangesGetMonthLabels() {
        // A run every week for a year.
        show((0L until 52L).map { run("w$it", today.minusWeeks(it)) })
        assertEquals(12, weekNodes())
        compose.onNodeWithTag("trend-month-labels").assertDoesNotExist()
        compose.onNodeWithTag("period-FOUR_WEEKS").performClick()
        assertEquals(4, weekNodes())
        compose.onNodeWithTag("period-SIX_MONTHS").performClick()
        assertEquals(26, weekNodes())
        compose.onNodeWithTag("trend-month-labels").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("period-ONE_YEAR").performClick()
        assertEquals(52, weekNodes())
        compose.onNodeWithTag("trend-month-labels").performScrollTo().assertIsDisplayed()
        compose
            .onNodeWithTag("trends-range")
            .assert(hasText(friendlyDate(weekOf(today.minusWeeks(51))), substring = true))
        // Every period opens on the current week, and the figure is the existing weekly total.
        compose.onNodeWithText("Distance this week").assertExists()
        compose.onNodeWithTag("dashboard-week-distance").assert(hasText("5.0", substring = true))
    }

    @Test
    fun steppingToAnEarlierWeekShowsOnlyItsRunsAndOpensThem() {
        val earlier = today.minusWeeks(3)
        show(listOf(run("now", today), run("old", earlier, metres = 8_000.0)))
        repeat(3) { compose.onNodeWithTag("trend-earlier").performScrollTo().performClick() }
        compose.onNodeWithText("Week of ${friendlyDate(weekOf(earlier))}").assertExists()
        compose.onNodeWithTag("trend-readout-value").assert(hasText("8.0", substring = true))
        compose.onNodeWithText("Runs that week").assertExists()
        compose
            .onNodeWithTag("week-runs")
            .assert(hasAnyDescendant(hasText(friendlyDate(earlier), substring = true)))
            .assert(!hasAnyDescendant(hasText(friendlyDate(today), substring = true)))
        compose.onNode(hasText(friendlyDate(earlier), substring = true) and hasClickAction())
            .performScrollTo()
            .performClick()
        assertEquals(listOf("old"), opened.toList())
        // The chart itself announces the selected week.
        compose
            .onNodeWithTag("running-trend-chart")
            .assert(
                SemanticsMatcher.expectValue(
                    androidx.compose.ui.semantics.SemanticsProperties.StateDescription,
                    "Week of ${shortDate(weekOf(earlier))}: 8.0 km, 1 run",
                )
            )
        repeat(3) { compose.onNodeWithTag("trend-later").performScrollTo().performClick() }
        compose.onNodeWithText("Distance this week").assertExists()
        compose.onNodeWithTag("trend-later").assertIsNotEnabled()
    }

    @Test
    fun weeksWithoutRunsAndRunsWithoutTheMeasureAreNeverShownAsZero() {
        show(
            listOf(
                run("no-distance", today, metres = null),
                run("measured", today.minusWeeks(2), metres = 6_000.0),
            )
        )
        compose
            .onNodeWithContentDescription("Week of ${shortDate(weekOf(today.minusWeeks(1)))}: no recorded runs")
            .assertExists()
        compose
            .onNode(
                hasContentDescription(
                    "Week of ${shortDate(weekOf(today))}: distance not recorded",
                    substring = true,
                )
            )
            .assertExists()
        compose.onNodeWithTag("trend-readout-value").assert(hasText("Not recorded"))
        compose.onNodeWithTag("trend-readout-sample").assert(hasText("based on 0 of 1", substring = true))
        compose.onNodeWithTag("trend-legend").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("0.0 km").assertDoesNotExist()
    }

    @Test
    fun heartRateUsesEachRunsRecordedAverageAndNamesItsSample() {
        show(
            listOf(
                run("high", today, average = 150.0, maximum = 175.0),
                run("low", today, average = 142.0, hour = 7),
                run("none", today, seconds = 1_500, hour = 18),
                run("earlier-none", today.minusWeeks(1)),
            )
        )
        compose.onNodeWithTag("trend-metric-HEART_RATE").performScrollTo().performClick()
        compose.onNodeWithText("Heart rate this week").assertExists()
        compose.onNodeWithTag("trend-readout-value").assert(hasText("142–150 bpm"))
        compose
            .onNodeWithTag("trend-readout-sample")
            .assert(hasText("heart rate on 2 of 3", substring = true))
        compose
            .onNodeWithTag("week-runs")
            .assert(hasAnyDescendant(hasText("150 bpm")))
            .assert(hasAnyDescendant(hasText("No heart rate")))
        compose.onNodeWithText("average · max 175", substring = true).assertExists()
        compose
            .onNode(
                hasContentDescription(
                    "Week of ${shortDate(weekOf(today.minusWeeks(1)))}: heart rate not recorded",
                    substring = true,
                )
            )
            .assertExists()
        compose.onNodeWithText("0 bpm", substring = true).assertDoesNotExist()
        compose.onNodeWithTag("heart-rate-empty").assertDoesNotExist()
    }

    @Test
    fun heartRateWithoutAnyRecordedSummaryShowsAnEmptyStateInsteadOfAChart() {
        show(listOf(run("plain", today), run("older", today.minusWeeks(2))))
        compose.onNodeWithTag("trend-metric-HEART_RATE").performScrollTo().performClick()
        compose.onNodeWithTag("heart-rate-empty").assertExists()
        compose.onNodeWithTag("running-trend-chart").assertDoesNotExist()
        compose.onNodeWithText("bpm", substring = true).assertDoesNotExist()
        // Switching back restores the chart.
        compose.onNodeWithTag("trend-metric-DISTANCE").performScrollTo().performClick()
        compose.onNodeWithTag("running-trend-chart").assertExists()
    }

    @Test
    fun paceSaysFasterIsHigherAndTimeUsesHoursAndMinutes() {
        show(listOf(run("now", today, seconds = 4_200), run("before", today.minusWeeks(1))))
        compose.onNodeWithTag("trend-metric-PACE").performScrollTo().performClick()
        compose.onNodeWithText("Higher is faster: less time per kilometre.").performScrollTo().assertExists()
        compose.onNodeWithTag("trend-readout-value").assert(hasText("/km", substring = true))
        compose.onNodeWithTag("trend-metric-TIME").performScrollTo().performClick()
        compose.onNodeWithTag("trend-readout-value").assert(hasText("1 h 10 min"))
    }

    @Test
    fun unselectedBarsKeepThreeToOneContrastInLightAndDarkThemes() {
        show(listOf(run("previous", today.minusWeeks(1)), run("current", today)))
        compose.onNodeWithTag("period-FOUR_WEEKS").performClick()
        assertUnselectedBarContrast()
        compose.runOnIdle { dark.value = true }
        compose.waitForIdle()
        assertUnselectedBarContrast()
    }

    private fun assertUnselectedBarContrast() {
        val pixels =
            compose
                .onNodeWithTag("running-trend-chart")
                .performScrollTo()
                .assertIsDisplayed()
                .captureToImage()
                .toPixelMap()
        // Four weeks: the previous and current weeks have equal distance, so the previous
        // (unselected) bar fills the third slot. Sample inside it and over the empty first week,
        // away from grid lines and the selection guide.
        val bar = pixels[(pixels.width * .625f).toInt(), (pixels.height * .4f).toInt()].luminance()
        val surface =
            pixels[(pixels.width * .125f).toInt(), (pixels.height * .4f).toInt()].luminance()
        val contrast =
            (kotlin.math.max(bar, surface) + .05f) / (kotlin.math.min(bar, surface) + .05f)
        assertTrue("Unselected bars need at least 3:1 contrast; observed $contrast", contrast >= 3f)
    }

    @Test
    fun controlsStayReadableAndAtLeast48dpAt200PercentOnANarrowScreen() {
        show(
            listOf(run("now", today, average = 148.0), run("before", today.minusWeeks(1))),
            fontScale = 2f,
            width = 300.dp,
        )
        for (period in listOf("FOUR_WEEKS", "TWELVE_WEEKS", "SIX_MONTHS", "ONE_YEAR"))
            compose
                .onNodeWithTag("period-$period")
                .performScrollTo()
                .assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("trend-metric-chips").performScrollTo().assertIsDisplayed()
        for (metric in listOf("DISTANCE", "TIME", "PACE", "HEART_RATE"))
            compose
                .onNodeWithTag("trend-metric-$metric")
                .performScrollTo()
                .assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
        for (tag in listOf("trend-earlier", "trend-later"))
            compose.onNodeWithTag(tag).performScrollTo().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("trend-earlier").performClick()
        compose.onNodeWithTag("trend-metric-HEART_RATE").performScrollTo().performClick()
        compose.onNodeWithTag("trend-metric-HEART_RATE").assertIsSelected()
        compose.onNodeWithTag("running-trend-chart").performScrollTo().assertIsDisplayed()
    }
}
