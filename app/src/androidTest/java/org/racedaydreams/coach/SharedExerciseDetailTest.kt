package org.racedaydreams.coach

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.presentation.AthleteLanguage
import org.racedaydreams.domain.strength.*
import org.racedaydreams.storage.SqliteStrengthChoices

/** Actual production tabs/controls over retained evidence, with explicit host operations. */
@RunWith(AndroidJUnit4::class)
class SharedExerciseDetailTest {
    @get:Rule val compose = createComposeRule()

    private fun session(exercise: StrengthExercise, seconds: Long? = null) =
        StrengthSession(
            "actual-workout",
            Instant.parse("2026-11-16T09:00:00Z"),
            ZoneOffset.UTC,
            Duration.ofMinutes(30),
            7.5,
            "Recorded evidence",
            listOf(
                StrengthSession.ExerciseSets(
                    exercise,
                    listOf(
                        StrengthSession.SetEntry(
                            "actual-set",
                            if (seconds == null) 6 else 0,
                            if (seconds == null) 82.25 else 0.0,
                            7.5,
                            false,
                            seconds,
                            null,
                        )
                    ),
                )
            ),
        )

    private fun render(
        state: CoachUiState,
        id: String,
        largeDark: Boolean = false,
        choice: (String, Boolean, Boolean) -> Unit = { _, _, _ ->
            error("Unexpected preference change")
        },
        save: (StrengthExercise) -> Unit = { error("Unexpected exercise edit") },
        delete: (String) -> Unit = { error("Unexpected historical deletion") },
        source: (String) -> Unit = { error("Unexpected source navigation") },
    ) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides
                    Density(density.density, if (largeDark) 2f else density.fontScale)
            ) {
                MaterialTheme(
                    colorScheme = if (largeDark) darkColorScheme() else MaterialTheme.colorScheme
                ) {
                    SharedExerciseDetail(
                        state = state,
                        id = id,
                        onExerciseChoice = choice,
                        onExclusion = { _, _ -> error("Unexpected exclusion") },
                        onSaveExercise = save,
                        onDeleteHistorical = delete,
                        source = source,
                        formatDate = ::friendlyDate,
                        formatNumber = ::decimal,
                        displaySet = StrengthMeasurements::display,
                        technique = { TechniqueSection(it) },
                    )
                }
            }
        }
    }

    @Test
    fun recordedAndEstimatedRecordsStayDistinctAndOpenTheActualSourceWorkout() {
        val bench = requireNotNull(ExerciseCatalog.definition("bench-press")).exercise()
        val actual = session(bench)
        val opened = AtomicReference<String>()
        render(
            CoachUiState(
                loading = false,
                exercises = listOf(bench),
                strengthSessions = listOf(actual),
            ),
            bench.id(),
            source = { opened.set(it) },
        )
        compose.onNodeWithText("PBs").performClick()
        compose
            .onNodeWithText(AthleteLanguage.recordLabel("6RM"))
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("82.3 kg × 6").assertExists()
        compose
            .onNodeWithText("Estimated maximum for one repetition:", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose.onAllNodesWithText("Edit source workout").onFirst().performScrollTo().performClick()
        compose.runOnIdle { assertEquals(actual.id(), opened.get()) }
    }

    @Test
    fun darkLargeTextRenameAndRestorePreservePreferenceFlagsMeasurementAndFrozenHistory() {
        val original =
            StrengthExercise(
                "custom-row",
                "Current row",
                StrengthExercise.Region.UPPER,
                StrengthExercise.Mode.WEIGHTED,
                false,
                StrengthExercise.MeasurementProfile.LOAD_REPS,
            )
        val frozen = original.copy(name = "Original logged name")
        val actual = session(frozen)
        val state =
            CoachUiState(
                loading = false,
                exercises = listOf(original),
                strengthSessions = listOf(actual),
                exerciseChoices = listOf(SqliteStrengthChoices.Choice(original.id(), true, true)),
            )
        val selected = AtomicReference<Triple<String, Boolean, Boolean>>()
        val saved = AtomicReference<StrengthExercise>()
        render(
            state,
            original.id(),
            largeDark = true,
            choice = { id, favourite, archived -> selected.set(Triple(id, favourite, archived)) },
            save = { saved.set(it) },
        )
        compose.onNodeWithContentDescription("Remove Current row from favourites").assertExists()
        compose.onNodeWithText("Restore to My Exercises").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(Triple(original.id(), true, false), selected.get()) }
        compose
            .onNodeWithText("Custom exercise name")
            .performScrollTo()
            .performTextReplacement("Renamed row")
        compose.onNodeWithText("Save exercise name").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(original.copy(name = "Renamed row"), saved.get())
            assertEquals(frozen, state.strengthSessions.single().exercises().single().exercise())
            assertEquals(original, state.exercises.single())
        }
    }

    @Test
    fun timedHistoricalEvidenceRetainsUnknownDateAndNeverBecomesAnEstimatedLift() {
        val plank = requireNotNull(ExerciseCatalog.definition("plank")).exercise()
        val old =
            HistoricalStrengthRecord(
                "manual-plank",
                plank,
                0.0,
                0,
                null,
                HistoricalStrengthRecord.DatePrecision.UNKNOWN,
                "Previously recorded hold",
                75L,
                null,
            )
        val removed = AtomicReference<String>()
        render(
            CoachUiState(
                loading = false,
                exercises = listOf(plank),
                historicalStrength = listOf(old),
            ),
            plank.id(),
            delete = { removed.set(it) },
        )
        compose.onNodeWithText("PBs").performClick()
        compose.onNodeWithText("1 min 15 sec").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Date unknown · outside chronology").assertExists()
        compose
            .onNodeWithText("Estimated maximum for one repetition:", substring = true)
            .assertDoesNotExist()
        compose.onNodeWithText("Delete historical PB").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(old.id(), removed.get()) }
    }
}
