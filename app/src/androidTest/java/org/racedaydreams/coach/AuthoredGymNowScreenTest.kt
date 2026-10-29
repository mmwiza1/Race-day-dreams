package org.racedaydreams.coach

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.lifecycle.*
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.racedaydreams.coach.strength.*
import org.racedaydreams.data.*
import org.racedaydreams.domain.strength.*

/** Real typed programmes, native execution and SQLite; prescriptions never populate actuals. */
@RunWith(AndroidJUnit4::class)
class AuthoredGymNowScreenTest {
    @get:Rule val compose = createComposeRule()
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val shown = mutableStateOf<GymNowViewModel?>(null)
    private val page = mutableStateOf("hub")
    private val stores = mutableListOf<ViewModelStore>()
    private val models = mutableListOf<GymNowViewModel>()
    private lateinit var model: GymNowViewModel
    private val programmes
        get() = SqliteStrengthProgrammingRepository(app)

    private val day = LocalDate.now()
    private val created = Instant.now().minusSeconds(86400)

    @Before
    fun seed() {
        io { SqliteUserPreferencesRepository(app).clear() }
        io { programmes.activate(programme(1), null, "activate", emptyList()) }
    }

    @After
    fun close() {
        compose.runOnIdle { shown.value = null }
        compose.waitForIdle()
        compose.waitUntil(20_000) { models.none { it.state.value.busy } }
        compose.runOnIdle { stores.forEach { it.clear() } }
        io { SqliteUserPreferencesRepository(app).clear() }
    }

    @Test
    fun authoredPlanStartsExactTargetsWithoutRunningSetupOrPrefilledActuals() {
        install()
        click("Start planned workout")
        compose.onNodeWithText("Authored programme").performScrollTo().assertIsDisplayed()
        click("Start this session")
        await { execution() != null }
        val original = execution()!!
        assertEquals(GymExecution.Origin.PLANNED, original.origin())
        assertEquals("authored", original.source()!!.id())
        assertEquals(1, original.source()!!.revision())
        assertEquals(
            GymWorkoutAdapter.fromAuthoredProgramme(programme(1), "session").definition(),
            original.prescription(),
        )
        assertTrue(original.actualSets().isEmpty())
        assertEquals("", model.state.value.form.kilograms)
        assertEquals("", model.state.value.form.repetitions)
        assertNull(original.slots().single().target().restSeconds())
        assertEquals(1.5, original.slots().single().target().effort().minimum(), 0.0)
        compose.runOnIdle {
            model.updateForm {
                it.copy(kilograms = "60", repetitions = "6", effort = "3", rir = true)
            }
            model.saveSet()
        }
        await { execution()!!.actualSets().size == 1 }
        compose.runOnIdle { model.finish(true) }
        await { execution()!!.status() == GymExecution.Status.FINISHED }
        val actual = io { CoachingStore(app).strength().single() }
        assertEquals(60.0, actual.exercises().single().sets().single().kilograms(), 0.0)
        assertEquals(3.0, actual.exercises().single().sets().single().repetitionsInReserve()!!, 0.0)
        assertEquals(
            "authored:v1",
            io { DevelopmentStore(app).gymLinks()[actual.id()]!!.versionKey() },
        )
        assertNull(io { GymProgrammeStore(app).current() })
        assertFalse(io { SqliteUserPreferencesRepository(app).load().isPresent })
    }

    @Test
    fun staleDisplayedRevisionCannotStartAndRefreshesWithoutCreatingActuals() {
        install()
        io { programmes.activate(programme(2), programmes.currentReference(), "next", emptyList()) }
        var navigated = false
        compose.runOnIdle { model.startPlanned("session") { navigated = true } }
        compose.waitUntil(20_000) { !model.state.value.busy && model.state.value.error != null }
        assertTrue(model.state.value.error!!.contains("programme changed"))
        assertFalse(navigated)
        assertNull(execution())
        assertEquals(2, model.state.value.authoredProgramme!!.revision())
        assertTrue(io { SqliteGymExecutionRepository(app).unfinished().isEmpty() })
        assertTrue(io { CoachingStore(app).strength().isEmpty() })
        assertEquals(programme(1), io { programmes.programme("authored", 1) })
    }

    @Test
    fun busyEquipmentUsesPreservedAuthoredPreferencesAndChangesOnlyTodayAtLargeText() {
        install(largeDark = true)
        start()
        compose.runOnIdle {
            model.openSubstitutions(ExerciseSubstitutionEngine.Reason.EQUIPMENT_BUSY)
        }
        await { model.state.value.substitution != null }
        val choices = model.state.value.substitution!!
        assertEquals(GymProgramme.Goal.STRENGTH, choices.goal)
        assertEquals(GymProgramme.Experience.INTERMEDIATE, choices.experience)
        assertEquals(programme(1).settings().equipment(), choices.equipment)
        assertTrue(choices.suggestions.isNotEmpty())
        val original = execution()!!
        // Ranking order is not a measurement contract. Exercise both supported paths explicitly.
        val replacement =
            choices.suggestions.first { it.exercise().id() == "dumbbell-bench-press" }.exercise()
        assertEquals(original.slots().single().exercise().measurement(), replacement.measurement())
        compose.runOnIdle { model.replaceForToday(replacement) }
        await { execution()!!.slots().single().exercise() == replacement }
        val compatibleTarget = execution()!!.slots().single().target()
        assertNotNull(compatibleTarget)
        assertEquals(original.slots().single().target().withoutLoadReference(), compatibleTarget)
        assertNull(compatibleTarget!!.referenceLoadKg())
        compose.runOnIdle {
            model.openSubstitutions(ExerciseSubstitutionEngine.Reason.EQUIPMENT_BUSY)
        }
        await { model.state.value.substitution != null }
        val bodyweight =
            model.state.value.substitution!!
                .suggestions
                .first { it.exercise().id() == "push-up" }
                .exercise()
        assertNotEquals(replacement.measurement(), bodyweight.measurement())
        compose.runOnIdle { model.replaceForToday(bodyweight) }
        await { execution()!!.slots().single().exercise() == bodyweight }
        assertNull(
            "An incompatible measurement must not inherit a load/repetition task",
            execution()!!.slots().single().target(),
        )
        assertEquals(original.prescription(), execution()!!.prescription())
        assertEquals(original.source(), execution()!!.source())
        assertTrue(execution()!!.actualSets().isEmpty())
        assertEquals(programme(1), io { programmes.programme("authored", 1) })
        assertEquals(1, io { programmes.currentReference()!!.revision() })
    }

    @Test
    fun pausedAuthoredWorkoutRecoversOriginalSourceWithoutAutoResumeOrDuplicateSets() {
        install()
        start()
        compose.runOnIdle {
            model.updateForm {
                it.copy(kilograms = "60", repetitions = "6", effort = "3", rir = true)
            }
            model.saveSet()
        }
        await { execution()!!.actualSets().size == 1 }
        compose.runOnIdle { model.pause() }
        await { execution()!!.status() == GymExecution.Status.PAUSED }
        val original = execution()!!
        compose.runOnIdle { shown.value = null }
        compose.waitForIdle()
        compose.runOnIdle {
            stores.last().clear()
            model = createModel()
            shown.value = model
            page.value = "hub"
        }
        await { true }
        assertNull(execution())
        compose.runOnIdle { model.recover(original.id()) { page.value = "workout" } }
        await { execution()?.status() == GymExecution.Status.PAUSED }
        assertEquals(original.source(), execution()!!.source())
        assertEquals(original.prescription(), execution()!!.prescription())
        assertEquals(original.actualSets(), execution()!!.actualSets())
        assertEquals(original.activeMillis(), execution()!!.activeMillis())
        assertTrue(io { CoachingStore(app).strength().isEmpty() })
    }

    @Test fun authoredTodayAndDetailsExposeExactPreservedTargets() = reviewToday(false)

    @Test fun authoredTodayAndDetailsAreUsableInDarkModeAtTwoHundredPercent() = reviewToday(true)

    private fun reviewToday(largeDark: Boolean) {
        var started = false
        page.value = "today"
        val programme = programme(1)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, if (largeDark) 2f else 1f)
            ) {
                MaterialTheme(
                    colorScheme = if (largeDark) darkColorScheme() else MaterialTheme.colorScheme
                ) {
                    if (page.value == "today")
                        StrengthTodayScreen(
                            CoachUiState(loading = false, authoredGymProgramme = programme),
                            { page.value = it },
                            { started = true },
                        )
                    else AuthoredProgrammeWorkout(programme, "session") { started = true }
                }
            }
        }
        compose.onNodeWithText("Authored session").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Start workout").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(started) }
        compose.onNodeWithText("View workout details").performScrollTo().performClick()
        compose.onNodeWithText("Bench Press").performScrollTo().assertIsDisplayed()
        compose
            .onNodeWithText("3 × 5–8 reps · RIR 1.5–3.5 · 80 kg reference")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("RPE", substring = true).assertDoesNotExist()
        assertTrue(io { CoachingStore(app).strength().isEmpty() })
    }

    private fun start() {
        compose.runOnIdle { model.startPlanned("session") { page.value = "workout" } }
        await { execution() != null }
    }

    private fun install(largeDark: Boolean = false) {
        compose.runOnIdle {
            model = createModel()
            shown.value = model
        }
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, if (largeDark) 2f else 1f)
            ) {
                MaterialTheme(
                    colorScheme = if (largeDark) darkColorScheme() else MaterialTheme.colorScheme
                ) {
                    shown.value?.let { feature ->
                        if (page.value == "hub")
                            GymNowScreen(feature, { page.value = "workout" }, {}, {})
                        else GymWorkoutScreen(feature) { page.value = "hub" }
                    }
                }
            }
        }
        await { true }
    }

    private fun createModel(): GymNowViewModel {
        val store = ViewModelStore().also { stores.add(it) }
        val factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    GymNowViewModel(app, SavedStateHandle()) as T
            }
        return ViewModelProvider(store, factory)[GymNowViewModel::class.java].also {
            models.add(it)
        }
    }

    private fun programme(revision: Int): StrengthProgramme {
        val target =
            GymWorkoutDefinition.Target(
                3,
                5,
                8,
                null,
                null,
                GymWorkoutDefinition.EffortRange(GymWorkoutDefinition.EffortScale.RIR, 1.5, 3.5),
                null,
                80.0,
            )
        val slots =
            listOf(
                GymWorkoutDefinition.Slot(
                    "bench",
                    ExerciseCatalog.definition("bench-press").exercise(),
                    target,
                    null,
                )
            )
        val workout =
            StrengthProgramme.Workout(
                "session",
                day,
                StrengthWorkoutBudget.workoutMinutes(slots),
                GymWorkoutDefinition("workout", 1, created, "Authored session", slots),
                null,
            )
        return StrengthProgramme(
            "authored",
            revision,
            revision - 1,
            created,
            "Authored programme",
            StrengthProgramme.Origin.CUSTOM,
            null,
            StrengthProgramme.Settings(
                GymProgramme.Goal.STRENGTH,
                GymProgramme.Priority.BALANCED,
                GymProgramme.Experience.INTERMEDIATE,
                setOf(day.dayOfWeek),
                1,
                45,
                setOf(
                    ExerciseMetadata.Equipment.BARBELL,
                    ExerciseMetadata.Equipment.BENCH,
                    ExerciseMetadata.Equipment.SQUAT_RACK,
                    ExerciseMetadata.Equipment.DUMBBELLS,
                ),
            ),
            listOf(
                StrengthProgramme.Block(
                    "block",
                    StrengthProgramme.Phase.FOUNDATION,
                    "Foundation",
                    day,
                    1,
                    listOf(workout),
                    "Athlete chose this block",
                )
            ),
            day,
            "Explicit programme revision",
            emptyList(),
        )
    }

    private fun execution() = model.state.value.view?.accepted()?.execution()

    private fun click(text: String) = compose.onNodeWithText(text).performScrollTo().performClick()

    private fun await(condition: () -> Boolean) {
        compose.waitUntil(20_000) {
            !model.state.value.loading && !model.state.value.busy && condition()
        }
        assertNull("Real gym operation failed: ${model.state.value.error}", model.state.value.error)
    }

    private fun <T> io(block: suspend () -> T): T = runBlocking(Dispatchers.IO) { block() }
}
