package org.racedaydreams.coach

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.strength.GymNowViewModel
import org.racedaydreams.coach.strength.GymWorkoutScreen
import org.racedaydreams.data.CoachingStore
import org.racedaydreams.data.GymProgrammeStore
import org.racedaydreams.data.SqliteGymExecutionRepository
import org.racedaydreams.data.SqliteUserPreferencesRepository
import org.racedaydreams.domain.strength.ExerciseCatalog
import org.racedaydreams.domain.strength.ExerciseMetadata.Equipment
import org.racedaydreams.domain.strength.GymExecution
import org.racedaydreams.domain.strength.GymProgramme

/** Accepted substitutions update only execution; old actuals never become replacement actuals. */
@RunWith(AndroidJUnit4::class)
class GymSubstitutionScreenTest {
    @get:Rule val compose = createComposeRule()
    private val application: Application = ApplicationProvider.getApplicationContext()
    private val shown = mutableStateOf(false)
    private val store = ViewModelStore()
    private lateinit var model: GymNowViewModel
    private lateinit var original: GymProgramme

    @Before
    fun seedFixture() {
        io {
            SqliteUserPreferencesRepository(application).clear()
            original = programme()
            GymProgrammeStore(application).save(original)
        }
    }

    @After
    fun closeFixture() {
        compose.runOnIdle { shown.value = false }
        compose.waitForIdle()
        if (::model.isInitialized) compose.waitUntil(20_000) { !model.state.value.busy }
        compose.runOnIdle { store.clear() }
        io { SqliteUserPreferencesRepository(application).clear() }
    }

    @Test
    fun busyEquipmentSwapPreservesOriginalActualAndClearsIncompatibleLoadReferenceAfterChoice() {
        install()
        val frozen = execution().prescription()
        val slotId = execution().selectedSlotId()
        saveOriginalSet()
        val firstActual = execution().actualSets().single()
        compose.runOnIdle { model.updateForm { it.copy(kilograms = "80", repetitions = "8") } }
        val unsubmittedId = model.state.value.form.id
        compose.onNodeWithText("Equipment busy?").performScrollTo().performClick()
        await { model.state.value.substitution != null }
        // Explicit equipment capabilities make the option valid without inferring every gym
        // machine or attachment. The displayed choices still come from the real domain ranker.
        compose.runOnIdle {
            model.changeSubstitutionEquipment(setOf(Equipment.DUMBBELLS, Equipment.BENCH))
        }
        await { model.state.value.substitution?.equipment?.size == 2 }
        chooseInSheet("Choose ${exerciseName("dumbbell-bench-press")}")
        compose
            .onNodeWithText("Use ${exerciseName("dumbbell-bench-press")} just for today?")
            .assertIsDisplayed()
        assertEquals("bench-press", execution().slots().single().exercise().id())
        assertEquals("80", model.state.value.form.kilograms)
        assertEquals(frozen, execution().prescription())
        assertEquals(original, io { GymProgrammeStore(application).current() })
        compose.onNodeWithText("Use just for today").performClick()
        await { execution().slots().single().exercise().id() == "dumbbell-bench-press" }

        val replaced = execution().slots().single()
        assertEquals(slotId, replaced.id())
        assertNull(replaced.target().referenceLoadKg())
        assertEquals(3, replaced.target().sets())
        assertEquals(frozen, execution().prescription())
        assertEquals(80.0, frozen.slots().single().target().referenceLoadKg()!!, 0.0)
        assertEquals(listOf(firstActual), execution().actualSets())
        assertEquals("", model.state.value.form.kilograms)
        assertEquals("", model.state.value.form.repetitions)
        assertNotEquals(unsubmittedId, model.state.value.form.id)
        compose
            .onNodeWithText("0 of 3 target working sets saved")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Before this swap").performScrollTo().assertIsDisplayed()
        assertTrue(io { CoachingStore(application).strength().isEmpty() })
        enter("Actual load · kg", "30")
        enter("Actual repetitions", "8")
        compose.onNodeWithText("Save actual set").performScrollTo().performClick()
        await { execution().actualSets().size == 2 }
        val entered = execution().actualSets().last()
        assertEquals("dumbbell-bench-press", entered.exercise().id())
        assertEquals(30.0, entered.actual().kilograms(), 0.0)
        assertEquals(firstActual, execution().actualSets().first())
        compose
            .onNodeWithText("1 of 3 target working sets saved")
            .performScrollTo()
            .assertIsDisplayed()
        finishAndVerifyOriginalProgramme()
    }

    @Test
    fun discomfortBodyweightAlternativeExplainsLimitationsAndRemovesTheOldMeasurementTarget() {
        install(dark = true)
        val frozen = execution().prescription()
        saveOriginalSet()
        val originalActual = execution().actualSets().single()
        compose.onNodeWithText("Swap exercise").performScrollTo().performClick()
        await { model.state.value.substitution != null }
        chooseInSheet("Movement uncomfortable")
        await { model.state.value.substitution?.reason?.name == "UNCOMFORTABLE" }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Movement comfort"))
        compose.onNodeWithText("Movement comfort").assertIsDisplayed()
        compose.runOnIdle { model.changeSubstitutionEquipment(emptySet()) }
        await { model.state.value.substitution?.equipment?.isEmpty() == true }
        chooseInSheet("Choose ${exerciseName("push-up")}")
        compose
            .onNodeWithText(
                "The measurement changes, so the old target is removed. Record what you perform rather than copying the previous target."
            )
            .assertIsDisplayed()
        assertEquals("bench-press", execution().slots().single().exercise().id())
        compose.onNodeWithText("Use just for today").performClick()
        await { execution().slots().single().exercise().id() == "push-up" }
        assertNull(execution().slots().single().target())
        assertEquals(frozen, execution().prescription())
        assertEquals(listOf(originalActual), execution().actualSets())
        compose.onNodeWithText("0 working sets saved").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Actual load · kg").assertCountEquals(0)
        assertEquals("", model.state.value.form.repetitions)
        enter("Actual repetitions", "6")
        compose.onNodeWithText("Save actual set").performScrollTo().performClick()
        await { execution().actualSets().size == 2 }
        val bodyweight = execution().actualSets().last()
        assertEquals("push-up", bodyweight.exercise().id())
        assertEquals(6, bodyweight.actual().reps())
        assertEquals(0.0, bodyweight.actual().kilograms(), 0.0)
        assertEquals(originalActual, execution().actualSets().first())
        finishAndVerifyOriginalProgramme()
    }

    private fun install(dark: Boolean = false) {
        compose.runOnIdle {
            val factory =
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        GymNowViewModel(application, SavedStateHandle()) as T
                }
            model = ViewModelProvider(store, factory)[GymNowViewModel::class.java]
        }
        compose.setContent {
            MaterialTheme(
                colorScheme = if (dark) darkColorScheme() else MaterialTheme.colorScheme
            ) {
                if (shown.value) GymWorkoutScreen(model) {}
            }
        }
        await { true }
        compose.runOnIdle { model.startPlanned("preserved-bench") { shown.value = true } }
        await {
            model.state.value.view?.accepted()?.execution()?.origin() == GymExecution.Origin.PLANNED
        }
    }

    private fun saveOriginalSet() {
        compose.runOnIdle {
            model.updateForm { it.copy(kilograms = "65", repetitions = "7", effort = "7") }
            model.saveSet()
        }
        await { execution().actualSets().size == 1 }
    }

    private fun chooseInSheet(text: String) {
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
        compose.onNodeWithText(text).performClick()
    }

    private fun enter(label: String, value: String) {
        compose.onNodeWithText(label).performScrollTo().performTextInput(value)
    }

    private fun finishAndVerifyOriginalProgramme() {
        compose.runOnIdle { model.finish(early = true) }
        await { execution().status() == GymExecution.Status.FINISHED }
        val saved = io {
            SqliteGymExecutionRepository(application).load(execution().id())!!.execution()
        }
        assertEquals(execution().actualSets(), saved.actualSets())
        assertTrue(saved.events().any { it.type() == GymExecution.EventType.EXERCISE_REPLACED })
        assertEquals(original, io { GymProgrammeStore(application).current() })
        assertEquals(listOf(original), io { GymProgrammeStore(application).versions() })
        val actual = io { CoachingStore(application).strength().single() }
        assertEquals(2, actual.exercises().size)
        assertEquals("bench-press", actual.exercises().first().exercise().id())
        assertEquals(65.0, actual.exercises().first().sets().single().kilograms(), 0.0)
        assertFalse(io { SqliteUserPreferencesRepository(application).load().isPresent })
    }

    private fun execution() = model.state.value.view!!.accepted().execution()

    private fun await(condition: () -> Boolean) {
        compose.waitUntil(20_000) {
            !model.state.value.loading && !model.state.value.busy && condition()
        }
        assertNull("Gym swap failed: ${model.state.value.error}", model.state.value.error)
    }

    private fun exerciseName(id: String) = ExerciseCatalog.definition(id).exercise().name()

    private fun programme(): GymProgramme {
        val date = LocalDate.now()
        return GymProgramme(
            "swap-programme",
            1,
            Instant.now(),
            "Explicit athlete programme",
            GymProgramme.Settings(
                GymProgramme.Goal.STRENGTH,
                GymProgramme.Priority.STRENGTH_FIRST,
                GymProgramme.Experience.INTERMEDIATE,
                setOf(date.dayOfWeek),
                1,
                setOf("full gym"),
                45,
                GymProgramme.Effort.RPE,
            ),
            date,
            date.plusDays(41),
            listOf(
                GymProgramme.Session(
                    "preserved-bench",
                    date,
                    "Upper body",
                    listOf(
                        GymProgramme.Prescription(
                            "bench-press",
                            3,
                            5,
                            8,
                            null,
                            null,
                            6,
                            8,
                            120,
                            "Original prescribed bench work",
                            80.0,
                        )
                    ),
                    "Today-only changes preserve this exact programme.",
                )
            ),
            emptyList(),
        )
    }

    private fun <T> io(block: () -> T): T = runBlocking(Dispatchers.IO) { block() }
}
