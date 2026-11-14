package org.racedaydreams.domain.recording

import kotlin.test.*
import org.racedaydreams.domain.time.*

class RecordingMetricsContractTest {
    private fun wall(t: Long) = Instants.ofEpochSecond(t / 1000, t % 1000 * 1000000)

    private fun fix(t: Long, metres: Double, accuracy: Double? = 3.0, speed: Double? = 3.0) =
        RunMetricsEngine.Fix(
            wall(t),
            t,
            0.0,
            metres / 111194.92664455874,
            accuracy,
            12.0,
            4.0,
            speed,
        )

    private fun engine() = RunMetricsEngine(0, wall(0))

    @Test
    fun cleanMovementKeepsQualifiedPaceAndExactRetainedEvidence() {
        val e = engine()
        for (i in 0..60) assertTrue(e.accept(fix(i * 1000L, i * 3.0)))
        val s = e.status(60000)
        assertEquals(180.0, s.metres(), 1e-7)
        assertEquals(60000L, s.elapsedMillis())
        assertEquals(60000L, s.movingMillis())
        assertEquals(1000.0 / 3, s.secondsPerKm()!!, 1e-7)
        assertTrue(s.hasReliablePace())
        assertEquals(61, e.pointCount())
        assertEquals(
            e.points().map { it.cumulativeMetres() },
            e.samples().map { it.distanceMetres() },
        )
        assertEquals(listOf("START", "GPS_RETURNED"), e.events().map { it.type() })
    }

    @Test
    fun receiptQualificationRejectsStaleFutureAndDuplicateFixesWithoutRetainingCoordinates() {
        val e = engine()
        assertTrue(e.qualifyDelivery(fix(0, 0.0), 0, wall(0)))
        assertTrue(e.accept(fix(0, 0.0)))
        assertFalse(e.qualifyDelivery(fix(0, 9999.0), 1000, wall(1000)))
        assertFalse(e.qualifyDelivery(fix(0, 9999.0), 2000, wall(2000)))
        assertEquals(1, e.events().count { it.type() == "GPS_FIX_ORDER" })
        assertFalse(e.qualifyDelivery(fix(5000, 15.0), 3000, wall(3000)))
        assertFalse(e.qualifyDelivery(fix(1000, 3.0), 12000, wall(12000)))
        assertEquals(
            listOf("GPS_FIX_ORDER", "GPS_FIX_FUTURE", "GPS_FIX_STALE"),
            e.events().drop(2).map { it.type() },
        )
        assertEquals(1, e.pointCount())
        assertFailsWith<IllegalArgumentException> {
            e.qualifyDelivery(fix(10000, 30.0), 11000, wall(11000))
        }
    }

    @Test
    fun unknownAccuracyRemainsUsableForHistoricalImportsButNotLiveQualification() {
        val historical = engine()
        assertTrue(historical.accept(fix(0, 0.0, null, null)))
        assertTrue(historical.accept(fix(1000, 3.0, null, null)))
        assertEquals(3.0, historical.status(1000).metres(), 1e-8)
        val live = engine()
        assertFalse(live.qualifyDelivery(fix(0, 0.0, null, null), 0, wall(0)))
        assertEquals(0, live.pointCount())
        assertEquals("GPS signal weak", live.status(0).gps())
    }

    @Test
    fun stationaryClustersKeepEveryPointWhileSlowMovementEventuallyLeavesTheCluster() {
        val e = engine()
        for (i in 0..120) assertTrue(
            e.accept(fix(i * 1000L, if (i % 2 == 0) 2.0 else -2.0, speed = 0.0))
        )
        assertEquals(121, e.pointCount())
        assertEquals(0.0, e.status(120000).metres(), 1e-8)
        assertNull(e.status(120000).secondsPerKm())
        val slow = engine()
        for (i in 0..60) slow.accept(fix(i * 1000L, i * .2, speed = .2))
        assertTrue(slow.status(60000).metres() > 8)
        assertTrue(slow.status(60000).metres() <= 12.000001)
    }

    @Test
    fun accurateSpikesAndImpossibleSpeedDoNotPoisonTheNextDistanceAnchor() {
        val e = engine()
        e.accept(fix(0, 0.0))
        e.accept(fix(1000, 3.0))
        assertFalse(e.accept(fix(2000, 5000.0)))
        assertFalse(e.accept(fix(3000, 9.0, speed = 12.1)))
        assertTrue(e.accept(fix(4000, 12.0)))
        assertEquals(12.0, e.status(4000).metres(), 1e-7)
        assertEquals(listOf(0L, 1000L, 4000L), e.points().map { it.fix().monotonicMillis() })
    }

    @Test
    fun pauseAndResumeExcludePausedTimeAndRejectQueuedOldObservations() {
        val e = engine()
        e.accept(fix(0, 0.0))
        e.accept(fix(1000, 3.0))
        e.pause(2000, wall(2000))
        assertFalse(e.accept(fix(5000, 100.0)))
        assertEquals("GPS ready · paused", e.status(5000).gps())
        e.resume(10000, wall(10000))
        assertFalse(e.accept(fix(9000, 100.0)))
        e.accept(fix(10000, 500.0))
        e.accept(fix(11000, 503.0))
        assertEquals(6.0, e.status(11000).metres(), 1e-7)
        assertEquals(3000L, e.status(11000).movingMillis())
        assertTrue(e.points()[2].discontinuity())
        assertEquals(10000L, e.samples()[2].elapsed().toMillis())
    }

    @Test
    fun lossAndRecoveryNeverBridgeUnsupportedDistanceOrInventPace() {
        val e = engine()
        e.accept(fix(0, 0.0))
        e.accept(fix(1000, 3.0))
        e.missingFix(7000, wall(7000))
        assertEquals("GPS signal weak", e.status(7000).gps())
        assertNull(e.status(7000).secondsPerKm())
        e.missingFix(17000, wall(17000))
        assertTrue(e.status(17000).gps()!!.contains("temporarily lost"))
        assertFalse(e.accept(fix(2000, 6.0)))
        e.accept(fix(18000, 500.0))
        assertEquals("GPS recovered", e.status(18000).gps())
        assertEquals(3.0, e.status(18000).metres(), 1e-7)
        assertNull(e.status(18000).secondsPerKm())
        e.accept(fix(19000, 503.0))
        assertEquals(6.0, e.status(19000).metres(), 1e-7)
        assertEquals(1, e.events().count { it.type() == "GPS_LOST" })
        assertEquals(2, e.points().count { it.discontinuity() })
    }

    @Test
    fun sensorsRemainUnknownWhenFutureOrStaleAndTheFreshBoundaryIsInclusive() {
        val e = engine()
        e.heartRate(130.0, 1000)
        e.cadence(170.0, 1000)
        e.accept(fix(0, 0.0))
        e.accept(fix(11000, 33.0))
        e.accept(fix(11001, 33.0))
        assertNull(e.points()[0].heartRate())
        assertEquals(130.0, e.points()[1].heartRate())
        assertEquals(170.0, e.points()[1].cadence())
        assertNull(e.points()[2].heartRate())
        assertNull(e.points()[2].cadence())
        assertFailsWith<IllegalArgumentException> { e.heartRate(251.0, 12000) }
        assertFailsWith<IllegalArgumentException> { e.cadence(Double.NaN, 12000) }
    }

    @Test
    fun durableCursorsAndSnapshotsStayIsolatedAndOriginalSamplesStayMutable() {
        val e = engine()
        e.accept(fix(0, 0.0))
        val points = e.points()
        val events = e.events()
        e.accept(fix(1000, 3.0))
        e.event("USER_MARK", 1000, wall(1000))
        assertEquals(1, points.size)
        assertEquals(2, events.size)
        assertFails { (points as MutableList).clear() }
        assertFails { (events as MutableList).clear() }
        assertEquals(listOf(e.points()[1]), e.pointsFrom(1))
        assertEquals(listOf(e.events()[2]), e.eventsFrom(2))
        assertTrue(e.pointsFrom(e.pointCount()).isEmpty())
        assertFailsWith<IndexOutOfBoundsException> { e.eventsFrom(-1) }
        (e.samples() as MutableList).clear()
        assertEquals(2, e.pointCount())
    }

    @Test
    fun timelinesSortStablyRespectBoundsAndNeverExcludeGpsOutages() {
        fun event(type: String, t: Long) = RunMetricsEngine.Event(type, t, wall(t))
        val rows =
            listOf(
                event("RESUME", 10000),
                event("PAUSE", 2000),
                event("AUTO_PAUSE", 15000),
                event("GPS_LOST", 12000),
                event("GPS_RETURNED", 14000),
            )
        assertEquals(7000L, RecordingTimeline.activeMillis(0, 20000, rows))
        assertTrue(RecordingTimeline.pausedAt(0, 20000, rows))
        assertFalse(RecordingTimeline.pausedAt(0, 14000, rows))
        assertEquals(
            10000L,
            RecordingTimeline.activeMillis(0, 10000, listOf(event("GPS_LOST", 1000))),
        )
        assertFalse(
            RecordingTimeline.pausedAt(
                0,
                10000,
                listOf(event("PAUSE", 1000), event("RESUME", 1000)),
            )
        )
        assertTrue(
            RecordingTimeline.pausedAt(
                0,
                10000,
                listOf(event("RESUME", 1000), event("PAUSE", 1000)),
            )
        )
        assertFailsWith<IllegalArgumentException> { RecordingTimeline.activeMillis(5, 4, rows) }
        assertFailsWith<IllegalArgumentException> { RecordingTimeline.pausedAt(-1, 1, rows) }
        assertFailsWith<NullPointerException> {
            RecordingTimeline.pausedAt(0, 10, listOf(RunMetricsEngine.Event(null, 0, null)))
        }
    }

    @Test
    fun autoPauseRequiresContinuousAccurateEvidenceAndFiveSecondsToResume() {
        val p = AutoPausePolicy()
        for (i in 0..29) assertEquals(
            AutoPausePolicy.Decision.NONE,
            p.observe(fix(i * 1000L, 0.0, speed = 0.0)),
        )
        assertEquals(AutoPausePolicy.Decision.PAUSE, p.observe(fix(30000, 0.0, speed = 0.0)))
        for (i in 31..35) assertEquals(
            AutoPausePolicy.Decision.NONE,
            p.observe(fix(i * 1000L, (i - 30) * 2.0, speed = 2.0)),
        )
        assertEquals(AutoPausePolicy.Decision.RESUME, p.observe(fix(36000, 12.0, speed = 2.0)))
        p.reset()
        for (i in 0..29) p.observe(fix(i * 1000L, 0.0, speed = 0.0))
        assertEquals(AutoPausePolicy.Decision.NONE, p.observe(fix(35000, 0.0, speed = 0.0)))
        assertEquals(AutoPausePolicy.Decision.NONE, p.observe(fix(35000, 0.0, speed = 0.0)))
        assertEquals(AutoPausePolicy.Decision.NONE, p.observe(fix(36000, 0.0, null, null)))
        for (i in 37..66) assertEquals(
            AutoPausePolicy.Decision.NONE,
            p.observe(fix(i * 1000L, 0.0, speed = 0.0)),
        )
        assertEquals(AutoPausePolicy.Decision.PAUSE, p.observe(fix(67000, 0.0, speed = 0.0)))
    }

    @Test
    fun rawRecordsRemainUnvalidatedWhileLocationAndRecordedPointValidationStayStrict() {
        assertNull(RunMetricsEngine.Event(null, -1, null, Double.NaN).timestamp())
        assertFalse(
            RunMetricsEngine.Status(Double.NaN, -1, -1, 300.0, null, false).hasReliablePace()
        )
        for (gps in listOf("GPS available", "GPS ready", "GPS recovered")) assertTrue(
            RunMetricsEngine.Status(0.0, 0, 0, 300.0, gps, false).hasReliablePace()
        )
        assertFalse(
            RunMetricsEngine.Status(0.0, 0, 0, 300.0, "GPS signal weak", false).hasReliablePace()
        )
        assertFalse(RunMetricsEngine.Status(0.0, 0, 0, 300.0, "GPS ready", true).hasReliablePace())
        assertFailsWith<NullPointerException> {
            RunMetricsEngine.Fix(null, 0, 0.0, 0.0, null, null, null, null)
        }
        assertFailsWith<IllegalArgumentException> { fix(0, 0.0, Double.NaN) }
        assertFailsWith<IllegalArgumentException> { fix(0, 0.0, speed = -1.0) }
        assertFailsWith<NullPointerException> {
            RunMetricsEngine.Point(null, 0.0, 0, 0, false, null, null)
        }
    }

    @Test
    fun longRecordingsRetainMonotonicDistanceWithoutUnboundedEventGrowth() {
        val e = engine()
        var last = 0.0
        for (i in 0..10000) {
            assertTrue(e.accept(fix(i * 1000L, i * 3.0)))
            val distance = e.status(i * 1000L).metres()
            assertTrue(distance >= last)
            last = distance
        }
        assertEquals(30000.0, last, .001)
        assertEquals(10001, e.pointCount())
        assertEquals(2, e.eventCount())
        assertEquals(10000000L, e.samples().last().elapsed().toMillis())
    }
}
