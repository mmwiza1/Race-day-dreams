package org.racedaydreams.coach

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.lifecycle.*
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.racedaydreams.coach.strength.*
import org.racedaydreams.data.*
import org.racedaydreams.domain.strength.*

/**
 * Real typed drafts, feature-owned orchestration and phone UI; activation never creates actuals.
 */
@RunWith(AndroidJUnit4::class)
class StrengthProgrammeBuilderScreenTest {
    @get:Rule val compose = createComposeRule()
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val stores = mutableListOf<ViewModelStore>()
    private val models = mutableListOf<StrengthProgrammeViewModel>()
    private lateinit var model: StrengthProgrammeViewModel
    private lateinit var retained: SavedStateHandle
    private val repository
        get() = SqliteStrengthProgrammingRepository(app)

    private var activations = 0

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
    fun phoneDraftPreviewNeedsExplicitConfirmationAndDoesNotCreateActuals() {
        install()
        click("Build a programme from scratch")
        await { model.state.value.draft != null }
        completeOneDayDraft()
        click("Preview programme")
        await { model.state.value.preview != null }
        compose.onNodeWithText("Review programme").assertIsDisplayed()
        val candidate = model.state.value.preview!!.programme
        assertEquals(4, candidate.workouts().size)
        candidate.workouts().forEach {
            val target = it.definition().slots().single().target()
            assertEquals(6, target.minimumReps())
            assertEquals(10, target.maximumReps())
            assertEquals(2.5, target.effort().minimum(), 0.0)
            assertNull(target.restSeconds())
            assertNull(target.referenceLoadKg())
        }
        assertNull(io { repository.currentReference() })
        click("Review activation")
        compose.onNodeWithText("Keep current").performClick()
        assertNull(io { repository.currentReference() })
        click("Review activation")
        compose.onNodeWithText("Activate programme").performClick()
        await { activations == 1 }
        assertEquals(candidate, io { repository.programme(candidate.id(), candidate.revision()) })
        assertEquals(candidate.id(), io { repository.currentReference()!!.id() })
        assertTrue(io { CoachingStore(app).strength().isEmpty() })
        assertFalse(io { SqliteUserPreferencesRepository(app).load().isPresent })
        // The command adds no new backup fields: the typed draft and accepted revision both survive
        // v9.
        val savedDrafts = io { repository.drafts(20, 0) }
        io {
            val manager =
                org.racedaydreams.portability.BackupManager(app, SqliteBackupRepository(app))
            val output = java.io.ByteArrayOutputStream()
            manager.exportTo(output)
            assertEquals(9, org.json.JSONObject(output.toString("UTF-8")).getInt("version"))
            SqliteUserPreferencesRepository(app).clear()
            manager.restoreFrom(java.io.ByteArrayInputStream(output.toByteArray()))
        }
        assertEquals(candidate, io { repository.programme(candidate.id(), candidate.revision()) })
        assertEquals(savedDrafts, io { repository.drafts(20, 0) })
        assertTrue(io { CoachingStore(app).strength().isEmpty() })
    }

    @Test
    fun recreationRecoversTypedDraftFromOnlyAnIdWithoutActivating() {
        install()
        compose.runOnIdle { model.newDraft() }
        await { model.state.value.draft != null }
        completeOneDayDraft()
        val saved = model.state.value.draft!!
        assertEquals(setOf("strength.programme.draft"), retained.keys())
        lateinit var reopened: StrengthProgrammeViewModel
        compose.runOnIdle {
            reopened = create(SavedStateHandle(mapOf("strength.programme.draft" to saved.id())))
        }
        compose.waitUntil(20_000) { !reopened.state.value.loading && !reopened.state.value.busy }
        assertNull(reopened.state.value.error)
        assertEquals(saved, reopened.state.value.draft)
        assertNull(reopened.state.value.preview)
        assertNull(io { repository.currentReference() })
    }

    @Test
    fun staleDraftOrChangedReviewCannotActivate() {
        install()
        compose.runOnIdle { model.newDraft() }
        await { model.state.value.draft != null }
        completeOneDayDraft()
        compose.runOnIdle { model.preview() }
        await { model.state.value.preview != null }
        val review = model.state.value.preview!!
        val old = model.state.value.draft!!
        val changed =
            StrengthProgrammeDraft(
                old.id(),
                old.revision() + 1,
                old.programmeId(),
                old.baseRevision(),
                "Another writer",
                old.settings(),
                old.start(),
                old.blocks(),
                old.origin(),
                old.templateId(),
            )
        io { repository.saveDraft(changed, old.revision()) }
        compose.runOnIdle { model.activate(review) }
        awaitError()
        assertNull(io { repository.currentReference() })
        assertNull(io { repository.programme(review.programme.id(), 1) })
        compose.runOnIdle { model.reload() }
        await { model.state.value.draft == changed }
        compose.runOnIdle { model.preview() }
        await { model.state.value.preview != null }
        val secondReview = model.state.value.preview!!
        compose.runOnIdle {
            model.header("Edited after review", changed.start(), changed.settings())
        }
        await { model.state.value.draft!!.name() == "Edited after review" }
        compose.runOnIdle { model.activate(secondReview) }
        awaitError()
        assertNull(io { repository.currentReference() })
        assertTrue(io { repository.decisions().isEmpty() })
    }

    @Test
    fun duplicateBlocksAndMoveExercisePreserveOriginalAndValidateEmptyWorkout() {
        install()
        compose.runOnIdle { model.newDraft() }
        await { model.state.value.draft != null }
        completeOneDayDraft()
        val original = model.state.value.draft!!
        val first = original.blocks().single()
        compose.runOnIdle { model.duplicateBlock(first.id()) }
        await { model.state.value.draft!!.blocks().size == 2 }
        val copy = model.state.value.draft!!.blocks()[1]
        assertNotEquals(first.days().single().id(), copy.days().single().id())
        // Clear the destination first: a duplicate block deliberately retains the same exercise.
        compose.runOnIdle {
            model.removeSlot(
                copy.id(),
                copy.days().single().id(),
                copy.days().single().slots().single().id(),
            )
        }
        await { model.state.value.draft!!.blocks()[1].days().single().slots().isEmpty() }
        compose.runOnIdle {
            model.moveSlot(
                first.id(),
                first.days().single().id(),
                copy.id(),
                copy.days().single().id(),
                first.days().single().slots().single().id(),
                0,
            )
        }
        await { model.state.value.draft!!.blocks()[0].days().single().slots().isEmpty() }
        assertEquals(1, model.state.value.draft!!.blocks()[1].days().single().slots().size)
        assertEquals(1, original.blocks().single().days().single().slots().size)
        compose.runOnIdle { model.preview() }
        awaitError()
        assertNull(io { repository.currentReference() })
    }

    @Test
    fun darkLargeTextSettingsAndPreviewRemainScrollable() {
        install(largeDark = true)
        click("Build a programme from scratch")
        await { model.state.value.draft != null }
        completeOneDayDraft()
        click("Programme settings")
        compose
            .onNodeWithText("Programme name")
            .performScrollTo()
            .performTextReplacement("Large text programme")
        compose.onNodeWithText("Save locally").performClick()
        await { model.state.value.draft!!.name() == "Large text programme" }
        click("Add training block")
        compose.onNodeWithText("Cancel").performClick()
        click("Preview programme")
        await { model.state.value.preview != null }
        compose.onNodeWithText("Review programme").assertIsDisplayed()
        compose.onNodeWithText("Review activation").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Keep editing draft").performScrollTo().performClick()
        await { model.state.value.preview == null }
        compose.onNodeWithText("Preview programme").performScrollTo().assertIsDisplayed()
        assertNull(io { repository.currentReference() })
    }

    @Test
    fun datedPreviewOrdersWeekdaysChronologicallyWithoutRewritingStoredPrescriptions() {
        install()
        click("Build a programme from scratch")
        await { model.state.value.draft != null }
        val draft = model.state.value.draft!!
        val block = draft.blocks().single()
        compose.runOnIdle { model.header("Two-day foundation", draft.start(), draft.settings()) }
        await { model.state.value.draft!!.name() == "Two-day foundation" }
        compose.runOnIdle {
            model.putSlot(
                block.id(),
                block.days()[0].id(),
                null,
                ExerciseCatalog.definition("push-up").exercise(),
                GymSlotInput(minimumReps = "6", maximumReps = "10"),
                {},
            )
        }
        await { model.state.value.draft!!.blocks().single().days()[0].slots().size == 1 }
        compose.runOnIdle {
            model.putSlot(
                block.id(),
                block.days()[1].id(),
                null,
                ExerciseCatalog.definition("plank").exercise(),
                GymSlotInput(seconds = "30"),
                {},
            )
        }
        await { model.state.value.draft!!.blocks().single().days()[1].slots().size == 1 }
        click("Preview programme")
        await { model.state.value.preview != null }
        val preserved = model.state.value.preview!!.programme
        val dates = preserved.workouts()
        assertEquals(8, dates.size)
        fun position(index: Int): Float {
            val workout = dates[index]
            val label = "${friendlyDate(workout.date())} · ${workout.definition().title()} · about"
            return compose
                .onNodeWithText(label, substring = true)
                .fetchSemanticsNode()
                .positionInRoot
                .y
        }
        assertTrue(
            "First week's second day must precede the following week",
            position(1) < position(2),
        )
        compose.onNodeWithText("Programme trade-offs").assertExists()
        // Rendering must not rewrite the evidence model or activate a programme.
        assertEquals(preserved, model.state.value.preview!!.programme)
        assertNull(io { repository.currentReference() })
    }

    @Test
    fun sharedTargetParserPreservesUnknownsAndRejectsInvalidNumericEvidence() {
        val push = ExerciseCatalog.definition("push-up").exercise()
        val input =
            GymSlotInput(
                minimumReps = "6",
                maximumReps = "10",
                minimumEffort = "2.5",
                maximumEffort = "4",
                rir = true,
            )
        val target = gymSlotFromInput("push", push, input).target()
        assertEquals(2.5, target.effort().minimum(), 0.0)
        assertNull(target.restSeconds())
        assertNull(target.referenceLoadKg())
        val timed =
            StrengthExercise(
                "test-hold",
                "Test hold",
                StrengthExercise.Region.FULL,
                StrengthExercise.Mode.BODYWEIGHT,
                false,
                StrengthExercise.MeasurementProfile.DURATION,
            )
        assertEquals(
            30L,
            gymSlotFromInput("hold", timed, GymSlotInput(seconds = "30"))
                .target()
                .seconds()!!
                .toLong(),
        )
        val carry =
            StrengthExercise(
                "test-carry",
                "Test carry",
                StrengthExercise.Region.FULL,
                StrengthExercise.Mode.WEIGHTED,
                false,
                StrengthExercise.MeasurementProfile.LOAD_DISTANCE,
            )
        val distance =
            gymSlotFromInput(
                    "carry",
                    carry,
                    GymSlotInput(metres = "25.5", referenceKilograms = "20"),
                )
                .target()
        assertEquals(25.5, distance.metres(), 0.0)
        assertEquals(20.0, distance.referenceLoadKg(), 0.0)
        assertThrows(IllegalArgumentException::class.java) {
            gymSlotFromInput("bad", push, input.copy(maximumEffort = ""))
        }
        assertThrows(IllegalArgumentException::class.java) {
            gymSlotFromInput("bad", carry, GymSlotInput(metres = "NaN"))
        }
    }

    private fun completeOneDayDraft() {
        val draft = model.state.value.draft!!
        val block = draft.blocks().single()
        val day = block.days().first()
        val settings = draft.settings()
        compose.runOnIdle {
            model.header(
                "My strength programme",
                LocalDate.now(),
                StrengthProgramme.Settings(
                    settings.goal(),
                    settings.priority(),
                    settings.experience(),
                    setOf(day.weekday()),
                    1,
                    45,
                    emptySet(),
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
                ExerciseCatalog.definition("push-up").exercise(),
                GymSlotInput(
                    minimumReps = "6",
                    maximumReps = "10",
                    minimumEffort = "2.5",
                    maximumEffort = "4",
                    rir = true,
                ),
                {},
            )
        }
        await { model.state.value.draft!!.blocks().single().days().single().slots().size == 1 }
    }

    private fun install(largeDark: Boolean = false) {
        retained = SavedStateHandle()
        compose.runOnIdle { model = create(retained) }
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, if (largeDark) 2f else 1f)
            ) {
                MaterialTheme(
                    colorScheme = if (largeDark) darkColorScheme() else MaterialTheme.colorScheme
                ) {
                    StrengthProgrammeBuilderScreen(model, {}, { activations++ })
                }
            }
        }
        await { true }
    }

    private fun create(handle: SavedStateHandle): StrengthProgrammeViewModel {
        val store = ViewModelStore().also { stores.add(it) }
        return ViewModelProvider(
                store,
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        StrengthProgrammeViewModel(app, handle) as T
                },
            )[StrengthProgrammeViewModel::class.java]
            .also { models.add(it) }
    }

    private fun click(text: String) = compose.onNodeWithText(text).performScrollTo().performClick()

    private fun await(condition: () -> Boolean) {
        compose.waitUntil(20_000) {
            !model.state.value.loading && !model.state.value.busy && condition()
        }
        assertNull("Authoring failed: ${model.state.value.error}", model.state.value.error)
    }

    private fun awaitError() {
        compose.waitUntil(20_000) { !model.state.value.busy && model.state.value.error != null }
    }

    private fun <T> io(block: suspend () -> T): T = runBlocking(Dispatchers.IO) { block() }
}
