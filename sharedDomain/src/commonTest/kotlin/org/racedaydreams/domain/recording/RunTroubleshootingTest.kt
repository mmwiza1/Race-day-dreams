package org.racedaydreams.domain.recording

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.racedaydreams.domain.time.Instants

class RunTroubleshootingTest {
    @Test
    fun capturesActualQualificationAndMissingAcknowledgement() {
        val start = 1000L
        val wall = Instants.ofEpochSecond(1_000)
        val engine = RunMetricsEngine(start, wall)
        val trace = RunTroubleshooting(start)
        fun fix(observed: Long, received: Long, accuracy: Double) {
            val point = RunMetricsEngine.Fix(wall, observed, 51.0, 0.0, accuracy, null, null, null)
            val valid = engine.qualifyDelivery(point, received, wall)
            val accepted = valid && engine.accept(point)
            trace.fix(received, observed, accepted, if (valid) null else engine.deliveryRejection())
            trace.status(engine.status(received).gps(), received)
        }
        trace.callback(2_000, 1, 2_000)
        fix(2_000, 2_000, 5.0)
        engine.missingFix(18_000, wall)
        trace.status(engine.status(18_000).gps(), 18_000)
        trace.callback(32_000, 3, 3_000)
        fix(3_000, 32_000, 5.0)
        fix(4_000, 32_000, 5.0)
        fix(32_000, 32_000, 5.0)
        trace.callback(33_000, 1, 33_000)
        fix(33_000, 33_000, 80.0)
        trace.flushFailed(34_000)
        trace.event("service_destroyed_with_unfinished_run", 34_000)
        val snapshot = trace.snapshot(34_000, engine.pointCount(), 1)
        assertEquals(3, snapshot.callbacks)
        assertEquals(1, snapshot.batchedCallbacks)
        assertEquals(5, snapshot.deliveredFixes)
        assertEquals(5, snapshot.callbackObservations)
        assertEquals(2, snapshot.lateFixes)
        assertEquals(2, snapshot.acceptedFixes)
        assertEquals(mapOf("GPS_FIX_STALE" to 2, "GPS_FIX_ACCURACY" to 1), snapshot.rejected)
        assertEquals(29_000L, snapshot.maximumReceiptDelayMillis)
        assertEquals(30_000L, snapshot.longestCallbackGapMillis)
        assertEquals(2, snapshot.pointCount)
        assertEquals(1, snapshot.acknowledgedPointCount)
        assertEquals(1, snapshot.flushFailures)
        assertTrue(snapshot.checkpoints.any { it.kind == "gps_lost" })
        assertTrue(snapshot.checkpoints.any { it.kind == "gps_ready" })
        assertTrue(snapshot.checkpoints.any { it.kind == "gps_returned" })
        assertTrue(snapshot.checkpoints.any { it.kind == "service_destroyed_with_unfinished_run" })
    }

    @Test
    fun boundsEventsAndRetainsTotalsAndStartup() {
        val trace = RunTroubleshooting(0)
        trace.event("service_started", 0)
        for (i in 1..500) trace.event("status", i.toLong())
        val snapshot = trace.snapshot(501, 0, 0)
        assertEquals(0, snapshot.callbacks)
        assertEquals(501L, snapshot.longestCallbackGapMillis)
        assertEquals(256, snapshot.checkpoints.size)
        assertEquals(245, snapshot.omittedEvents)
        assertEquals("service_started", snapshot.checkpoints.first().kind)
        assertEquals(500L, snapshot.checkpoints.last().elapsedMillis)
        assertFalse(snapshot.checkpoints.any { it.elapsedMillis == 100L })
    }
}
