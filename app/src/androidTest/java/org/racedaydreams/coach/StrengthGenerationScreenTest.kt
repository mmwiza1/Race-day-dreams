package org.racedaydreams.coach

import android.app.Application
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
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

/** Production phone workflow, typed SQLite and explicit review; no mock executor or actuals. */
@RunWith(AndroidJUnit4::class)
class StrengthGenerationScreenTest {
    @get:Rule val compose = createComposeRule()
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val stores = mutableListOf<ViewModelStore>()
    private val models = mutableListOf<StrengthGenerationViewModel>()
    private lateinit var model: StrengthGenerationViewModel
    private var activations = 0
    private val programmes
        get() = SqliteStrengthProgrammingRepository(app)

    @Before
    fun clear() {
        io { SqliteUserPreferencesRepository(app).clear() }
    }

    @After
    fun close() {
        compose.waitUntil(20_000) { models.none { it.state.value.busy } }
        compose.runOnIdle { stores.forEach { it.clear() } }
        io { SqliteUserPreferencesRepository(app).clear() }
    }

    @Test
    fun templateConfigurationCreatesDatedEditablePreviewAndNeedsExplicitActivation() {
        install()
        click("Configure 2-day Full Body")
        compose
            .onNodeWithText("Programme duration · weeks (1–26)")
            .performScrollTo()
            .performTextReplacement("4")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        click("Generate saved preview")
        await { model.state.value.draft != null }
        val candidate = model.state.value.draft!!.programme()
        assertEquals("full-body-2", candidate.templateId())
        assertEquals(8, candidate.workouts().size)
        assertTrue(candidate.settings().equipment().isEmpty())
        assertTrue(
            candidate.workouts().all { w -> w.definition().slots().all { it.target() != null } }
        )
        assertFalse(candidate.tradeoffs().isEmpty())
        assertNull(io { programmes.currentReference() })
        click("Review activation")
        compose.onNodeWithText("Keep reviewing").performClick()
        assertNull(io { programmes.currentReference() })
        click("Review activation")
        compose.onNodeWithText("Activate programme").performClick()
        await { activations == 1 }
        assertEquals(candidate, io { programmes.programme(candidate.id(), 1) })
        assertTrue(io { CoachingStore(app).strength().isEmpty() })
        assertFalse(io { SqliteUserPreferencesRepository(app).load().isPresent })
    }

    @Test
    fun recoveryKeepsEveryPhaseAndEditsOnlySelectedDatedWorkout() {
        install()
        generate()
        val old = model.state.value.draft!!
        assertEquals(64, old.programme().workouts().size)
        val workout = old.programme().workouts().first()
        compose.runOnIdle {
            model.reorder(workout.id(), workout.definition().slots().first().id(), 1)
        }
        await { model.state.value.draft!!.revision() == 2 }
        val edited = model.state.value.draft!!
        assertEquals(old.programme().tradeoffs(), edited.programme().tradeoffs())
        assertEquals(
            old.programme().workouts().filterNot { it.id() == workout.id() },
            edited.programme().workouts().filterNot { it.id() == workout.id() },
        )
        assertEquals(
            workout.definition().slots().first(),
            edited.programme().workouts().first().definition().slots()[1],
        )
        lateinit var reopened: StrengthGenerationViewModel
        val handle = SavedStateHandle(mapOf("strength.concrete.draft" to old.id()))
        compose.runOnIdle { reopened = create(handle) }
        compose.waitUntil(20_000) { !reopened.state.value.loading && !reopened.state.value.busy }
        assertEquals(edited, reopened.state.value.draft)
        assertEquals(setOf("strength.concrete.draft"), handle.keys())
        assertNull(io { programmes.currentReference() })
        assertTrue(io { CoachingStore(app).strength().isEmpty() })
    }

    @Test
    fun darkLargeTextLibraryAndRecoveryRemainScrollableAndDoNotActivate() {
        install(largeDark = true)
        compose.onNodeWithText("Templates & generation").assertIsDisplayed()
        click("Configure 4-day Upper / Lower")
        compose.onNodeWithText("Shape your programme").assertIsDisplayed()
        click("Available equipment · 0 capabilities")
        compose.onNodeWithText("Dumbbells", substring = false).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Generate saved preview").performScrollTo().assertIsDisplayed()
        assertNull(io { programmes.currentReference() })
    }

    @Test
    fun excludedExerciseChangedAfterPreviewPreventsActivationWithoutLosingDraft() {
        install()
        generate()
        val draft = model.state.value.draft!!
        val exercise =
            draft.programme().workouts().first().definition().slots().first().exercise().id()
        io {
            SqliteExerciseExclusionRepository(app)
                .exclude(
                    ExerciseExclusionRepository.Exclusion(
                        exercise,
                        ExerciseExclusionRepository.Reason.PREFERENCE,
                        Instant.now(),
                    )
                )
        }
        compose.runOnIdle { model.activate(draft, "excluded-ui") { activations++ } }
        compose.waitUntil(20_000) { !model.state.value.busy && model.state.value.error != null }
        assertEquals(0, activations)
        assertEquals(draft, model.state.value.draft)
        assertNull(io { programmes.currentReference() })
        assertEquals(
            draft,
            io { SqliteConcreteStrengthProgrammeDraftRepository(app).get(draft.id()) },
        )
    }

    @Test
    fun changedWorkingSetsRecalculateBudgetBeforeExplicitActivation() {
        install()
        generate()
        val initial = model.state.value.draft!!
        val workout = initial.programme().workouts().first()
        val slot = workout.definition().slots().first()
        val target = slot.target()!!
        compose.runOnIdle {
            model.putSlot(
                workout.id(),
                slot.id(),
                slot.exercise(),
                GymSlotInput(
                    sets = "1",
                    minimumReps = target.minimumReps()?.toString() ?: "",
                    maximumReps = target.maximumReps()?.toString() ?: "",
                    seconds = target.seconds()?.toString() ?: "",
                    metres = target.metres()?.toString() ?: "",
                    restSeconds = target.restSeconds()?.toString() ?: "",
                ),
                {},
            )
        }
        await { model.state.value.draft!!.revision() == 2 }
        val edited = model.state.value.draft!!
        val changed = edited.programme().workouts().first()
        assertEquals(1, changed.definition().slots().first().target().sets())
        assertEquals(
            StrengthWorkoutBudget.workoutMinutes(changed.definition().slots()),
            changed.expectedMinutes(),
        )
        assertTrue(changed.expectedMinutes() < workout.expectedMinutes())
        assertEquals(initial.programme().workouts().drop(1), edited.programme().workouts().drop(1))
        compose.runOnIdle { model.activate(edited, "edited-duration") { activations++ } }
        await { activations == 1 }
        assertEquals(edited.programme(), io { programmes.programme(edited.programme().id(), 1) })
        assertTrue(io { CoachingStore(app).strength().isEmpty() })
    }

    @Test
    fun futureEditsCreateExplicitRevisionAndRetainOriginalPrescriptions() {
        install()
        generate()
        val original = model.state.value.draft!!
        compose.runOnIdle { model.activate(original, "initial-future-edit") { activations++ } }
        await { activations == 1 }
        click("Review future programme changes")
        await { model.state.value.draft?.programme()?.parentRevision() == 1 }
        val future = model.state.value.draft!!
        assertEquals(original.programme().blocks(), future.programme().blocks())
        assertEquals(original.programme().id(), future.before()!!.id())
        assertEquals(1, io { programmes.currentReference()!!.revision() })
        val workout = future.programme().workouts().last()
        compose.runOnIdle {
            model.reorder(workout.id(), workout.definition().slots().first().id(), 1)
        }
        await { model.state.value.draft!!.revision() == 2 }
        val edited = model.state.value.draft!!
        assertEquals(
            original.programme(),
            io { programmes.programme(original.programme().id(), 1) },
        )
        click("Review activation")
        compose.onNodeWithText("Activate programme").performClick()
        await { activations == 2 }
        assertEquals(2, io { programmes.currentReference()!!.revision() })
        assertEquals(edited.programme(), io { programmes.programme(original.programme().id(), 2) })
        assertEquals(
            original.programme(),
            io { programmes.programme(original.programme().id(), 1) },
        )
        assertTrue(io { CoachingStore(app).strength().isEmpty() })
    }

    @Test
    fun regenerationRetainsNativeCompletedWorkoutAndActualsAcrossChangedGoalAndDays() {
        install()
        generate()
        val original = model.state.value.draft!!
        compose.runOnIdle { model.activate(original, "initial-regeneration") { activations++ } }
        await { activations == 1 }
        val first = original.programme().workouts().first()
        val actual = io {
            val prepared = GymWorkoutAdapter.fromAuthoredProgramme(original.programme(), first.id())
            val now = Instant.now().minusSeconds(10)
            fun tick(milliseconds: Long) =
                GymExecution.Tick("test-boot", milliseconds, now.plusMillis(milliseconds))
            val engine =
                GymExecutionEngine.start(
                    "future-retained-actual",
                    GymExecution.Origin.PLANNED,
                    prepared.definition(),
                    prepared.source(),
                    ZoneOffset.UTC,
                    tick(0),
                )
            val gym = SqliteGymExecutionRepository(app)
            val started = gym.start(engine.snapshot())
            engine.saveSet(
                first.definition().slots().first().id(),
                StrengthSession.SetEntry("retained-set", 6, 20.0, 7.0, false),
                tick(1000),
            )
            gym.finish(
                    engine.finish(tick(2000), null, "Retained native actual", false),
                    started.revision(),
                )
                .execution()
        }
        click("Review future programme changes")
        await { model.state.value.draft?.programme()?.parentRevision() == 1 }
        assertTrue(first.id() in model.state.value.protectedWorkoutIds)
        val before = model.state.value.draft!!
        compose.runOnIdle { model.reorder(first.id(), first.definition().slots().first().id(), 1) }
        compose.waitUntil(20_000) { !model.state.value.busy && model.state.value.error != null }
        assertEquals(before, model.state.value.draft)
        compose.runOnIdle {
            model.clearMessage()
            model.generate(
                "Reviewed muscle programme",
                original.programme().start(),
                16,
                StrengthProgramme.Settings(
                    GymProgramme.Goal.MUSCLE,
                    GymProgramme.Priority.BALANCED,
                    GymProgramme.Experience.INTERMEDIATE,
                    setOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY),
                    2,
                    60,
                    ExerciseMetadata.Equipment.values().toSet(),
                ),
                null,
                false,
                {},
            )
        }
        await { model.state.value.draft!!.id() != before.id() }
        val regenerated = model.state.value.draft!!
        assertEquals(2, regenerated.programme().revision())
        assertEquals(first, regenerated.programme().workouts().first { it.id() == first.id() })
        assertEquals(1, io { programmes.currentReference()!!.revision() })
        compose.runOnIdle { model.activate(regenerated, "accept-regeneration") { activations++ } }
        await { activations == 2 }
        assertEquals(
            original.programme(),
            io { programmes.programme(original.programme().id(), 1) },
        )
        assertEquals(actual, io { SqliteGymExecutionRepository(app).load(actual.id()).execution() })
        assertTrue(io { programmes.decisions().last().protectedWorkoutIds().contains(first.id()) })
        io {
            val backup = SqliteBackupRepository(app)
            backup.restoreUserData(backup.exportUserData(), false)
        }
        assertEquals(
            regenerated.programme(),
            io { programmes.programme(original.programme().id(), 2) },
        )
        assertEquals(actual, io { SqliteGymExecutionRepository(app).load(actual.id()).execution() })
        compose.runOnIdle { model.open(before.id()) {} }
        await { model.state.value.draft?.id() == before.id() }
        assertTrue(model.state.value.superseded)
        assertFalse(model.state.value.accepted)
        assertEquals(
            regenerated.programme(),
            io { programmes.programme(original.programme().id(), 2) },
        )
    }

    @Test
    fun futureRegenerationFormCanChangeFrequencyWithoutKeepingAnIncompatibleTemplate() {
        install()
        generate()
        val original = model.state.value.draft!!
        compose.runOnIdle { model.activate(original, "initial-frequency") { activations++ } }
        await { activations == 1 }
        click("Review future programme changes")
        await { model.state.value.draft?.programme()?.parentRevision() == 1 }
        val future = model.state.value.draft!!
        click("Regenerate future programme")
        compose.onNodeWithText("Start date · dd/mm/yyyy").assertIsNotEnabled()
        click("2")
        compose.onNodeWithText("Customised structure").assertExists()
        click("Generate saved preview")
        await { model.state.value.draft?.id() != future.id() }
        val candidate = model.state.value.draft!!.programme()
        assertEquals(2, candidate.settings().frequency())
        assertEquals(2, candidate.revision())
        assertNull(candidate.templateId())
        assertEquals(original.programme().start(), candidate.start())
        assertEquals(1, io { programmes.currentReference()!!.revision() })
        click("Review activation")
        compose.onNodeWithText("Activate programme").performClick()
        await { activations == 2 }
        assertEquals(candidate, io { programmes.programme(candidate.id(), 2) })
        assertEquals(original.programme(), io { programmes.programme(candidate.id(), 1) })
    }

    private fun generate() {
        compose.runOnIdle {
            model.generate(
                "Development preview",
                LocalDate.now(),
                16,
                StrengthProgramme.Settings(
                    GymProgramme.Goal.STRENGTH,
                    GymProgramme.Priority.BALANCED,
                    GymProgramme.Experience.INTERMEDIATE,
                    DayOfWeek.values().toSet(),
                    4,
                    60,
                    ExerciseMetadata.Equipment.values().toSet(),
                ),
                "upper-lower-4",
                true,
                {},
            )
        }
        await { model.state.value.draft != null }
    }

    private fun install(largeDark: Boolean = false) {
        compose.runOnIdle { model = create(SavedStateHandle()) }
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, if (largeDark) 2f else 1f)
            ) {
                MaterialTheme(
                    colorScheme = if (largeDark) darkColorScheme() else MaterialTheme.colorScheme
                ) {
                    StrengthGenerationScreen(model, {}, { activations++ })
                }
            }
        }
        await { true }
    }

    private fun create(handle: SavedStateHandle): StrengthGenerationViewModel {
        val store = ViewModelStore().also { stores.add(it) }
        return ViewModelProvider(
                store,
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        StrengthGenerationViewModel(app, handle) as T
                },
            )[StrengthGenerationViewModel::class.java]
            .also { models.add(it) }
    }

    private fun click(text: String) = compose.onNodeWithText(text).performScrollTo().performClick()

    private fun await(condition: () -> Boolean) {
        compose.waitUntil(20_000) {
            !model.state.value.loading && !model.state.value.busy && condition()
        }
        assertNull("Generation failed: ${model.state.value.error}", model.state.value.error)
    }

    private fun <T> io(block: suspend () -> T): T = runBlocking(Dispatchers.IO) { block() }
}
