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
import org.racedaydreams.storage.*

@RunWith(AndroidJUnit4::class)
class SharedGymExecutionRecordsTest {
    private val at = Instant.parse("2026-11-15T09:00:00.123456789Z")
    private val bench = ExerciseCatalog.definition("bench-press")!!.exercise()
    private val rowExercise = ExerciseCatalog.definition("dumbbell-row")!!.exercise()
    private val clock = 9007199254741017L

    private fun tick(ms: Long, epoch: String = "boot") =
        GymExecution.Tick(
            epoch,
            clock + ms,
            Instant.ofEpochSecond(at.getEpochSecond() + ms / 1000, at.getNano().toLong()),
        )

    private fun set(id: String, warm: Boolean = false) =
        StrengthSession.SetEntry(id, 6, 80.0, null, warm, null, null, 2.5)

    private fun engine(id: String = "execution", grouped: Boolean = false): GymExecutionEngine {
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
        val slots =
            if (grouped)
                listOf(
                    GymWorkoutDefinition.Slot("a", bench, target, "pair"),
                    GymWorkoutDefinition.Slot("b", rowExercise, target, "pair"),
                )
            else listOf(GymWorkoutDefinition.Slot("a", bench, target, null))
        return GymExecutionEngine.start(
            id,
            GymExecution.Origin.CUSTOM,
            GymWorkoutDefinition("workout", 1, at, "Upper", slots),
            null,
            ZoneOffset.ofTotalSeconds(19815),
            tick(0),
        )
    }

    private fun finished(id: String = "execution"): GymExecution =
        engine(id)
            .also { it.saveSet("a", set("working"), tick(1000)) }
            .finish(tick(2000), null, "Original performed note", true)

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var original: SqliteGymExecutionRepository
    private lateinit var records: SqliteGymExecutionRecords
    private lateinit var db: android.database.sqlite.SQLiteDatabase

    @Before
    fun prepare() {
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
        db = ProfileDatabase.get(context).writableDatabase
        original = SqliteGymExecutionRepository(context)
        records = SqliteGymExecutionRecords(AndroidSqlTestConnection(db))
    }

    @After
    fun cleanup() {
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
    }

    @Test
    fun originalLiveWritesAndRecoveryRemainExactWithIndexedSummariesAndNoCompletedActuals() {
        val e = engine()
        val first = original.start(e.snapshot())!!
        e.saveSet("a", set("warm", true), tick(100))
        e.saveSet("a", set("working"), tick(1000))
        val saved = original.save(e.snapshot(), first.revision())!!
        assertEquals(saved, records.load("execution"))
        assertEquals(original.unfinished(), records.unfinished())
        assertEquals(SqliteGymExecutionRepository.hasUnfinished(db), records.hasUnfinished())
        val recovered =
            GymExecutionEngine.recover(saved.execution(), tick(100000, "new")).snapshot()
        val stored = original.save(recovered, saved.revision())!!
        assertEquals(stored, records.load("execution"))
        assertEquals(stored, records.evidence("execution")!!.original)
        assertNull(records.evidence("execution")!!.currentActual)
        assertEquals(GymExecution.Status.RECOVERY_REQUIRED, stored.execution().status())
        assertTrue(CoachingStore(context).strength().isEmpty())
    }

    @Test
    fun completedMixedExerciseBlocksAndExplicitActualEditStaySeparateFromOriginalExecution() {
        val e = engine(grouped = true)
        val initial = original.start(e.snapshot())!!
        e.saveSet("a", set("original"), tick(100))
        e.replaceExercise("a", rowExercise, tick(200))
        e.saveSet("a", set("replacement"), tick(300))
        e.breakSuperset("b", tick(400))
        val ended = e.finish(tick(1000), null, "Original", true)
        original.finish(ended, initial.revision())
        assertEquals(original.load("execution"), records.load("execution"))
        assertEquals(ended.completedSession(), records.evidence("execution")!!.currentActual)
        val changed = ended.completedSession()!!.copy(notes = "Athlete edited actual note")
        CoachingStore(context).saveStrength(changed)
        val read = records.evidence("execution")!!
        assertEquals(ended, read.original.execution())
        assertEquals(changed, read.currentActual)
        assertNotNull(read.actualEditedAt)
        assertEquals(
            listOf(bench, rowExercise),
            read.original.execution().actualSets().map { it.exercise() },
        )
        assertEquals(setOf("execution"), records.finishedActualIds(listOf("execution", "missing")))
        assertEquals(
            original.finishedActualIds(listOf("execution")),
            records.finishedActualIds(listOf("execution")),
        )
    }

    @Test
    fun bothReadersRejectCorruptSummaryAndStagingOrderWithoutRepairingTheStoredEvidence() {
        original.start(engine().snapshot())
        db.execSQL("UPDATE strength_executions SET title='Corrupt index'")
        assertThrows(IllegalArgumentException::class.java) { original.load("execution") }
        assertThrows(IllegalArgumentException::class.java) { records.load("execution") }
        assertEquals(original.unfinished(), records.unfinished())
        db.execSQL("UPDATE strength_executions SET title='Upper'")
        db.execSQL("UPDATE strength_execution_events SET sequence=9")
        assertThrows(IllegalArgumentException::class.java) { original.load("execution") }
        assertThrows(IllegalArgumentException::class.java) { records.load("execution") }
        db.rawQuery("SELECT COUNT(*) FROM strength_execution_events", null).use {
            assertTrue(it.moveToFirst())
            assertEquals(1L, it.getLong(0))
        }
    }
}
