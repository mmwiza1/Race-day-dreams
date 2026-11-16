package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

abstract class GymExecutionRecordsContract {
    protected abstract fun database(): SqlDatabase

    private val at = StoredInstant.parse("2026-11-15T09:00:00.123456789Z")
    private val bench = ExerciseCatalog.definition("bench-press")!!.exercise()
    private val rowExercise = ExerciseCatalog.definition("dumbbell-row")!!.exercise()
    private val clock = 9007199254741017L

    private fun tick(ms: Long, epoch: String = "boot") =
        GymExecution.Tick(
            epoch,
            clock + ms,
            Instants.ofEpochSecond(at.getEpochSecond() + ms / 1000, at.getNano().toLong()),
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
            Offsets.ofTotalSeconds(19815),
            tick(0),
        )
    }

    private fun finished(id: String = "execution"): GymExecution =
        engine(id)
            .also { it.saveSet("a", set("working"), tick(1000)) }
            .finish(tick(2000), null, "Original performed note", true)

    private fun texts(vararg values: String): List<SqlValue> = values.map { SqlValue.Text(it) }

    private fun <T> storage(block: (SqlDatabase, SqliteGymExecutionRecords) -> T): T {
        val db = database()
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(at.toString(), "test-install")
            }
            return block(db, SqliteGymExecutionRecords(db))
        } finally {
            db.close()
        }
    }

    /** Original normalised shape; seeding fixtures is deliberately not a production finaliser. */
    private fun seed(
        db: SqlDatabase,
        snapshot: GymExecution,
        revision: Long = 1,
        current: StrengthSession? = snapshot.completedSession(),
        edited: Instant? = null,
    ) {
        db.execute("DELETE FROM strength_executions WHERE id=?", texts(snapshot.id()))
        db.execute("DELETE FROM strength_sessions WHERE id=?", texts(snapshot.id()))
        val p = snapshot.prescription()
        if (p != null)
            db.execute(
                "INSERT OR IGNORE INTO strength_workout_versions(workout_id,revision,source_kind,source_id,payload) VALUES(?,?,NULL,NULL,?)",
                listOf(
                    SqlValue.Text(p.id()),
                    SqlValue.Integer(p.revision().toLong()),
                    SqlValue.Text(
                        FrozenGymWorkoutRecordJson.encode(
                            FrozenGymWorkoutRecordJson.Record(p, null, null)
                        )
                    ),
                ),
            )
        val root = GymExecutionSnapshotJson.element(snapshot)
        val header =
            if (snapshot.completedSession() == null)
                JsonObject(root - setOf("actualSets", "events"))
            else
                JsonObject(
                    (root - setOf("actualSets", "events")) +
                        ("completedSession" to
                            JsonObject(root.getValue("completedSession").jsonObject - "exercises"))
                )
        db.execute(
            "INSERT INTO strength_executions(id,revision,origin,state,started_at,title,recorded_elapsed,active_millis,set_count,event_count,workout_id,workout_revision,header,accepted_actual,actual_edited_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            listOf(
                SqlValue.Text(snapshot.id()),
                SqlValue.Integer(revision),
                SqlValue.Text(snapshot.origin().name),
                SqlValue.Text(snapshot.status().name),
                SqlValue.Text(snapshot.startedAt().toString()),
                SqlValue.Text(p?.title() ?: "Free workout"),
                SqlValue.Integer(snapshot.recordedElapsedMillis()),
                SqlValue.Integer(snapshot.activeMillis()),
                SqlValue.Integer(snapshot.actualSets().size.toLong()),
                SqlValue.Integer(snapshot.events().size.toLong()),
                sqlText(p?.id()),
                p?.revision()?.let { SqlValue.Integer(it.toLong()) } ?: SqlValue.Null,
                SqlValue.Text(header.toString()),
                current?.let { SqlValue.Text(StrengthEvidenceJson.encodeAcceptedSession(it)) }
                    ?: SqlValue.Null,
                sqlText(edited?.toString()),
            ),
        )
        for ((i, value) in root.getValue("actualSets").jsonArray.withIndex()) {
            val row = value.jsonObject
            db.execute(
                "INSERT INTO strength_execution_sets(execution_id,sequence,actual_set_id,payload) VALUES(?,?,?,?)",
                listOf(
                    SqlValue.Text(snapshot.id()),
                    SqlValue.Integer(i.toLong()),
                    SqlValue.Text(
                        row.getValue("actual").jsonObject.getValue("id").jsonPrimitive.content
                    ),
                    SqlValue.Text(row.toString()),
                ),
            )
        }
        for ((i, value) in root.getValue("events").jsonArray.withIndex()) db.execute(
            "INSERT INTO strength_execution_events(execution_id,sequence,payload) VALUES(?,?,?)",
            listOf(
                SqlValue.Text(snapshot.id()),
                SqlValue.Integer(i.toLong()),
                SqlValue.Text(value.toString()),
            ),
        )
        if (current != null) actual(db, current)
    }

    private fun actual(db: SqlDatabase, session: StrengthSession) {
        db.execute(
            "INSERT OR REPLACE INTO strength_sessions(id,started_at,payload) VALUES(?,?,?)",
            texts(
                session.id(),
                session.startedAt().toString(),
                StrengthEvidenceJson.encodeSession(session),
            ),
        )
    }

    @Test
    fun activePausedAndRecoverySnapshotsRetainExactClocksWithoutCreatingActuals(): Unit =
        storage { db, records ->
            val e = engine()
            e.saveSet("a", set("warm", true), tick(100))
            e.saveSet("a", set("working"), tick(1000))
            val active = e.snapshot()
            val paused = e.pause(tick(2000))
            val recovered = GymExecutionEngine.recover(active, tick(100000, "new")).snapshot()
            for (s in listOf(active, paused, recovered)) {
                seed(db, s, 9007199254740993L)
                val loaded = records.load(s.id())!!
                assertEquals(s, loaded.execution())
                assertEquals(9007199254740993L, loaded.revision())
                assertEquals(
                    s.lastTick().monotonicMillis(),
                    loaded.execution().lastTick().monotonicMillis(),
                )
                assertEquals(loaded, records.evidence(s.id())!!.original)
                assertNull(records.evidence(s.id())!!.currentActual)
                assertTrue(records.hasUnfinished())
                assertEquals(s.status(), records.unfinished().single().status())
                assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
            }
        }

    @Test
    fun finishedSubstitutionsAndSupersetChangesKeepOriginalPrescriptionAndActualBlocks(): Unit =
        storage { db, records ->
            val e = engine(grouped = true)
            e.saveSet("a", set("original"), tick(100))
            e.replaceExercise("a", rowExercise, tick(200))
            e.saveSet("a", set("replacement"), tick(300))
            e.breakSuperset("b", tick(400))
            val ended = e.finish(tick(1000), null, "Retained note", true)
            seed(db, ended, 5)
            assertEquals(ended, records.load(ended.id())!!.execution())
            val restored = records.evidence(ended.id())!!
            assertEquals(ended.completedSession(), restored.currentActual)
            assertEquals(
                listOf(bench, rowExercise),
                restored.currentActual!!.exercises().map { it.exercise() },
            )
            assertEquals(
                bench,
                restored.original.execution().prescription()!!.slots()[0].exercise(),
            )
            assertNull(restored.original.execution().slots()[0].target()!!.referenceLoadKg())
            assertFalse(records.hasUnfinished())
            assertTrue(records.unfinished().isEmpty())
        }

    @Test
    fun actualEditsStaySeparateFromOriginalPerformedSetsAndRequireAnObservation(): Unit =
        storage { db, records ->
            val ended = finished()
            val original = ended.completedSession()!!
            val changed = original.copy(notes = "Athlete corrected actual")
            val editedAt = StoredInstant.parse("2026-11-16T10:00:00Z")
            seed(db, ended, 7, changed, editedAt)
            val read = records.evidence(ended.id())!!
            assertEquals(ended, read.original.execution())
            assertEquals(changed, read.currentActual)
            assertEquals(editedAt, read.actualEditedAt)
            db.execute("UPDATE strength_executions SET actual_edited_at=NULL")
            assertFailsWith<IllegalArgumentException> { records.evidence(ended.id()) }
            assertEquals(ended, records.load(ended.id())!!.execution())
            assertEquals(changed, read.currentActual)
        }

    @Test
    fun missingOrDivergentAcceptedActualFailsWithoutRepairingEitherHistory(): Unit =
        storage { db, records ->
            val ended = finished()
            seed(db, ended)
            actual(db, ended.completedSession()!!.copy(notes = "Unacknowledged divergent actual"))
            assertFailsWith<IllegalArgumentException> { records.evidence(ended.id()) }
            assertEquals(ended, records.load(ended.id())!!.execution())
            db.execute("DELETE FROM strength_sessions")
            assertFailsWith<IllegalArgumentException> { records.evidence(ended.id()) }
            assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
            assertEquals(
                1L,
                db.query("SELECT COUNT(*) FROM strength_executions").single().single().sqlLong(),
            )
        }

    @Test
    fun unfinishedAndDiscardedSnapshotsCannotClaimManualCompletedEvidence(): Unit =
        storage { db, records ->
            val e = engine()
            val discarded = e.discard(tick(1000))
            seed(db, discarded)
            assertNull(records.evidence(discarded.id())!!.currentActual)
            actual(db, finished().completedSession()!!)
            assertFailsWith<IllegalArgumentException> { records.evidence(discarded.id()) }
            assertEquals(discarded, records.load(discarded.id())!!.execution())
            seed(db, engine().snapshot())
            actual(db, finished().completedSession()!!)
            assertFailsWith<IllegalArgumentException> { records.evidence("execution") }
        }

    @Test
    fun summariesAndBoundedMembershipDoNotHydrateUnrelatedCorruptTerminalHeaders(): Unit =
        storage { db, records ->
            seed(db, finished("finished"))
            seed(db, engine("active").snapshot())
            db.execute("UPDATE strength_executions SET header='not JSON' WHERE id='finished'")
            assertEquals("active", records.unfinished().single().id())
            assertEquals(
                setOf("finished"),
                records.finishedActualIds(
                    listOf("finished", "active", "missing", "finished", "' OR 1=1--")
                ),
            )
            assertTrue(records.finishedActualIds(emptyList()).isEmpty())
            assertFailsWith<IllegalArgumentException> {
                records.finishedActualIds(List(21) { "finished" })
            }
            assertFailsWith<IllegalArgumentException> { records.load("finished") }
        }

    @Test
    fun missingOutOfOrderAndMiscountedStagingRowsReject(): Unit = storage { db, records ->
        val e = engine()
        e.saveSet("a", set("first"), tick(1000))
        e.saveSet("a", set("second"), tick(2000))
        val s = e.snapshot()
        seed(db, s)
        db.execute("DELETE FROM strength_execution_sets WHERE sequence=0")
        assertFailsWith<IllegalArgumentException> { records.load(s.id()) }
        seed(db, s)
        db.execute("UPDATE strength_executions SET set_count=1")
        assertFailsWith<IllegalArgumentException> { records.load(s.id()) }
        seed(db, s)
        db.execute("UPDATE strength_execution_events SET sequence=9 WHERE sequence=0")
        assertFailsWith<IllegalArgumentException> { records.load(s.id()) }
        assertEquals(
            2L,
            db.query("SELECT COUNT(*) FROM strength_execution_sets").single().single().sqlLong(),
        )
    }

    @Test
    fun indexedSummaryIdentityAndTimingMismatchRejectWithoutRewriting(): Unit =
        storage { db, records ->
            val s = engine().snapshot()
            seed(db, s)
            db.execute("UPDATE strength_executions SET title='Different indexed title'")
            assertEquals("Different indexed title", records.unfinished().single().title())
            assertFailsWith<IllegalArgumentException> { records.load(s.id()) }
            db.execute("UPDATE strength_executions SET title='Upper',recorded_elapsed=100")
            assertFailsWith<IllegalArgumentException> { records.load(s.id()) }
            assertEquals(
                100L,
                db.query("SELECT recorded_elapsed FROM strength_executions")
                    .single()
                    .single()
                    .sqlLong(),
            )
        }

    @Test
    fun forgedEventContentCannotProduceAValidLoadedExecution(): Unit = storage { db, records ->
        val e = engine()
        e.saveSet("a", set("working"), tick(1000))
        seed(db, e.snapshot())
        val value =
            db.query("SELECT payload FROM strength_execution_events WHERE sequence=1")
                .single()
                .single()
                .sqlText()
        val row = Json.parseToJsonElement(value).jsonObject
        db.execute(
            "UPDATE strength_execution_events SET payload=? WHERE sequence=1",
            texts(JsonObject(row + ("actualSetId" to JsonPrimitive("forged"))).toString()),
        )
        assertFailsWith<IllegalArgumentException> { records.load("execution") }
        assertEquals(
            1L,
            db.query("SELECT COUNT(*) FROM strength_execution_sets").single().single().sqlLong(),
        )
    }

    @Test
    fun missingAndUnusualIdsRemainExactAndReadOnly(): Unit = storage { db, records ->
        assertNull(records.load("missing"))
        assertNull(records.evidence("missing"))
        for (id in listOf("", " ", "x".repeat(257))) assertFailsWith<IllegalArgumentException> {
            records.load(id)
        }
        val s = engine("kept\u0000🏋️").snapshot()
        seed(db, s)
        assertEquals(s, records.load(s.id())!!.execution())
        assertEquals(
            1L,
            db.query("SELECT COUNT(*) FROM strength_executions").single().single().sqlLong(),
        )
        assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
    }
}
