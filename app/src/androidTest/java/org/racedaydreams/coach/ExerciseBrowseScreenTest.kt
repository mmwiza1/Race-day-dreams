package org.racedaydreams.coach

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.strength.ExerciseBrowseScreen
import org.racedaydreams.data.StrengthUserStore
import org.racedaydreams.domain.strength.CustomStrengthExercise
import org.racedaydreams.domain.strength.ExerciseCatalog
import org.racedaydreams.domain.strength.ExerciseMetadata
import org.racedaydreams.domain.strength.GymProgramme
import org.racedaydreams.domain.strength.GymWorkoutDefinition
import org.racedaydreams.domain.strength.StrengthExercise
import org.racedaydreams.domain.strength.StrengthProgramme
import org.racedaydreams.domain.strength.StrengthProgrammeDraft
import org.racedaydreams.domain.strength.StrengthSession

/** Production metadata and search rules, real selectable rows, with no invented custom taxonomy. */
@RunWith(AndroidJUnit4::class)
class ExerciseBrowseScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun aliasSearchReachesBeyondStartingPointsAndOpensTheCanonicalExercise() {
        var opened: String? = null
        compose.setContent {
            MaterialTheme {
                ExerciseBrowseScreen(CoachUiState(loading = false), {}, { opened = it })
            }
        }
        compose.onNodeWithTag("exercise-search").performTextInput("Romanian DL")
        row("romanian-deadlift").performClick()
        compose.runOnIdle { assertEquals("romanian-deadlift", opened) }
        search("Bench Press")
        row("bench-press").performClick()
        compose.runOnIdle { assertEquals("bench-press", opened) }
    }

    @Test
    fun primaryAreaAndEquipmentCombineWithoutClaimingOneItemIsACompleteSetup() {
        var opened: String? = null
        val state =
            CoachUiState(
                loading = false,
                exercises =
                    exercises("bench-press", "dumbbell-bench-press", "goblet-squat", "pull-up"),
            )
        compose.setContent { MaterialTheme { ExerciseBrowseScreen(state, {}, { opened = it }) } }
        clickTag("exercise-browse-Body area")
        clickTag("exercise-area-CHEST")
        summary("2 matching exercises")
        clickTag("exercise-browse-Equipment")
        clickTag("exercise-equipment-DUMBBELLS")
        summary("1 matching exercise")
        row("dumbbell-bench-press").performClick()
        compose.runOnIdle { assertEquals("dumbbell-bench-press", opened) }
        compose
            .onNodeWithTag("exercise-browser-list")
            .performScrollToNode(
                hasText(
                    "Uses this equipment. A complete setup may also need the other items shown on each exercise."
                )
            )
        compose
            .onNodeWithText(
                "Uses this equipment. A complete setup may also need the other items shown on each exercise."
            )
            .assertIsDisplayed()
        compose
            .onNodeWithTag("exercise-browser-list")
            .performScrollToNode(hasText("Clear category filters"))
        compose.onNodeWithText("Clear category filters").performClick()
        search("Goblet Squat")
        row("goblet-squat").assertIsDisplayed()
        compose
            .onNodeWithText("Quads & Glutes · Squat · Dumbbells OR Kettlebells")
            .assertIsDisplayed()
    }

    @Test
    fun personalShortcutsUseActualRecentAndPreservedProgrammeIdentities() {
        var opened: String? = null
        val bench = exercise("bench-press")
        val goblet = exercise("goblet-squat")
        val date = LocalDate.of(2026, 10, 26)
        val programme =
            GymProgramme(
                "browse-programme",
                1,
                Instant.parse("2026-10-25T10:00:00Z"),
                "Browse fixture",
                GymProgramme.Settings(
                    GymProgramme.Goal.GENERAL,
                    GymProgramme.Priority.STRENGTH_FIRST,
                    GymProgramme.Experience.BEGINNER,
                    setOf(DayOfWeek.MONDAY),
                    1,
                    setOf("bodyweight"),
                    30,
                    GymProgramme.Effort.RPE,
                ),
                date,
                date.plusDays(41),
                listOf(
                    GymProgramme.Session(
                        "preserved-plank",
                        date,
                        "Core session",
                        listOf(
                            GymProgramme.Prescription(
                                "plank",
                                2,
                                0,
                                0,
                                30,
                                null,
                                5,
                                7,
                                60,
                                "Controlled trunk work",
                            )
                        ),
                        "Fixture",
                    )
                ),
                emptyList(),
            )
        val recent =
            StrengthSession(
                "actual-goblet",
                Instant.parse("2026-10-26T10:00:00Z"),
                ZoneOffset.UTC,
                Duration.ofMinutes(20),
                null,
                "",
                listOf(
                    StrengthSession.ExerciseSets(
                        goblet,
                        listOf(StrengthSession.SetEntry("entered-set", 8, 15.0, 7.0, false)),
                    )
                ),
            )
        val state =
            CoachUiState(
                loading = false,
                exercises = listOf(bench, goblet, exercise("plank"), exercise("pull-up")),
                strengthSessions = listOf(recent),
                gymProgramme = programme,
                exerciseChoices = listOf(StrengthUserStore.Choice(bench.id(), true, false)),
            )
        compose.setContent { MaterialTheme { ExerciseBrowseScreen(state, {}, { opened = it }) } }
        clickTag("exercise-shortcut-Recent")
        summary("1 matching exercise")
        row("goblet-squat").performClick()
        compose.runOnIdle { assertEquals("goblet-squat", opened) }
        clickTag("exercise-shortcut-In my programme")
        summary("1 matching exercise")
        row("plank").performClick()
        compose.runOnIdle { assertEquals("plank", opened) }
        clickTag("exercise-shortcut-Favourites")
        summary("1 matching exercise")
        row("bench-press").performClick()
        compose.runOnIdle { assertEquals("bench-press", opened) }
    }

    @Test
    fun archivedRowsAreExplicitlyRecoverableInDarkModeAtTwoHundredPercentText() {
        var opened: String? = null
        val state =
            CoachUiState(
                loading = false,
                exercises = exercises("bench-press", "plank"),
                exerciseChoices = listOf(StrengthUserStore.Choice("bench-press", false, true)),
            )
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    ExerciseBrowseScreen(state, {}, { opened = it })
                }
            }
        }
        compose.onNodeWithTag("exercise-search").performTextInput("Bench Press")
        summary("0 matching exercises")
        compose.onNodeWithTag("exercise-browser-list").performScrollToNode(hasText("Show archived"))
        compose.onNodeWithText("Show archived").performClick()
        summary("1 matching exercise")
        row("bench-press").performClick()
        compose.runOnIdle { assertEquals("bench-press", opened) }
        compose.onNodeWithText("Archived").assertIsDisplayed()
    }

    @Test
    fun customCreationKeepsMeasurementAndRegionWithoutInventingCanonicalMetadata() {
        var created: StrengthExercise? = null
        val custom =
            StrengthExercise(
                "my-existing",
                "My carry",
                StrengthExercise.Region.FULL,
                StrengthExercise.Mode.WEIGHTED,
                false,
                StrengthExercise.MeasurementProfile.LOAD_DISTANCE,
            )
        compose.setContent {
            MaterialTheme {
                ExerciseBrowseScreen(
                    CoachUiState(loading = false, exercises = listOf(custom)),
                    { created = it },
                    {},
                )
            }
        }
        row("my-existing").assertIsDisplayed()
        compose
            .onNodeWithText(
                "Custom exercise · Full region · Load distance · equipment and movement not classified"
            )
            .assertIsDisplayed()
        compose
            .onNodeWithTag("exercise-browser-list")
            .performScrollToNode(hasText("Add custom exercise"))
        compose.onNodeWithText("Add custom exercise").performClick()
        compose.onNodeWithText("Exercise name").performTextInput("My new carry")
        compose.onNodeWithTag("custom-measurement-LOAD_DISTANCE").performScrollTo().performClick()
        compose.onNodeWithTag("custom-region-LOWER").performScrollTo().performClick()
        compose.onNodeWithText("Save custom exercise").performClick()
        compose.runOnIdle {
            assertEquals("My new carry", created!!.name())
            assertEquals(StrengthExercise.Region.LOWER, created!!.region())
            assertEquals(StrengthExercise.MeasurementProfile.LOAD_DISTANCE, created!!.measurement())
            assertEquals(StrengthExercise.Mode.WEIGHTED, created!!.mode())
            assertFalse(created!!.estimateOneRepMax())
        }
    }

    @Test
    fun authoredInventoryTakesPrecedenceAndRefreshesWithoutChangingLegacyHistory() {
        val custom = classifiedCustom()
        val date = LocalDate.of(2026, 10, 26)
        val legacy =
            GymProgramme(
                "legacy-inventory",
                1,
                Instant.parse("2026-10-25T10:00:00Z"),
                "Preserved legacy",
                GymProgramme.Settings(
                    GymProgramme.Goal.GENERAL,
                    GymProgramme.Priority.STRENGTH_FIRST,
                    GymProgramme.Experience.BEGINNER,
                    setOf(DayOfWeek.MONDAY),
                    1,
                    setOf("bodyweight"),
                    30,
                    GymProgramme.Effort.RPE,
                ),
                date,
                date.plusDays(6),
                listOf(
                    GymProgramme.Session(
                        "legacy-day",
                        date,
                        "Core",
                        listOf(
                            GymProgramme.Prescription(
                                "plank",
                                2,
                                0,
                                0,
                                30,
                                null,
                                5,
                                7,
                                60,
                                "Controlled",
                            )
                        ),
                        "Preserved legacy context",
                    )
                ),
                emptyList(),
            )
        val authored = authoredInventory(custom)
        val shown =
            mutableStateOf(
                CoachUiState(
                    loading = false,
                    exercises = listOf(custom.exercise(), exercise("plank")),
                    gymProgramme = legacy,
                    authoredGymProgramme = authored,
                )
            )
        var opened: String? = null
        compose.setContent {
            MaterialTheme {
                ExerciseBrowseScreen(
                    shown.value,
                    {},
                    { opened = it },
                    customMetadata = mapOf(custom.exercise().id() to custom),
                )
            }
        }
        clickTag("exercise-shortcut-In my programme")
        summary("1 matching exercise")
        row(custom.exercise().id()).performClick()
        compose.runOnIdle { assertEquals(custom.exercise().id(), opened) }
        compose.onNodeWithTag("exercise-row-plank").assertDoesNotExist()
        // A new snapshot with only a legacy current owner must rebuild the inventory too.
        compose.runOnIdle { shown.value = shown.value.copy(authoredGymProgramme = null) }
        summary("1 matching exercise")
        row("plank").performClick()
        compose.runOnIdle { assertEquals("plank", opened) }
        assertEquals("plank", legacy.sessions().single().exercises().single().exerciseId())
        assertEquals(
            custom.exercise(),
            authored.workouts().single().definition().slots().single().exercise(),
        )
    }

    @Test
    fun userClassifiedAreasRolesAndEquipmentParticipateInRealBrowserFilters() {
        val custom = classifiedCustom()
        var opened: String? = null
        compose.setContent {
            MaterialTheme {
                ExerciseBrowseScreen(
                    CoachUiState(
                        loading = false,
                        exercises = listOf(custom.exercise(), exercise("goblet-squat")),
                    ),
                    {},
                    { opened = it },
                    customMetadata = mapOf(custom.exercise().id() to custom),
                )
            }
        }
        // Chest/horizontal push exist only in this user's metadata, not the supplied canonical row.
        clickTag("exercise-browse-Body area")
        clickTag("exercise-area-CHEST")
        summary("1 matching exercise")
        clickTag("exercise-browse-Movement")
        clickTag("exercise-role-HORIZONTAL_PUSH")
        summary("1 matching exercise")
        clickTag("exercise-browse-Equipment")
        clickTag("exercise-equipment-DUMBBELLS")
        summary("1 matching exercise")
        row(custom.exercise().id()).performClick()
        compose.runOnIdle { assertEquals(custom.exercise().id(), opened) }
        compose
            .onNodeWithText("Chest · Horizontal push · Dumbbells · User defined")
            .assertIsDisplayed()
    }

    private fun classifiedCustom() =
        CustomStrengthExercise(
            StrengthExercise(
                "custom-classified-press",
                "My press",
                StrengthExercise.Region.UPPER,
                StrengthExercise.Mode.WEIGHTED,
                false,
                StrengthExercise.MeasurementProfile.LOAD_REPS,
            ),
            ExerciseMetadata.BodyArea.CHEST,
            ExerciseMetadata.Role.HORIZONTAL_PUSH,
            ExerciseMetadata.EquipmentRequirement(
                listOf(setOf(ExerciseMetadata.Equipment.DUMBBELLS))
            ),
            null,
        )

    private fun authoredInventory(custom: CustomStrengthExercise): StrengthProgramme {
        val date = LocalDate.of(2026, 10, 26)
        val slot =
            GymWorkoutDefinition.Slot(
                "press",
                custom.exercise(),
                GymWorkoutDefinition.Target(3, 6, 10, null, null, null, null, null),
                null,
            )
        return StrengthProgrammeDraft(
                "inventory-draft",
                1,
                "inventory-programme",
                0,
                "My authored programme",
                StrengthProgramme.Settings(
                    GymProgramme.Goal.GENERAL,
                    GymProgramme.Priority.STRENGTH_FIRST,
                    GymProgramme.Experience.BEGINNER,
                    setOf(DayOfWeek.MONDAY),
                    1,
                    45,
                    setOf(ExerciseMetadata.Equipment.DUMBBELLS),
                ),
                date,
                listOf(
                    StrengthProgrammeDraft.WeekBlock(
                        "week",
                        StrengthProgramme.Phase.FOUNDATION,
                        "Foundation",
                        1,
                        listOf(
                            StrengthProgrammeDraft.Day(
                                "day",
                                DayOfWeek.MONDAY,
                                "Press",
                                listOf(slot),
                                null,
                            )
                        ),
                    )
                ),
                StrengthProgramme.Origin.CUSTOM,
                null,
            )
            .preview(
                Instant.parse("2026-10-25T10:00:00Z"),
                emptySet(),
                mapOf(custom.exercise().id() to custom),
            )
    }

    private fun exercise(id: String) = ExerciseCatalog.definition(id).exercise()

    private fun exercises(vararg ids: String) = ids.map(::exercise)

    private fun clickTag(tag: String) {
        compose.onNodeWithTag("exercise-browser-list").performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).performClick()
    }

    private fun summary(text: String) {
        compose.onNodeWithTag("exercise-browser-list").performScrollToNode(hasText(text))
        compose.onNodeWithText(text).assertIsDisplayed()
    }

    private fun search(text: String) {
        compose
            .onNodeWithTag("exercise-browser-list")
            .performScrollToNode(hasTestTag("exercise-search"))
        compose.onNodeWithTag("exercise-search").performTextReplacement(text)
    }

    private fun row(id: String): androidx.compose.ui.test.SemanticsNodeInteraction {
        val tag = "exercise-row-$id"
        compose.onNodeWithTag("exercise-browser-list").performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag)
    }
}
