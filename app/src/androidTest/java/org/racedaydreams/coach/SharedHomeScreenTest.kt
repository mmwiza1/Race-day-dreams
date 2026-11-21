package org.racedaydreams.coach

import android.net.Uri
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
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

    private fun state(completed: Boolean = false): CoachUiState {
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
                TrainingPlan("plan", day, day.plusWeeks(2), emptyList(), listOf(target, future)),
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
}
