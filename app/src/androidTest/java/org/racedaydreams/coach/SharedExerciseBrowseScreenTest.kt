package org.racedaydreams.coach

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.strength.SharedExerciseBrowseScreen
import org.racedaydreams.domain.strength.StrengthExercise

/**
 * Direct shared entry point, real production controls/artwork, and explicit native ID authority.
 */
@RunWith(AndroidJUnit4::class)
class SharedExerciseBrowseScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun darkLargeTextCustomDialogUsesHostIdentityOnlyAfterExplicitSave() {
        val identities = AtomicInteger()
        val saved = AtomicReference<StrengthExercise>()
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    SharedExerciseBrowseScreen(
                        state = CoachUiState(loading = false),
                        onCreateCustom = { saved.set(it) },
                        open = {},
                        thumbnail = { ExerciseLibraryThumbnail(it) },
                        nextExerciseId = {
                            identities.incrementAndGet()
                            "host-chosen-exercise"
                        },
                    )
                }
            }
        }
        compose.runOnIdle {
            assertEquals(0, identities.get())
            assertNull(saved.get())
        }
        compose
            .onNodeWithContentDescription("Add custom exercise")
            .assertIsDisplayed()
            .performClick()
        compose.onNodeWithText("Exercise name").performTextInput("My exercise")
        compose.runOnIdle {
            assertEquals(0, identities.get())
            assertNull(saved.get())
        }
        compose.onNodeWithText("Save custom exercise").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(1, identities.get())
            assertEquals("host-chosen-exercise", saved.get().id())
            assertEquals("My exercise", saved.get().name())
            assertEquals(StrengthExercise.MeasurementProfile.LOAD_REPS, saved.get().measurement())
            assertFalse(saved.get().estimateOneRepMax())
        }
        compose.onNodeWithText("Custom exercise").assertDoesNotExist()
    }

    @Test
    fun compactStrengthHeaderCarriesTheSingleLibraryHeadingAndAddAction() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MaterialTheme {
                    SharedExerciseBrowseScreen(
                        state = CoachUiState(loading = false),
                        onCreateCustom = {},
                        open = {},
                        thumbnail = { ExerciseLibraryThumbnail(it) },
                        nextExerciseId = { "unused" },
                    )
                }
            }
        }
        compose.onNodeWithTag("screen-header").assertIsDisplayed()
        compose
            .onAllNodes(hasText("Exercise Library") and isHeading())
            .assertCountEquals(1)
            .onFirst()
            .assertIsDisplayed()
        // Decorative glyph and eyebrow never replace the spoken add label.
        compose
            .onNode(hasAnyAncestor(hasTestTag("screen-header")) and hasClickAction())
            .assertContentDescriptionEquals("Add custom exercise")
            .assertHeightIsAtLeast(CoachSpace.Touch)
        compose
            .onNodeWithTag("exercise-browser-list")
            .performScrollToNode(hasText("A few useful starting points"))
        compose.onNode(hasText("A few useful starting points") and isHeading()).assertIsDisplayed()
    }

    @Test
    fun classifiedCustomActionRemainsOwnedByHostWithoutCreatingAnUnclassifiedExercise() {
        val classified = AtomicInteger()
        compose.setContent {
            MaterialTheme {
                SharedExerciseBrowseScreen(
                    state = CoachUiState(loading = false),
                    onCreateCustom = { error("The classified flow must retain its metadata.") },
                    open = {},
                    thumbnail = { ExerciseLibraryThumbnail(it) },
                    nextExerciseId = {
                        error("Opening a classified form is not a saved exercise.")
                    },
                    onCreateClassifiedCustom = { classified.incrementAndGet() },
                )
            }
        }
        compose.onNodeWithContentDescription("Add custom exercise").performClick()
        compose.runOnIdle { assertEquals(1, classified.get()) }
        compose.onNodeWithText("Custom exercise").assertDoesNotExist()
    }
}
