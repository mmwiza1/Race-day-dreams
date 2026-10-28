package org.racedaydreams.coach

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.*
import androidx.test.core.app.ApplicationProvider
import java.time.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.racedaydreams.coach.strength.*
import org.racedaydreams.data.*
import org.racedaydreams.domain.strength.*

/**
 * Real feature VM, native execution, retained prescription, SQL projection and user confirmation.
 */
class GymAssociationScreenTest {
    @get:Rule val compose = createComposeRule()
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val owner = ViewModelStore()
    private val shown = mutableStateOf(false)
    private lateinit var model: GymNowViewModel
    private lateinit var original: GymProgramme

    @Before
    fun seed() {
        io {
            SqliteUserPreferencesRepository(app).clear()
            original = programme()
            GymProgrammeStore(app).save(original)
        }
    }

    @After
    fun close() {
        compose.runOnIdle { shown.value = false }
        compose.waitForIdle()
        if (::model.isInitialized) compose.waitUntil(20_000) { !model.state.value.busy }
        compose.runOnIdle { owner.clear() }
        io { SqliteUserPreferencesRepository(app).clear() }
    }

    @Test
    fun spontaneousWorkoutCountsOnlyAfterReviewAndCanBeExplicitlyKeptExtraAgain() {
        install()
        compose.runOnIdle { model.startFree { shown.value = true } }
        await { execution()?.status() == GymExecution.Status.ACTIVE }
        compose.runOnIdle {
            model.addExercise(ExerciseCatalog.definition("bench-press").exercise())
        }
        await { execution()?.slots()?.size == 1 }
        compose.runOnIdle {
            model.updateForm { it.copy(kilograms = "60", repetitions = "6", effort = "7") }
            model.saveSet()
        }
        await { execution()?.actualSets()?.size == 1 }
        compose.runOnIdle { model.finish() }
        await {
            execution()?.status() == GymExecution.Status.FINISHED &&
                model.state.value.association?.proposals()?.size == 1
        }
        val actualId = execution()!!.id()
        val originalActual = io { CoachingStore(app).strength().single() }
        Assert.assertTrue(io { DevelopmentStore(app).gymLinks().isEmpty() })
        compose
            .onNodeWithTag("gym-association-count-planned-bench")
            .performScrollTo()
            .performClick()
        Assert.assertTrue(io { DevelopmentStore(app).gymLinks().isEmpty() })
        compose.onNodeWithTag("gym-association-confirm").performClick()
        await {
            model.state.value.association?.current()?.choice() ==
                GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION
        }
        Assert.assertEquals(
            GymExecution.Origin.FREE,
            io { SqliteGymExecutionRepository(app).load(actualId).execution().origin() },
        )
        Assert.assertEquals(originalActual, io { CoachingStore(app).strength().single() })
        Assert.assertEquals(original, io { GymProgrammeStore(app).current() })
        Assert.assertTrue(io { DevelopmentStore(app).gymLinks().containsKey(actualId) })
        compose.runOnIdle { model.openCompleted(actualId) {} }
        await {
            model.state.value.association?.current()?.choice() ==
                GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION
        }
        compose.onNodeWithTag("gym-association-unlink").performScrollTo().performClick()
        compose.onNodeWithTag("gym-association-confirm").performClick()
        await {
            model.state.value.association?.current()?.choice() ==
                GymWorkoutAssociation.Choice.KEEP_EXTRA
        }
        Assert.assertTrue(io { DevelopmentStore(app).gymLinks().isEmpty() })
        Assert.assertEquals(originalActual, io { CoachingStore(app).strength().single() })
        Assert.assertEquals(original, io { GymProgrammeStore(app).current() })
    }

    private fun install() {
        compose.runOnIdle {
            model =
                ViewModelProvider(
                    owner,
                    object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T =
                            GymNowViewModel(app, SavedStateHandle()) as T
                    },
                )[GymNowViewModel::class.java]
        }
        compose.setContent { MaterialTheme { if (shown.value) GymWorkoutScreen(model) {} } }
        await { true }
    }

    private fun execution() = model.state.value.view?.accepted()?.execution()

    private fun await(condition: () -> Boolean) {
        compose.waitUntil(20_000) {
            !model.state.value.loading && !model.state.value.busy && condition()
        }
        Assert.assertNull(model.state.value.error, model.state.value.error)
    }

    private fun <T> io(action: () -> T): T = runBlocking(Dispatchers.IO) { action() }

    private fun programme(): GymProgramme {
        val date = LocalDate.now()
        return GymProgramme(
            "association-ui",
            1,
            Instant.now(),
            "Athlete's explicit programme",
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
                    "planned-bench",
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
                            "Preserved target",
                            80.0,
                        )
                    ),
                    "Original programme version",
                )
            ),
            emptyList(),
        )
    }
}
