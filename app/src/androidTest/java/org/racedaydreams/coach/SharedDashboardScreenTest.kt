package org.racedaydreams.coach

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.Assert.assertEquals
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

        // Back squat has the most eligible sets; choose bench press instead.
        compose.onNodeWithTag("exercise-selector").performScrollTo().assert(hasText("Back Squat ▾"))
        compose.onNodeWithTag("exercise-selector").performClick()
        // Recent workouts also say "Bench Press"; pick the menu entry.
        compose.onNode(hasText("Bench Press") and hasAnyAncestor(isPopup())).performClick()
        compose
            .onNodeWithTag("selected-one-rep-max")
            .performScrollTo()
            .assert(hasText("From 80.0 kg × 5 on 17 November 2026"))
        compose.onNodeWithText("Heaviest recorded set").performScrollTo().assertExists()
        compose.onNodeWithText("‹ Earlier").performScrollTo().performClick()
        compose
            .onNodeWithTag("selected-one-rep-max")
            .assert(hasText("From 75.0 kg × 5 on 10 November 2026"))
        compose.onNodeWithText("Open workout").performScrollTo().performClick()
        assertEquals("last-week", workouts.last())

        // Tapping the chart near its right edge selects the latest week again.
        compose.onNodeWithTag("one-rep-max-chart").performScrollTo().performTouchInput {
            click(centerRight)
        }
        compose
            .onNodeWithTag("selected-one-rep-max")
            .assert(hasText("From 80.0 kg × 5 on 17 November 2026"))

        // Muscle workload keeps unclassified and secondary involvement visible and separate.
        compose.onNodeWithText("Unclassified").performScrollTo().assertExists()
        compose
            .onNodeWithText("Also involved secondarily", substring = true)
            .performScrollTo()
            .assertExists()
        // Recent workouts label accumulated load × reps as training volume, grouped by thousands,
        // so it cannot be read as a lifted weight, a PB or an estimated 1RM.
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
            .onNodeWithText("Recorded workload only — not a measure of muscle growth or recovery.")
            .performScrollTo()
            .assertExists()

        // Recent workouts open the actual recorded session.
        compose
            .onNodeWithText("Back Squat, Overhead Press, My machine")
            .performScrollTo()
            .performClick()
        assertEquals("today", workouts.last())
        compose.onNodeWithText("See all").performScrollTo().performClick()
        assertEquals(StrengthLink.HISTORY, links.last())
        compose.onNodeWithText("Recovery & training balance").performScrollTo().performClick()
        assertEquals(StrengthLink.RECOVERY, links.last())
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
        compose.onNodeWithTag("kpi-Longest run").performClick()
        assertEquals("old-long", runs.last())

        compose.onNodeWithTag("running-trend-chart").performScrollTo().assertExists()
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
        compose.onNodeWithTag("kpi-Working sets").assert(hasText("2")).assertIsDisplayed()
        compose.onNodeWithTag("kpi-Workouts").assert(hasText("2"))
        // Strength focus opens on the strength tab: lifting trends, no running figures.
        compose.onNodeWithTag("progress-tab-STRENGTH").assertIsSelected()
        compose.onNodeWithTag("progress-strength-hero").assertExists()
        compose.onNodeWithText("Recent workouts").assertExists()
        compose.onNodeWithText("Estimated 1RM trend").assertExists()
        compose.onNodeWithText("Muscle-group workload").assertExists()
        compose.onNodeWithText("Distance this week").assertDoesNotExist()
        compose.onNodeWithTag("kpi-Distance").assertDoesNotExist()
        compose.onNodeWithText("Bench Press").performScrollTo().performClick()
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
        compose.onNodeWithTag("kpi-Distance").assert(hasText("1 run", substring = true))
        compose.onNodeWithTag("kpi-Working sets").performScrollTo().assert(hasText("2"))
        compose.onNodeWithText("Recent runs").performScrollTo().assertExists()
        compose.onNodeWithText("Recent workouts").performScrollTo().assertExists()
        compose.onNodeWithTag("kpi-New records").performScrollTo().performClick()
        assertEquals(ProgressLink.STRENGTH_ANALYTICS, links.last())
    }
}
