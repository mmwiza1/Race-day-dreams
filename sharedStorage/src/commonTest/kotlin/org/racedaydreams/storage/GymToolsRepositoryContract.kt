package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.GymRestTimer

abstract class GymToolsRepositoryContract {
    protected abstract fun database(): SqlDatabase

    private fun clock(ms: Long, epoch: String = "boot") = GymRestTimer.Clock(epoch, ms)

    private fun start(trigger: String = "first", seconds: Int = 90) =
        GymRestTimer.start("workout", trigger, seconds, clock(1000))

    private fun <T> storage(block: (SqlDatabase, SqliteGymTools) -> T): T {
        val db = database()
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation("2026-11-16T09:00:00Z", "test-install")
            }
            return block(db, SqliteGymTools(db))
        } finally {
            db.close()
        }
    }

    private fun seed(db: SqlDatabase, state: String = "ACTIVE") {
        db.execute(
            "INSERT INTO strength_executions(id,revision,origin,state,started_at,title,recorded_elapsed,active_millis,set_count,event_count,header,accepted_actual) VALUES('workout',1,'CUSTOM',?,'2026-11-16T09:00:00Z','Retained workout',0,0,0,1,'{}',?)",
            listOf(
                SqlValue.Text(state),
                if (state == "FINISHED") SqlValue.Text("{}") else SqlValue.Null,
            ),
        )
    }

    @Test
    fun preferencesSurviveNewRepositoryAndDoNotCreateWorkoutEvidence(): Unit =
        storage { db, tools ->
            assertEquals(GymRestTimer.Preferences.defaults(), tools.preferences())
            val p = GymRestTimer.Preferences(180, false, true, true, true)
            tools.preferences(p)
            assertEquals(p, SqliteGymTools(db).preferences())
            tools.preferences(GymRestTimer.Preferences.defaults())
            assertEquals(GymRestTimer.Preferences.defaults(), tools.preferences())
            assertTrue(db.query("SELECT id FROM strength_executions").isEmpty())
            assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
        }

    @Test
    fun persistedPauseResumeAdjustSkipAndResetReuseTheExistingEngine(): Unit =
        storage { db, tools ->
            seed(db)
            assertNull(tools.timer("workout"))
            tools.change("workout") {
                assertNull(it)
                start()
            }
            val paused = tools.change("workout") { it!!.pause(clock(11000)) }
            assertEquals(80000L, paused.remainingMillis())
            assertEquals(paused, SqliteGymTools(db).timer("workout"))
            tools.change("workout") { it!!.resume(clock(21000)) }
            assertEquals(
                100000L,
                tools.change("workout") { it!!.adjust(30, clock(31000)) }.remainingMillis(),
            )
            assertEquals(
                GymRestTimer.Phase.SKIPPED,
                tools.change("workout") { it!!.skip(clock(32000)) }.phase(),
            )
            assertEquals(
                120000L,
                tools.change("workout") { it!!.reset(clock(33000)) }.remainingMillis(),
            )
            assertEquals(
                0L,
                db.query("SELECT set_count FROM strength_executions").single().single().sqlLong(),
            )
            assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
        }

    @Test
    fun completionRequiresCurrentTriggerDueTimeAndAcknowledgesOnlyOnce(): Unit =
        storage { db, tools ->
            seed(db)
            tools.change("workout") { start() }
            assertFalse(tools.completeIfDue("workout", "first", clock(90999)))
            assertEquals(start(), tools.timer("workout"))
            tools.change("workout") { start("new") }
            assertFalse(tools.completeIfDue("workout", "first", clock(91000)))
            assertTrue(tools.completeIfDue("workout", "new", clock(91000)))
            assertFalse(tools.completeIfDue("workout", "new", clock(100000)))
            assertEquals(GymRestTimer.Phase.COMPLETE, tools.timer("workout")!!.phase())
            assertFalse(tools.completeIfDue("missing", "first", clock(100000)))
        }

    @Test
    fun clockDiscontinuityNeverPretendsRestCompleted(): Unit = storage { db, tools ->
        seed(db)
        tools.change("workout") { start() }
        assertFalse(tools.completeIfDue("workout", "first", clock(0, "new-boot")))
        assertFalse(tools.completeIfDue("workout", "first", clock(999)))
        assertEquals(start(), tools.timer("workout"))
        val interrupted = tools.change("workout") { it!!.observe(clock(0, "new-boot")) }
        assertEquals(GymRestTimer.Phase.INTERRUPTED, interrupted.phase())
        assertEquals(90000L, interrupted.remainingMillis())
        assertFalse(tools.completeIfDue("workout", "first", clock(100000, "new-boot")))
        assertEquals(
            90000L,
            tools.change("workout") { it!!.resume(clock(100000, "new-boot")) }.remainingMillis(),
        )
    }

    @Test
    fun allUnfinishedStatesCanRetainRestButTerminalHistoryCannotChange(): Unit =
        storage { db, tools ->
            seed(db)
            for (state in listOf("ACTIVE", "PAUSED", "RECOVERY_REQUIRED")) {
                db.execute("UPDATE strength_executions SET state=?", listOf(SqlValue.Text(state)))
                assertEquals(start(), tools.change("workout") { start() })
            }
            db.execute("UPDATE strength_executions SET state='FINISHED',accepted_actual='{}'")
            var called = false
            assertFailsWith<IllegalArgumentException> {
                tools.change("workout") {
                    called = true
                    start()
                }
            }
            assertFalse(called)
            assertFalse(tools.completeIfDue("workout", "first", clock(91000)))
            db.execute("UPDATE strength_executions SET state='DISCARDED',accepted_actual=NULL")
            assertFailsWith<IllegalArgumentException> { tools.change("workout") { start() } }
            assertFalse(tools.completeIfDue("workout", "first", clock(91000)))
            assertEquals(start(), tools.timer("workout"))
        }

    @Test
    fun missingOwnershipAndWrongIdentityCommandsCannotCreateTimers(): Unit = storage { db, tools ->
        assertFailsWith<IllegalArgumentException> { tools.change("workout") { start() } }
        seed(db)
        tools.change("workout") { start() }
        assertFailsWith<IllegalArgumentException> {
            tools.change("workout") { GymRestTimer.start("other", "trigger", 90, clock(1000)) }
        }
        assertFailsWith<IllegalStateException> {
            tools.change("workout") { error("command failure") }
        }
        assertEquals(start(), tools.timer("workout"))
        assertNull(tools.timer("other"))
    }

    @Test
    fun timerAndPreferencesRespectOuterTransactionRollbackAndInsertFailure(): Unit =
        storage { db, tools ->
            seed(db)
            tools.change("workout") { start() }
            assertFails {
                db.transaction {
                    tools.preferences(GymRestTimer.Preferences(180, false, true, true, true))
                    tools.change("workout") { start("new") }
                    error("outer failure")
                }
            }
            assertEquals(start(), tools.timer("workout"))
            assertEquals(GymRestTimer.Preferences.defaults(), tools.preferences())
            db.execute(
                "CREATE TRIGGER reject_rest BEFORE INSERT ON gym_rest_timers BEGIN SELECT RAISE(ABORT,'test failure'); END"
            )
            assertFails { tools.completeIfDue("workout", "first", clock(91000)) }
            assertEquals(start(), tools.timer("workout"))
        }

    @Test
    fun archivePreservesExactClocksPreferencesAndTerminalTimersWithoutChangingData(): Unit =
        storage { db, tools ->
            seed(db)
            tools.preferences(GymRestTimer.Preferences(180, false, true, true, false))
            val precise =
                GymRestTimer(
                    "workout",
                    "trigger",
                    GymRestTimer.Phase.PAUSED,
                    "boot",
                    9007199254740993L,
                    12345,
                    180000,
                )
            tools.change("workout") { precise }
            val snapshot = tools.snapshot()
            val encoded = GymToolsPayloadJson.encode(snapshot)
            assertEquals(snapshot, GymToolsPayloadJson.decode(encoded, setOf("workout")))
            assertEquals(precise, tools.timer("workout"))
            for (phase in GymRestTimer.Phase.entries) {
                val t =
                    precise.copy(
                        phase = phase,
                        remainingMillis =
                            if (
                                phase in
                                    setOf(GymRestTimer.Phase.COMPLETE, GymRestTimer.Phase.SKIPPED)
                            )
                                0
                            else 12345,
                    )
                val s = GymToolsPayloadJson.Snapshot(snapshot.preferences, listOf(t))
                assertEquals(
                    s,
                    GymToolsPayloadJson.decode(GymToolsPayloadJson.encode(s), setOf("workout")),
                )
            }
        }

    @Test
    fun archiveRejectsOrphansDuplicatesUnknownVersionsFieldsAndCoercedValues(): Unit =
        storage { _, _ ->
            val snapshot =
                GymToolsPayloadJson.Snapshot(GymRestTimer.Preferences.defaults(), listOf(start()))
            val encoded = GymToolsPayloadJson.encode(snapshot)
            assertFailsWith<IllegalArgumentException> {
                GymToolsPayloadJson.decode(encoded, emptySet())
            }
            assertFailsWith<IllegalArgumentException> {
                GymToolsPayloadJson.decode(
                    GymToolsPayloadJson.encode(snapshot.copy(timers = listOf(start(), start()))),
                    setOf("workout"),
                )
            }
            val bad =
                listOf(
                    encoded.replace("\"version\":1", "\"version\":2"),
                    encoded.replace("\"version\":1", "\"version\":1e0"),
                    encoded.replace("\"defaultSeconds\":90", "\"defaultSeconds\":\"90\""),
                    encoded.replace("\"audio\":false", "\"audio\":0"),
                    encoded.replace("\"observed\":1000", "\"observed\":1e3"),
                    encoded.replace("\"remaining\":90000", "\"remaining\":90001"),
                    encoded.replace("\"phase\":\"RUNNING\"", "\"phase\":\"COMPLETE\""),
                    encoded.replace("\"version\":1", "\"version\":1,\"extra\":0"),
                    encoded.replace("\"version\":1", "\"version\":1,\"version\":1"),
                )
            for (payload in bad) assertFailsWith<IllegalArgumentException>(payload) {
                GymToolsPayloadJson.decode(payload, setOf("workout"))
            }
            assertEquals(
                GymToolsPayloadJson.Snapshot(GymRestTimer.Preferences.defaults(), emptyList()),
                GymToolsPayloadJson.decode(null, emptySet()),
            )
        }

    @Test
    fun archiveBoundsIdentitiesAndCollectionsIndependentlyOfTimerEngine(): Unit = storage { _, _ ->
        val p = GymRestTimer.Preferences.defaults()
        val wide = start().copy(triggerId = "x".repeat(129))
        assertFailsWith<IllegalArgumentException> {
            GymToolsPayloadJson.decode(
                GymToolsPayloadJson.encode(GymToolsPayloadJson.Snapshot(p, listOf(wide))),
                setOf("workout"),
            )
        }
        val root =
            Json.parseToJsonElement(
                    GymToolsPayloadJson.encode(GymToolsPayloadJson.Snapshot(p, emptyList()))
                )
                .jsonObject
        assertFailsWith<IllegalArgumentException> {
            GymToolsPayloadJson.decode(
                JsonObject(root + ("timers" to JsonArray(List(100001) { JsonNull }))).toString(),
                emptySet(),
            )
        }
    }
}
