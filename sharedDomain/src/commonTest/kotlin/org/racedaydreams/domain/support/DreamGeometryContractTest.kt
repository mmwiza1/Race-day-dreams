package org.racedaydreams.domain.support

import kotlin.test.*
import org.racedaydreams.domain.presentation.*

class DreamGeometryContractTest {
    @Test
    fun allRetainedGeographicVerticesKeepTheirOriginalBitPatternsAndCount() {
        val expected =
            listOf(
                Triple("berlin-2014-geo-v1", 191, -7157157604657766814L),
                Triple("tokyo-2026-geo-v1", 91, 140492971489249038L),
                Triple("london-historical-geo-v1", 164, 5555139995962662881L),
                Triple("new-york-2026-geo-v1", 371, -4888557259426257759L),
                Triple("paris-2024-olympic-geo-v1", 328, 5461873015770530807L),
                Triple("paris-2026-geo-v1", 1960, -2554359959045750037L),
            )
        for ((id, count, fingerprint) in expected) {
            val vertices = DreamRoutes.forId(id).vertices()
            var actual = 0L
            for (v in vertices) {
                actual = 31 * actual + v.x().toBits()
                actual = 31 * actual + v.y().toBits()
            }
            assertEquals(count, vertices.size, id)
            assertEquals(fingerprint, actual, id)
        }
    }

    @Test
    fun savedHistoricalIdsStayDistinctAndRegistryObjectsAreCached() {
        assertEquals(
            listOf("london-v2", "berlin-v2", "paris-v2", "tokyo-v2", "new-york-v2"),
            DreamRoutes.routes().map { it.id() },
        )
        val legacy = DreamRoutes.forId("paris-v2")
        val historical = DreamRoutes.forId("paris-2024-olympic-geo-v1")
        assertSame(legacy, DreamRoutes.forId("paris-v2"))
        assertSame(historical, DreamRoutes.forId(historical.id()))
        assertEquals("paris-2026-geo-v1", DreamRoutes.currentGeometry(legacy).id())
        assertSame(historical, DreamRoutes.currentGeometry(historical))
        assertEquals("paris-v2", legacy.id())
        assertTrue(historical.name().contains("2024"))
    }

    @Test
    fun clippedSectionsRetainInterveningOriginalCorners() {
        val corner = DreamRoutes.Vertex(5.0, 0.0)
        val route =
            DreamRoutes.Route(
                "test",
                "Test",
                listOf(DreamRoutes.Vertex(0.0, 0.0), corner, DreamRoutes.Vertex(5.0, 5.0)),
            )
        assertEquals(corner, DreamRoutes.at(route, 21097.5))
        assertEquals(route.vertices(), DreamRoutes.section(route, 0.0, 42195.0))
        val section = DreamRoutes.section(route, 10000.0, 31000.0)
        assertEquals(3, section.size)
        assertEquals(corner, section[1])
        assertEquals(1, DreamRoutes.section(route, 10000.0, 10000.0).size)
        assertEquals(listOf(route.vertices().last()), DreamRoutes.section(route, 50000.0, 60000.0))
    }

    @Test
    fun constructorSnapshotsProtectHistoricGeometryFromCallerEdits() {
        val original = mutableListOf(DreamRoutes.Vertex(0.0, 0.0), DreamRoutes.Vertex(1.0, 1.0))
        val route = DreamRoutes.Route("test", "Test", original)
        original.clear()
        assertEquals(2, route.vertices().size)
        assertEquals("Vertex[x=1.0, y=2.0]", DreamRoutes.Vertex(1.0, 2.0).toString())
        val raw = DreamRoutes.Landmark(null, Double.NaN, -1.0, null)
        assertNull(raw.label())
        assertNull(raw.shape())
        assertTrue(raw.x().isNaN())
    }

    @Test
    fun invalidGeometryAndUnsupportedAssignmentsFailWithoutFabricatingAPath() {
        val point = DreamRoutes.Vertex(1.0, 1.0)
        assertFailsWith<IllegalArgumentException> {
            DreamRoutes.Route("test", "Test", listOf(point))
        }
        assertFailsWith<IllegalArgumentException> {
            DreamRoutes.Route("test", "Test", listOf(point, point))
        }
        assertFailsWith<IllegalArgumentException> {
            DreamRoutes.Route("test", "Test", listOf(point, DreamRoutes.Vertex(Double.NaN, 0.0)))
        }
        assertFailsWith<IllegalArgumentException> { DreamRoutes.forId("unknown") }
        assertFailsWith<IllegalArgumentException> { DreamRoutes.Assignment("unknown", 0.0, 5000.0) }
        assertFailsWith<IllegalArgumentException> {
            DreamRoutes.Assignment("london", 40000.0, 5000.0)
        }
        assertFailsWith<IllegalArgumentException> {
            DreamRoutes.assign(5000.0, DreamRandoms.seeded(0), emptySet())
        }
        assertFailsWith<IllegalArgumentException> {
            DreamRoutes.section(DreamRoutes.forId("london"), 1.0, 0.0)
        }
    }

    @Test
    fun seededAssignmentsKeepOriginalCityOrderAndRandomConsumption() {
        val cases =
            listOf(
                Triple(0L, "london-historical-geo-v1", 30925.44757793474),
                Triple(1L, "london-historical-geo-v1", 3737.1012812548256),
                Triple(-1L, "tokyo-2026-geo-v1", 16349.50888635787),
                Triple(Long.MIN_VALUE, "london-historical-geo-v1", 30925.44757793474),
                Triple(Long.MAX_VALUE, "tokyo-2026-geo-v1", 16349.50888635787),
            )
        for ((seed, id, start) in cases) {
            val a = DreamRoutes.assign(5000.0, DreamRandoms.seeded(seed))
            assertEquals(id, a.routeId())
            assertEquals(start, a.startMetres())
            assertEquals(5000.0, a.coveredMetres())
        }
    }

    @Test
    fun actualRetainedMetresAndExtraDistanceStaySeparateFromRoundedLabels() {
        val assignment = DreamRoutes.Assignment("london", 0.0, 50000.0)
        val progress = DreamRouteProgress.of(assignment)
        assertEquals(42195.0, progress.highlightedMetres())
        assertEquals(7805.0, progress.extraMetres())
        assertEquals(1.0, progress.fraction())
        assertEquals(100, progress.percentage())
        assertEquals("42.20 km / 42.20 km", progress.distanceLabel())
        assertEquals("0.00 km", progress.remainingDistanceLabel())
        assertTrue(
            progress
                .equivalenceStatement(" London ")
                .contains("7.81 km beyond one marathon equivalent")
        )
        assertTrue(progress.accessibilitySummary("London").endsWith("not your physical location."))
        val shorter = DreamRouteProgress.of(DreamRoutes.Assignment("london", 0.0, 5000.0))
        assertEquals(37195.0, shorter.remainingMetres())
        assertEquals("37.20 km", shorter.remainingDistanceLabel())
    }

    @Test
    fun courseNamesAreRequiredAndNeverInventedFromMissingAssignments() {
        val progress =
            DreamRouteProgress.of(DreamRoutes.Assignment("paris-2024-olympic-geo-v1", 0.0, 5000.0))
        assertFailsWith<IllegalArgumentException> { progress.equivalenceStatement(null) }
        assertFailsWith<IllegalArgumentException> { progress.accessibilitySummary(" ") }
        assertTrue(
            progress
                .equivalenceStatement(DreamRoutes.forId("paris-2024-olympic-geo-v1").name())
                .contains("2024 Olympic")
        )
    }

    @Test
    fun geographicContextRetainsItsSourceAttributionAndNeverClaimsNavigation() {
        for (city in DreamRoutes.routes()) {
            val route = DreamRoutes.currentGeometry(city)
            assertEquals(3, DreamCartography.landmarks(route.id()).size)
            assertTrue(DreamRoutes.attribution(route).contains("OpenStreetMap"))
            assertTrue(DreamRoutes.provenance(route).contains("not", ignoreCase = true))
        }
        assertTrue(
            DreamRoutes.provenance(DreamRoutes.forId("paris-2024-olympic-geo-v1"))
                .contains("Thomas Krakow")
        )
        assertEquals(emptyList(), DreamCartography.landmarks("unknown"))
        assertEquals(emptyList(), DreamCartography.rivers("unknown"))
    }

    @Test
    fun fixedDecimalLabelsKeepHalfUpRoundingAndNegativeZeroAcrossFormats() {
        assertEquals("42.20", fixedDecimalText(42.195, 2))
        assertEquals("1.01", fixedDecimalText(1.005, 2))
        assertEquals("0.01", fixedDecimalText(0.005, 2))
        assertEquals("100.00", fixedDecimalText(99.999, 2))
        assertEquals("-0.00", fixedDecimalText(-0.0, 2))
        assertEquals("-0.0", fixedDecimalText(-0.0, 1))
        assertEquals("-0", fixedDecimalText(-0.0, 0))
        assertEquals("NaN", fixedDecimalText(Double.NaN, 2))
    }
}
