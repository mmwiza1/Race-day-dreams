package org.racedaydreams.domain.polish

import kotlin.test.*
import org.racedaydreams.domain.recording.RunMetricsEngine
import org.racedaydreams.domain.time.Instants

class RouteRenderingContractTest {
    private fun point(
        i: Int,
        metres: Double = i.toDouble(),
        segment: Int = 1,
        gap: Boolean = false,
        altitude: Double? = null,
        verticalAccuracy: Double? = null,
    ): RunMetricsEngine.Point {
        val fix =
            RunMetricsEngine.Fix(
                Instants.ofEpochSecond(i.toLong()),
                i * 1000L,
                51.0,
                -.1 + i * .00001,
                3.0,
                altitude,
                verticalAccuracy,
                3.0,
            )
        return RunMetricsEngine.Point(fix, metres, i * 1000L, segment, gap, null, null)
    }

    @Test
    fun projectionPreservesNorthUpBoundsSymmetryAndAllOriginalObservations() {
        val points = listOf(point(0), point(10), point(20))
        val original = points.toList()
        val xy = RoutePresentation.fit(points)
        assertEquals(points.size, xy.size)
        assertTrue(xy.first().x() < xy.last().x())
        assertEquals(.5, xy[1].x(), 1e-8)
        assertEquals(.5, xy[1].y(), 1e-8)
        assertEquals(.84, xy.last().x() - xy.first().x(), 1e-8)
        assertTrue(xy.all { it.x() in 0.079999..0.920001 && it.y() in 0.079999..0.920001 })
        assertEquals(original, points)
        assertFails { (xy as MutableList).clear() }
    }

    @Test
    fun emptyAndSinglePointProjectionNeverCreatesARouteLine() {
        assertTrue(RoutePresentation.fit(emptyList()).isEmpty())
        val points = listOf(point(0))
        val xy = RoutePresentation.fit(points)
        assertEquals(listOf(RoutePresentation.XY(.5, .5)), xy)
        assertTrue(RouteRenderGeometry.prepare(points, xy, 0.0).isEmpty())
        assertTrue(RouteRenderGeometry.prepare(emptyList(), emptyList(), .0015).isEmpty())
    }

    @Test
    fun kilometresInterpolateActiveTimeAndKeepIncompleteSectionsOutOfTheStory() {
        val points =
            listOf(
                point(0, 0.0),
                point(300, 1000.0),
                point(600, 2000.0),
                point(900, 3000.0),
                point(1200, 4000.0),
            )
        val km = RoutePresentation.kilometres(points)
        assertEquals(listOf(1, 2, 3, 4), km.map { it.kilometre() })
        assertEquals(listOf(300.0, 300.0, 300.0, 300.0), km.map { it.secondsPerKm() })
        val story = RoutePresentation.story(points)
        assertEquals("Fastest complete kilometre: km 1 at 5:00/km.", story.first())
        assertTrue(story.last().contains("0 sec less"))
        assertTrue(story.last().contains("not fitness"))
        (story as MutableList).clear()
        assertEquals(4, RoutePresentation.kilometres(points).size)
        assertFails { (km as MutableList).clear() }
        val gap = points.toMutableList()
        gap[2] = point(600, 2000.0, 2, true)
        gap[3] = point(900, 3000.0, 2)
        gap[4] = point(1200, 4000.0, 2)
        assertEquals(listOf(1, 3, 4), RoutePresentation.kilometres(gap).map { it.kilometre() })
    }

    @Test
    fun sharpTurnsAndBothSidesOfGapsSurviveBoundedRenderingReduction() {
        val points =
            (0 until 6000).map { point(it, segment = if (it < 3001) 1 else 2, gap = it == 3001) }
        val original = points.toList()
        val xy =
            points.indices.map { RoutePresentation.XY(it / 6000.0, if (it == 1501) .9 else .5) }
        val edges = RouteRenderGeometry.prepare(points, xy, .0015)
        assertTrue(edges.size < 1500)
        assertEquals(0, edges.first().from())
        assertEquals(5999, edges.last().to())
        assertTrue(edges.any { it.to() == 1501 })
        assertTrue(edges.any { it.to() == 3000 })
        assertTrue(edges.any { it.from() == 3001 })
        for (edge in edges) {
            assertTrue(edge.to() > edge.from())
            assertTrue(edge.to() - edge.from() <= 64)
            assertEquals(points[edge.from()].segment(), points[edge.to()].segment())
            for (i in edge.from() + 1..edge.to()) assertFalse(points[i].discontinuity())
        }
        assertEquals(original, points)
        assertFails { (edges as MutableList).clear() }
    }

    @Test
    fun kilometreColourTransitionsStayLocalToTheExactObservationPair() {
        val points = (0..205).map { point(it, it * 10.0) }
        val edges = RouteRenderGeometry.prepare(points, RoutePresentation.fit(points), .0015)
        assertTrue(edges.contains(RouteRenderGeometry.Edge(99, 100)))
        assertTrue(edges.contains(RouteRenderGeometry.Edge(199, 200)))
        assertTrue(
            edges.none { it.from() < 99 && it.to() > 99 || it.from() < 199 && it.to() > 199 }
        )
    }

    @Test
    fun invalidGeometryIsRejectedBeforeDrawingRatherThanChangingEvidence() {
        val points = listOf(point(0), point(1))
        assertFailsWith<IllegalArgumentException> {
            RouteRenderGeometry.prepare(points, emptyList(), .0015)
        }
        for (tolerance in listOf(-.1, Double.NaN, Double.POSITIVE_INFINITY)) assertFailsWith<
            IllegalArgumentException
        > {
            RouteRenderGeometry.prepare(points, RoutePresentation.fit(points), tolerance)
        }
    }

    @Test
    fun altitudeNeedsEnoughKnownAccurateEvidenceAndNeverInventsMissingValues() {
        fun points(known: Int) =
            (0..4).map {
                point(
                    it,
                    altitude = if (it < known) 10.0 else null,
                    verticalAccuracy = if (it < known) 10.0 else null,
                )
            }
        assertTrue(RoutePresentation.reliableAltitude(points(4)))
        assertFalse(RoutePresentation.reliableAltitude(points(3)))
        assertFalse(RoutePresentation.reliableAltitude(points(4).take(4)))
        assertFalse(
            RoutePresentation.reliableAltitude(
                (0..4).map { point(it, altitude = 10.0, verticalAccuracy = 10.1) }
            )
        )
    }

    @Test
    fun zeroToleranceRetainsEveryNonCollinearBendAndPositiveToleranceRemainsBounded() {
        val points = (0..200).map { point(it) }
        val xy =
            points.indices.map { RoutePresentation.XY(it / 200.0, if (it % 2 == 0) .4 else .6) }
        val exact = RouteRenderGeometry.prepare(points, xy, 0.0)
        assertEquals(200, exact.size)
        assertTrue(exact.all { it.to() == it.from() + 1 })
        val reduced = RouteRenderGeometry.prepare(points, xy, .3)
        assertTrue(reduced.size < exact.size)
        assertTrue(reduced.all { it.to() - it.from() <= 64 })
    }
}
