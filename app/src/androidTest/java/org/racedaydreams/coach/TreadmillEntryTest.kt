package org.racedaydreams.coach

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.racedaydreams.coach.theme.RaceDayTheme
import org.racedaydreams.data.FeatureStore
import org.racedaydreams.data.SqliteUserPreferencesRepository
import org.racedaydreams.domain.activity.PhysicalRunSessions
import org.racedaydreams.domain.activity.RunEnvironment
import org.racedaydreams.domain.activity.SourceIdentity

class TreadmillEntryTest {
    @get:Rule val compose = createComposeRule()
    private val state = ViewModelStore()

    @After
    fun clearModel() {
        compose.runOnIdle { state.clear() }
    }

    @Test
    fun unplannedTreadmillNeedsOnlyDistanceAndTimeAndCountsOnceWithProviderCopy() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        SqliteUserPreferencesRepository(app).clear()
        val model =
            compose.runOnIdle {
                ViewModelProvider(
                    state,
                    object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(
                            modelClass: Class<T>
                        ): T = CoachViewModel(app, SavedStateHandle()) as T
                    },
                )[CoachViewModel::class.java]
            }
        val store = FeatureStore(app)
        val dark = InstrumentationRegistry.getArguments().getString("part2Evidence") == "dark"
        compose.setContent {
            RaceDayTheme(darkTheme = dark, dynamicColour = false) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    CoachPage {
                        val view by model.state.collectAsState()
                        SharedTreadmillEntry(
                            view,
                            model.runningEvidence,
                            AndroidCoachCalendarDates,
                            { it },
                            null,
                            null,
                            {},
                        ) { label, value, change, decimal ->
                            CoachInput(label, value, change, decimal)
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Log an indoor treadmill run").performScrollTo().performClick()
        compose.onNodeWithText("Indoor treadmill run").performScrollTo().assertIsDisplayed()
        capturePart2(app, "treadmill-entry")
        compose
            .onNodeWithText("Actual distance (km)")
            .performScrollTo()
            .performTextReplacement("5.1")
        compose.onNodeWithText("Duration").performScrollTo().performTextReplacement("33:30")
        compose
            .onNodeWithText("For example, 33:30 or 1:05:00", useUnmergedTree = true)
            .assertExists()
        capturePart2(app, "treadmill-duration")
        compose.onNodeWithText("Duration").performImeAction()
        compose.onNodeWithText("Start time (HH:mm, local)").assertDoesNotExist()
        compose.onNodeWithText("Set start time or add notes").performScrollTo().performClick()
        compose.onNodeWithText("Start time (HH:mm, local)").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Use the current finish time").performScrollTo().performClick()
        compose.onNodeWithText("Start time (HH:mm, local)").assertDoesNotExist()
        compose.onNodeWithText("Save treadmill run").performScrollTo().assertIsDisplayed()
        capturePart2(app, "treadmill-save")
        compose.onNodeWithText("Save treadmill run").performClick()
        compose.waitUntil(20_000) { store.activitySummaries().size == 1 }
        val manual = store.activitySummaries().single()
        assertEquals(5100.0, manual.distanceMetres()!!, 0.0)
        assertEquals(2010L, manual.elapsedTime().seconds)
        assertEquals(RunEnvironment.TREADMILL, manual.environment())
        assertNull(manual.plannedWorkoutId())
        assertNull(manual.movingTime())
        assertTrue(manual.samples().isEmpty())
        val imported =
            manual.copy(
                id = "hc:treadmill",
                source = SourceIdentity("health-connect", "treadmill", "com.fitbit.FitbitMobile"),
            )
        store.applyImport("health-connect", listOf(imported), emptyList(), "checkpoint")
        val activities = store.activitySummaries()
        assertEquals(2, activities.size)
        assertEquals(
            5100.0,
            PhysicalRunSessions.counted(activities).single().distanceMetres()!!,
            0.0,
        )
    }
}
