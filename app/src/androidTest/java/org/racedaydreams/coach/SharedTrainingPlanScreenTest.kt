package org.racedaydreams.coach

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

/** Real original plan UI; fixed dates and required actions cannot mutate prescribed evidence. */
@RunWith(AndroidJUnit4::class)
class SharedTrainingPlanScreenTest {
    @get:Rule val compose = createComposeRule()
    private val day = LocalDate.of(2026, 11, 17)
    private val actions = mutableListOf<String>()
    private val dates =
        object : CoachCalendarDates by AndroidCoachCalendarDates {
            override fun today() = day
        }

    private fun state(): CoachUiState {
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
        val workout =
            PlannedWorkout(
                "original/today",
                day,
                WorkoutCategory.EASY_RUN,
                5.0,
                Duration.ofMinutes(30),
                emptyList(),
                "Original prescription",
            )
        val plan =
            PlanVersion(
                "original/version",
                "plan",
                1,
                Instant.parse("2026-11-16T12:00:00Z"),
                "Athlete approved",
                profile,
                "Original retained evidence",
                TrainingPlan(
                    "plan",
                    day,
                    day.plusWeeks(2),
                    emptyList(),
                    listOf(workout, workout.copy(id = "original/next", date = day.plusWeeks(1))),
                ),
            )
        return CoachUiState(
            loading = false,
            profile = profile,
            currentPlan = plan,
            planVersions = listOf(plan),
        )
    }

    private fun show(state: CoachUiState, large: Boolean = false) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, if (large) 2f else 1f)
            ) {
                RaceDayTheme(darkTheme = large, dynamicColour = false) {
                    SharedTrainingPlanScreen(
                        state,
                        dates,
                        { "Week beginning $it" },
                        { formatDistance(it, state) },
                        { actions.add("generate") },
                        { proposal, choice -> actions.add("${proposal.id()}:$choice") },
                        { actions.add("workout:$it") },
                        { actions.add("goal") },
                        { actions.add("strength:$it") },
                        { id, related -> actions.add("routine:$id:$related") },
                        { id, related, open -> WarmupTestLink(id, related, open) },
                        {
                            androidx.compose.material3.TextButton(
                                onClick = { actions.add("mobility") }
                            ) {
                                androidx.compose.material3.Text("Existing mobility evidence")
                            }
                        },
                    )
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WarmupTestLink(id: String, related: String, open: () -> Unit) {
        androidx.compose.material3.TextButton(onClick = open) {
            androidx.compose.material3.Text("Original warm-up:$related")
        }
    }

    @Test
    fun emptyPlanDoesNotInventWorkoutsOrGenerateBeforeAthleteChooses() {
        show(CoachUiState(loading = false))
        compose.onNodeWithText("Your next chapter").assertIsDisplayed()
        compose.runOnIdle { assertTrue(actions.isEmpty()) }
        compose.onNodeWithText("Generate my programme").performScrollTo().performClick()
        compose.onNodeWithText("Edit goal and routine").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf("generate", "goal"), actions) }
    }

    @Test
    fun dark200PercentWeekNavigationAndRebuildConfirmationKeepOriginalHistory() {
        val original = state()
        show(original, large = true)
        compose.onNodeWithText("Week beginning 2026-11-16").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Previous week").assertIsNotEnabled()
        compose
            .onNodeWithContentDescription("View workout and purpose")
            .performScrollTo()
            .performClick()
        compose.runOnIdle { assertEquals(listOf("workout:original/today"), actions) }
        compose.onNodeWithText("Original warm-up:original/today").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Next week").performScrollTo().performClick()
        compose.onNodeWithText("Week beginning 2026-11-23").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Current week").performScrollTo().performClick()
        compose.onNodeWithText("Week beginning 2026-11-16").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Programme details").performScrollTo().performClick()
        compose
            .onNodeWithText("Saved plan version 1 · 1 preserved versions")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Rebuild remaining programme…").performScrollTo().performClick()
        compose.onNodeWithText("Keep current plan").performClick()
        compose.runOnIdle { assertFalse(actions.contains("generate")) }
        compose.onNodeWithText("Rebuild remaining programme…").performScrollTo().performClick()
        compose.onNodeWithText("Rebuild", substring = false).performClick()
        compose.runOnIdle {
            assertEquals(1, actions.count { it == "generate" })
            assertEquals("original/version", original.currentPlan!!.id())
            assertEquals(1, original.planVersions.size)
            assertEquals(5.0, original.currentPlan!!.plan().workouts().first().distanceKm()!!, 0.0)
            assertTrue(original.activities.isEmpty())
            assertTrue(actions.any { it.startsWith("routine:") && it.endsWith(":original/today") })
        }
    }

    @Test
    fun optionalAdjustmentRetainsOriginalProposalAndOnlyDispatchesExplicitChoice() {
        val original = state()
        val proposal =
            AdjustmentProposal(
                "choice",
                "original/version",
                "original/today",
                Instant.parse("2026-11-16T12:00:00Z"),
                "Recorded demands",
                emptyList(),
                "Your programme is unchanged",
                "Limited evidence",
                listOf(
                    AdjustmentProposal.Option(
                        "keep",
                        "Keep original workout",
                        AdjustmentProposal.Action.KEEP,
                        null,
                    )
                ),
            )
        show(original.copy(proposals = listOf(proposal)))
        compose.onNodeWithText("Keep original workout").performScrollTo().assertIsEnabled()
        compose.runOnIdle {
            assertTrue(actions.isEmpty())
            assertEquals(5.0, original.currentPlan!!.plan().workouts().first().distanceKm()!!, 0.0)
        }
        compose.onNodeWithText("Keep original workout").performClick()
        compose.runOnIdle { assertEquals(listOf("choice:keep"), actions) }
    }
}
