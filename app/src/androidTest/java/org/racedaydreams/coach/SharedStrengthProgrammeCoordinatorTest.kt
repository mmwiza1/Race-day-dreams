package org.racedaydreams.coach

import android.database.sqlite.SQLiteDatabase
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.strength.*
import org.racedaydreams.data.AndroidSqlConnection
import org.racedaydreams.data.createAndroidSchemaFixture
import org.racedaydreams.domain.strength.*
import org.racedaydreams.storage.*

/** Real common authoring and typed SQL, not a substitute programme/execution engine. */
@RunWith(AndroidJUnit4::class)
class SharedStrengthProgrammeCoordinatorTest {
    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private val now = Instant.parse("2026-11-17T18:00:00Z")
    private val today = LocalDate.of(2026, 11, 17)

    private class Fixture(
        val model: SharedStrengthProgrammeCoordinator,
        val repository: SqliteStrengthProgramming,
        val db: SqlDatabase,
        val retained: AtomicReference<String?>,
    )

    private fun fixture(block: (Fixture) -> Unit) {
        val raw = SQLiteDatabase.create(null)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        try {
            createAndroidSchemaFixture(raw)
            val calls = AtomicInteger()
            val db = AndroidSqlConnection {
                check(Looper.myLooper() != Looper.getMainLooper()) {
                    "Authoring SQL must stay off Main."
                }
                calls.incrementAndGet()
                raw
            }
            val ids = AtomicInteger()
            val next = { "author-event-${ids.incrementAndGet()}" }
            val repository = SqliteStrengthProgramming(db, { now }, { today })
            val coaching =
                SqliteCoachingRepository(db, { now }, next, PortableLegacyStrengthFingerprint)
            val executions = SqliteGymExecutions(db, { now }, PortableLegacyStrengthFingerprint)
            val retained = AtomicReference<String?>(null)
            val model = AtomicReference<SharedStrengthProgrammeCoordinator>()
            instrumentation.runOnMainSync {
                model.set(
                    SharedStrengthProgrammeCoordinator(
                        db,
                        repository,
                        coaching,
                        executions::recentActual,
                        scope,
                        retained::get,
                        {
                            check(Looper.myLooper() == Looper.getMainLooper()) {
                                "Retained IDs must publish on Main."
                            }
                            retained.set(it)
                        },
                        { now },
                        { today },
                        next,
                    )
                )
            }
            assertEquals("Construction must not query the borrowed database", 0, calls.get())
            block(Fixture(model.get(), repository, db, retained))
        } finally {
            instrumentation.runOnMainSync { scope.cancel() }
            raw.close()
        }
    }

    private fun command(f: Fixture, action: () -> Unit): ProgrammeAuthoringState {
        instrumentation.runOnMainSync { action() }
        runBlocking { withTimeout(20000) { f.model.state.first { !it.loading && !it.busy } } }
        instrumentation.waitForIdleSync()
        return f.model.state.value
    }

    private fun complete(f: Fixture): StrengthProgrammeDraft {
        val created = command(f) { f.model.newDraft() }.draft!!
        val block = created.blocks().single()
        val day = block.days().first()
        command(f) { f.model.deleteDay(block.id(), block.days().last().id()) }
        val settings =
            StrengthProgramme.Settings(
                GymProgramme.Goal.GENERAL,
                GymProgramme.Priority.STRENGTH_FIRST,
                GymProgramme.Experience.BEGINNER,
                setOf(day.weekday()),
                1,
                45,
                emptySet(),
            )
        command(f) { f.model.header("Original custom programme", today, settings) }
        val value =
            command(f) {
                f.model.putSlot(
                    block.id(),
                    day.id(),
                    null,
                    requireNotNull(ExerciseCatalog.definition("push-up")).exercise(),
                    GymSlotInput(
                        sets = "2",
                        minimumReps = "6",
                        maximumReps = "10",
                        minimumEffort = "2.5",
                        maximumEffort = "3.5",
                        rir = true,
                        restSeconds = "90",
                        referenceKilograms = "82",
                    ),
                    {},
                )
            }
        assertNull(value.error)
        return value.draft!!
    }

    @Test
    fun draftPersistenceAndRetainedIdPublishBeforeSuccessWithoutActivatingAnything() =
        fixture { f ->
            val empty = command(f) { f.model.reload() }
            assertNull(empty.draft)
            assertNull(empty.current)
            assertTrue(empty.recent.isEmpty())
            val callback = AtomicReference<String>()
            val state =
                command(f) {
                    f.model.newDraft {
                        assertFalse(f.model.state.value.busy)
                        assertEquals(f.model.state.value.draft!!.id(), f.retained.get())
                        callback.set(f.retained.get())
                    }
                }
            assertNull(state.error)
            val draft = state.draft!!
            assertEquals(draft.id(), callback.get())
            assertEquals(today, draft.start())
            assertEquals(
                setOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY),
                draft.blocks().single().days().map { it.weekday() }.toSet(),
            )
            assertEquals(draft, f.repository.draft(draft.id()))
            assertNull(f.repository.currentReference())
            assertTrue(f.db.query("SELECT * FROM strength_sessions").isEmpty())
            val failed = command(f) { f.model.preview() }
            assertNotNull(failed.error)
            assertEquals(draft, failed.draft)
            assertNull(failed.preview)
            assertEquals(draft.id(), f.retained.get())
            command(f) { f.model.closeEditor() }
            assertNull(f.retained.get())
            assertEquals(draft, f.repository.draft(draft.id()))
            val reopened = command(f) { f.model.openDraft(draft.id()) }
            assertEquals(draft, reopened.draft)
            assertNull(reopened.error)
        }

    @Test
    fun staleDraftAndFreshExclusionCannotActivateReviewedProgrammingOrCreateActuals() =
        fixture { f ->
            val draft = complete(f)
            val first = command(f) { f.model.preview() }
            assertNull(first.error)
            val review = first.preview!!
            assertEquals(4, review.programme.workouts().size)
            review.programme.workouts().forEach {
                val target = it.definition().slots().single().target()!!
                assertEquals(2, target.sets())
                assertEquals(6, target.minimumReps())
                assertEquals(10, target.maximumReps())
                assertEquals(GymWorkoutDefinition.EffortScale.RIR, target.effort()!!.scale())
                assertEquals(2.5, target.effort()!!.minimum(), 0.0)
                assertEquals(90, target.restSeconds())
                assertNull(target.referenceLoadKg())
            }
            assertNull(f.repository.currentReference())
            val changed =
                StrengthProgrammeDraft(
                    draft.id(),
                    draft.revision() + 1,
                    draft.programmeId(),
                    draft.baseRevision(),
                    "Changed elsewhere",
                    draft.settings(),
                    draft.start(),
                    draft.blocks(),
                    draft.origin(),
                    draft.templateId(),
                )
            f.repository.saveDraft(changed, draft.revision())
            val stale =
                command(f) {
                    f.model.activate(review) { fail("Stale review must not call success") }
                }
            assertNotNull(stale.error)
            assertEquals(review, stale.preview)
            assertNull(f.repository.currentReference())
            command(f) { f.model.reload() }
            val fresh = command(f) { f.model.preview() }.preview!!
            val exclusions = SqliteExerciseExclusions(f.db)
            exclusions.exclude(
                ExerciseExclusionRepository.Exclusion(
                    "push-up",
                    ExerciseExclusionRepository.Reason.PREFERENCE,
                    now,
                )
            )
            val rejected =
                command(f) {
                    f.model.activate(fresh) { fail("Excluded preview must not activate") }
                }
            assertNotNull(rejected.error)
            assertNull(f.repository.currentReference())
            assertTrue(f.db.query("SELECT * FROM strength_sessions").isEmpty())
            exclusions.restoreExercise("push-up")
            val acceptedReview = command(f) { f.model.preview() }.preview!!
            val accepted = command(f) { f.model.activate(acceptedReview) }
            assertNull(accepted.error)
            assertNull(accepted.draft)
            assertNull(accepted.preview)
            assertNull(f.retained.get())
            assertEquals(
                acceptedReview.programme,
                f.repository.programme(acceptedReview.programme.id(), 1),
            )
            assertEquals(acceptedReview.programme.id(), f.repository.currentReference()!!.id())
            assertEquals(changed, f.repository.draft(changed.id()))
            assertTrue(f.db.query("SELECT * FROM strength_sessions").isEmpty())
        }

    @Test
    fun sharedSlotParserKeepsModalitiesEffortAndOptionalReferenceSeparateFromActualSets() {
        val push = requireNotNull(ExerciseCatalog.definition("push-up")).exercise()
        val existing =
            gymSlotFromInput(
                "preserved",
                push,
                GymSlotInput(
                    sets = "2",
                    minimumReps = "6",
                    maximumReps = "10",
                    minimumEffort = "2.5",
                    maximumEffort = "3.5",
                    rir = true,
                    referenceKilograms = "82",
                ),
            ) {
                error("Existing slot ID must remain")
            }
        assertEquals("preserved", existing.id())
        assertNull(existing.target()!!.referenceLoadKg())
        assertEquals(GymWorkoutDefinition.EffortScale.RIR, existing.target()!!.effort()!!.scale())
        val timed =
            gymSlotFromInput(
                null,
                requireNotNull(ExerciseCatalog.definition("plank")).exercise(),
                GymSlotInput(seconds = "30", minimumEffort = "5", maximumEffort = "7"),
            ) {
                "timed"
            }
        assertEquals(30L, timed.target()!!.seconds())
        assertNull(timed.target()!!.minimumReps())
        assertEquals(GymWorkoutDefinition.EffortScale.RPE, timed.target()!!.effort()!!.scale())
        val open = gymSlotFromInput(null, push, GymSlotInput(open = true)) { "open" }
        assertNull(open.target())
        val error =
            runCatching {
                    gymSlotFromInput(null, push, GymSlotInput(minimumEffort = "NaN")) { "bad" }
                }
                .exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertEquals("Enter a valid minimum effort.", error!!.message)
    }
}
