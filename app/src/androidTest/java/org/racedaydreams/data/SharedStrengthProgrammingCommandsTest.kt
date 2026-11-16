package org.racedaydreams.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.StrengthProgrammingRepository.*
import org.racedaydreams.storage.*

@RunWith(AndroidJUnit4::class)
class SharedStrengthProgrammingCommandsTest {
    private val at = Instant.parse("2026-11-16T09:00:00.123456789Z")
    private val today = LocalDate.of(2026, 11, 16)
    private val start = today.plusDays(1)
    private val bench = ExerciseCatalog.definition("bench-press")!!.exercise()
    private val replacement = ExerciseCatalog.definition("dumbbell-bench-press")!!.exercise()

    private fun target() =
        GymWorkoutDefinition.Target(
            2,
            4,
            6,
            null,
            null,
            GymWorkoutDefinition.EffortRange(GymWorkoutDefinition.EffortScale.RIR, 2.5, 4.5),
            180,
            82.25,
        )

    private fun settings() =
        StrengthProgramme.Settings(
            GymProgramme.Goal.STRENGTH,
            GymProgramme.Priority.BALANCED,
            GymProgramme.Experience.INTERMEDIATE,
            setOf(DayOfWeek.TUESDAY),
            1,
            60,
            ExerciseMetadata.Equipment.entries.toSet(),
        )

    private fun programme(
        id: String = "programme",
        exercise: StrengthExercise = bench,
    ): StrengthProgramme {
        val slots = listOf(GymWorkoutDefinition.Slot("slot", exercise, target(), null))
        val workouts =
            List(2) { i ->
                val wid = "workout-$i"
                StrengthProgramme.Workout(
                    wid,
                    start.plusWeeks(i.toLong()),
                    StrengthWorkoutBudget.workoutMinutes(slots),
                    GymWorkoutDefinition(wid, 1, at, "Upper", slots),
                    null,
                )
            }
        return StrengthProgramme(
            id,
            1,
            0,
            at,
            "Original",
            StrengthProgramme.Origin.CUSTOM,
            null,
            settings(),
            listOf(
                StrengthProgramme.Block(
                    "block",
                    StrengthProgramme.Phase.DEVELOPMENT,
                    "Development",
                    start,
                    2,
                    workouts,
                    "Original explanation",
                )
            ),
            start,
            "Athlete authored",
            emptyList(),
        )
    }

    private fun revised(p: StrengthProgramme = programme(), index: Int = 1): StrengthProgramme {
        val old = p.workouts()[index]
        val slots =
            old.definition().slots().map {
                it.copy(target = it.target()!!.copy(referenceLoadKg = 85.0))
            }
        val changed =
            old.copy(
                definition = old.definition().copy(revision = 2, slots = slots),
                expectedMinutes = StrengthWorkoutBudget.workoutMinutes(slots),
            )
        return p.copy(
            revision = 2,
            parentRevision = 1,
            effectiveFrom = old.date(),
            blocks =
                listOf(
                    p.blocks()
                        .single()
                        .copy(
                            workouts = p.workouts().map { if (it.id() == old.id()) changed else it }
                        )
                ),
            reason = "Athlete accepted future change",
        )
    }

    private fun draft() =
        StrengthProgrammeDraft(
            "draft",
            1,
            "programme",
            0,
            "Manual",
            settings(),
            start,
            listOf(
                StrengthProgrammeDraft.WeekBlock(
                    "block",
                    StrengthProgramme.Phase.DEVELOPMENT,
                    "Development",
                    2,
                    listOf(
                        StrengthProgrammeDraft.Day(
                            "day",
                            DayOfWeek.TUESDAY,
                            "Upper",
                            listOf(GymWorkoutDefinition.Slot("slot", bench, target(), null)),
                            null,
                        )
                    ),
                )
            ),
            StrengthProgramme.Origin.CUSTOM,
            null,
        )

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var original: SqliteStrengthProgrammingRepository
    private lateinit var shared: SqliteStrengthProgramming
    private lateinit var concrete: SqliteConcreteStrengthProgrammes
    private lateinit var db: android.database.sqlite.SQLiteDatabase

    @Before
    fun prepare() {
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
        db = ProfileDatabase.get(context).writableDatabase
        val clock = Clock.fixed(at, ZoneOffset.UTC)
        original = SqliteStrengthProgrammingRepository(context, clock)
        val bridge = AndroidSqlTestConnection(db)
        shared = SqliteStrengthProgramming(bridge, { at }, { today })
        concrete = SqliteConcreteStrengthProgrammes(bridge, { at }, { today })
    }

    @After
    fun cleanup() {
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
    }

    @Test
    fun bothWritersShareInitialAcceptanceRetryStopAndRetainedVersionHistory() {
        val p = programme()
        val accepted = shared.activate(p, null, "activate", emptyList())
        assertEquals(p, original.programme(p.id(), 1))
        assertEquals(accepted, original.activate(p, null, "activate", emptyList()))
        assertEquals(original.decisions(), shared.decisions())
        val stop = original.stop(accepted.after()!!, "stop")
        assertEquals(stop, shared.stop(accepted.after()!!, "stop"))
        assertNull(shared.currentReference())
        val second = programme("other")
        original.activate(second, null, "other", emptyList())
        assertEquals(original.versions(100, 0), shared.versions(100, 0))
        assertEquals(second, shared.programme("other", 1))
        assertTrue(CoachingStore(context).strength().isEmpty())
    }

    @Test
    fun reviewedWeeklyAndConcreteDraftsRequireTheSameFreshSavedPreview() {
        val d = draft()
        original.saveDraft(d, 0)
        val preview = d.preview(at, emptySet(), emptyMap())
        shared.saveDraft(d.copy(revision = 2, name = "Changed"), 1)
        assertThrows(IllegalArgumentException::class.java) {
            shared.activateDraft(preview, null, "weekly", d)
        }
        assertThrows(IllegalArgumentException::class.java) {
            original.activateDraft(preview, null, "weekly", d)
        }
        val reviewed = shared.draft(d.id())!!
        val proposed = reviewed.preview(at, emptySet(), emptyMap())
        val accepted = shared.activateDraft(proposed, null, "weekly", reviewed)
        assertEquals(accepted, original.activateDraft(proposed, null, "weekly", reviewed))
        original.stop(accepted.after()!!, "stop")
        val cp =
            ConcreteStrengthProgrammeDraft(
                "concrete",
                1,
                null,
                programme("concrete-programme"),
                emptyList(),
            )
        concrete.save(cp, 0)
        val result = concrete.activate(cp, "concrete")
        val originalConcrete =
            SqliteConcreteStrengthProgrammeDraftRepository(context, Clock.fixed(at, ZoneOffset.UTC))
        assertEquals(result, originalConcrete.activate(cp, "concrete"))
        assertEquals(original.currentReference(), shared.currentReference())
        assertEquals(original.decisions(), shared.decisions())
    }

    @Test
    fun newlyCompletedActualAndLiveProgrammeExecutionBlockUnsafeFutureRevisions() {
        val p = programme()
        val ref = original.activate(p, null, "activate", emptyList()).after()!!
        val review = shared.reviewRevision(ref)
        val actual =
            StrengthSession(
                "actual",
                at.minusSeconds(86400),
                ZoneOffset.UTC,
                Duration.ofMinutes(30),
                null,
                "Recorded actual",
                listOf(
                    StrengthSession.ExerciseSets(
                        bench,
                        listOf(StrengthSession.SetEntry("set", 6, 80.0, 7.0, false, null, null)),
                    )
                ),
            )
        CoachingStore(context).saveStrength(actual)
        db.execSQL(
            "INSERT INTO strength_association_targets(actual_id,programme_key,session_id,planned_date) VALUES('actual','programme:v1','workout-0','2026-11-17')"
        )
        assertTrue(review.completedWorkoutIds().isEmpty())
        assertThrows(IllegalArgumentException::class.java) {
            shared.activate(revised(index = 0), ref, "bad", emptyList())
        }
        assertThrows(IllegalArgumentException::class.java) {
            original.activate(revised(index = 0), ref, "bad", emptyList())
        }
        val prepared = GymWorkoutAdapter.fromAuthoredProgramme(p, "workout-1")
        val engine =
            GymExecutionEngine.start(
                "live",
                GymExecution.Origin.PLANNED,
                prepared.definition(),
                prepared.source(),
                ZoneOffset.UTC,
                GymExecution.Tick("boot", 1000, at),
            )
        val gym = SqliteGymExecutionRepository(context)
        val staged = gym.start(engine.snapshot())!!
        assertThrows(IllegalArgumentException::class.java) {
            shared.activate(revised(), ref, "future", emptyList())
        }
        assertThrows(IllegalArgumentException::class.java) {
            original.activate(revised(), ref, "future", emptyList())
        }
        gym.save(
            engine.discard(GymExecution.Tick("boot", 2000, at.plusSeconds(1))),
            staged.revision(),
        )
        shared.activate(revised(), ref, "future", emptyList())
        assertEquals(p, original.programme("programme", 1))
        assertEquals(revised(), original.programme("programme", 2))
        assertEquals(actual, CoachingStore(context).strength().single())
    }

    @Test
    fun substitutionRechecksExclusionsAndPreservesOptionalReferenceAndDecisionMeaning() {
        val p = programme()
        val ref = shared.activate(p, null, "activate", emptyList()).after()!!
        val context =
            ExerciseSubstitutionEngine.Context(
                bench,
                p.settings().goal(),
                p.settings().experience(),
                p.settings().equipment(),
                emptySet(),
                emptySet(),
                emptySet(),
                ExerciseSubstitutionEngine.Reason.EQUIPMENT_BUSY,
            )
        val proposal =
            StrengthProgrammeEditor.substitute(
                p,
                "workout-1",
                "slot",
                context,
                replacement.id(),
                null,
                p.workouts()[1].date(),
                at,
                emptySet(),
            )
        val exclusions = SqliteExerciseExclusionRepository(this.context)
        exclusions.exclude(
            ExerciseExclusionRepository.Exclusion(
                replacement.id(),
                ExerciseExclusionRepository.Reason.EQUIPMENT_UNAVAILABLE,
                at,
            )
        )
        assertThrows(IllegalArgumentException::class.java) {
            shared.activate(proposal.proposed(), ref, "swap", proposal.substitutions())
        }
        assertThrows(IllegalArgumentException::class.java) {
            original.activate(proposal.proposed(), ref, "swap", proposal.substitutions())
        }
        exclusions.restoreExercise(replacement.id())
        val changed = shared.activate(proposal.proposed(), ref, "swap", proposal.substitutions())
        assertEquals(changed, original.decisions().last())
        assertEquals(proposal.proposed(), original.programme("programme", 2))
        assertNull(
            original
                .programme("programme", 2)!!
                .workouts()[1]
                .definition()
                .slots()
                .single()
                .target()!!
                .referenceLoadKg()
        )
    }

    @Test
    fun injectedDecisionFailureRollsBackNativeWriterOnOriginalDatabaseAndOuterTransaction() {
        db.execSQL(
            "CREATE TRIGGER fail_shared_accept BEFORE INSERT ON strength_programme_decisions BEGIN SELECT RAISE(ABORT,'test failure'); END"
        )
        assertThrows(android.database.sqlite.SQLiteException::class.java) {
            shared.activate(programme(), null, "fail", emptyList())
        }
        assertTrue(original.versions(100, 0).isEmpty())
        assertNull(original.currentReference())
        db.execSQL("DROP TRIGGER fail_shared_accept")
        db.beginTransaction()
        try {
            shared.activate(programme(), null, "outer", emptyList())
        } finally {
            db.endTransaction()
        }
        assertNull(original.currentReference())
        assertTrue(original.decisions().isEmpty())
        assertTrue(original.versions(100, 0).isEmpty())
    }
}
