package org.racedaydreams.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.MessageDigest
import java.time.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.strength.*
import org.racedaydreams.storage.*

@RunWith(AndroidJUnit4::class)
class SharedGymExecutionCommandsTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val at = Instant.parse("2026-11-15T09:00:00.123456789Z")
    private val day = LocalDate.of(2026, 11, 15)
    private val bench = ExerciseCatalog.definition("bench-press")!!.exercise()
    private val row = ExerciseCatalog.definition("dumbbell-row")!!.exercise()
    private lateinit var db: android.database.sqlite.SQLiteDatabase
    private lateinit var original: SqliteGymExecutionRepository
    private lateinit var shared: SqliteGymExecutions
    private lateinit var bridge: AndroidSqlTestConnection

    // Real original Android hash-input contract; portable fixture hashes are NOT used here.
    private fun fingerprint(s: StrengthSession): String =
        MessageDigest.getInstance("SHA-256")
            .digest(CoachingJson.strength(s).toString().toByteArray(Charsets.UTF_8))
            .joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }

    @Before
    fun prepare() {
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
        db = ProfileDatabase.get(context).writableDatabase
        bridge = AndroidSqlTestConnection(db)
        original = SqliteGymExecutionRepository(context)
        shared =
            SqliteGymExecutions(bridge, { Instant.now() }, LegacyStrengthFingerprint(::fingerprint))
    }

    @After
    fun cleanup() {
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
    }

    private fun tick(ms: Long, epoch: String = "boot") =
        GymExecution.Tick(epoch, 9007199254741017L + ms, at.plusMillis(ms))

    private fun set(id: String, warm: Boolean = false) =
        StrengthSession.SetEntry(id, 6, 80.0, null, warm, null, null, 2.5)

    private fun free(id: String = "execution") =
        GymExecutionEngine.start(
            id,
            GymExecution.Origin.FREE,
            null,
            null,
            ZoneOffset.ofTotalSeconds(19815),
            tick(0),
        )

    private fun add(e: GymExecutionEngine) =
        e.addExercises(
            listOf(
                GymWorkoutDefinition.Slot("a", bench, null, "pair"),
                GymWorkoutDefinition.Slot("b", row, null, "pair"),
            ),
            tick(10),
        )

    private fun finish(id: String = "execution"): GymExecution {
        val e = free(id)
        val initial = shared.start(e.snapshot())
        add(e)
        e.saveSet("a", set("working"), tick(1000))
        val ended = e.finish(tick(2000), null, "Recorded / \\ \" 🏋️\u0000", true)
        shared.finish(ended, initial.revision())
        return ended
    }

    private fun legacy() =
        GymProgramme(
            "legacy",
            1,
            at,
            "Athlete chose",
            GymProgramme.Settings(
                GymProgramme.Goal.STRENGTH,
                GymProgramme.Priority.BALANCED,
                GymProgramme.Experience.INTERMEDIATE,
                setOf(DayOfWeek.SUNDAY),
                1,
                setOf("full gym"),
                45,
                GymProgramme.Effort.RPE,
            ),
            day,
            day.plusDays(41),
            listOf(
                GymProgramme.Session(
                    "session",
                    day,
                    "Upper",
                    listOf(
                        GymProgramme.Prescription(
                            "bench-press",
                            2,
                            4,
                            6,
                            null,
                            null,
                            6,
                            8,
                            180,
                            "Practice",
                            82.25,
                        )
                    ),
                    "Retained",
                )
            ),
            emptyList(),
        )

    private fun authored(): StrengthProgramme {
        val slots =
            listOf(
                GymWorkoutDefinition.Slot(
                    "a",
                    bench,
                    GymWorkoutDefinition.Target(
                        2,
                        4,
                        6,
                        null,
                        null,
                        GymWorkoutDefinition.EffortRange(
                            GymWorkoutDefinition.EffortScale.RIR,
                            2.5,
                            4.5,
                        ),
                        180,
                        82.25,
                    ),
                    "pair",
                ),
                GymWorkoutDefinition.Slot("b", row, null, "pair"),
            )
        // Both grouped exercises need explicit targets in an authored programme.
        val targeted =
            slots.map { if (it.target() == null) it.copy(target = slots.first().target()) else it }
        return StrengthProgramme(
            "authored",
            1,
            0,
            at,
            "Upper",
            StrengthProgramme.Origin.CUSTOM,
            null,
            StrengthProgramme.Settings(
                GymProgramme.Goal.STRENGTH,
                GymProgramme.Priority.BALANCED,
                GymProgramme.Experience.INTERMEDIATE,
                setOf(DayOfWeek.SUNDAY),
                1,
                60,
                ExerciseMetadata.Equipment.entries.toSet(),
            ),
            listOf(
                StrengthProgramme.Block(
                    "block",
                    StrengthProgramme.Phase.DEVELOPMENT,
                    "Development",
                    day,
                    1,
                    listOf(
                        StrengthProgramme.Workout(
                            "session",
                            day,
                            StrengthWorkoutBudget.workoutMinutes(targeted),
                            GymWorkoutDefinition("workout", 1, at, "Upper", targeted),
                            null,
                        )
                    ),
                    "Practice",
                )
            ),
            day,
            "Athlete chose",
            emptyList(),
        )
    }

    private fun plan(authored: Boolean): GymWorkoutAdapter.PreparedWorkout =
        if (authored) {
            val p = authored()
            SqliteStrengthProgrammingRepository(context, Clock.fixed(at, ZoneOffset.UTC))
                .activate(p, null, "activate", emptyList())
            GymWorkoutAdapter.fromAuthoredProgramme(p, "session")
        } else {
            val p = legacy()
            GymProgrammeStore(context).save(p)
            GymWorkoutAdapter.fromProgramme(p, "session")
        }

    @Test
    fun originalAndSharedWritersInterleaveSetStagingRecoveryAndAtomicCompletion() {
        val e = free()
        val initial = original.start(e.snapshot())!!
        assertEquals(initial, shared.start(e.snapshot()))
        add(e)
        e.saveSet("a", set("warm", true), tick(100))
        val staged = shared.save(e.snapshot(), initial.revision())
        assertEquals(staged, original.load("execution"))
        e.saveSet("a", set("working"), tick(1000))
        e.replaceExercise("a", row, tick(1100))
        e.saveSet("a", set("replacement"), tick(1200))
        e.breakSuperset("b", tick(1300))
        val second = original.save(e.snapshot(), staged.revision())!!
        val recovered = GymExecutionEngine.recover(second.execution(), tick(100000, "new"))
        val recovery = shared.save(recovered.snapshot(), second.revision())
        assertEquals(recovery, original.load("execution"))
        assertTrue(CoachingStore(context).strength().isEmpty())
        recovered.resume(tick(100100, "new"))
        val ended = recovered.finish(tick(100200, "new"), null, "Actual", true)
        val completed = original.finish(ended, recovery.revision())!!
        assertEquals(completed, shared.finish(ended, 1))
        assertEquals(completed, shared.load("execution"))
        assertEquals(listOf(ended.completedSession()), CoachingStore(context).strength())
        assertEquals(
            listOf(bench, row),
            ended.completedSession()!!.exercises().map { it.exercise() },
        )
    }

    @Test
    fun sharedFinalizationProjectsBothOriginalProgrammeFamiliesAndPreservesSupersets() {
        for (authored in listOf(false, true)) {
            if (authored) {
                cleanup()
                prepare()
            }
            val p = plan(authored)
            val e =
                GymExecutionEngine.start(
                    "execution",
                    GymExecution.Origin.PLANNED,
                    p.definition(),
                    p.source(),
                    ZoneOffset.UTC,
                    tick(0),
                )
            original.start(e.snapshot())
            e.saveSet(p.definition().slots().first().id(), set("working"), tick(1000))
            val ended = e.finish(tick(2000), null, "Actual", true)
            val stored = shared.finish(ended, 1)
            assertEquals(stored, original.finish(ended, 1))
            assertEquals(
                if (authored) "authored:v1" else "legacy:v1",
                DevelopmentStore(context).gymLinks().getValue("execution").versionKey(),
            )
            assertNull(SqliteGymAssociationRepository(context).current("execution"))
            assertEquals(ended, original.load("execution")!!.execution())
            if (authored) assertEquals("pair", ended.slots().first().supersetGroup())
        }
    }

    @Test
    fun realOriginalFingerprintsValidateSharedActualRepeatCreationEditAndDeletionReceipts() {
        val ended = finish()
        val actual = ended.completedSession()!!
        val repeat = GymWorkoutAdapter.repeatActual("repeat", 1, at, "Repeat", actual)
        shared.saveWorkout(repeat.definition(), repeat.source())
        original.saveWorkout(
            repeat.definition(),
            repeat.source(),
        ) // ORIGINAL validates the new proof.
        val records = SqliteGymWorkouts(bridge)
        val before = records.record("repeat", 1)!!
        assertEquals(fingerprint(actual), before.sourceSnapshot!!.originalSourceFingerprint)
        val edited = actual.copy(notes = "Explicit / edit 🏋️\u0000")
        shared.saveActual(edited)
        original.saveWorkout(repeat.definition(), repeat.source())
        assertEquals(edited, CoachingStore(context).strength().single())
        assertEquals(ended, original.load("execution")!!.execution())
        assertEquals(
            fingerprint(edited),
            records.record("repeat", 1)!!.sourceSnapshot!!.currentSourceFingerprint,
        )
        assertThrows(IllegalArgumentException::class.java) { original.finish(ended, 1) }
        assertThrows(IllegalArgumentException::class.java) { shared.finish(ended, 1) }
        val secondEdit = edited.copy(notes = "Original writer edit")
        CoachingStore(context).saveStrength(secondEdit)
        shared.saveWorkout(repeat.definition(), repeat.source())
        shared.deleteActual("execution")
        original.saveWorkout(repeat.definition(), repeat.source())
        assertTrue(CoachingStore(context).strength().isEmpty())
        assertNotNull(records.record("repeat", 1)!!.sourceSnapshot!!.sourceDeletedAt)
        assertEquals(
            fingerprint(actual),
            records.record("repeat", 1)!!.sourceSnapshot!!.originalSourceFingerprint,
        )
    }

    @Test
    fun originalCreatedExecutionRepeatCanBeEditedDeletedAndReadBySharedWriter() {
        val p = plan(true)
        val e =
            GymExecutionEngine.start(
                "execution",
                GymExecution.Origin.PLANNED,
                p.definition(),
                p.source(),
                ZoneOffset.UTC,
                tick(0),
            )
        val initial = original.start(e.snapshot())!!
        e.saveSet("a", set("working"), tick(1000))
        e.breakSuperset("a", tick(1100))
        val ended = e.finish(tick(2000), null, "Original", true)
        original.finish(ended, initial.revision())
        val repeat = GymWorkoutAdapter.repeatExecution("repeat", 1, at, "Repeat", ended)
        original.saveWorkout(repeat.definition(), repeat.source())
        shared.saveWorkout(repeat.definition(), repeat.source())
        val edited = ended.completedSession()!!.copy(notes = "Shared accepted edit")
        shared.saveActual(edited)
        original.saveWorkout(repeat.definition(), repeat.source())
        val receipt = SqliteGymWorkouts(bridge).record("repeat", 1)!!.sourceSnapshot!!
        assertEquals(FrozenGymWorkoutRecordJson.TemplateKind.EXECUTION, receipt.templateKind)
        assertEquals(ended.slots(), receipt.originalSourceSlots)
        assertNull(repeat.definition().slots().first().target()!!.referenceLoadKg())
        CoachingStore(context).deleteStrength("execution")
        shared.saveWorkout(repeat.definition(), repeat.source())
        assertNotNull(
            SqliteGymWorkouts(bridge).record("repeat", 1)!!.sourceSnapshot!!.sourceDeletedAt
        )
    }

    @Test
    fun failedFinalizationAndActualEditRollbackOriginalVisibleStateAndReceipts() {
        val e = free()
        val initial = shared.start(e.snapshot())
        add(e)
        e.saveSet("a", set("working"), tick(1000))
        val ended = e.finish(tick(2000), null, "Actual", true)
        db.execSQL(
            "CREATE TRIGGER fail_set BEFORE INSERT ON strength_execution_sets BEGIN SELECT RAISE(ABORT,'failed'); END"
        )
        assertThrows(android.database.sqlite.SQLiteException::class.java) {
            shared.finish(ended, initial.revision())
        }
        assertEquals(initial, original.load("execution"))
        assertTrue(CoachingStore(context).strength().isEmpty())
        db.execSQL("DROP TRIGGER fail_set")
        shared.finish(ended, 1)
        val repeat =
            GymWorkoutAdapter.repeatActual("repeat", 1, at, "Repeat", ended.completedSession())
        original.saveWorkout(repeat.definition(), repeat.source())
        val before = SqliteGymWorkouts(bridge).record("repeat", 1)
        db.execSQL(
            "CREATE TRIGGER fail_edit BEFORE UPDATE ON strength_sessions BEGIN SELECT RAISE(ABORT,'failed'); END"
        )
        assertThrows(android.database.sqlite.SQLiteException::class.java) {
            shared.saveActual(ended.completedSession()!!.copy(notes = "Changed"))
        }
        assertEquals(ended.completedSession(), CoachingStore(context).strength().single())
        assertEquals(before, SqliteGymWorkouts(bridge).record("repeat", 1))
        assertNull(SqliteGymExecutionRecords(bridge).evidence("execution")!!.actualEditedAt)
    }

    @Test
    fun sharedWritesRoundTripThroughOriginalBackupWithEditedActualAndRepeatedSource() {
        val ended = finish()
        val repeat = GymWorkoutAdapter.repeatExecution("repeat", 1, at, "Repeat", ended)
        shared.saveWorkout(repeat.definition(), repeat.source())
        shared.savePreference(GymExecutionRepository.Preference("repeat", 1, true))
        val edited = ended.completedSession()!!.copy(notes = "Explicit accepted edit")
        shared.saveActual(edited)
        val backups = SqliteBackupRepository(context)
        backups.restoreUserData(backups.exportUserData(), false)
        assertEquals(ended, original.load("execution")!!.execution())
        assertEquals(
            edited,
            SqliteGymExecutionRecords(bridge).evidence("execution")!!.currentActual,
        )
        assertEquals(original.preferences(), shared.preferences())
        original.saveWorkout(repeat.definition(), repeat.source())
        shared.deleteActual("execution")
        backups.restoreUserData(backups.exportUserData(), false)
        original.saveWorkout(repeat.definition(), repeat.source())
        assertTrue(CoachingStore(context).strength().isEmpty())
        assertEquals(repeat.definition(), shared.favourites(1).single())
    }
}
