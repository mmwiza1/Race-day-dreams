package org.racedaydreams.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.*
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.StrengthProgrammingRepository.*
import org.racedaydreams.storage.*

@RunWith(AndroidJUnit4::class)
class SharedStrengthDraftCommandsTest {
    private val at = Instant.parse("2026-11-16T09:00:00.123456789Z")
    private val start = LocalDate.of(2026, 11, 17)
    private val ref = Reference(Owner.AUTHORED, "programme", 1)

    private fun programme(): StrengthProgramme {
        val settings =
            StrengthProgramme.Settings(
                GymProgramme.Goal.STRENGTH,
                GymProgramme.Priority.BALANCED,
                GymProgramme.Experience.INTERMEDIATE,
                setOf(DayOfWeek.TUESDAY),
                1,
                60,
                ExerciseMetadata.Equipment.entries.toSet(),
            )
        val target =
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
        val workouts =
            List(2) { i ->
                val id = "workout-$i"
                StrengthProgramme.Workout(
                    id,
                    start.plusWeeks(i.toLong()),
                    30,
                    GymWorkoutDefinition(
                        id,
                        1,
                        at,
                        "Upper",
                        listOf(
                            GymWorkoutDefinition.Slot(
                                "slot",
                                ExerciseCatalog.definition("bench-press")!!.exercise(),
                                target,
                                null,
                            )
                        ),
                    ),
                    null,
                )
            }
        return StrengthProgramme(
            "programme",
            1,
            0,
            at,
            "Original",
            StrengthProgramme.Origin.CUSTOM,
            null,
            settings,
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

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var shared: SqliteStrengthDrafts
    private lateinit var records: SqliteStrengthProgrammingRecords
    private lateinit var original: SqliteStrengthProgrammingRepository
    private lateinit var concrete: SqliteConcreteStrengthProgrammeDraftRepository
    private lateinit var db: android.database.sqlite.SQLiteDatabase

    @Before
    fun prepare() {
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
        db = ProfileDatabase.get(context).writableDatabase
        val bridge = AndroidSqlTestConnection(db)
        shared = SqliteStrengthDrafts(bridge)
        records = SqliteStrengthProgrammingRecords(bridge)
        original = SqliteStrengthProgrammingRepository(context)
        concrete = SqliteConcreteStrengthProgrammeDraftRepository(context)
    }

    @After
    fun cleanup() {
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
    }

    private fun draft(id: String = "weekly", revision: Int = 1) =
        StrengthProgrammeDraft(
            id,
            revision,
            "programme",
            0,
            "",
            programme().settings(),
            start,
            emptyList(),
            StrengthProgramme.Origin.CUSTOM,
            null,
        )

    private fun preview(revision: Int = 1) =
        ConcreteStrengthProgrammeDraft("preview", revision, null, programme(), emptyList())

    private fun custom() =
        CustomStrengthExercise(
            StrengthExercise(
                "custom-press",
                "My press",
                StrengthExercise.Region.UPPER,
                StrengthExercise.Mode.WEIGHTED,
                false,
                StrengthExercise.MeasurementProfile.LOAD_REPS,
            ),
            ExerciseMetadata.BodyArea.CHEST,
            ExerciseMetadata.Role.HORIZONTAL_PUSH,
            ExerciseMetadata.EquipmentRequirement(
                listOf(setOf(ExerciseMetadata.Equipment.DUMBBELLS))
            ),
            "Athlete notes",
        )

    private fun mirror(id: String): StrengthExercise =
        db.rawQuery("SELECT payload FROM strength_exercises WHERE id=?", arrayOf(id)).use {
            assertTrue(it.moveToFirst())
            CoachingJson.exercise(JSONObject(it.getString(0)))
        }

    @Test
    fun weeklyDraftWritesRetriesForwardJumpsAndDeletesAreCompatibleInBothDirections() {
        original.saveDraft(draft(), 0)
        shared.saveDraft(draft(revision = 3).copy(name = "Edited through shared"), 1)
        assertEquals(original.draft("weekly"), records.draft("weekly"))
        val edited = original.draft("weekly")!!
        shared.saveDraft(edited, -7)
        assertThrows(IllegalArgumentException::class.java) {
            original.saveDraft(draft(revision = 4), 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            shared.saveDraft(draft(revision = 4), 1)
        }
        shared.deleteDraft("weekly", 3)
        original.deleteDraft("weekly", 3)
        shared.saveDraft(draft(id = "kept\u0000🏋️"), 0)
        original.saveDraft(draft(id = "kept\u0000🏋️", revision = 2), 1)
        assertEquals(original.draft("kept\u0000🏋️"), records.draft("kept\u0000🏋️"))
        assertNull(original.currentReference())
    }

    @Test
    fun customMetadataAndLegacyLoggingMirrorsStayCompatibleAcrossBothWriters() {
        val c = custom()
        original.saveCustom(c, null)
        val changed = c.copy(exercise = c.exercise().copy(name = "Shared rename"))
        shared.saveCustom(changed, c)
        assertEquals(changed, original.customExercise(c.exercise().id()))
        assertEquals(changed.exercise(), mirror(c.exercise().id()))
        val fromLogging = changed.exercise().copy(name = "Logging rename")
        assertTrue(shared.updateLoggingDefinition(fromLogging))
        assertEquals(
            changed.copy(exercise = fromLogging),
            original.customExercise(c.exercise().id()),
        )
        val originalLogging = fromLogging.copy(name = "Original logging edit")
        assertTrue(SqliteStrengthProgrammingRepository.updateLoggingDefinition(db, originalLogging))
        assertEquals(
            changed.copy(exercise = originalLogging),
            records.customExercise(c.exercise().id()),
        )
        assertEquals(originalLogging, mirror(c.exercise().id()))
        assertThrows(IllegalArgumentException::class.java) { shared.saveCustom(changed, c) }
        assertThrows(IllegalArgumentException::class.java) { original.saveCustom(changed, c) }
    }

    @Test
    fun concreteDraftWritesAndIndependentRevisionsAreCompatibleWithoutActivation() {
        concrete.save(preview(), 0)
        val changed = preview(2).copy(programme = programme().copy(name = "Shared preview"))
        shared.saveConcrete(changed, 1)
        assertEquals(changed, concrete.get("preview"))
        val third = changed.copy(revision = 3)
        concrete.save(third, 2)
        assertEquals(third, records.concreteDraft("preview"))
        assertThrows(IllegalArgumentException::class.java) { shared.saveConcrete(preview(5), 3) }
        assertThrows(IllegalArgumentException::class.java) { concrete.save(preview(5), 3) }
        assertThrows(IllegalArgumentException::class.java) { shared.deleteConcrete("preview", 2) }
        shared.deleteConcrete("preview", 3)
        assertNull(concrete.get("preview"))
        assertNull(original.currentReference())
        assertTrue(original.versions(100, 0).isEmpty())
    }

    @Test
    fun reviewedBaseAndHistoricalPrescriptionChecksMatchOriginalDraftStorage() {
        val base = programme()
        val ref = Reference(Owner.AUTHORED, base.id(), 1)
        val graph =
            StrengthProgrammingArchiveJson.Snapshot(
                mapOf(ref.key() to base),
                emptyList(),
                emptyList(),
                emptyList(),
                listOf(Decision("activate", null, ref, at, "Accepted", emptyList(), emptyList())),
                ref,
            )
        val validated =
            StrengthProgrammingArchive.validate(
                JSONObject(StrengthProgrammingArchiveJson.encode(graph)),
                null,
            )
        db.beginTransaction()
        try {
            SqliteStrengthProgrammingRepository.restore(db, validated)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        val forward =
            ConcreteStrengthProgrammeDraft(
                "forward",
                1,
                ref,
                base.copy(
                    revision = 2,
                    parentRevision = 1,
                    effectiveFrom = base.workouts()[1].date(),
                ),
                emptyList(),
            )
        shared.saveConcrete(forward, 0)
        assertEquals(forward, concrete.get("forward"))
        val past =
            base
                .workouts()[0]
                .copy(
                    definition = base.workouts()[0].definition().copy(title = "Retroactive change")
                )
        val invalid =
            forward.copy(
                revision = 2,
                programme =
                    forward
                        .programme()
                        .copy(
                            blocks =
                                listOf(
                                    base
                                        .blocks()
                                        .single()
                                        .copy(workouts = listOf(past, base.workouts()[1]))
                                )
                        ),
            )
        assertThrows(IllegalArgumentException::class.java) { shared.saveConcrete(invalid, 1) }
        assertThrows(IllegalArgumentException::class.java) { concrete.save(invalid, 1) }
        assertEquals(base, records.programme(base.id(), 1))
        assertEquals(ref, original.currentReference())
        assertEquals(1, original.decisions().size)
    }

    @Test
    fun sharedCommandsUseTheExistingOuterTransactionAndRollbackChildFailures() {
        shared.saveDraft(draft(), 0)
        shared.saveConcrete(preview(), 0)
        db.beginTransaction()
        try {
            original.saveDraft(draft(revision = 2), 1)
            shared.saveCustom(custom(), null)
            shared.deleteConcrete("preview", 1)
        } finally {
            db.endTransaction()
        }
        assertEquals(draft(), original.draft("weekly"))
        assertEquals(preview(), concrete.get("preview"))
        assertNull(original.customExercise(custom().exercise().id()))
        db.execSQL(
            "CREATE TRIGGER fail_shared_preview BEFORE INSERT ON strength_concrete_draft_workouts BEGIN SELECT RAISE(ABORT,'test failure'); END"
        )
        val changed = preview(2).copy(programme = programme().copy(name = "Failed edit"))
        assertThrows(android.database.sqlite.SQLiteException::class.java) {
            shared.saveConcrete(changed, 1)
        }
        assertEquals(preview(), concrete.get("preview"))
        assertEquals(preview(), records.concreteDraft("preview"))
    }
}
