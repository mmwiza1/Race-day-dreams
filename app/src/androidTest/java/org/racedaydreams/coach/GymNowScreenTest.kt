package org.racedaydreams.coach

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
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
import org.racedaydreams.coach.strength.GymNowScreen
import org.racedaydreams.coach.strength.GymNowViewModel
import org.racedaydreams.coach.strength.GymWorkoutBuilderScreen
import org.racedaydreams.coach.strength.GymWorkoutScreen
import org.racedaydreams.data.CoachingStore
import org.racedaydreams.data.DevelopmentStore
import org.racedaydreams.data.GymProgrammeStore
import org.racedaydreams.data.SqliteGymExecutionRepository
import org.racedaydreams.data.SqliteUserPreferencesRepository
import org.racedaydreams.domain.strength.ExerciseCatalog
import org.racedaydreams.domain.strength.GymExecution
import org.racedaydreams.domain.strength.GymProgramme

/** Production screens, feature orchestration and real SQLite; no mocked set acknowledgements. */
@RunWith(AndroidJUnit4::class)
class GymNowScreenTest {
    @get:Rule val compose = createComposeRule()

    private val application: Application = ApplicationProvider.getApplicationContext()
    private val shown = mutableStateOf<GymNowViewModel?>(null)
    private val page = mutableStateOf("hub")
    private val stores = mutableListOf<ViewModelStore>()
    private val models = mutableListOf<GymNowViewModel>()
    private lateinit var model: GymNowViewModel

    @Before
    fun clearFixture() {
        io { SqliteUserPreferencesRepository(application).clear() }
    }

    @After
    fun closeFixture() {
        compose.runOnIdle { shown.value = null }
        compose.waitForIdle()
        compose.waitUntil(20_000) { models.none { it.state.value.busy } }
        compose.runOnIdle { stores.forEach { it.clear() } }
        io { SqliteUserPreferencesRepository(application).clear() }
    }

    @Test
    fun openWorkoutSavesOnlyEnteredActualsAndPauseRequiresExplicitResume() {
        install()
        click("Start open workout")
        await { execution()?.origin() == GymExecution.Origin.FREE }
        assertTrue(execution()!!.actualSets().isEmpty())
        assertTrue(io { CoachingStore(application).strength().isEmpty() })

        click("Add exercise")
        compose.onNodeWithText("Exercise name or alias").performTextInput("bench press")
        click(ExerciseCatalog.definition("bench-press").exercise().name())
        await { execution()?.slots()?.size == 1 }
        val originalInputId = model.state.value.form.id
        assertEquals("", model.state.value.form.kilograms)
        assertEquals("", model.state.value.form.repetitions)
        enter("Actual load · kg", "65")
        enter("Actual repetitions", "7")
        click("Use RIR")
        enter("RIR · optional", "3")
        click("Save actual set")
        await { execution()?.actualSets()?.size == 1 && model.state.value.form.kilograms.isEmpty() }
        assertNotEquals(originalInputId, model.state.value.form.id)
        assertEquals("", model.state.value.form.repetitions)
        assertEquals("", model.state.value.form.effort)
        val entered = execution()!!.actualSets().single().actual()
        assertEquals(65.0, entered.kilograms(), 0.0)
        assertEquals(7, entered.reps())
        assertEquals(3.0, entered.repetitionsInReserve()!!, 0.0)
        assertNull(entered.rpe())
        assertTrue(io { CoachingStore(application).strength().isEmpty() })

        click("Pause workout")
        await { execution()?.status() == GymExecution.Status.PAUSED }
        compose.onNodeWithText("Actual repetitions").performScrollTo().assertIsNotEnabled()
        click("Resume workout")
        await { execution()?.status() == GymExecution.Status.ACTIVE }
        compose.onNodeWithText("Actual repetitions").performScrollTo().assertIsEnabled()
        click("Finish and save workout")
        await { execution()?.status() == GymExecution.Status.FINISHED }
        compose.onNodeWithText("Workout saved").performScrollTo().assertIsDisplayed()
        val saved = io { CoachingStore(application).strength().single() }
        assertEquals(execution()!!.id(), saved.id())
        assertEquals(entered, saved.exercises().single().sets().single())
        assertTrue(io { DevelopmentStore(application).gymLinks().isEmpty() })
        assertFalse(io { SqliteUserPreferencesRepository(application).load().isPresent })
        assertNull(io { GymProgrammeStore(application).current() })
    }

    @Test
    fun quickAndGeneratedEntryFlowsReviewIntentionsAndStartWithDistinctOrigins() {
        install()
        for ((entry, start, origin) in
            listOf(
                Triple("Quick workout", "Start quick workout", GymExecution.Origin.QUICK),
                Triple(
                    "Generate one for me",
                    "Start generated workout",
                    GymExecution.Origin.GENERATED,
                ),
            )) {
            click(entry)
            click("Review suggested workout")
            await { model.state.value.generated != null }
            assertNull(execution()?.completedSession())
            click(start)
            await { execution()?.origin() == origin }
            assertTrue(execution()!!.slots().isNotEmpty())
            assertTrue(execution()!!.actualSets().isEmpty())
            assertNull(execution()!!.source())
            assertEquals("", model.state.value.form.repetitions)
            assertTrue(io { CoachingStore(application).strength().isEmpty() })
            compose.runOnIdle { model.discard() }
            await { execution()?.status() == GymExecution.Status.DISCARDED }
            click("Done")
            await { model.state.value.unfinished.isEmpty() }
        }
        assertNull(io { GymProgrammeStore(application).current() })
        assertTrue(io { DevelopmentStore(application).gymLinks().isEmpty() })
    }

    @Test
    fun builtFavouriteAndRepeatPreserveIntentionsWithoutCopyingActualEvidence() {
        install()
        click("Build a workout")
        enter("Workout name", "Bench practice")
        click("Add exercise")
        enter("Search by name or alias", "bench press")
        click(ExerciseCatalog.definition("bench-press").exercise().name())
        // Keep the editor's three-set default. Targets remain separate from entered actuals.
        enter("Minimum repetitions", "5")
        enter("Maximum repetitions", "8")
        enter("Reference load · kg, optional", "80")
        click("Save exercise")
        await { model.state.value.builder.slots.size == 1 }
        click("Save as a favourite")
        await { model.state.value.favourites.size == 1 }
        val frozen = model.state.value.favourites.single()
        assertEquals("Bench practice", frozen.title())
        assertEquals(80.0, frozen.slots().single().target().referenceLoadKg()!!, 0.0)
        click("Start this workout")
        await { execution()?.origin() == GymExecution.Origin.CUSTOM }
        assertTrue(execution()!!.actualSets().isEmpty())
        assertEquals("", model.state.value.form.kilograms)
        compose.runOnIdle {
            model.updateForm { it.copy(kilograms = "60", repetitions = "7", effort = "7") }
            model.saveSet()
        }
        await { execution()?.actualSets()?.size == 1 }
        click("Finish and save workout")
        compose.onNodeWithText("Finish early and save").performClick()
        await { execution()?.status() == GymExecution.Status.FINISHED }
        val firstActual = execution()!!.completedSession()
        click("Done")
        click("Favourite workouts")
        click("Start favourite")
        await { execution()?.origin() == GymExecution.Origin.FAVOURITE }
        assertEquals(frozen, execution()!!.prescription())
        assertEquals(frozen.id(), execution()!!.source().id())
        assertTrue(execution()!!.actualSets().isEmpty())
        assertEquals("", model.state.value.form.kilograms)
        compose.runOnIdle { model.discard() }
        await { execution()?.status() == GymExecution.Status.DISCARDED }
        click("Done")
        click("Repeat previous workout")
        click("Repeat workout")
        await { execution()?.origin() == GymExecution.Origin.REPEATED }
        assertTrue(execution()!!.actualSets().isEmpty())
        assertEquals("bench-press", execution()!!.slots().single().exercise().id())
        assertNull(execution()!!.slots().single().target()?.referenceLoadKg())
        assertEquals("", model.state.value.form.kilograms)
        assertEquals(firstActual, io { CoachingStore(application).strength().single() })
        assertTrue(io { DevelopmentStore(application).gymLinks().isEmpty() })
    }

    @Test
    fun earlyFinishRequiresChoiceAndRetainsTheStartedProgrammeVersion() {
        val original = programme(1)
        io { GymProgrammeStore(application).save(original) }
        install()
        click("Start planned workout")
        click("Start this session")
        await { execution()?.origin() == GymExecution.Origin.PLANNED }
        val executionId = execution()!!.id()
        assertTrue(execution()!!.actualSets().isEmpty())
        assertEquals("", model.state.value.form.kilograms)
        assertEquals(
            80.0,
            execution()!!.prescription().slots().single().target().referenceLoadKg()!!,
            0.0,
        )
        io { GymProgrammeStore(application).save(programme(2)) }
        enter("Actual load · kg", "60")
        enter("Actual repetitions", "6")
        click("Save actual set")
        await { execution()?.actualSets()?.size == 1 }
        click("Finish and save workout")
        compose.onNodeWithText("Finish this workout early?").assertIsDisplayed()
        assertTrue(io { CoachingStore(application).strength().isEmpty() })
        compose.onNodeWithText("Keep training").performClick()
        assertEquals(GymExecution.Status.ACTIVE, execution()!!.status())
        click("Finish and save workout")
        compose.onNodeWithText("Finish early and save").performClick()
        await { execution()?.status() == GymExecution.Status.FINISHED }
        assertTrue(execution()!!.partial())
        val link = io { DevelopmentStore(application).gymLinks().getValue(executionId) }
        assertEquals(GymProgrammeStore.key(original), link.versionKey())
        assertEquals("bench-session", link.prescribedSessionId())
        assertEquals(original, io { GymProgrammeStore(application).versions().last() })
        assertEquals(2, io { GymProgrammeStore(application).current()!!.version() })
        assertEquals(
            60.0,
            io {
                CoachingStore(application)
                    .strength()
                    .single()
                    .exercises()
                    .single()
                    .sets()
                    .single()
                    .kilograms()
            },
            0.0,
        )
    }

    @Test
    fun explicitPausedRecoveryRetainsTimedActualsAndUnsubmittedFormAtLargeTextInDarkMode() {
        val retained = SavedStateHandle()
        install(retained, largeDark = true)
        click("Start open workout")
        await { execution()?.origin() == GymExecution.Origin.FREE }
        compose.runOnIdle { model.addExercise(ExerciseCatalog.definition("plank").exercise()) }
        await { execution()?.slots()?.size == 1 }
        enter("Actual duration · seconds", "20")
        click("Save actual set")
        await { execution()?.actualSets()?.size == 1 }
        enter("Actual duration · seconds", "25")
        val unsubmittedId = model.state.value.form.id
        click("Pause workout")
        await { execution()?.status() == GymExecution.Status.PAUSED }
        val original = execution()!!

        // A fresh feature ViewModel receives only Android-saveable draft values. It must not
        // silently adopt the old execution clock or count the unfinished stage as training.
        val nextHandle =
            compose.runOnIdle {
                SavedStateHandle(retained.keys().associateWith { retained.get<Any?>(it) })
            }
        compose.runOnIdle { shown.value = null }
        compose.waitForIdle()
        awaitReady()
        compose.runOnIdle {
            stores.last().clear()
            model = createModel(nextHandle)
            page.value = "hub"
            shown.value = model
        }
        awaitReady()
        assertNull(execution())
        assertEquals("25", model.state.value.form.seconds)
        assertEquals(unsubmittedId, model.state.value.form.id)
        assertTrue(io { CoachingStore(application).strength().isEmpty() })
        click("Review unfinished workout")
        await { execution()?.status() == GymExecution.Status.PAUSED }
        assertEquals(original.id(), execution()!!.id())
        assertEquals(original.activeMillis(), execution()!!.activeMillis())
        assertEquals(original.actualSets(), execution()!!.actualSets())
        assertEquals("25", model.state.value.form.seconds)
        compose.onNodeWithText("Actual duration · seconds").performScrollTo().assertIsNotEnabled()
        click("Resume workout")
        await { execution()?.status() == GymExecution.Status.ACTIVE }
        click("Save actual set")
        await { execution()?.actualSets()?.size == 2 }
        assertEquals(
            listOf(20L, 25L),
            execution()!!.actualSets().map { it.actual().durationSeconds() },
        )
        assertEquals(2, execution()!!.actualSets().map { it.actual().id() }.distinct().size)
        click("Finish and save workout")
        await { execution()?.status() == GymExecution.Status.FINISHED }
        compose.onNodeWithText("Workout saved").performScrollTo().assertIsDisplayed()
        val saved = io {
            SqliteGymExecutionRepository(application).load(original.id())!!.execution()
        }
        assertEquals(execution()!!.actualSets(), saved.actualSets())
        assertTrue(saved.events().any { it.type() == GymExecution.EventType.RECOVERED_WITH_GAP })
        assertTrue(io { SqliteGymExecutionRepository(application).unfinished().isEmpty() })
    }

    private fun install(
        retained: SavedStateHandle = SavedStateHandle(),
        largeDark: Boolean = false,
    ) {
        compose.runOnIdle {
            model = createModel(retained)
            shown.value = model
        }
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides
                    Density(density.density, if (largeDark) 2f else density.fontScale)
            ) {
                MaterialTheme(
                    colorScheme = if (largeDark) darkColorScheme() else MaterialTheme.colorScheme
                ) {
                    shown.value?.let { feature ->
                        when (page.value) {
                            "hub" ->
                                GymNowScreen(
                                    feature,
                                    { page.value = "workout" },
                                    { page.value = "builder" },
                                    {},
                                )
                            "builder" ->
                                GymWorkoutBuilderScreen(
                                    feature,
                                    { page.value = "workout" },
                                    { page.value = "hub" },
                                )
                            "workout" -> GymWorkoutScreen(feature) { page.value = "hub" }
                        }
                    }
                }
            }
        }
        awaitReady()
    }

    private fun createModel(retained: SavedStateHandle): GymNowViewModel {
        val store = ViewModelStore()
        stores.add(store)
        val factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    GymNowViewModel(application, retained) as T
            }
        return ViewModelProvider(store, factory)[GymNowViewModel::class.java].also {
            models.add(it)
        }
    }

    private fun execution() = model.state.value.view?.accepted()?.execution()

    private fun click(text: String) {
        compose.onNodeWithText(text).performScrollTo().performClick()
    }

    private fun enter(label: String, value: String) {
        compose.onNodeWithText(label).performScrollTo().performTextInput(value)
    }

    private fun awaitReady() = await { true }

    private fun await(condition: () -> Boolean) {
        compose.waitUntil(20_000) {
            !model.state.value.loading && !model.state.value.busy && condition()
        }
        assertNull("Real gym operation failed: ${model.state.value.error}", model.state.value.error)
    }

    private fun programme(version: Int): GymProgramme {
        val today = LocalDate.now()
        return GymProgramme(
            "screen-programme",
            version,
            Instant.now(),
            "Athlete accepted retained prescription",
            GymProgramme.Settings(
                GymProgramme.Goal.STRENGTH,
                GymProgramme.Priority.STRENGTH_FIRST,
                GymProgramme.Experience.INTERMEDIATE,
                setOf(today.dayOfWeek),
                1,
                setOf("full gym"),
                45,
                GymProgramme.Effort.RPE,
            ),
            today,
            today.plusDays(41),
            listOf(
                GymProgramme.Session(
                    "bench-session",
                    today,
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
                            "Choose manageable actual load",
                            if (version == 1) 80.0 else 82.5,
                        )
                    ),
                    "Original prescription is retained when another version is accepted.",
                )
            ),
            emptyList(),
        )
    }

    private fun <T> io(block: () -> T): T = runBlocking(Dispatchers.IO) { block() }
}
