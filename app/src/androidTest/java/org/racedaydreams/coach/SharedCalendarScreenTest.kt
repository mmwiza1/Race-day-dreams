package org.racedaydreams.coach

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.PlannedWorkout
import org.racedaydreams.domain.WorkoutCategory

/**
 * Real common week/month/list presentation; date/units and operations remain required host hooks.
 */
@RunWith(AndroidJUnit4::class)
class SharedCalendarScreenTest {
    @get:Rule val compose = createComposeRule()
    private val today = LocalDate.of(2026, 11, 17)
    private val dates =
        object : CoachCalendarDates by AndroidCoachCalendarDates {
            override fun today() = this@SharedCalendarScreenTest.today
        }

    @Test
    fun darkLargeTextWeekAndMonthKeepPrescriptionAndNavigationIdentity() {
        val planned =
            PlannedWorkout(
                "original-prescription",
                today,
                WorkoutCategory.ZONE_2_RUN,
                5.0,
                null,
                emptyList(),
                "Retained instruction",
            )
        val state =
            mutableStateOf(
                CoachUiState(
                    loading = false,
                    calendarView = "Week",
                    plannedWorkouts = listOf(planned),
                )
            )
        val opened = AtomicReference<String>()
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    SharedActivityCalendarScreen(
                        state.value,
                        onViewChange = { state.value = state.value.copy(calendarView = it) },
                        workout = { opened.set(it) },
                        strength = { error("Unexpected strength navigation") },
                        activity = { error("Unexpected actual navigation") },
                        dates = dates,
                        distanceLabel = { formatDistance(it, state.value) },
                        history = { error("Unexpected list presentation") },
                    )
                }
            }
        }
        val label = "Zone 2 run · 5.0 km · Planned"
        compose.onNodeWithText(label).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(planned.id(), opened.get())
            assertEquals(planned, state.value.plannedWorkouts.single())
            assertTrue(state.value.activities.isEmpty())
        }
        compose.onNodeWithText("Month").performClick()
        compose.onNodeWithText("November 2026").performScrollTo().assertIsDisplayed()
        compose
            .onNodeWithContentDescription("17 November 2026, today, selected.", substring = true)
            .performScrollTo()
            .performClick()
        compose.onNodeWithText(label).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun monthGridDescribesSeveralSessionsAndSelectorAndArrowsNavigate() {
        val planned =
            PlannedWorkout(
                "original-prescription",
                today,
                WorkoutCategory.ZONE_2_RUN,
                5.0,
                null,
                emptyList(),
                "Retained instruction",
            )
        val gym =
            org.racedaydreams.domain.strength.GymProgramme(
                "gym/one",
                1,
                java.time.Instant.parse("2026-11-16T12:00:00Z"),
                "Athlete accepted programme",
                org.racedaydreams.domain.strength.GymProgramme.Settings(
                    org.racedaydreams.domain.strength.GymProgramme.Goal.SUPPORT_RUNNING,
                    org.racedaydreams.domain.strength.GymProgramme.Priority.RUNNING_FIRST,
                    org.racedaydreams.domain.strength.GymProgramme.Experience.INTERMEDIATE,
                    setOf(today.dayOfWeek),
                    1,
                    setOf("full gym"),
                    45,
                    org.racedaydreams.domain.strength.GymProgramme.Effort.RPE,
                ),
                today,
                today.plusDays(41),
                listOf(
                    org.racedaydreams.domain.strength.GymProgramme.Session(
                        "gym/today",
                        today,
                        "Lower body strength",
                        listOf(
                            org.racedaydreams.domain.strength.GymProgramme.Prescription(
                                "back-squat",
                                3,
                                5,
                                8,
                                null,
                                null,
                                6,
                                7,
                                120,
                                "Controlled working sets",
                            )
                        ),
                        "Original prescription",
                    )
                ),
                emptyList(),
            )
        val state =
            mutableStateOf(
                CoachUiState(
                    loading = false,
                    calendarView = "Month",
                    plannedWorkouts = listOf(planned),
                    gymProgramme = gym,
                )
            )
        val strength = AtomicReference<String>()
        compose.setContent {
            MaterialTheme {
                SharedActivityCalendarScreen(
                    state.value,
                    onViewChange = { state.value = state.value.copy(calendarView = it) },
                    workout = { error("Unexpected workout navigation") },
                    strength = { strength.set(it) },
                    activity = { error("Unexpected actual navigation") },
                    dates = dates,
                    distanceLabel = { formatDistance(it, state.value) },
                    history = { error("Unexpected list presentation") },
                )
            }
        }
        compose.onNodeWithText("Training calendar").assertIsDisplayed()
        compose.onNode(hasText("Month") and isSelected()).assertExists()
        compose.onNode(hasText("Week") and isNotSelected()).assertExists()
        compose.onNodeWithTag("calendar-mark-key", useUnmergedTree = true).assertExists()
        // The arrows move by month and Today returns; the selector switches views.
        compose.onNodeWithContentDescription("Next month").performScrollTo().performClick()
        compose.onNodeWithText("December 2026").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Today").performScrollTo().performClick()
        compose.onNodeWithText("November 2026").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Week").performClick()
        compose.onNode(hasText("Week") and isSelected()).assertExists()
        compose.onNodeWithText("Week of 16 November 2026").assertExists()
        compose.onNodeWithContentDescription("Previous week").assertExists()
        compose.onNodeWithText("Month").performClick()
        // Both of today's sessions are spoken in the one day cell.
        compose
            .onNodeWithContentDescription("17 November 2026, today, selected.", substring = true)
            .assert(hasContentDescription("Zone 2 run: planned", substring = true))
            .assert(
                hasContentDescription(
                    "Lower body strength · programmed strength: planned",
                    substring = true,
                )
            )
            .assertHeightIsAtLeast(48.dp)
            .performScrollTo()
            .performClick()
        compose.onNodeWithText("Zone 2 run · 5.0 km · Planned").assertExists()
        compose
            .onNodeWithText("Lower body strength · programmed strength · Planned · Log workout")
            .performScrollTo()
            .performClick()
        compose.runOnIdle { assertEquals(today.toString(), strength.get()) }
        compose.runOnIdle {
            assertEquals(planned, state.value.plannedWorkouts.single())
            assertTrue(state.value.strengthSessions.isEmpty())
        }
    }

    @Test
    fun listUsesRequiredHistoryContentAndViewSelectionReturnsToTheHost() {
        val selected = AtomicReference<String>()
        compose.setContent {
            MaterialTheme {
                SharedActivityCalendarScreen(
                    CoachUiState(loading = false, calendarView = "List"),
                    onViewChange = { selected.set(it) },
                    workout = { error("Unexpected workout") },
                    strength = { error("Unexpected strength") },
                    activity = { error("Unexpected activity") },
                    dates = dates,
                    distanceLabel = { error("Unexpected invented distance") },
                    history = { Text("Retained history content") },
                )
            }
        }
        compose.onNodeWithText("Retained history content").assertIsDisplayed()
        compose.onNodeWithText("Training calendar").assertDoesNotExist()
        compose.onNodeWithText("Week").performClick()
        compose.runOnIdle { assertEquals("Week", selected.get()) }
    }
}
