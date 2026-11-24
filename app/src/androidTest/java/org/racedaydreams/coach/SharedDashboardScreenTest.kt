package org.racedaydreams.coach

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.PlannedWorkout
import org.racedaydreams.domain.WorkoutCategory
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.SourceIdentity
import org.racedaydreams.domain.strength.ExerciseCatalog
import org.racedaydreams.domain.strength.StrengthExercise
import org.racedaydreams.domain.strength.StrengthSession

/** Real shared dashboards over synthetic records: periods, selection and drill-down callbacks. */
@RunWith(AndroidJUnit4::class)
class SharedDashboardScreenTest {
    @get:Rule val compose = createComposeRule()

    // Wednesday; the ISO week began on Monday 16 November 2026.
    private val gymToday = LocalDate.of(2026, 11, 18)
    private val asOf = Instant.parse("2026-11-18T20:00:00Z")

    private fun at(date: LocalDate, hour: Int) = date.atTime(hour, 0).toInstant(ZoneOffset.UTC)

    private val bench = requireNotNull(ExerciseCatalog.definition("bench-press")).exercise()
    private val squat = requireNotNull(ExerciseCatalog.definition("back-squat")).exercise()
    private val press = requireNotNull(ExerciseCatalog.definition("overhead-press")).exercise()
    private val custom =
        StrengthExercise(
            "custom-machine",
            "My machine",
            StrengthExercise.Region.UPPER,
            StrengthExercise.Mode.WEIGHTED,
            true,
            StrengthExercise.MeasurementProfile.LOAD_REPS,
        )

    private fun gym(
        id: String,
        date: LocalDate,
        vararg blocks: Pair<StrengthExercise, List<Pair<Int, Double>>>,
    ) =
        StrengthSession(
            id,
            at(date, 18),
            ZoneOffset.UTC,
            Duration.ofMinutes(45),
            null,
            "",
            blocks.map { (exercise, sets) ->
                StrengthSession.ExerciseSets(
                    exercise,
                    sets.mapIndexed { i, (reps, kg) ->
                        StrengthSession.SetEntry("$id-${exercise.id()}-$i", reps, kg, null, false)
                    },
                )
            },
        )

    private val sessions =
        listOf(
            gym("year-ago", gymToday.minusDays(200), bench to listOf(5 to 60.0)),
            gym("last-week", gymToday.minusDays(8), bench to listOf(5 to 75.0)),
            gym("yesterday", gymToday.minusDays(1), bench to listOf(5 to 80.0, 5 to 80.0)),
            gym(
                "today",
                gymToday,
                squat to listOf(5 to 100.0, 5 to 100.0, 5 to 100.0),
                press to listOf(8 to 40.0),
                custom to listOf(10 to 30.0),
            ),
        )

    private fun strength(
        state: CoachUiState,
        workouts: MutableList<String> = CopyOnWriteArrayList(),
        links: MutableList<StrengthLink> = CopyOnWriteArrayList(),
        fontScale: Float = 1f,
    ) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    SharedStrengthDashboard(
                        state,
                        gymToday,
                        asOf,
                        ::shortDate,
                        ::friendlyDate,
                        ::decimal,
                        StrengthLink.entries.toList(),
                        open = { links.add(it) },
                        openWorkout = { workouts.add(it) },
                    )
                }
            }
        }
    }

    @Test
    fun strengthPeriodsExerciseSelectionAndEvidenceLinksUseRecordedWorkouts() {
        val workouts = CopyOnWriteArrayList<String>()
        val links = CopyOnWriteArrayList<StrengthLink>()
        strength(CoachUiState(loading = false, strengthSessions = sessions), workouts, links)

        compose.onNodeWithText("Gym now").performClick()
        assertEquals(listOf(StrengthLink.GYM_NOW), links.toList())

        // 4 weeks: 1 + 2 + 5 working sets; the 200-day-old workout is outside the period.
        compose.onNodeWithTag("kpi-Working sets").assert(hasText("8"))
        compose.onNodeWithTag("kpi-Workouts").assert(hasText("3"))
        compose.onNodeWithTag("kpi-Programme").assert(hasText("No gym programme scheduled"))
        compose.onNodeWithTag("period-ONE_YEAR").performClick()
        compose.onNodeWithTag("kpi-Working sets").assert(hasText("9"))
        compose.onNodeWithTag("kpi-Workouts").assert(hasText("4"))
        compose.onNodeWithTag("period-FOUR_WEEKS").performClick()
        // C-09: Strength uses Progress's tonal period pills (selected with a checkmark and radio
        // semantics), not the plain chip fallback whose selection was shown by colour only.
        compose
            .onNodeWithTag("period-FOUR_WEEKS")
            .assertIsSelected()
            .assert(
                androidx.compose.ui.test.SemanticsMatcher.expectValue(
                    androidx.compose.ui.semantics.SemanticsProperties.Role,
                    androidx.compose.ui.semantics.Role.RadioButton,
                )
            )

        // Back squat has the most eligible sets; choose bench press instead.
        compose.onNodeWithTag("exercise-back-squat").performScrollTo().assertIsSelected()
        // Exercises are chips: choose bench press directly.
        compose.onNodeWithTag("exercise-bench-press").performScrollTo().performClick()
        // The readout keeps the actual heaviest set and the estimate apart, with its sample.
        compose
            .onNodeWithTag("selected-one-rep-max")
            .performScrollTo()
            .assert(hasText("80.0 kg × 5"))
            .assert(hasText("Heaviest set · recorded"))
            .assert(hasText("Estimated 1RM · from 80.0 kg × 5 · 2 qualifying sets"))
        compose.onNodeWithTag("progression-earlier").performScrollTo().performClick()
        compose
            .onNodeWithTag("selected-one-rep-max")
            .assert(hasText("75.0 kg × 5"))
            .assert(hasText("Estimated 1RM · from 75.0 kg × 5 · 1 qualifying set"))
        // Heaviest set and estimate come from the same workout here: one button opens it.
        compose.onNodeWithText("Open workout").performScrollTo().performClick()
        assertEquals("last-week", workouts.last())

        // Tapping the chart near its right edge selects the latest week again.
        compose.onNodeWithTag("one-rep-max-chart").performScrollTo().performTouchInput {
            click(centerRight)
        }
        compose.onNodeWithTag("selected-one-rep-max").assert(hasText("80.0 kg × 5"))

        // Training volume: load × reps by week, with the selected week's workouts to open.
        compose.onNodeWithTag("volume-chart").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("volume-period").assert(hasText("4 weeks:", substring = true))
        compose
            .onNode(
                hasText("Back Squat, Overhead Press, My machine") and
                    hasAnyAncestor(hasTestTag("volume-week-workouts"))
            )
            .performScrollTo()
            .performClick()
        assertEquals("today", workouts.last())
        compose.onNodeWithTag("volume-by-exercise").performScrollTo().assert(
            hasAnyDescendant(hasText("Back Squat"))
        )

        // Muscle workload keeps unclassified and secondary involvement visible and separate,
        // and a group opens the exercises that contributed.
        compose.onNodeWithText("not assigned to a group", substring = true).performScrollTo().assertExists()
        compose.onNodeWithTag("muscle-CHEST").performScrollTo().performClick()
        compose.onNodeWithTag("muscle-CHEST-exercises", useUnmergedTree = true).assert(hasText("Bench Press", substring = true))
        // Volume is labelled as accumulated load × reps, grouped by thousands, so it cannot be
        // read as a lifted weight, a PB or an estimated 1RM.
        org.junit.Assert.assertTrue(
            compose
                .onAllNodesWithText("Training volume (load × reps)")
                .fetchSemanticsNodes()
                .isNotEmpty()
        )
        compose.onAllNodesWithText("load × reps").assertCountEquals(0)
        compose
            .onAllNodes(
                SemanticsMatcher("thousands-grouped volume") { node ->
                    node.config.getOrNull(SemanticsProperties.Text).orEmpty().any {
                        Regex("""\d{1,3}(,\d{3})+ kg""").matches(it.text)
                    }
                }
            )
            .fetchSemanticsNodes()
            .let { org.junit.Assert.assertTrue("A grouped volume is shown", it.isNotEmpty()) }
        compose
            .onNodeWithText("Recorded workload only — not a measure of muscle growth or recovery.", substring = true)
            .performScrollTo()
            .assertExists()

        // Recent workouts still open the actual recorded session.
        compose
            .onNode(
                hasText("Back Squat, Overhead Press, My machine") and
                    !hasAnyAncestor(hasTestTag("volume-week-workouts"))
            )
            .performScrollTo()
            .performClick()
        assertEquals("today", workouts.last())
        compose.onNodeWithText("See all").performScrollTo().performClick()
        assertEquals(StrengthLink.HISTORY, links.last())
        compose.onNodeWithText("Recovery & training balance").performScrollTo().performClick()
        assertEquals(StrengthLink.RECOVERY, links.last())
    }

    @Test
    fun volumeOpensOnTheLatestRecordedWeekAndReturnsToThisWeek() {
        // Nothing recorded this week yet: the chart opens on last week, says so, and one tap
        // returns to the (empty) current week.
        strength(
            CoachUiState(
                loading = false,
                strengthSessions = sessions.filter { it.id() in setOf("year-ago", "last-week") },
            )
        )
        compose.onNodeWithTag("volume-chart").performScrollTo()
        compose.onNodeWithTag("volume-selected-week").assert(hasText("Week of", substring = true))
        compose.onNodeWithText("Latest week with workouts", substring = true).assertExists()
        compose.onNodeWithTag("volume-this-week").performScrollTo().performClick()
        compose.onNodeWithTag("volume-selected-week").assert(hasText("This week"))
        compose.onNodeWithTag("volume-readout").assert(hasText("No workouts"))
        compose.onNodeWithTag("volume-this-week").assertDoesNotExist()
        compose.onNodeWithText("Latest week with workouts", substring = true).assertDoesNotExist()
    }

    @Test
    fun newStrengthUserSeesHonestEmptyStatesAtLargeText() {
        strength(CoachUiState(loading = false), fontScale = 2f)
        compose.onNodeWithTag("kpi-Working sets").assert(hasText("0"))
        compose
            .onNodeWithText(
                "Record weighted working sets of 2–10 repetitions in this period to see an estimate."
            )
            .performScrollTo()
            .assertExists()
        compose
            .onNodeWithText("Record working sets to see how your training is spread.")
            .performScrollTo()
            .assertExists()
        compose.onNodeWithText("No strength workouts recorded yet.").performScrollTo().assertExists()
        compose.onNodeWithTag("one-rep-max-chart").assertDoesNotExist()
    }

    private fun run(
        id: String,
        date: LocalDate,
        metres: Double?,
        movingSeconds: Long?,
        planned: String? = null,
        provider: String = "rdd",
    ): ActivityRecord {
        val start = date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant()
        return ActivityRecord(
            id,
            SourceIdentity(provider, id, null),
            start,
            start.plusSeconds(maxOf(3600L, (movingSeconds ?: 0L) + 60)),
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            metres,
            movingSeconds?.let { Duration.ofSeconds(it) },
            null,
            null,
            null,
            null,
            null,
            null,
            emptyList(),
            emptyList(),
            planned,
            null,
        )
    }

    @Test
    fun progressPeriodsComparisonsAndRunDrillDownUseRecordedRuns() {
        val today = LocalDate.now()
        val activities =
            listOf(
                run("today-run", today, 5_000.0, 1_500),
                run("linked", today.minusDays(2), 6_000.0, 1_800, planned = "plan-done"),
                run("imported", today.minusDays(3), null, null, provider = "gpx"),
                run("old-long", today.minusDays(120), 21_100.0, 7_200),
            )
        val plans =
            listOf(
                PlannedWorkout(
                    "plan-done",
                    today.minusDays(2),
                    WorkoutCategory.EASY_RUN,
                    6.0,
                    null,
                    emptyList(),
                    "",
                ),
                PlannedWorkout(
                    "plan-missed",
                    today.minusDays(4),
                    WorkoutCategory.EASY_RUN,
                    5.0,
                    null,
                    emptyList(),
                    "",
                ),
            )
        val state =
            CoachUiState(loading = false, activities = activities, plannedWorkouts = plans)
        val runs = CopyOnWriteArrayList<String>()
        val links = CopyOnWriteArrayList<ProgressLink>()
        compose.setContent {
            MaterialTheme {
                SharedProgressDashboard(
                    state,
                    androidHomePresentation(state),
                    { it.atZone(ZoneId.systemDefault()).toLocalDate() },
                    ProgressLink.entries.toList(),
                    { runs.add(it) },
                ) {
                    links.add(it)
                }
            }
        }

        compose.onNodeWithText("Distance this week").assertExists()
        compose.onNodeWithTag("dashboard-week-comparison").assert(hasText("Same days last week", substring = true))
        compose.onNodeWithTag("kpi-Distance").assert(hasText("3 runs", substring = true))
        // An average from part of the period names its qualifying sample.
        compose.onNodeWithTag("kpi-Average pace").assert(hasText("Moving pace · based on 2 of 3 runs"))
        compose.onNodeWithTag("kpi-Plan completion").assert(hasText("1 / 2"))
        compose.onNodeWithTag("kpi-Longest run").assert(hasText("6", substring = true))

        compose.onNodeWithTag("period-SIX_MONTHS").performClick()
        compose.onNodeWithTag("kpi-Distance").assert(hasText("4 runs", substring = true))
        // Chart first: the figures sit below the chart, so bring the tile into view to tap it.
        compose.onNodeWithTag("kpi-Longest run").performScrollTo().performClick()
        assertEquals("old-long", runs.last())

        compose.onNodeWithTag("running-trend-chart").performScrollTo().assertExists()
        // Drill-down: select the imported run's week on the chart, then open it from that week.
        val imported = today.minusDays(3)
        val importedWeek = imported.minusDays((imported.dayOfWeek.value - 1).toLong())
        compose
            .onNode(hasContentDescription("Week of ${shortDate(importedWeek)}:", substring = true))
            .performScrollTo()
            .performClick()
        compose.onNodeWithText("Imported GPX", substring = true).performScrollTo().performClick()
        assertEquals("imported", runs.last())
        compose.onNodeWithText("Distance, pace and time trends").performScrollTo().performClick()
        assertEquals(ProgressLink.TRENDS, links.last())
        compose.onNodeWithText("See all").performScrollTo().performClick()
        assertEquals(ProgressLink.HISTORY, links.last())
        compose.onNodeWithText("Strength analytics").performScrollTo().performClick()
        assertEquals(ProgressLink.STRENGTH_ANALYTICS, links.last())
    }

    private fun focusedProgress(
        focus: org.racedaydreams.domain.settings.PrimaryFocus,
        workouts: MutableList<String>,
        links: MutableList<ProgressLink>,
    ) {
        val today = LocalDate.now()
        val state =
            CoachUiState(
                loading = false,
                primaryFocus = focus,
                activities = listOf(run("easy", today.minusDays(1), 5_000.0, 1_500)),
                strengthSessions =
                    listOf(
                        gym("recent-lift", today.minusDays(2), bench to listOf(5 to 80.0)),
                        gym("older-lift", today.minusDays(9), squat to listOf(5 to 100.0)),
                    ),
            )
        compose.setContent {
            MaterialTheme {
                SharedProgressDashboard(
                    state,
                    androidHomePresentation(state),
                    { it.atZone(ZoneId.systemDefault()).toLocalDate() },
                    ProgressLink.entries.toList(),
                    {},
                    Instant.now(),
                    { workouts.add(it) },
                ) {
                    links.add(it)
                }
            }
        }
    }

    @Test
    fun strengthFocusLeadsWithLiftingAndKeepsRunningSecondaryButReachable() {
        val workouts = CopyOnWriteArrayList<String>()
        val links = CopyOnWriteArrayList<ProgressLink>()
        focusedProgress(org.racedaydreams.domain.settings.PrimaryFocus.STRENGTH, workouts, links)
        // The chart leads, so the figures may start below the fold on small screens.
        compose.onNodeWithTag("kpi-Working sets").assert(hasText("2")).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("kpi-Workouts").assert(hasText("2"))
        // Strength focus opens on the strength tab, chart first: progression above the figures.
        compose.onNodeWithTag("progress-tab-STRENGTH").assertIsSelected()
        org.junit.Assert.assertTrue(
            compose.onNodeWithText("Exercise progression").fetchSemanticsNode().positionInRoot.y <
                compose.onNodeWithTag("kpi-Working sets").fetchSemanticsNode().positionInRoot.y
        )
        compose.onNodeWithText("Recent workouts").assertExists()
        compose.onNodeWithText("Exercise progression").assertExists()
        compose.onNodeWithText("Training volume").assertExists()
        compose.onNodeWithText("Muscle-group workload").assertExists()
        compose.onNodeWithText("Distance this week").assertDoesNotExist()
        compose.onNodeWithTag("kpi-Distance").assertDoesNotExist()
        compose
            .onAllNodes(hasText("Bench Press") and hasAnyAncestor(hasClickAction()))
            .onFirst()
            .performScrollTo()
            .performClick()
        assertEquals("recent-lift", workouts.last())
        // Every running analytic stays one tap away.
        compose.onNodeWithText("Training trends").performScrollTo().performClick()
        assertEquals(ProgressLink.TRENDS, links.last())
        compose.onNodeWithText("Run history").performScrollTo().performClick()
        assertEquals(ProgressLink.HISTORY, links.last())
        compose.onNodeWithText("Running PBs").performScrollTo().performClick()
        assertEquals(ProgressLink.PBS, links.last())
        // Running analytics are one tab away.
        compose.onNodeWithTag("progress-tab-RUNNING").performScrollTo().performClick()
        compose.onNodeWithText("Distance this week").assertExists()
        compose.onNodeWithTag("kpi-Distance").assert(hasText("1 run", substring = true))
        compose.onNodeWithTag("progress-strength-hero").assertDoesNotExist()
    }

    @Test
    fun hybridFocusShowsRunningAndStrengthAtTheSameDepth() {
        val workouts = CopyOnWriteArrayList<String>()
        val links = CopyOnWriteArrayList<ProgressLink>()
        focusedProgress(org.racedaydreams.domain.settings.PrimaryFocus.HYBRID, workouts, links)
        // Hybrid opens on an overview with both disciplines; each has its own tab too.
        compose.onNodeWithTag("progress-tab-OVERVIEW").assertIsSelected()
        compose.onNodeWithTag("progress-tab-RUNNING").assertExists()
        compose.onNodeWithTag("progress-tab-STRENGTH").assertExists()
        compose.onNodeWithText("Distance this week").assertExists()
        compose.onNodeWithTag("progress-strength-hero").assertExists()
        // The compact overview never repeats the chart's distance: counts and last dates only,
        // for both disciplines alike.
        compose
            .onNodeWithTag("progress-running-summary")
            .assert(hasText("Last: ", substring = true))
            .assert(!hasText(" km", substring = true))
        compose.onNodeWithTag("progress-strength-hero").assert(hasText("Last: ", substring = true))
        compose.onNodeWithTag("kpi-Distance").assert(hasText("1 run", substring = true))
        compose.onNodeWithTag("kpi-Working sets").performScrollTo().assert(hasText("2"))
        compose.onNodeWithTag("week-runs").performScrollTo().assertExists()
        compose.onNodeWithText("Recent workouts").performScrollTo().assertExists()
        compose.onNodeWithTag("kpi-New records").performScrollTo().performClick()
        assertEquals(ProgressLink.STRENGTH_ANALYTICS, links.last())
    }

    @Test
    fun overviewComposesOneHybridPanelLabelledSectionsAndGroupedDrillDowns() {
        val workouts = CopyOnWriteArrayList<String>()
        val links = CopyOnWriteArrayList<ProgressLink>()
        focusedProgress(org.racedaydreams.domain.settings.PrimaryFocus.HYBRID, workouts, links)
        // Running and strength share one panel, side by side at default text.
        compose.onNodeWithTag("progress-hybrid-summary").assertIsDisplayed()
        compose
            .onNode(hasTestTag("progress-running-summary") and hasAnyAncestor(hasTestTag("progress-hybrid-summary")))
            .assertExists()
        compose
            .onNode(hasTestTag("progress-strength-hero") and hasAnyAncestor(hasTestTag("progress-hybrid-summary")))
            .assertExists()
        val running = compose.onNodeWithTag("progress-running-summary").fetchSemanticsNode().boundsInRoot
        val strength = compose.onNodeWithTag("progress-strength-hero").fetchSemanticsNode().boundsInRoot
        assertTrue("Running and strength sit side by side", running.right <= strength.left + 1f)
        // Both figure sets are labelled sections at the same depth, below the running chart.
        for (title in listOf("Weekly trend", "Runs this week", "Running", "Strength"))
            compose
                .onNode(hasText(title) and isHeading())
                .performScrollTo()
                .assertIsDisplayed()
        // Drill-downs are glyph-labelled groups of the existing destinations, not one card.
        for (group in listOf("Running detail", "Milestones & body", "Strength detail"))
            compose
                .onNode(
                    hasText(group) and isHeading() and hasAnyAncestor(hasTestTag("progress-drill-downs"))
                )
                .performScrollTo()
                .assertIsDisplayed()
        compose.onNodeWithText("More detail").assertDoesNotExist()
        compose.onNodeWithText("Body weight").performScrollTo().performClick()
        assertEquals(ProgressLink.BODY_WEIGHT, links.last())
        compose.onNodeWithText("Distance, pace and time trends").performScrollTo().performClick()
        assertEquals(ProgressLink.TRENDS, links.last())
    }

    @Test
    fun overviewHybridSummaryStacksAt200PercentText() {
        val today = LocalDate.now()
        val state =
            CoachUiState(
                loading = false,
                primaryFocus = org.racedaydreams.domain.settings.PrimaryFocus.HYBRID,
                activities = listOf(run("easy", today.minusDays(1), 5_000.0, 1_500)),
                strengthSessions = listOf(gym("lift", today.minusDays(2), bench to listOf(5 to 80.0))),
            )
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MaterialTheme {
                    SharedProgressDashboard(
                        state,
                        androidHomePresentation(state),
                        { it.atZone(ZoneId.systemDefault()).toLocalDate() },
                        ProgressLink.entries.toList(),
                        {},
                        Instant.now(),
                        {},
                    ) {}
                }
            }
        }
        val running = compose.onNodeWithTag("progress-running-summary").fetchSemanticsNode().boundsInRoot
        val strength = compose.onNodeWithTag("progress-strength-hero").fetchSemanticsNode().boundsInRoot
        assertTrue("Strength is stacked below running at 200%", strength.top >= running.bottom - 1f)
        compose.onNodeWithTag("progress-strength-hero").performScrollTo().assertIsDisplayed()
        for (period in listOf("FOUR_WEEKS", "TWELVE_WEEKS", "SIX_MONTHS", "ONE_YEAR"))
            compose
                .onNodeWithTag("period-$period")
                .performScrollTo()
                .assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("period-TWELVE_WEEKS").performClick()
        compose.onNodeWithTag("period-TWELVE_WEEKS").assertIsSelected()
    }

    @Test
    fun chartFirstRunningExplorerSwitchesMeasureAndSelectsWeeksWithoutNewFigures() {
        val today = LocalDate.now()
        val state =
            CoachUiState(
                loading = false,
                activities =
                    listOf(
                        run("now", today, 5_000.0, 1_500),
                        run("earlier", today.minusDays(14), 8_000.0, 2_400),
                    ),
            )
        compose.setContent {
            MaterialTheme {
                SharedProgressDashboard(
                    state,
                    androidHomePresentation(state),
                    { it.atZone(ZoneId.systemDefault()).toLocalDate() },
                    ProgressLink.entries.toList(),
                    {},
                ) {}
            }
        }
        // The readout leads with this week's recorded distance.
        compose.onNodeWithText("Distance this week").assertIsDisplayed()
        compose.onNodeWithTag("dashboard-week-distance").assert(hasText("5", substring = true))
        compose.onNodeWithText("Runs this week").assertExists()
        // Selecting an earlier week updates the readout and its runs.
        val earlier = today.minusDays(14)
        val earlierWeek = earlier.minusDays((earlier.dayOfWeek.value - 1).toLong())
        compose
            .onNode(hasContentDescription("Week of ${shortDate(earlierWeek)}:", substring = true))
            .performClick()
        compose.onNodeWithText("Runs that week").assertExists()
        compose.onNodeWithText("Week of ${friendlyDate(earlierWeek)}").assertExists()
        // The figure, period label and runs all follow the selection: 8 km from the earlier run
        // only, never this week's 5 km.
        compose.onNodeWithText("Distance this week").assertDoesNotExist()
        compose.onNodeWithTag("trend-readout-value").assert(hasText("8", substring = true))
        compose.onNodeWithTag("trend-readout-value").assert(!hasText("5", substring = true))
        compose.onNodeWithTag("week-runs").performScrollTo()
        compose
            .onNodeWithTag("week-runs")
            .assert(hasAnyDescendant(hasText(friendlyDate(earlier), substring = true)))
            .assert(!hasAnyDescendant(hasText(friendlyDate(today), substring = true)))
        // Returning to the current week restores its label, figure and runs.
        compose
            .onNode(hasContentDescription("Week of ${shortDate(weekOf(today))}:", substring = true))
            .performScrollTo()
            .performClick()
        compose.onNodeWithText("Distance this week").assertExists()
        compose.onNodeWithTag("dashboard-week-distance").assert(hasText("5", substring = true))
        compose
            .onNodeWithTag("week-runs")
            .assert(hasAnyDescendant(hasText(friendlyDate(today), substring = true)))
            .assert(!hasAnyDescendant(hasText(friendlyDate(earlier), substring = true)))
        // The same-days comparison is about distance, so only Distance shows it.
        compose.onNodeWithTag("dashboard-week-comparison").assertExists()
        // Pace is the existing weekly elapsed pace; switching measure never adds a figure.
        compose.onNodeWithTag("trend-metric-PACE").performClick()
        compose.onNodeWithText("Higher is faster: less time per kilometre.").assertExists()
        compose.onNodeWithText("Pace this week").assertExists()
        compose.onNodeWithTag("trend-readout-value").assert(hasText("/km", substring = true))
        compose.onNodeWithTag("dashboard-week-comparison").assertDoesNotExist()
        compose.onNodeWithText("Same days last week", substring = true).assertDoesNotExist()
        compose.onNodeWithTag("trend-metric-TIME").performClick()
        compose.onNodeWithTag("running-trend-chart").assertExists()
        compose.onNodeWithText("Training time this week").assertExists()
        compose.onNodeWithTag("trend-readout-value").assert(hasText("min", substring = true))
        compose.onNodeWithTag("dashboard-week-comparison").assertDoesNotExist()
        // Back on Distance, the original figure and comparison return.
        compose.onNodeWithTag("trend-metric-DISTANCE").performClick()
        compose.onNodeWithText("Distance this week").assertExists()
        compose.onNodeWithTag("dashboard-week-distance").assert(hasText("5", substring = true))
        compose.onNodeWithTag("dashboard-week-comparison").assertExists()
    }

    private fun weekOf(date: LocalDate) = date.minusDays((date.dayOfWeek.value - 1).toLong())

    /**
     * Shows running Progress at [fontScale] (and, when given, a narrow [width]) and checks every
     * measure option is shown whole on one line, at least 48 dp tall, and selectable.
     */
    private fun checkMeasureSelector(fontScale: Float, width: androidx.compose.ui.unit.Dp?, chips: Boolean?) {
        val today = LocalDate.now()
        val state =
            CoachUiState(
                loading = false,
                activities = listOf(run("now", today, 5_000.0, 1_500)),
            )
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MaterialTheme {
                    androidx.compose.foundation.layout.Box(
                        if (width == null) androidx.compose.ui.Modifier
                        else androidx.compose.ui.Modifier.width(width)
                    ) {
                        SharedProgressDashboard(
                            state,
                            androidHomePresentation(state),
                            { it.atZone(ZoneId.systemDefault()).toLocalDate() },
                            ProgressLink.entries.toList(),
                            {},
                        ) {}
                    }
                }
            }
        }
        // Null: the device width decides; either layout must pass the same checks.
        if (chips == true) compose.onNodeWithTag("trend-metric-chips").performScrollTo().assertIsDisplayed()
        else if (chips == false) compose.onNodeWithTag("trend-metric-chips").assertDoesNotExist()
        for ((metric, label) in listOf("DISTANCE" to "Distance", "TIME" to "Time", "PACE" to "Pace")) {
            val option = compose.onNodeWithTag("trend-metric-$metric")
            option.performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
            option.assert(hasText(label))
            // The full word is laid out on one line, not ellipsised, cut off at the side or
            // drawn outside its control.
            val labelNode =
                compose
                    .onNode(
                        hasText(label) and hasAnyAncestor(hasTestTag("trend-metric-$metric")),
                        useUnmergedTree = true,
                    )
                    .fetchSemanticsNode()
            val layout = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            labelNode.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layout)
            val text = layout.single()
            assertEquals("$label lines", 1, text.lineCount)
            assertEquals("$label ellipsised", false, text.isLineEllipsized(0))
            val outer = option.fetchSemanticsNode().boundsInRoot
            val inner = labelNode.boundsInRoot
            // The word's natural width fits the label's actual on-screen width.
            val natural = text.multiParagraph.intrinsics.maxIntrinsicWidth
            assertTrue("$label needs $natural px, has ${inner.width}", natural <= inner.width + 1f)
            assertTrue(
                "$label $inner outside $outer",
                inner.left >= outer.left - 1f &&
                    inner.right <= outer.right + 1f &&
                    inner.top >= outer.top - 1f &&
                    inner.bottom <= outer.bottom + 1f,
            )
        }
        compose.onNodeWithTag("trend-metric-DISTANCE").assertIsSelected()
        compose.onNodeWithTag("trend-metric-PACE").performClick()
        compose.onNodeWithTag("trend-metric-PACE").assertIsSelected()
        compose.onNodeWithTag("trend-metric-DISTANCE").assertIsNotSelected()
        compose
            .onNodeWithText("Higher is faster: less time per kilometre.")
            .performScrollTo()
            .assertExists()
    }

    @Test
    fun runningMeasureSelectorKeepsEveryChoiceReadableAt200PercentText() =
        checkMeasureSelector(2f, null, chips = true)

    @Test
    fun runningMeasureSelectorWrapsOnANarrowScreenAt200PercentText() =
        checkMeasureSelector(2f, 280.dp, chips = true)

    @Test
    fun runningMeasureSelectorWrapsOnANarrowScreenAtDefaultText() =
        checkMeasureSelector(1f, 280.dp, chips = true)

    @Test
    fun runningMeasureSelectorIsWholeAtDefaultTextOnThisDevice() =
        checkMeasureSelector(1f, null, chips = null)

    @Test
    fun oneRepMaxWithASingleWeekShowsTheEstimateWithoutImplyingATrend() {
        // Back squat, the default exercise, has an estimate in this week only.
        strength(CoachUiState(loading = false, strengthSessions = sessions))
        compose.onNodeWithTag("selected-one-rep-max").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("one-rep-max-single-week").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("one-rep-max-chart").assertDoesNotExist()
        compose.onNodeWithText("Range", substring = true).assertDoesNotExist()
        compose.onNodeWithTag("progression-earlier").assertDoesNotExist()
        compose.onNodeWithTag("selected-one-rep-max").assert(hasText("Heaviest set · recorded"))
        compose.onAllNodesWithText("Open workout").onFirst().assertExists()

        // Bench press has estimates in two weeks, so the trend line appears.
        compose.onNodeWithTag("exercise-bench-press").performScrollTo().performClick()
        compose.onNodeWithTag("exercise-bench-press").assertIsSelected()
        compose.onNodeWithTag("one-rep-max-chart").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("one-rep-max-single-week").assertDoesNotExist()
        compose.onNodeWithTag("progression-earlier").performScrollTo().assertIsEnabled()
    }
}
