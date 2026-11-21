package org.racedaydreams.domain.recording

import kotlin.test.*
import org.racedaydreams.domain.polish.RoutePresentation
import org.racedaydreams.domain.polish.RouteRenderGeometry
import org.racedaydreams.domain.time.*

/**
 * The live route reads only newly saved points, yet always yields exactly the route a full read
 * would: same order, gaps and segments, and therefore the same projection.
 */
class IncrementalRoutePointsTest {
    /** Append-only staging like RecordingStore: gap-free sequences per session. */
    private class Staging {
        val sessions = mutableMapOf<String, MutableList<RunMetricsEngine.Point>>()
        var reads = 0
        var failNext = false

        fun full(id: String): List<RunMetricsEngine.Point> = sessions[id].orEmpty().toList()

        fun from(id: String, from: Int): List<RunMetricsEngine.Point> {
            reads++
            if (failNext) {
                failNext = false
                error("database is locked")
            }
            val all = sessions[id].orEmpty()
            check(from <= all.size) { "Staged route sequence gap" }
            return all.subList(from, all.size).toList()
        }
    }

    /** A 1 Hz loop with a GPS gap every 600 s and a new segment after each gap. */
    private fun point(second: Int): RunMetricsEngine.Point {
        val gap = second > 0 && second % 600 == 0
        val metres = second * 2.9
        val angle = second / 400.0
        return RunMetricsEngine.Point(
            RunMetricsEngine.Fix(
                Instants.ofEpochSecond(1_794_787_200L + second),
                second * 1_000L,
                51.5 + 0.01 * kotlin.math.sin(angle),
                -0.05 + 0.015 * kotlin.math.cos(angle),
                4.0,
                12.0,
                5.0,
                2.9,
            ),
            metres,
            second * 1_000L,
            second / 600,
            gap,
            if (second % 7 == 0) null else 150.0,
            172.0,
        )
    }

    private fun projection(points: List<RunMetricsEngine.Point>) =
        RoutePresentation.fit(points).let { xy ->
            xy to RouteRenderGeometry.prepare(points, xy, .0015)
        }

    /**
     * Records for [seconds], flushing every 5 s, while the route is refreshed every 5 s. Returns
     * (points decoded by full reloads, points decoded incrementally).
     */
    private fun simulate(seconds: Int, checkEvery: Int = 60): Pair<Long, Long> {
        val staging = Staging()
        val route = IncrementalRoutePoints(staging::from)
        val stored = mutableListOf<RunMetricsEngine.Point>()
        staging.sessions["run"] = stored
        var fullDecoded = 0L
        var refreshes = 0
        for (second in 0 until seconds) {
            stored += point(second)
            if (second % 5 != 4) continue
            val incremental = route.refresh("run")
            val full = staging.full("run")
            fullDecoded += full.size
            refreshes++
            assertNotNull(incremental, "new points were saved since the last refresh")
            assertEquals(full, incremental)
            if (refreshes % checkEvery == 0) assertEquals(projection(full), projection(incremental))
        }
        // A refresh with nothing new saved does no projection work.
        assertNull(route.refresh("run"))
        return fullDecoded to route.decoded
    }

    @Test
    fun shortAndLongSimulatedRunsMatchAFullReadAndDecodeEachPointOnce() {
        // 5K, 10K, half-marathon and marathon recordings at 1 Hz.
        for ((label, seconds) in
            listOf("5K" to 1_800, "10K" to 3_600, "half" to 7_200, "marathon" to 14_400)) {
            val (full, incremental) = simulate(seconds)
            assertEquals(seconds.toLong(), incremental, "$label: every point decoded once")
            val refreshes = seconds / 5L
            assertEquals(5 * refreshes * (refreshes + 1) / 2, full, "$label: full reloads")
        }
    }

    @Test
    fun gapsSegmentsAndOrderArePreserved() {
        val staging = Staging()
        val stored = (0 until 1_300).map(::point).toMutableList()
        staging.sessions["run"] = stored
        val route = IncrementalRoutePoints(staging::from)
        val first = assertNotNull(route.refresh("run"))
        stored += (1_300 until 1_900).map(::point)
        val second = assertNotNull(route.refresh("run"))
        assertEquals(staging.full("run"), second)
        assertEquals(1_900, second.size)
        assertEquals(
            listOf(600, 1_200, 1_800),
            second.indices.filter { second[it].discontinuity() },
        )
        assertEquals((0 until 1_900).map { it * 1_000L }, second.map { it.fix().monotonicMillis() })
        assertEquals(first, second.take(1_300))
    }

    @Test
    fun reopeningTheRouteAfterRotationOrProcessRecoveryStartsFromAFullRead() {
        val staging = Staging()
        staging.sessions["run"] = (0 until 500).map(::point).toMutableList()
        // A new view (rotation, reopening, or a restarted process) has no held points.
        val reopened = IncrementalRoutePoints(staging::from)
        assertEquals(staging.full("run"), reopened.refresh("run"))
        assertEquals(500L, reopened.decoded)
    }

    @Test
    fun aDifferentRecordingReplacesTheHeldRoute() {
        val staging = Staging()
        staging.sessions["first"] = (0 until 300).map(::point).toMutableList()
        staging.sessions["second"] = (0 until 40).map(::point).toMutableList()
        val route = IncrementalRoutePoints(staging::from)
        assertEquals(300, route.refresh("first")?.size)
        assertEquals(staging.full("second"), route.refresh("second"))
        // Recording finished or discarded: the route is cleared once.
        assertEquals(emptyList(), route.refresh(null))
        assertNull(route.refresh(null))
        assertEquals(staging.full("second"), route.refresh("second"))
    }

    @Test
    fun withNoActiveRecordingTheFirstAnswerIsAnEmptyRoute() {
        val route = IncrementalRoutePoints { _, _ -> error("not read without a session") }
        assertEquals(emptyList(), route.refresh(null))
        assertNull(route.refresh(null))
    }

    @Test
    fun aReadFailureIsReportedAndTheNextRefreshRebuildsFromAFullRead() {
        val staging = Staging()
        val stored = (0 until 200).map(::point).toMutableList()
        staging.sessions["run"] = stored
        val route = IncrementalRoutePoints(staging::from)
        route.refresh("run")
        stored += (200 until 260).map(::point)
        staging.failNext = true
        assertFailsWith<IllegalStateException> { route.refresh("run") }
        assertEquals(staging.full("run"), route.refresh("run"))
        // The recording itself is untouched by reads, failed or not.
        assertEquals((0 until 260).map(::point), stored)
    }

    @Test
    fun aStorageGapIsNeverHidden() {
        val route = IncrementalRoutePoints { _, from ->
            if (from == 0) (0 until 10).map(::point) else error("Staged route sequence gap")
        }
        route.refresh("run")
        assertFailsWith<IllegalStateException> { route.refresh("run") }
    }
}
