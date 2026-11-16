package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.recording.RunMetricsEngine.*
import org.racedaydreams.domain.storage.*
import org.racedaydreams.domain.time.*

class RecordingArchiveRowsTest {
    private val start = StoredInstant.parse("2026-11-16T09:00:00.123456789Z")
    private val end = StoredInstant.parse("2026-11-16T09:01:00.123456789Z")
    private val mono = 9007199254740993L

    private fun session(state: String = "FINISHED", id: String = "run") = buildJsonObject {
        put("kind", "session")
        put("id", id)
        put("state", state)
        put("start", start.toString())
        put("mono", mono)
        put("last", mono + 60000)
        if (state == "FINISHED") put("activity", "activity-$id")
    }

    private fun row(kind: String, payload: JsonObject, sequence: Long? = null, id: String = "run") =
        buildJsonObject {
            put("kind", kind)
            put("id", id)
            if (sequence != null) put("seq", sequence)
            put("payload", payload)
        }

    private fun point(
        sequence: Long,
        delta: Long,
        distance: Double,
        gap: Boolean = false,
    ): JsonObject {
        val p =
            Point(
                Fix(start, mono + delta, 51.5, -0.12, 4.0, null, null, null),
                distance,
                delta,
                if (gap) 1 else 0,
                gap,
                null,
                null,
            )
        return row(
            "point",
            Json.parseToJsonElement(RecordingPointJson.encode(p)).jsonObject,
            sequence,
        )
    }

    private fun activity(provider: String = "rdd", source: String = "run") =
        row(
            "activity",
            Json.parseToJsonElement(
                    ActivityJson.encode(
                        ActivityRecord(
                            "activity-run",
                            SourceIdentity(provider, source, null),
                            start,
                            end,
                            null,
                            null,
                            125.0,
                            Durations.ofSeconds(50),
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            emptyList(),
                            emptyList(),
                            null,
                            null,
                        )
                    )
                )
                .jsonObject,
        )

    private fun event(sequence: Long = 0, delta: Long = 5000) =
        row(
            "event",
            buildJsonObject {
                put("type", "GPS_RECOVERED")
                put("mono", mono + delta)
                put("wall", start.toString())
            },
            sequence,
        )

    private fun proof() =
        RemovedRecordingDetail(
            "run",
            "activity-run",
            "rdd",
            "run",
            end,
            end,
            DetailRetention.Policy.LAST_24_MONTHS,
            start,
            mono,
            mono + 60000,
            2,
            1,
            "ab".repeat(32),
            "cd".repeat(32),
        )

    private fun RecordingArchiveRows.add(row: JsonObject) = acceptLine(row.toString())

    private fun change(row: JsonObject, name: String, value: JsonElement) =
        JsonObject(row + (name to value))

    @Test
    fun supportedVersionsPreserveExactClocksCountsAndGapsWithoutRecalculatingSummary() {
        for (version in 1..3) {
            val guard = RecordingArchiveRows(version)
            guard.add(session())
            guard.add(point(0, 0, 0.0))
            guard.add(point(1, 10000, 12.5))
            guard.add(point(2, 40000, 12.5, true))
            guard.add(event())
            guard.add(activity())
            val s = guard.finish().sessions.getValue("run")
            assertEquals(mono, s.startedMono)
            assertEquals(mono + 60000, s.lastMono)
            assertEquals(3, s.points)
            assertEquals(1L, s.events)
            assertTrue(s.hasActivity)
            assertNull(s.removal)
            assertFails { guard.add(event(1)) }
            assertFails { guard.finish() }
        }
    }

    @Test
    fun activePausedAndRecoverableSessionsNeverBecomeCompletedActivities() {
        for (state in listOf("ACTIVE", "PAUSED", "RECOVERABLE")) {
            val guard = RecordingArchiveRows(3)
            guard.add(session(state))
            guard.add(point(0, 0, 0.0))
            val kept = guard.finish().sessions.getValue("run")
            assertEquals(state, kept.state)
            assertFalse(kept.hasActivity)
            assertNull(kept.activityId)
        }
    }

    @Test
    fun finishedSessionRequiresActualOwnedSummaryAndARejectedRowPoisonsTheGuard() {
        val missing = RecordingArchiveRows(3)
        missing.add(session())
        assertFails { missing.finish() }
        for (bad in listOf(activity("health-connect"), activity(source = "other"))) {
            val guard = RecordingArchiveRows(3)
            guard.add(session())
            assertFails { guard.add(bad) }
            assertFails { guard.add(activity()) }
            assertFails { guard.finish() }
        }
    }

    @Test
    fun pointsRequireContiguousSequenceIncreasingClockAndNondecreasingDistance() {
        for (bad in
            listOf(
                point(0, 2000, 10.0),
                point(1, 1000, 10.0),
                point(1, 2000, 9.0),
                point(2, 2000, 11.0),
                point(1, 60001, 11.0),
            )) {
            val guard = RecordingArchiveRows(3)
            guard.add(session())
            guard.add(point(0, 1000, 10.0))
            assertFails { guard.add(bad) }
            assertFails { guard.finish() }
        }
        val tooMuchActive = point(0, 1000, 0.0)
        val payload = tooMuchActive.getValue("payload").jsonObject
        val guard = RecordingArchiveRows(3)
        guard.add(session())
        assertFails {
            guard.add(
                change(tooMuchActive, "payload", change(payload, "active", JsonPrimitive(1001)))
            )
        }
    }

    @Test
    fun removedDetailIsVersionedAndCannotCoexistWithRawPointsOrEvents() {
        val removed = row("removed", DetailRetentionArchiveJson.element(proof()))
        val guard = RecordingArchiveRows(3)
        guard.add(session())
        guard.add(activity())
        guard.add(removed)
        assertEquals(proof(), guard.finish().sessions.getValue("run").removal)
        for (raw in listOf(point(0, 1000, 0.0), event())) {
            val mixed = RecordingArchiveRows(3)
            mixed.add(session())
            mixed.add(activity())
            mixed.add(removed)
            mixed.add(raw)
            assertFails { mixed.finish() }
        }
        for (v in 1..2) {
            val old = RecordingArchiveRows(v)
            old.add(session())
            assertFails { old.add(removed) }
        }
    }

    @Test
    fun duplicateSourcesBadEventsAndMismatchedRemovalProofReject() {
        val removal = DetailRetentionArchiveJson.element(proof())
        val badRows =
            listOf(
                session(),
                activity(source = "wrong"),
                event(1),
                event(delta = -1),
                row("removed", change(removal, "last", JsonPrimitive(mono + 59999))),
            )
        for (bad in badRows) {
            val guard = RecordingArchiveRows(3)
            guard.add(session())
            assertFails { guard.add(bad) }
        }
        val duplicate = RecordingArchiveRows(3)
        duplicate.add(session())
        duplicate.add(activity())
        assertFails { duplicate.add(activity()) }
        val orphan = RecordingArchiveRows(3)
        assertFails { orphan.add(point(0, 0, 0.0)) }
    }

    @Test
    fun contextsReuseOriginalExecutionGuardsAndCannotBeDuplicated() {
        val payload = buildJsonObject {
            put("version", 1)
            put("mode", "quick")
            put(
                "steps",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("label", "Easy running")
                            put("kind", "RUN")
                            put("millis", 60000)
                        }
                    )
                },
            )
        }
        val guard = RecordingArchiveRows(3)
        guard.add(session("PAUSED"))
        guard.add(row("context", payload))
        assertTrue(guard.finish().sessions.getValue("run").hasContext)
        val duplicate = RecordingArchiveRows(3)
        duplicate.add(session())
        duplicate.add(row("context", payload))
        assertFails { duplicate.add(row("context", payload)) }
        val bad = RecordingArchiveRows(3)
        bad.add(session())
        assertFails { bad.add(row("context", change(payload, "steps", JsonArray(emptyList())))) }
    }

    @Test
    fun malformedRowsUnsupportedVersionsAndWrongScalarTypesNeverReturnASnapshot() {
        for (v in listOf(0, 4, Int.MAX_VALUE)) assertFails { RecordingArchiveRows(v) }
        for (line in
            listOf(
                "{",
                "[]",
                "{}",
                "{\"id\":\"run\",\"id\":\"other\"}",
                session().toString() + "\n",
                change(session(), "mono", JsonPrimitive("100")).toString(),
            )) {
            val guard = RecordingArchiveRows(3)
            assertFails { guard.acceptLine(line) }
            assertFails { guard.finish() }
        }
        val empty = RecordingArchiveRows(3)
        empty.acceptLine("")
        assertTrue(empty.finish().sessions.isEmpty())
        val bom = RecordingArchiveRows(3)
        bom.acceptLine("\uFEFF" + session("RECOVERABLE").toString())
        assertEquals("RECOVERABLE", bom.finish().sessions.getValue("run").state)
    }

    @Test
    fun oversizedOrExcessivelyNestedRowsFailBeforeAnyArchiveSnapshotCanEscape() {
        for (line in
            listOf(
                " ".repeat(RecordingArchiveRows.MAX_LINE + 1),
                "{\"extra\":" + "[".repeat(34) + "0" + "]".repeat(34) + "}",
            )) {
            val guard = RecordingArchiveRows(3)
            assertFails { guard.acceptLine(line) }
            assertFails { guard.finish() }
        }
    }

    @Test
    fun oldSessionLimitsRemainBoundedWithoutTruncatingOrClaimingSuccess() {
        val guard = RecordingArchiveRows(1)
        for (i in 0 until 1000) guard.add(session("RECOVERABLE", "run-$i"))
        assertFails { guard.add(session("PAUSED", "run-over-limit")) }
        assertFails { guard.finish() }
        val modern = RecordingArchiveRows(3)
        for (i in 0..1000) modern.add(session("RECOVERABLE", "run-$i"))
        val snapshot = modern.finish()
        assertEquals(1001, snapshot.sessions.size)
        assertFails { (snapshot.sessions as MutableMap).clear() }
    }
}
