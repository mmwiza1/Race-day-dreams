package org.racedaydreams.coach

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.theme.RaceDayTheme
import org.racedaydreams.domain.*
import org.racedaydreams.domain.coaching.*

@RunWith(AndroidJUnit4::class)
class SharedWeeklyReviewContentTest {
    @get:Rule val compose = createComposeRule()
    private val today = LocalDate.of(2026, 11, 15)
    private val workout = PlannedWorkout("future", today.plusDays(1), WorkoutCategory.TEMPO_THRESHOLD_RUN, 10.0, null, emptyList(), "original")
    private val plan = TrainingPlan("p", today.minusDays(28), today.plusDays(28), emptyList(), listOf(workout))
    private val profile = UserProfile(RaceGoal(RaceDistance.FIVE_K, "", null, GoalMode.FINISH, null), TrainingPreferences(TrainingStrategy.BALANCED, setOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.FRIDAY), 3, java.time.DayOfWeek.FRIDAY, emptySet(), ScheduleType.FIXED, UnitSystem.METRIC), false)
    private val version = PlanVersion("v", "p", 1, Instant.EPOCH, "", profile, "", plan)
    private val decisions = mutableListOf<String>()

    private fun show(interrupted: Boolean = false, dark: Boolean = false, large: Boolean = false) {
        val review = WeeklyCoaching.review(version, listOf(version), emptyList(), emptyList(), emptyList(), emptyList(), today, if (interrupted) WeeklyCoaching.Interruption(today.minusDays(35), today.minusDays(1), "travel") else null)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (large) 2f else 1f)) {
                RaceDayTheme(darkTheme = dark, dynamicColour = false) {
                    CoachPage { SharedWeeklyReviewContent(review, plan, null, false, {}, { decisions.add(it) }, ::friendlyDate, ::shortDate, ::decimal, { "${it.distanceKm()} km ${it.category()}" }) }
                }
            }
        }
    }

    @Test fun incompleteEvidenceStaysHonestAndKeepRequiresAnExplicitAction() {
        show()
        compose.onNodeWithText("Still learning your training").assertExists()
        compose.onNodeWithText("0 of 0 planned runs completed").assertExists()
        compose.runOnIdle { assertEquals(emptyList<String>(), decisions) }
        compose.onNodeWithText("Keep original plan").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf("keep"), decisions) }
    }

    @Test fun previewCanBeDismissedWithoutAcceptingAnyProgrammeChange() {
        show(interrupted = true, dark = true)
        compose.onNodeWithText("Use a three-week return bridge").performScrollTo().performClick()
        compose.onNodeWithText("Preview plan change").assertIsDisplayed()
        compose.onNodeWithText("Current: 10.0 km TEMPO_THRESHOLD_RUN").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Proposed: 5.0 km EASY_RUN").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(emptyList<String>(), decisions) }
        compose.onNode(hasText("Keep original plan") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText("Preview plan change").assertDoesNotExist()
        compose.runOnIdle { assertEquals(emptyList<String>(), decisions) }
    }

    @Test fun largeTextPreviewScrollsAndOnlyExplicitAcceptanceCallsThePlatformAction() {
        show(interrupted = true, dark = true, large = true)
        compose.onNodeWithText("Use a three-week return bridge").performScrollTo().performClick()
        compose.onNodeWithText("Other sessions stay unchanged. Your race goal stays unchanged.").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(emptyList<String>(), decisions) }
        compose.onNodeWithText("Accept change").performClick()
        compose.runOnIdle { assertEquals(listOf("return"), decisions) }
        compose.onNodeWithText("Preview plan change").assertDoesNotExist()
    }
}
