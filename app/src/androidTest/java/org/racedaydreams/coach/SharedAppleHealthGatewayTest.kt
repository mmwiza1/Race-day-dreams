package org.racedaydreams.coach

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant as HealthInstant
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.health.*
import org.racedaydreams.data.AndroidSqlConnection
import org.racedaydreams.data.createAndroidSchemaFixture
import org.racedaydreams.domain.body.BodyWeight
import org.racedaydreams.health.HealthSyncStatus
import org.racedaydreams.health.body.*
import org.racedaydreams.health.workouts.HealthKitRun
import org.racedaydreams.storage.*

/**
 * Actual shared Apple orchestration and SQLite; only SDK observations/availability are supplied.
 */
@RunWith(AndroidJUnit4::class)
class SharedAppleHealthGatewayTest {
    private val now = Instant.parse("2026-11-17T09:00:00Z")
    private val window =
        BodyReadWindow(
            HealthInstant.parse("2026-11-01T00:00:00Z"),
            HealthInstant.parse(now.toString()),
        )

    private class Selection(
        var workouts: Boolean = true,
        var metrics: Set<BodyMetric> = BodyMetric.entries.toSet(),
    )

    private class Source : AppleHealthReadSource {
        var available = true
        var workoutCalls = 0
        var bodyCalls = 0
        var failBody = false
        var wrongScope = false

        override fun available() = available

        override suspend fun workouts(window: BodyReadWindow): List<HealthKitRun> {
            workoutCalls++
            return listOf(
                HealthKitRun(
                    "workout",
                    "watch.source",
                    HealthInstant.parse("2026-11-16T09:00:00Z"),
                    HealthInstant.parse("2026-11-16T09:30:00Z"),
                    5000.0,
                )
            )
        }

        override suspend fun body(
            window: BodyReadWindow,
            metrics: Set<BodyMetric>,
        ): BodyReadSnapshot {
            bodyCalls++
            check(!failBody) { "SDK read failed before import." }
            val selected = if (wrongScope) emptySet() else metrics
            val records =
                selected.map { metric ->
                    BodyObservation(
                        metric,
                        if (metric == BodyMetric.HEIGHT_METRES) 1.83 else 87.25,
                        HealthInstant.parse("2026-11-16T08:30:01.123456789Z"),
                        null,
                        "health-kit",
                        if (metric == BodyMetric.HEIGHT_METRES) "height-source"
                        else "weight-source",
                        "body.source",
                    )
                }
            return BodyReadSnapshot(
                window,
                selected,
                records,
                ReadCompleteness.VISIBLE_RECORDS_ONLY,
            )
        }
    }

    private class Fixture(
        val db: SqlDatabase,
        val gateway: SharedAppleHealthGateway,
        val activities: SqliteActivities,
        val context: SqliteCoachingContext,
        val heights: SqliteBodyHeights,
        val source: Source,
        val selection: Selection,
    )

    private fun fixture(block: (Fixture) -> Unit) {
        val raw = SQLiteDatabase.create(null)
        try {
            createAndroidSchemaFixture(raw)
            val db = AndroidSqlConnection { raw }
            val activities = SqliteActivities(db) { now }
            val ids = { error("Imports must not allocate athlete decisions.") }
            val context = SqliteCoachingContext(db, { now }, ids)
            val source = Source()
            val selection = Selection()
            val gateway =
                SharedAppleHealthGateway(
                    db,
                    activities,
                    "own.app",
                    source,
                    { window },
                    { selection.workouts },
                    { selection.metrics },
                    { now },
                    ids,
                )
            block(
                Fixture(db, gateway, activities, context, SqliteBodyHeights(db), source, selection)
            )
        } finally {
            raw.close()
        }
    }

    private fun rows(f: Fixture) =
        listOf(
            f.db.query("SELECT * FROM activities ORDER BY id"),
            f.db.query("SELECT * FROM coach_evidence ORDER BY kind,id"),
            f.db.query("SELECT * FROM body_heights ORDER BY id"),
            f.db.query("SELECT * FROM import_checkpoints ORDER BY provider"),
        )

    private fun rejected(block: suspend () -> Any?): Exception {
        try {
            runBlocking { block() }
        } catch (e: Exception) {
            return e
        }
        throw AssertionError("Expected import rejection")
    }

    @Test
    fun bodyOnlyReadNeverQueriesWorkoutsOrInventsGrantsCoverageOrProteinSelection() = fixture { f ->
        f.selection.workouts = false
        f.selection.metrics = setOf(BodyMetric.WEIGHT_KILOGRAMS)
        val manual =
            BodyWeight(
                "manual",
                70.0,
                now.minusSeconds(3600),
                ZoneOffset.UTC,
                "manual",
                "manual",
                null,
            )
        f.context.saveWeight(manual)
        val result = runBlocking { f.gateway.sync(f.activities) }
        assertEquals(HealthSyncStatus.PARTIAL, result.status)
        assertEquals(0, f.source.workoutCalls)
        assertEquals(1, f.source.bodyCalls)
        assertTrue(runBlocking { f.gateway.grantedPermissions() }.isEmpty())
        assertNull(result.windowStart)
        assertNull(result.windowEnd)
        assertFalse(result.message.contains("workouts"))
        assertTrue(result.message.contains("does not confirm read access"))
        val actual = f.context.weights().single { it.provider() == "health-kit" }
        assertEquals(87.25, actual.kilograms(), 0.0)
        assertEquals("weight-source", actual.sourceId())
        assertEquals("body.source", actual.origin())
        assertEquals(123456789, actual.at().nano)
        assertTrue(f.context.weights().contains(manual))
        assertTrue(f.activities.activities().isEmpty())
        assertTrue(f.heights.heights(500, 0).isEmpty())
        assertNull(f.heights.preferences().basis())
        val before = rows(f)
        runBlocking { f.gateway.sync(f.activities) }
        assertEquals(before, rows(f))
        assertEquals(0, f.source.workoutCalls)
        assertEquals(2, f.source.bodyCalls)
    }

    @Test
    fun selectedSdkFailureAndLateBodyWriteFailureCannotLeaveHalfImportedWorkouts() = fixture { f ->
        f.activities.applyImport("health-kit", emptyList(), emptyList(), "retained-anchor")
        val before = rows(f)
        val changes = f.db.query("SELECT total_changes()")
        f.source.failBody = true
        assertEquals(
            "SDK read failed before import.",
            rejected { f.gateway.sync(f.activities) }.message,
        )
        assertEquals(changes, f.db.query("SELECT total_changes()"))
        assertEquals(before, rows(f))
        f.source.failBody = false
        f.db.execute(
            "CREATE TRIGGER reject_health_weight BEFORE INSERT ON coach_evidence WHEN NEW.kind='WEIGHT' BEGIN SELECT RAISE(ABORT,'Rejected body write'); END"
        )
        rejected { f.gateway.sync(f.activities) }
        assertEquals(before, rows(f))
        assertEquals("retained-anchor", f.activities.importToken("health-kit"))
        assertTrue(f.context.weights().isEmpty())
        assertTrue(f.heights.heights(500, 0).isEmpty())
        assertEquals(2, f.source.workoutCalls)
        assertEquals(2, f.source.bodyCalls)
    }

    @Test
    fun unavailableUnselectedMismatchedAndDifferentOwnerReadsDoNotModifyHistory() = fixture { f ->
        val before = rows(f)
        f.source.available = false
        assertEquals(
            HealthSyncStatus.UNAVAILABLE,
            runBlocking { f.gateway.sync(f.activities) }.status,
        )
        f.source.available = true
        f.selection.workouts = false
        f.selection.metrics = emptySet()
        assertEquals(
            HealthSyncStatus.PERMISSION_REQUIRED,
            runBlocking { f.gateway.sync(f.activities) }.status,
        )
        assertEquals(0, f.source.workoutCalls)
        assertEquals(0, f.source.bodyCalls)
        f.selection.workouts = true
        f.selection.metrics = setOf(BodyMetric.WEIGHT_KILOGRAMS)
        f.source.wrongScope = true
        rejected { f.gateway.sync(f.activities) }
        assertEquals(before, rows(f))
        val calls = f.source.workoutCalls + f.source.bodyCalls
        rejected { f.gateway.sync(SqliteActivities(f.db) { now }) }
        assertEquals(calls, f.source.workoutCalls + f.source.bodyCalls)
        assertEquals(before, rows(f))
    }
}
