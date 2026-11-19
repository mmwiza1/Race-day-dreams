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
