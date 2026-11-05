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
        compose.onNodeWithText("Start date · dd/mm/yyyy").assert(hasSetTextAction().not())
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

    @Test
    fun programmeReplacementRequiresSavedProposalThenExplicitActivationAndClearsLoad() {
        install()
        val original = prepareSwapProgramme()
        val draft = model.state.value.draft!!
        val workout = draft.programme().workouts().first()
        val slot = workout.definition().slots().first()
        click(
            "${friendlyDate(workout.date())} · ${workout.definition().title()} · about ${workout.expectedMinutes()} min"
        )
        click("Swap ${slot.exercise().name()} in programme")
        await { model.state.value.swapReview != null }
        compose.onNodeWithText("Programme replacement").assertIsDisplayed()
        revealProgrammeAlternative("dumbbell-bench-press")
        chooseProgrammeAlternative("Choose Dumbbell Bench Press")
        chooseProgrammeAlternative("Save programme replacement proposal")
        await { model.state.value.draft!!.revision() == draft.revision() + 1 }
        val proposed = model.state.value.draft!!
        assertEquals(1, proposed.substitutions().size)
        assertEquals("bench-press", proposed.substitutions().single().fromExerciseId())
        val changed = proposed.programme().workouts().first().definition().slots().first()
        assertEquals("dumbbell-bench-press", changed.exercise().id())
        assertNull(changed.target().referenceLoadKg())
        assertEquals(
            80.0,
            original
                .programme()
                .workouts()
                .first()
                .definition()
                .slots()
                .first()
                .target()
                .referenceLoadKg()!!,
            0.0,
        )
        assertEquals(
            original.programme(),
            io { programmes.programme(original.programme().id(), 1) },
        )
        assertEquals(1, io { programmes.currentReference()!!.revision() })
        click("Programme preview")
        click("Review activation")
        compose.onNodeWithText("Activate programme").performClick()
        await { activations == 2 }
        assertEquals(
            proposed.programme(),
            io { programmes.programme(original.programme().id(), 2) },
        )
        assertEquals(
            original.programme(),
            io { programmes.programme(original.programme().id(), 1) },
        )
        assertTrue(io { CoachingStore(app).strength().isEmpty() })
        io {
            val backup = SqliteBackupRepository(app)
            backup.restoreUserData(backup.exportUserData(), false)
        }
        assertEquals(proposed.substitutions(), io { programmes.decisions().last().substitutions() })
    }

    @Test
    fun programmeSwapRechecksExclusionAfterChoicesWithoutChangingProgrammeOrDraft() {
        install()
        prepareSwapProgramme()
        val draft = model.state.value.draft!!
        val workout = draft.programme().workouts().first()
        val slot = workout.definition().slots().first()
        compose.runOnIdle { model.openProgrammeSwap(workout.id(), slot.id()) }
        await { model.state.value.swapReview != null }
        assertTrue(
            model.state.value.swapReview!!.suggestions.any {
                it.exercise().id() == "dumbbell-bench-press"
            }
        )
        io {
            SqliteExerciseExclusionRepository(app)
                .exclude(
                    ExerciseExclusionRepository.Exclusion(
                        "dumbbell-bench-press",
                        ExerciseExclusionRepository.Reason.PREFERENCE,
                        Instant.now(),
                    )
                )
        }
        compose.runOnIdle { model.useProgrammeSwap("dumbbell-bench-press", null, false) }
        compose.waitUntil(20_000) { !model.state.value.busy && model.state.value.error != null }
        assertEquals(draft, model.state.value.draft)
        assertEquals(1, io { programmes.currentReference()!!.revision() })
        assertEquals(
            draft,
            io { SqliteConcreteStrengthProgrammeDraftRepository(app).get(draft.id()) },
        )
        assertTrue(io { CoachingStore(app).strength().isEmpty() })
    }

    @Test
    fun changedProgrammeMeasurementNeedsFreshTargetAndDoesNotTransferReference() {
        install(largeDark = true)
        prepareSwapProgramme()
        val draft = model.state.value.draft!!
        val workout = draft.programme().workouts().first()
        val slot = workout.definition().slots().first()
        compose.runOnIdle { model.openProgrammeSwap(workout.id(), slot.id()) }
        await { model.state.value.swapReview != null }
        val suggestion =
            model.state.value.swapReview!!.suggestions.first { it.measurementChanged() }
        var visible = 4
        while (model.state.value.swapReview!!.suggestions.indexOf(suggestion) >= visible) {
            chooseProgrammeAlternative("Show more alternatives")
            visible += 4
        }
        chooseProgrammeAlternative("Choose ${suggestion.exercise().name()}")
        chooseProgrammeAlternative("Review new measurement target")
        compose.onNodeWithText("Minimum repetitions").performScrollTo().assertExists()
        compose.onNodeWithText("Reference load · kg, optional").assertDoesNotExist()
        compose.runOnIdle {
            model.useProgrammeSwap(
                suggestion.exercise().id(),
                GymSlotInput(
                    sets = "2",
                    minimumReps = "5",
                    maximumReps = "8",
                    minimumEffort = "2",
                    maximumEffort = "3",
                    rir = true,
                ),
                false,
            )
        }
        await { model.state.value.draft!!.revision() == draft.revision() + 1 }
        val replacement =
            model.state.value.draft!!.programme().workouts().first().definition().slots().first()
        assertEquals(suggestion.exercise(), replacement.exercise())
        assertNull(replacement.target().referenceLoadKg())
        assertEquals(5, replacement.target().minimumReps())
        assertEquals(1, io { programmes.currentReference()!!.revision() })
        assertTrue(io { CoachingStore(app).strength().isEmpty() })
    }

    @Test
    fun bulkProgrammeChoiceKeepsEachPhaseTargetWithoutAutomaticActivation() {
        install()
        val original = prepareSwapProgramme()
        val draft = model.state.value.draft!!
        val workout = draft.programme().workouts().first()
        val slot = workout.definition().slots().first()
        compose.runOnIdle { model.openProgrammeSwap(workout.id(), slot.id()) }
        await { model.state.value.swapReview != null }
        revealProgrammeAlternative("dumbbell-bench-press")
        chooseProgrammeAlternative("Choose Dumbbell Bench Press")
        chooseProgrammeAlternative(
            "Apply to all eligible future occurrences of ${slot.exercise().name()}"
        )
        chooseProgrammeAlternative("Save programme replacement proposal")
        await { model.state.value.draft!!.revision() == draft.revision() + 1 }
        val proposed = model.state.value.draft!!
        val expected =
            original.programme().workouts().flatMap { w ->
                w.definition().slots().filter { it.exercise().id() == slot.exercise().id() }
            }
        assertTrue(expected.size > 1)
        assertEquals(expected.size, proposed.substitutions().size)
        proposed.substitutions().forEach { change ->
            val before =
                original
                    .programme()
                    .workouts()
                    .first { it.id() == change.workoutId() }
                    .definition()
                    .slots()
                    .first { it.id() == change.slotId() }
            val after =
                proposed
                    .programme()
                    .workouts()
                    .first { it.id() == change.workoutId() }
                    .definition()
                    .slots()
                    .first { it.id() == change.slotId() }
            assertEquals(before.target().withoutLoadReference(), after.target())
        }
        assertEquals(1, io { programmes.currentReference()!!.revision() })
        assertEquals(
            original.programme(),
            io { programmes.programme(original.programme().id(), 1) },
        )
        io {
            val backup = SqliteBackupRepository(app)
            backup.restoreUserData(backup.exportUserData(), false)
        }
        assertEquals(
            proposed,
            io { SqliteConcreteStrengthProgrammeDraftRepository(app).get(proposed.id()) },
        )
        assertTrue(io { CoachingStore(app).strength().isEmpty() })
    }

    @Test
    fun futureBulkSwapProtectsTheActiveWorkoutThenCanActivateAfterItFinishes() {
        install()
        val original = prepareSwapProgramme()
        val first = original.programme().workouts().first()
        val started = io {
            val prepared = GymWorkoutAdapter.fromAuthoredProgramme(original.programme(), first.id())
            val engine =
                GymExecutionEngine.start(
                    "active-swap-evidence",
                    GymExecution.Origin.PLANNED,
                    prepared.definition(),
                    prepared.source(),
                    ZoneOffset.UTC,
                    GymExecution.Tick("active-swap-boot", 0, Instant.now().minusSeconds(10)),
                )
            val stored = SqliteGymExecutionRepository(app).start(engine.snapshot())
            engine to stored
        }
        compose.runOnIdle { model.reload() }
        await { first.id() in model.state.value.protectedWorkoutIds }
        val draft = model.state.value.draft!!
        val later =
            draft.programme().workouts().first { w ->
                w.id() != first.id() &&
                    w.definition().slots().any { it.exercise().id() == "bench-press" }
            }
        val slot = later.definition().slots().first { it.exercise().id() == "bench-press" }
        compose.runOnIdle { model.openProgrammeSwap(later.id(), slot.id()) }
        await { model.state.value.swapReview != null }
        compose.runOnIdle { model.useProgrammeSwap("dumbbell-bench-press", null, true) }
        await { model.state.value.draft!!.revision() == draft.revision() + 1 }
        val proposal = model.state.value.draft!!
        assertEquals(first, proposal.programme().workouts().first { it.id() == first.id() })
        assertTrue(proposal.substitutions().none { it.workoutId() == first.id() })
        compose.runOnIdle { model.activate(proposal, "blocked-active-swap") { activations++ } }
        compose.waitUntil(20_000) { !model.state.value.busy && model.state.value.error != null }
        assertEquals(1, io { programmes.currentReference()!!.revision() })
        val actual = io {
            val engine = started.first
            val tick = engine.snapshot().lastTick()
            engine.saveSet(
                first.definition().slots().first().id(),
                StrengthSession.SetEntry("active-retained-set", 6, 20.0, 7.0, false),
                GymExecution.Tick(tick.continuityId(), 1000, tick.wallTime().plusSeconds(1)),
            )
            SqliteGymExecutionRepository(app)
                .finish(
                    engine.finish(
                        GymExecution.Tick(
                            tick.continuityId(),
                            2000,
                            tick.wallTime().plusSeconds(2),
                        ),
                        null,
                        "Recorded independently",
                        false,
                    ),
                    started.second.revision(),
                )
                .execution()
        }
        compose.runOnIdle { model.activate(proposal, "accept-finished-swap") { activations++ } }
        await { activations == 2 }
        assertEquals(actual, io { SqliteGymExecutionRepository(app).load(actual.id()).execution() })
        assertEquals(
            first,
            io {
                programmes.programme(original.programme().id(), 2).workouts().first {
                    it.id() == first.id()
                }
            },
        )
        assertEquals(
            original.programme(),
            io { programmes.programme(original.programme().id(), 1) },
        )
    }

    @Test
    fun optionalLoadProposalRequiresEvidenceAndExplicitSaveThenActivation() {
        install()
        val original = prepareSwapProgramme()
        val draft = model.state.value.draft!!
        val workout = draft.programme().workouts().first()
        val slot = workout.definition().slots().first()
        val evidence = seedProgressionEvidence(slot)
        click(
            "${friendlyDate(workout.date())} · ${workout.definition().title()} · about ${workout.expectedMinutes()} min"
        )
        click("Review progression for ${slot.exercise().name()}")
        await { model.state.value.progressionReview != null }
        compose.onNodeWithText("Your workout suggestion").assertIsDisplayed()
        val advice = model.state.value.progressionReview!!.advice
        assertEquals(2, advice.supportingSessionIds().size)
        assertEquals(82.0, advice.proposedTarget().referenceLoadKg()!!, 0.0)
        assertEquals(
            draft,
            io { SqliteConcreteStrengthProgrammeDraftRepository(app).get(draft.id()) },
        )
        compose.onNodeWithText("Keep my current target").performClick()
        assertEquals(draft, model.state.value.draft)
        click("Review progression for ${slot.exercise().name()}")
        await { model.state.value.progressionReview != null }
        chooseProgrammeAlternative("Save this suggestion for review")
        await { model.state.value.draft!!.revision() == draft.revision() + 1 }
        val proposed = model.state.value.draft!!
        assertTrue(proposed.programme().reason().contains("optional target proposal"))
        advice.supportingSessionIds().forEach {
            assertTrue(proposed.programme().reason().contains(it))
        }
        assertEquals(
            advice.proposedTarget(),
            proposed.programme().workouts().first().definition().slots().first().target(),
        )
        assertEquals(draft.programme().workouts().drop(1), proposed.programme().workouts().drop(1))
        assertEquals(
            original.programme(),
            io { programmes.programme(original.programme().id(), 1) },
        )
        assertEquals(evidence.toSet(), io { CoachingStore(app).strength().toSet() })
        compose.runOnIdle { model.activate(proposed, "accept-optional-target") { activations++ } }
        await { activations == 2 }
        assertEquals(
            proposed.programme(),
            io { programmes.programme(original.programme().id(), 2) },
        )
        io {
            val backup = SqliteBackupRepository(app)
            backup.restoreUserData(backup.exportUserData(), false)
        }
        assertEquals(evidence.toSet(), io { CoachingStore(app).strength().toSet() })
    }

    @Test
    fun progressionRechecksChangedActualEvidenceBeforeSaving() {
        install()
        prepareSwapProgramme()
        val draft = model.state.value.draft!!
        val workout = draft.programme().workouts().first()
        val slot = workout.definition().slots().first()
        val evidence = seedProgressionEvidence(slot)
        compose.runOnIdle { model.reviewProgression(workout.id(), slot.id()) }
        await { model.state.value.progressionReview?.advice?.proposedTarget() != null }
        io { CoachingStore(app).deleteStrength(evidence.first().id()) }
        compose.runOnIdle { model.saveProgressionProposal() }
        compose.waitUntil(20_000) { !model.state.value.busy && model.state.value.error != null }
        assertEquals(draft, model.state.value.draft)
        assertEquals(
            draft,
            io { SqliteConcreteStrengthProgrammeDraftRepository(app).get(draft.id()) },
        )
        assertEquals(1, io { programmes.currentReference()!!.revision() })
        assertEquals(setOf(evidence.last()), io { CoachingStore(app).strength().toSet() })
    }

    @Test
    fun missingEffortHasNoNumericProgressionAndLargeDarkReviewRemainsUsable() {
        install(largeDark = true)
        prepareSwapProgramme()
        val draft = model.state.value.draft!!
        val workout = draft.programme().workouts().first()
        val slot = workout.definition().slots().first()
        val evidence = seedProgressionEvidence(slot, effort = null)
        compose.runOnIdle { model.reviewProgression(workout.id(), slot.id()) }
        await { model.state.value.progressionReview != null }
        assertNull(model.state.value.progressionReview!!.advice.proposedTarget())
        compose.onNodeWithText("Save this suggestion for review").assertDoesNotExist()
        compose.onNodeWithText("Keep my current target").assertIsDisplayed().performClick()
        assertEquals(draft, model.state.value.draft)
        assertEquals(evidence.toSet(), io { CoachingStore(app).strength().toSet() })
    }

    @Test
    fun acceptedRevisionHistoryIsReadOnlyAndPreservesTargetsActualsAndCurrentPointer() {
        install()
        val original = prepareSwapProgramme()
        val draft = model.state.value.draft!!
        val workout = draft.programme().workouts().first()
        val slot = workout.definition().slots().first()
        val evidence = seedProgressionEvidence(slot)
        compose.runOnIdle { model.openProgrammeSwap(workout.id(), slot.id()) }
        await { model.state.value.swapReview != null }
        compose.runOnIdle { model.useProgrammeSwap("dumbbell-bench-press", null, false) }
        await { model.state.value.draft!!.revision() == draft.revision() + 1 }
        val revision2 = model.state.value.draft!!.programme()
        compose.runOnIdle {
            model.activate(model.state.value.draft!!, "history-second-revision") { activations++ }
        }
        await { activations == 2 }
        click("Programme revision history")
        await { model.state.value.history.size == 2 }
        compose
            .onNode(hasScrollToIndexAction())
            .performScrollToNode(hasText("Review ${original.programme().name()} revision 1"))
        compose.onNodeWithText("Review ${original.programme().name()} revision 1").performClick()
        await { model.state.value.historicalProgramme != null }
        assertEquals(original.programme(), model.state.value.historicalProgramme)
        compose.onNodeWithText("${original.programme().name()} · revision 1").assertIsDisplayed()
        compose.onNodeWithText("Activate programme").assertDoesNotExist()
        assertEquals(revision2, io { programmes.programme(original.programme().id(), 2) })
        assertEquals(2, io { programmes.currentReference()!!.revision() })
        assertEquals(evidence.toSet(), io { CoachingStore(app).strength().toSet() })
        compose.onNodeWithText("Back to programme library").performClick()
        assertFalse(model.state.value.historyOpen)
    }

    @Test
    fun supportedModalitiesSaveOptionalTasksWithoutInventingLoadOrActualSets() {
        install()
        generate()
        val initialWorkout = model.state.value.draft!!.programme().workouts().first()
        initialWorkout.definition().slots().drop(1).forEach { other ->
            val revision = model.state.value.draft!!.revision()
            compose.runOnIdle { model.removeSlot(initialWorkout.id(), other.id()) }
            await { model.state.value.draft!!.revision() == revision + 1 }
        }
        for (exerciseId in
            listOf("push-up", "weighted-pull-up", "plank", "weighted-plank", "farmers-carry")) {
            val exercise = ExerciseCatalog.definition(exerciseId).exercise()
            val before = model.state.value.draft!!
            val workout = before.programme().workouts().first()
            val slot = workout.definition().slots().first()
            val profile = exercise.measurement()
            val input =
                GymSlotInput(
                    sets = "2",
                    minimumReps = if (profile.usesReps()) "5" else "",
                    maximumReps = if (profile.usesReps()) "8" else "",
                    seconds = if (profile.usesDuration()) "30" else "",
                    metres = if (profile.usesDistance()) "20" else "",
                    minimumEffort = "6",
                    maximumEffort = "8",
                    restSeconds = "90",
                )
            compose.runOnIdle { model.putSlot(workout.id(), slot.id(), exercise, input, {}) }
            await { model.state.value.draft!!.revision() == before.revision() + 1 }
            val edited = model.state.value.draft!!
            val newSlot = edited.programme().workouts().first().definition().slots().first()
            val load = if (profile.usesLoad()) 40.0 else 0.0
            io {
                for (day in 1..2) CoachingStore(app)
                    .saveStrength(
                        StrengthSession(
                            "$exerciseId-evidence-$day",
                            Instant.now().minus(Duration.ofDays(day.toLong())),
                            ZoneOffset.UTC,
                            Duration.ofMinutes(20),
                            null,
                            "",
                            listOf(
                                StrengthSession.ExerciseSets(
                                    exercise,
                                    (1..2).map { set ->
                                        StrengthSession.SetEntry(
                                            "$exerciseId-$day-$set",
                                            if (profile.usesReps()) 8 else 0,
                                            load,
                                            7.0,
                                            false,
                                            if (profile.usesDuration()) 30L else null,
                                            if (profile.usesDistance()) 20.0 else null,
                                        )
                                    },
                                )
                            ),
                        )
                    )
            }
            val actuals = io { CoachingStore(app).strength().toSet() }
            compose.runOnIdle { model.reviewProgression(workout.id(), newSlot.id()) }
            await { model.state.value.progressionReview != null }
            val advice = model.state.value.progressionReview!!.advice
            assertNotNull(exerciseId, advice.proposedTarget())
            if (profile.usesDuration())
                assertEquals(35L, advice.proposedTarget().seconds()!!.toLong())
            if (profile.usesDistance()) assertEquals(25.0, advice.proposedTarget().metres()!!, 0.0)
            if (profile.usesReps())
                assertEquals(
                    if (profile.usesLoad()) 8 else 9,
                    advice.proposedTarget().maximumReps(),
                )
            if (profile.usesLoad())
                assertEquals(
                    if (profile.usesReps()) 41.0 else load,
                    advice.proposedTarget().referenceLoadKg()!!,
                    0.0,
                )
            else assertNull(advice.proposedTarget().referenceLoadKg())
            compose.runOnIdle { model.saveProgressionProposal() }
            await { model.state.value.draft!!.revision() == edited.revision() + 1 }
            assertEquals(
                advice.proposedTarget(),
                model.state.value.draft!!
                    .programme()
                    .workouts()
                    .first()
                    .definition()
                    .slots()
                    .first()
                    .target(),
            )
            assertEquals(actuals, io { CoachingStore(app).strength().toSet() })
            assertNull(io { programmes.currentReference() })
        }
    }

    private fun seedProgressionEvidence(
        slot: GymWorkoutDefinition.Slot,
        effort: Double? = 7.0,
    ): List<StrengthSession> = io {
        (1..2).map { day ->
            val session =
                StrengthSession(
                    "optional-evidence-$day",
                    Instant.now().minus(Duration.ofDays(day.toLong())),
                    ZoneOffset.UTC,
                    Duration.ofMinutes(30),
                    null,
                    "Actuals entered independently",
                    listOf(
                        StrengthSession.ExerciseSets(
                            slot.exercise(),
                            (1..slot.target().sets()).map { set ->
                                StrengthSession.SetEntry(
                                    "evidence-$day-$set",
                                    slot.target().maximumReps(),
                                    80.0,
                                    effort,
                                    false,
                                )
                            },
                        )
                    ),
                )
            CoachingStore(app).saveStrength(session)
            session
        }
    }

    private fun prepareSwapProgramme(): ConcreteStrengthProgrammeDraft {
        generate()
        val initial = model.state.value.draft!!
        val workout = initial.programme().workouts().first()
        val slot = workout.definition().slots().first()
        assertEquals("bench-press", slot.exercise().id())
        val target = slot.target()
        compose.runOnIdle {
            model.putSlot(
                workout.id(),
                slot.id(),
                slot.exercise(),
                GymSlotInput(
                    sets = target.sets().toString(),
                    minimumReps = target.minimumReps().toString(),
                    maximumReps = target.maximumReps().toString(),
                    minimumEffort = target.effort().minimum().toString(),
                    maximumEffort = target.effort().maximum().toString(),
                    restSeconds = target.restSeconds().toString(),
                    referenceKilograms = "80",
                ),
                {},
            )
        }
        await { model.state.value.draft!!.revision() == initial.revision() + 1 }
        val original = model.state.value.draft!!
        compose.runOnIdle { model.activate(original, "initial-programme-swap") { activations++ } }
        await { activations == 1 }
        click("Review future programme changes")
        await { model.state.value.draft?.programme()?.parentRevision() == 1 }
        return original
    }

    private fun revealProgrammeAlternative(exerciseId: String) {
        val index =
            model.state.value.swapReview!!.suggestions.indexOfFirst {
                it.exercise().id() == exerciseId
            }
        assertTrue("Expected a comparable ranked alternative: $exerciseId", index >= 0)
        repeat(index / 4) { chooseProgrammeAlternative("Show more alternatives") }
    }

    private fun chooseProgrammeAlternative(text: String) {
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
        compose.onNodeWithText(text).performClick()
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
