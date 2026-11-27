package org.racedaydreams.coach

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.theme.RaceDayTheme
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.WorkoutSegment
import org.racedaydreams.domain.execution.WorkoutExecutionEngine
import org.racedaydreams.domain.recording.RunMetricsEngine

@RunWith(AndroidJUnit4::class)
class SharedActiveRunPanelTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun platformRouteAndStatisticsSwitchWithoutCommandsOrChangingTheRecorderState() {
        val commands = mutableListOf<String>()
        val state =
            mutableStateOf(
                RunMetricsEngine.Status(5000.0, 1800000, 1500000, 300.0, "GPS ready", false)
            )
        compose.setContent {
            RaceDayTheme(dynamicColour = false) {
                SharedActiveRunPanel(
                    state.value,
                    null,
                    140.0,
                    null,
                    null,
                    null,
                    false,
                    commands::add,
                    { PaceCalculator.formatTime(Duration.ofMillis(it)) },
                    ::formatPaceSeconds,
                    ::decimal,
                    { "${decimal(it / 1000)} km" },
                    { Text("Persisted platform route") },
                    {},
                    diagnostics = { Text("Existing diagnostics controls") },
                )
            }
        }
        compose.onNodeWithTag("live-elapsed").assertExists()
        compose.onNodeWithText("Existing diagnostics controls").assertDoesNotExist()
        compose.onNodeWithTag("live-gps-troubleshooting").performScrollTo().performClick()
        compose
            .onNodeWithText("Existing diagnostics controls")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithTag("live-gps-troubleshooting").performScrollTo().performClick()
        compose.onNodeWithText("Existing diagnostics controls").assertDoesNotExist()
        compose.onNodeWithTag("live-route-tab").performScrollTo().performClick()
        compose.onNodeWithText("Persisted platform route").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("5.0 km").performScrollTo().assertIsDisplayed()
        compose.runOnIdle {
            assertTrue(commands.isEmpty())
            assertEquals(5000.0, state.value.metres(), 0.0)
        }
        compose.onNodeWithTag("live-statistics-tab").performScrollTo().performClick()
        compose.onNodeWithTag("live-elapsed").assertExists()
        compose.onNodeWithText("Pause").performClick()
        compose.runOnIdle {
            assertEquals(listOf("PAUSE"), commands)
            assertFalse(state.value.paused())
            state.value =
                RunMetricsEngine.Status(5000.0, 1801000, 1500000, null, "GPS ready · paused", true)
        }
        compose.onNodeWithText("Resume").assertIsDisplayed()
    }

    @Test
    fun sharedDetailsKeepOpenStepsDreamContextAndFinishApprovalSeparate() {
        val commands = mutableListOf<String>()
        val e =
            WorkoutExecutionEngine(
                listOf(
                    WorkoutExecutionEngine.Step(
                        "Run by effort",
                        WorkoutSegment.Kind.RUN,
                        null,
                        null,
                        null,
                        null,
                        5.5,
                    )
                )
            )
        e.update(WorkoutExecutionEngine.Input(10.0, 10000, 10000, null, false, false))
        compose.setContent {
            RaceDayTheme(darkTheme = true, dynamicColour = false) {
                SharedActiveRunPanel(
                    RunMetricsEngine.Status(10.0, 10000, 10000, null, "GPS ready", false),
                    e.state(),
                    null,
                    "Retained cue",
                    "Retained race context",
                    null,
                    false,
                    commands::add,
                    { PaceCalculator.formatTime(Duration.ofMillis(it)) },
                    ::formatPaceSeconds,
                    ::decimal,
                    { "${decimal(it / 1000)} km" },
                    {},
                    { Text("Platform Dream Route") },
                )
            }
        }
        compose.onNodeWithText("Workout details / controls").performScrollTo().performClick()
        compose.onNodeWithText("Platform Dream Route").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Next: Run").assertDoesNotExist()
        compose.onNodeWithText("Complete open step").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(listOf("COMPLETE_OPEN"), commands)
            assertFalse(e.state().complete())
        }
        compose.onNodeWithText("Finish & save").performClick()
        compose.onNodeWithText("Keep recording").performClick()
        compose.runOnIdle { assertEquals(listOf("COMPLETE_OPEN"), commands) }
    }

    @Test
    fun largeTextDarkViewKeepsControlsAndHonestUnknownStatusWithoutInventingPace() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                RaceDayTheme(darkTheme = true, dynamicColour = false) {
                    SharedActiveRunPanel(
                        RunMetricsEngine.Status(0.0, 60000, 60000, 300.0, null, false),
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        {},
                        { PaceCalculator.formatTime(Duration.ofMillis(it)) },
                        ::formatPaceSeconds,
                        ::decimal,
                        { "0.0 km" },
                        { Text("Route not yet recorded") },
                        {},
                    )
                }
            }
        }
        compose.onNodeWithText("Pause").assertIsDisplayed()
        compose.onNodeWithText("Finish & save").assertIsDisplayed()
        compose.onNodeWithText("Pace unavailable").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Active average pace").assertDoesNotExist()
        compose.onNodeWithText("GPS status unavailable").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("live-route-tab").performScrollTo().performClick()
        compose.onNodeWithText("Route not yet recorded").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Finish & save").assertIsDisplayed()
    }
}
