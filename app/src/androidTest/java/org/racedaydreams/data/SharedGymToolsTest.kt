package org.racedaydreams.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.time.ZoneOffset
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.strength.*
import org.racedaydreams.storage.*

@RunWith(AndroidJUnit4::class)
class SharedGymToolsTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var original: SqliteGymToolsRepository
    private lateinit var shared: SqliteGymTools
    private lateinit var bridge: AndroidSqlTestConnection
    private lateinit var executions: SqliteGymExecutionRepository

    private fun clock(ms: Long, epoch: String = "boot") = GymRestTimer.Clock(epoch, ms)

    @Before
    fun prepare() {
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
        bridge = AndroidSqlTestConnection(ProfileDatabase.get(context).writableDatabase)
        original = SqliteGymToolsRepository(context)
        shared = SqliteGymTools(bridge)
        executions = SqliteGymExecutionRepository(context)
    }

    @After
    fun cleanup() {
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
    }

    private fun start(): GymExecution {
        val snapshot =
            GymExecutionEngine.start(
                    "workout",
                    GymExecution.Origin.FREE,
                    null,
                    null,
                    ZoneOffset.UTC,
                    GymExecution.Tick("process", 1000, Instant.parse("2026-11-16T09:00:00Z")),
                )
                .snapshot()
        executions.start(snapshot)
        return snapshot
    }

    @Test
    fun existingAndSharedTimerCommandsAndPreferencesUseOneDatabaseWithoutChangingActuals() {
        val execution = start()
        val p = GymRestTimer.Preferences(180, false, true, true, false)
        original.preferences(p)
        assertEquals(p, shared.preferences())
        shared.preferences(GymRestTimer.Preferences.defaults())
        assertEquals(shared.preferences(), original.preferences())
        original.change("workout") { GymRestTimer.start("workout", "first", 90, clock(1000)) }
        assertEquals(original.timer("workout"), shared.timer("workout"))
        shared.change("workout") { it!!.pause(clock(11000)) }
        assertEquals(original.timer("workout"), shared.timer("workout"))
        original.change("workout") { it.resume(clock(21000)) }
        shared.change("workout") { it!!.adjust(30, clock(31000)) }
        assertEquals(100000L, original.timer("workout").remainingMillis())
        assertFalse(shared.completeIfDue("workout", "wrong", clock(131000)))
        assertTrue(original.completeIfDue("workout", "first", clock(131000)))
        assertFalse(shared.completeIfDue("workout", "first", clock(131000)))
        assertEquals(execution, executions.load("workout")!!.execution())
        assertEquals(1L, executions.load("workout")!!.revision())
        assertTrue(bridge.query("SELECT id FROM strength_sessions").isEmpty())
    }

    @Test
    fun bothArchiveReadersPreserveReferencesAndRejectOrphansAndCoercion() {
        start()
        original.change("workout") {
            GymRestTimer(
                "workout",
                "trigger",
                GymRestTimer.Phase.PAUSED,
                "boot",
                9007199254740993L,
                12345,
                180000,
            )
        }
        val db = ProfileDatabase.get(context).writableDatabase
        val oldPayload = SqliteGymToolsRepository.exportData(db)
        val snapshot = shared.snapshot()
        assertEquals(snapshot, GymToolsPayloadJson.decode(oldPayload.toString(), setOf("workout")))
        val engine =
            SqliteBackupRepository(context).exportUserData().getJSONObject("strengthEngine")
        val parsed =
            SqliteGymToolsRepository.validate(
                JSONObject(GymToolsPayloadJson.encode(snapshot)),
                engine,
            )
        assertEquals(snapshot.preferences, parsed.preferences())
        assertEquals(snapshot.timers, parsed.timers())
        for (bad in
            listOf(
                oldPayload
                    .toString()
                    .replace("\"execution\":\"workout\"", "\"execution\":\"missing\""),
                oldPayload
                    .toString()
                    .replace("\"observed\":9007199254740993", "\"observed\":\"9007199254740993\""),
            )) {
            assertThrows(IllegalArgumentException::class.java) {
                GymToolsPayloadJson.decode(bad, setOf("workout"))
            }
            assertThrows(IllegalArgumentException::class.java) {
                SqliteGymToolsRepository.validate(JSONObject(bad), engine)
            }
        }
        assertEquals(snapshot, shared.snapshot())
    }

    @Test
    fun sharedCallbacksAndCommandsRespectInterruptionsAndExistingOuterRollback() {
        val execution = start()
        original.change("workout") { GymRestTimer.start("workout", "first", 90, clock(1000)) }
        assertFalse(shared.completeIfDue("workout", "first", clock(100000, "reboot")))
        val db = ProfileDatabase.get(context).writableDatabase
        db.beginTransaction()
        try {
            shared.change("workout") { it!!.skip(clock(2000)) }
            original.preferences(GymRestTimer.Preferences(300, false, true, true, true))
        } finally {
            db.endTransaction()
        }
        assertEquals(GymRestTimer.Phase.RUNNING, original.timer("workout").phase())
        assertEquals(GymRestTimer.Preferences.defaults(), shared.preferences())
        assertThrows(IllegalArgumentException::class.java) {
            shared.change("workout") { GymRestTimer.start("other", "trigger", 90, clock(1000)) }
        }
        assertEquals(execution, executions.load("workout")!!.execution())
        assertEquals(shared.timer("workout"), original.timer("workout"))
    }
}
