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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.racedaydreams.coach.strength.*
import org.racedaydreams.data.*
import org.racedaydreams.domain.strength.*

@RunWith(AndroidJUnit4::class)
class CustomStrengthExerciseScreenTest {
    @get:Rule val compose = createComposeRule()
    private val app: Application = ApplicationProvider.getApplicationContext()
    private lateinit var model: StrengthProgrammeViewModel
    private val stores = mutableListOf<ViewModelStore>()
    private var savedId: String? = null
    private var gym: GymNowViewModel? = null
    private val repository
        get() = SqliteStrengthProgrammingRepository(app)

    @Before
    fun clear() {
        io { SqliteUserPreferencesRepository(app).clear() }
    }

    @After
    fun close() {
        compose.waitUntil(20_000) { !model.state.value.busy && gym?.state?.value?.busy != true }
        compose.runOnIdle { stores.forEach { it.clear() } }
        io { SqliteUserPreferencesRepository(app).clear() }
    }

    @Test
    fun phoneCreatesStructuredMetadataAndLoggingMirrorWithoutFakeGuidance() {
        install()
        field("Exercise name", "My dumbbell press")
        choose("Body area · Choose", "Chest")
        choose("Movement role · Choose", "Horizontal push")
        choose("Measurement · Choose", "Load + repetitions")
        click("Equipment · 0 selected")
        click("Dumbbells")
        field("Notes · optional", "Use the low bench at my gym")
        click("Save custom exercise")
        await { savedId != null }
        val custom = io { repository.customExercise(savedId)!! }
        assertEquals("My dumbbell press", custom.exercise().name())
        assertEquals(ExerciseMetadata.BodyArea.CHEST, custom.bodyArea())
        assertEquals(ExerciseMetadata.Role.HORIZONTAL_PUSH, custom.role())
        assertEquals(
            setOf(ExerciseMetadata.Equipment.DUMBBELLS),
            custom.equipment().alternatives().single(),
        )
        assertEquals("Use the low bench at my gym", custom.notes())
        assertFalse(custom.exercise().estimateOneRepMax())
        assertNull(ExerciseCatalog.definition(custom.exercise().id()))
        assertNull(TechniqueGuide.forExercise(custom.exercise().id()))
        assertEquals(
            custom.exercise(),
            io { CoachingStore(app).exercises().single { it.id() == savedId } },
        )
        assertEquals(custom, model.state.value.customExercises[savedId])
        assertTrue(exerciseBrowseContext(custom.exercise(), custom).contains("Horizontal push"))
        assertNull(io { repository.currentReference() })
        assertTrue(io { CoachingStore(app).strength().isEmpty() })
    }

    @Test
    fun darkLargeTextRequiresExplicitMetadataAndEquipmentKnowledge() {
        install(largeDark = true)
        click("Save custom exercise")
        compose.onNodeWithText("Enter an exercise name.").performScrollTo().assertIsDisplayed()
        field("Exercise name", "My hold")
        click("Save custom exercise")
        compose
            .onNodeWithText("Choose body area, movement role and measurement.")
            .performScrollTo()
            .assertIsDisplayed()
        choose("Body area · Choose", "Trunk")
        choose("Movement role · Choose", "Core")
        choose("Measurement · Choose", "Duration")
        click("Save custom exercise")
        compose
            .onNodeWithText("Select the required equipment, or explicitly choose no equipment.")
            .performScrollTo()
            .assertIsDisplayed()
        assertTrue(io { repository.customExercises().isEmpty() })
        click("Equipment · 0 selected")
        click("No equipment required")
        click("Save custom exercise")
        await { savedId != null }
        val custom = io { repository.customExercise(savedId)!! }
        assertEquals(StrengthExercise.MeasurementProfile.DURATION, custom.exercise().measurement())
        assertTrue(custom.equipment().equipmentFree())
        assertFalse(custom.exercise().estimateOneRepMax())
        assertNull(io { repository.currentReference() })
    }

    @Test
    fun allMeasurementProfilesAndLegacyCustomIdentityStayIndependent() {
        install()
        val legacy =
            StrengthExercise(
                "72d30918-817f-45ec-9da4-108e40ce2f2b",
                "Legacy custom",
                StrengthExercise.Region.FULL,
                StrengthExercise.Mode.BODYWEIGHT,
                false,
            )
        io { CoachingStore(app).saveExercise(legacy) }
        val ids = mutableSetOf<String>()
        StrengthExercise.MeasurementProfile.values().forEach { profile ->
            var created: String? = null
            compose.runOnIdle {
                model.createCustom(
                    "My ${profile.name}",
                    ExerciseMetadata.BodyArea.TRUNK,
                    ExerciseMetadata.Role.CORE,
                    profile,
                    false,
                    emptySet(),
                    null,
                ) {
                    created = it
                }
            }
            await { created != null }
            val custom = io { repository.customExercise(created)!! }
            assertTrue(ids.add(created!!))
            assertTrue(custom.exercise().id().startsWith("custom-"))
            assertEquals(profile, custom.exercise().measurement())
            assertFalse(custom.exercise().estimateOneRepMax())
            assertNull(TechniqueGuide.forExercise(created))
        }
        compose.runOnIdle {
            model.createCustom(
                "My weighted pull-up",
                ExerciseMetadata.BodyArea.LATS,
                ExerciseMetadata.Role.VERTICAL_PULL,
                StrengthExercise.MeasurementProfile.LOAD_REPS,
                true,
                setOf(ExerciseMetadata.Equipment.PULL_UP_BAR),
                null,
            ) {
                savedId = it
            }
        }
        await { savedId != null }
        assertEquals(
            StrengthExercise.Mode.WEIGHTED_BODYWEIGHT,
            io { repository.customExercise(savedId)!!.exercise().mode() },
        )
        assertEquals(
            legacy,
            io { CoachingStore(app).exercises().single { it.id() == legacy.id() } },
        )
        assertEquals(7, io { repository.customExercises().size })
        assertTrue(io { CoachingStore(app).strength().isEmpty() })
    }

    @Test
    fun customProgrammeUsesNativeExecutionAndBacksUpActualEnteredLoadSeparately() {
        install()
        compose.runOnIdle {
            model.createCustom(
                "My press",
                ExerciseMetadata.BodyArea.CHEST,
                ExerciseMetadata.Role.HORIZONTAL_PUSH,
                StrengthExercise.MeasurementProfile.LOAD_REPS,
                false,
                setOf(ExerciseMetadata.Equipment.DUMBBELLS),
                "Personal setup",
            ) {
                savedId = it
            }
        }
        await { savedId != null }
        val custom = io { repository.customExercise(savedId)!! }
        compose.runOnIdle { model.newDraft() }
        await { model.state.value.draft != null }
        val draft = model.state.value.draft!!
        val block = draft.blocks().single()
        val day = block.days().first()
        val settings = draft.settings()
        compose.runOnIdle {
            model.header(
                "Custom movement programme",
                draft.start(),
                StrengthProgramme.Settings(
                    settings.goal(),
                    settings.priority(),
                    settings.experience(),
                    setOf(day.weekday()),
                    1,
                    45,
                    setOf(ExerciseMetadata.Equipment.DUMBBELLS),
                ),
            )
        }
        await { model.state.value.draft!!.settings().frequency() == 1 }
        compose.runOnIdle { model.deleteDay(block.id(), block.days()[1].id()) }
        await { model.state.value.draft!!.blocks().single().days().size == 1 }
        compose.runOnIdle {
            model.putSlot(
                block.id(),
                day.id(),
                null,
                custom.exercise(),
                GymSlotInput(
                    minimumReps = "8",
                    maximumReps = "12",
                    minimumEffort = "6",
                    maximumEffort = "8",
                    referenceKilograms = "20",
                ),
                {},
            )
        }
        await { model.state.value.draft!!.blocks().single().days().single().slots().size == 1 }
        compose.runOnIdle { model.preview() }
        await { model.state.value.preview != null }
        val candidate = model.state.value.preview!!.programme
        compose.runOnIdle { model.activate() }
        await { model.state.value.current?.id() == candidate.id() }
        compose.runOnIdle {
            val store = ViewModelStore().also { stores.add(it) }
            gym =
                ViewModelProvider(store, factory { GymNowViewModel(app, SavedStateHandle()) })[
                    GymNowViewModel::class.java]
        }
        awaitGym { true }
        compose.runOnIdle { gym!!.startPlanned(candidate.workouts().first().id()) {} }
        awaitGym { it.view?.accepted() != null }
        assertTrue(gym!!.state.value.view!!.accepted().execution().actualSets().isEmpty())
        assertEquals("", gym!!.state.value.form.kilograms)
        compose.runOnIdle {
            gym!!.updateForm {
                it.copy(kilograms = "15", repetitions = "9", effort = "7", rir = false)
            }
            gym!!.saveSet()
        }
        awaitGym { it.view?.accepted()?.execution()?.actualSets()?.size == 1 }
        compose.runOnIdle { gym!!.finish(true) }
        awaitGym { it.view?.accepted()?.execution()?.status() == GymExecution.Status.FINISHED }
        val actual = io { CoachingStore(app).strength().single() }
        assertEquals(savedId, actual.exercises().single().exercise().id())
        assertEquals(15.0, actual.exercises().single().sets().single().kilograms(), 0.0)
        assertEquals(
            20.0,
            candidate.workouts().first().definition().slots().single().target().referenceLoadKg(),
            0.0,
        )
        io {
            val backup =
                org.racedaydreams.portability.BackupManager(app, SqliteBackupRepository(app))
            val out = java.io.ByteArrayOutputStream()
            backup.exportTo(out)
            SqliteUserPreferencesRepository(app).clear()
            backup.restoreFrom(java.io.ByteArrayInputStream(out.toByteArray()))
        }
        assertEquals(custom, io { repository.customExercise(savedId) })
        assertEquals(candidate, io { repository.programme(candidate.id(), candidate.revision()) })
        assertEquals(actual, io { CoachingStore(app).strength().single() })
    }

    private fun install(largeDark: Boolean = false) {
        compose.runOnIdle {
            val store = ViewModelStore().also { stores.add(it) }
            model =
                ViewModelProvider(
                    store,
                    factory { StrengthProgrammeViewModel(app, SavedStateHandle()) },
                )[StrengthProgrammeViewModel::class.java]
        }
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, if (largeDark) 2f else 1f)
            ) {
                MaterialTheme(
                    colorScheme = if (largeDark) darkColorScheme() else MaterialTheme.colorScheme
                ) {
                    CustomStrengthExerciseScreen(model, {}) { savedId = it }
                }
            }
        }
        await { true }
    }

    private fun factory(create: () -> ViewModel) =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
        }

    private fun choose(title: String, value: String) {
        click(title)
        click(value)
    }

    private fun click(text: String) = compose.onNodeWithText(text).performScrollTo().performClick()

    private fun field(label: String, text: String) =
        compose.onNodeWithText(label).performScrollTo().performTextReplacement(text)

    private fun await(condition: () -> Boolean) {
        compose.waitUntil(20_000) {
            !model.state.value.loading && !model.state.value.busy && condition()
        }
        assertNull(model.state.value.error)
    }

    private fun awaitGym(condition: (GymNowState) -> Boolean) {
        compose.waitUntil(20_000) {
            gym!!.state.value.let { !it.loading && !it.busy && condition(it) }
        }
        assertNull(gym!!.state.value.error)
    }

    private fun <T> io(block: suspend () -> T): T = runBlocking(Dispatchers.IO) { block() }
}
