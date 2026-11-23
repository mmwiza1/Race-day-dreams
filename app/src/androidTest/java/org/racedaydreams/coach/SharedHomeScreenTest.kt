package org.racedaydreams.coach

import android.net.Uri
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.theme.RaceDayTheme
import org.racedaydreams.domain.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.presentation.StrengthTodaySummary
import org.racedaydreams.domain.settings.PrimaryFocus

/** Full existing Home UI, original Android formatters/routes, fixed test clock and real targets. */
@RunWith(AndroidJUnit4::class)
class SharedHomeScreenTest {
    @get:Rule val compose = createComposeRule()
    private val day = LocalDate.of(2026, 11, 17)
    private val actions = mutableListOf<String>()

    private fun state(completed: Boolean = false, second: Boolean = false): CoachUiState {
        val profile =
            UserProfile(
                RaceGoal(RaceDistance.GENERAL_FITNESS, "", null, GoalMode.FINISH, null),
                TrainingPreferences(
                    TrainingStrategy.BALANCED,
                    DayOfWeek.values().toSet(),
                    3,
                    DayOfWeek.SUNDAY,
                    emptySet(),
                    ScheduleType.FLEXIBLE,
                    UnitSystem.METRIC,
                ),
                false,
            )
        val target =
            PlannedWorkout(
                "today/one",
                day,
                WorkoutCategory.EASY_RUN,
                5.0,
                Duration.ofMinutes(30),
                listOf(
                    WorkoutSegment(
                        WorkoutSegment.Kind.RUN,
                        null,
                        Duration.ofMinutes(30),
                        300.0,
                        360.0,
                        null,
                        5.0,
                        "Original target",
                    )
                ),
                "Original",
            )
        val future = target.copy(id = "next/one", date = day.plusDays(2))
        val plan =
            PlanVersion(
                "version/one",
                "plan",
                1,
                Instant.parse("2026-11-16T12:00:00Z"),
                "Athlete choice",
                profile,
                "Retained evidence",
                TrainingPlan(
                    "plan",
                    day,
                    day.plusWeeks(2),
                    emptyList(),
                    listOf(target, future) +
                        if (second) listOf(future.copy(id = "next/two")) else emptyList(),
                ),
            )
        return CoachUiState(
            loading = false,
            primaryFocus = PrimaryFocus.RUNNING,
            profile = profile,
            currentPlan = plan,
            runResponses =
                if (completed)
                    listOf(
                        RunExecution(
                            "actual",
                            "today/one",
                            "version/one",
                            null,
                            null,
                            "Actual response",
                        )
                    )
                else emptyList(),
        )
    }

    private fun show(state: CoachUiState, dark: Boolean = false, large: Boolean = false) {
        val summary =
            StrengthTodaySummary(
                null,
                false,
                day.minusDays((day.dayOfWeek.value - 1).toLong()),
                0,
                0,
                0,
                0,
                null,
            )
        val presentation =
            HomePresentation(
                { day },
                { "Tuesday, 17 November" },
                ::friendlyDate,
                ::shortDate,
                ::workoutName,
                ::decimal,
                ::formatPaceSeconds,
                ::displayActivityDate,
                { formatDistance(it, state) },
                Uri::encode,
            )
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, if (large) 2f else 1f)
            ) {
                RaceDayTheme(darkTheme = dark, dynamicColour = false) {
                    SharedHomeScreen(
                        state,
                        presentation,
                        {
                            SharedStrengthTodayScreen(
                                summary,
                                day,
                                "Tuesday, 17 November",
                                false,
                                null,
                                { actions.add(it) },
                                { actions.add("session:$it") },
                                { actions.add("gym-now") },
                            )
                        },
                        { actions.add("weekly") },
                        { actions.add("baseline") },
                        { actions.add("assessment") },
                        { actions.add("history") },
                        { actions.add("plan") },
                        { actions.add("workout:$it") },
                        { actions.add("record") },
                        { actions.add(it) },
                        { actions.add("race") },
                    )
                }
            }
        }
    }

    @Test
    fun runningTargetsAndEncodedLiveCoachRouteStaySeparateFromActualEvidence() {
        val original = state()
        show(original)
        compose.onNodeWithText("Tuesday, 17 November").assertIsDisplayed()
        // Today's own distance: the plain fact line, not a tappable Coming up row that may
        // show the same distance for a later session.
        compose
            .onNode(hasText("${decimal(5.0)} km") and !hasAnyAncestor(hasClickAction()))
            .assertExists()
        compose.onNodeWithText("5:00–6:00/km").assertExists()
        compose.onNodeWithText("Start with Live Coach").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(listOf("execute/version%2Fone/today%2Fone"), actions)
            assertTrue(original.activities.isEmpty())
            assertTrue(original.runResponses.isEmpty())
            assertEquals(5.0, original.currentPlan!!.plan().workouts().first().distanceKm()!!, 0.0)
        }
        compose.onNodeWithText("View workout details").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("workout:today/one", actions.last()) }
    }

    @Test
    fun completedRunningAt200PercentKeepsDetailsUpcomingAndIndependentRecordingScrollable() {
        show(state(completed = true), dark = true, large = true)
        compose
            .onNodeWithText("Completed training is retained. No catch-up mileage.")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Start with Live Coach").assertDoesNotExist()
        compose.onNodeWithText("View completed workout").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf("workout:today/one"), actions) }
        compose.onNodeWithText(shortDate(day.plusDays(2))).performScrollTo().assertIsDisplayed()
        compose.onNode(hasText("Run now") and hasClickAction()).performScrollTo().performClick()
        compose.runOnIdle { assertEquals("record", actions.last()) }
        compose.onNodeWithText("Review running baseline").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("baseline", actions.last()) }
    }

    @Test
    fun strengthOnlyHomeUsesExistingStrengthTodayWithoutRunningCardsOrRaceSetup() {
        show(CoachUiState(loading = false, primaryFocus = PrimaryFocus.STRENGTH), dark = true)
        compose
            .onNodeWithText(
                "Build a programme or log a workout independently. No race goal is needed."
            )
            .assertExists()
        compose.onNodeWithText("Current goal").assertDoesNotExist()
        compose.onNodeWithText("Review running baseline").assertDoesNotExist()
        compose.onNodeWithText("Run now").assertDoesNotExist()
        compose
            .onNode(hasText("Start unplanned workout") and hasClickAction())
            .performScrollTo()
            .performClick()
        compose.runOnIdle { assertEquals(listOf("gym-now"), actions) }
    }

    @Test
    fun nextSevenDaysStartsTodayAndCountsOnlyWhatIsStillAhead() {
        // Tuesday's run is today and Thursday's is next; the other days are rest days.
        val days = WeekAtAGlance.nextSevenDays(state(), day, null, ::workoutName)
        assertEquals((0L..6L).map { day.plusDays(it) }, days.map { it.date })
        assertEquals(
            listOf("Tue", "Wed", "Thu", "Fri", "Sat", "Sun", "Mon"),
            days.map { it.weekday },
        )
        assertEquals(SessionMark.PLANNED, days[0].sessions.single().mark)
        assertEquals(SessionMark.PLANNED, days[2].sessions.single().mark)
        assertTrue(days.filterIndexed { i, _ -> i != 0 && i != 2 }.all { it.sessions.isEmpty() })
        assertEquals("2 runs planned", WeekAtAGlance.summary(days))
        // Today's run, once recorded, is done and no longer counted as upcoming.
        val done = WeekAtAGlance.nextSevenDays(state(completed = true), day, null, ::workoutName)
        assertEquals(SessionMark.DONE, done[0].sessions.single().mark)
        assertEquals("1 run planned", WeekAtAGlance.summary(done))
        // Seen from Thursday, Tuesday has left the window rather than showing as missed.
        val later = WeekAtAGlance.nextSevenDays(state(), day.plusDays(2), null, ::workoutName)
        assertEquals(day.plusDays(2), later.first().date)
        assertEquals("1 run planned", WeekAtAGlance.summary(later))
    }

    @Test
    fun nextSevenDaysFollowsTheCalendarFromSundayAndAcrossMonthAndYearEnds() {
        val sunday = LocalDate.of(2026, 11, 22)
        val fromSunday = WeekAtAGlance.nextSevenDays(state(), sunday, null, ::workoutName)
        assertEquals(
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"),
            fromSunday.map { it.weekday },
        )
        assertEquals("Sun 22 Nov – Sat 28 Nov", WeekAtAGlance.range(fromSunday))
        val monday = WeekAtAGlance.nextSevenDays(state(), sunday.plusDays(1), null, ::workoutName)
        assertEquals("Mon 23 Nov – Sun 29 Nov", WeekAtAGlance.range(monday))
        val monthEnd =
            WeekAtAGlance.nextSevenDays(state(), LocalDate.of(2026, 11, 28), null, ::workoutName)
        assertEquals(LocalDate.of(2026, 12, 4), monthEnd.last().date)
        assertEquals("Sat 28 Nov – Fri 4 Dec", WeekAtAGlance.range(monthEnd))
        val yearEnd =
            WeekAtAGlance.nextSevenDays(state(), LocalDate.of(2026, 12, 29), null, ::workoutName)
        assertEquals(LocalDate.of(2027, 1, 4), yearEnd.last().date)
        assertEquals("Tue 29 Dec 2026 – Mon 4 Jan 2027", WeekAtAGlance.range(yearEnd))
        // A leap day is an ordinary calendar day in the window.
        val leap =
            WeekAtAGlance.nextSevenDays(state(), LocalDate.of(2028, 2, 27), null, ::workoutName)
        assertEquals(listOf(27, 28, 29, 1, 2, 3, 4), leap.map { it.date.dayOfMonth })
    }

    @Test
    fun multipleSessionsOnOneDayAreAllShownAndCounted() {
        val days = WeekAtAGlance.nextSevenDays(state(second = true), day, null, ::workoutName)
        assertEquals(2, days[2].sessions.size)
        assertEquals("3 runs planned", WeekAtAGlance.summary(days))
    }

    @Test
    fun nextSevenDaysDescribesEachDayAndOpensTheSelectedDaysSessions() {
        show(state(second = true))
        compose.onNodeWithText("Your next 7 days").assertExists()
        compose.onNodeWithText("3 runs planned").assertExists()
        compose.onNodeWithText("Tue 17 Nov – Mon 23 Nov").assertExists()
        compose
            .onNodeWithContentDescription("Tuesday 17 November, today:", substring = true)
            .assert(hasContentDescription("planned", substring = true))
        compose
            .onNodeWithContentDescription("Wednesday 18 November: rest day, nothing scheduled")
            .assertExists()
        // Nothing is open until a day is chosen, so the strip does not repeat today's hero.
        compose.onNodeWithTag("week-day-detail").assertDoesNotExist()
        // The strip follows today's hero, so bring it into view first.
        compose.onNodeWithTag("week-strip").performScrollTo()
        compose.onNodeWithTag("week-day-${day.plusDays(2)}").performClick()
        compose.onNodeWithTag("week-day-${day.plusDays(2)}").assertIsSelected()
        compose.onNodeWithTag("week-day-detail").assertExists()
        compose.onNodeWithText("Thu 19 Nov").assertExists()
        compose.onAllNodesWithText(" · Planned", substring = true).assertCountEquals(2)
        // Choosing a rest day says so; choosing it again closes the detail.
        compose.onNodeWithTag("week-day-${day.plusDays(1)}").performClick()
        compose.onNodeWithText("Rest day: nothing scheduled.").assertExists()
        compose.onNodeWithTag("week-day-${day.plusDays(1)}").performClick()
        compose.onNodeWithTag("week-day-detail").assertDoesNotExist()
        // Every day is at least a 48 dp target.
        for (offset in 0L..6L) compose
            .onNodeWithTag("week-day-${day.plusDays(offset)}")
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun todayLeadsHomeAndUnplannedStartsAreRecognisableTiles() {
        show(state())
        // Today's training comes first, then the days ahead.
        val hero = compose.onNodeWithTag("today-hero").fetchSemanticsNode().positionInRoot.y
        val strip = compose.onNodeWithTag("week-strip").fetchSemanticsNode().positionInRoot.y
        assertTrue("Today's hero is above the next 7 days", hero < strip)
        // "Today's run" is read as words, not spelt out as capitals.
        compose.onNodeWithText("Today's run").assertExists()
        compose.onNodeWithText("Start with Live Coach").assertHeightIsAtLeast(48.dp)
        compose.onNode(hasText("Gym now") and hasClickAction()).performScrollTo().performClick()
        compose.runOnIdle { assertEquals("gym-now", actions.last()) }
        compose.onNode(hasText("Run now") and hasClickAction()).assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun nextSevenDaysStacksAsReadableRowsAt200PercentText() {
        show(state(), large = true)
        // Each row speaks as a whole (its visible "Tue 17 Nov · Today" is part of that).
        compose
            .onNodeWithContentDescription("Tuesday 17 November, today:", substring = true)
            .assertExists()
        for (offset in 0L..6L) {
            val row = compose.onNodeWithTag("week-day-${day.plusDays(offset)}")
            row.performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        }
        compose.onNodeWithTag("week-day-${day.plusDays(2)}").performScrollTo().performClick()
        compose.onNodeWithTag("week-day-detail").performScrollTo().assertIsDisplayed()
    }
}
