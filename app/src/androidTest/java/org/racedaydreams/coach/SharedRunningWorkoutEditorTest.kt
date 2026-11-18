package org.racedaydreams.coach

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.WorkoutSegment
import org.racedaydreams.domain.execution.WorkoutExecutionEngine.Step
import org.racedaydreams.domain.polish.CustomWorkout

@RunWith(AndroidJUnit4::class)
class SharedRunningWorkoutEditorTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun favouriteCanBeEditedAndSavedAtLargeTextWithoutRecordingPermission() {
        val saved = AtomicReference<CustomWorkout>()
        val open = Step("Easy warm-up", WorkoutSegment.Kind.WARM_UP, null, null, null, null, null)
        val original =
            CustomWorkout("retained", "Intervals", listOf(CustomWorkout.Group(1, listOf(open))))
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    CoachPage {
                        SharedRunningWorkoutEditor(
                            draftId = original.id(),
                            initial = original,
                            start = null,
                            save = { saved.set(it) },
                            close = {},
                        )
                    }
                }
            }
        }

        compose.onNodeWithText("Start custom workout").assertDoesNotExist()
        compose.onNodeWithText("Duplicate group").performScrollTo().performClick()
        compose.runOnIdle { assertNull(saved.get()) }
        compose
            .onNodeWithText("Save favourite workout")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        compose.runOnIdle {
            assertEquals(original.id(), saved.get().id())
            assertEquals(listOf(open, open), saved.get().flattened())
            assertEquals(listOf(open), original.flattened())
        }
    }
}
