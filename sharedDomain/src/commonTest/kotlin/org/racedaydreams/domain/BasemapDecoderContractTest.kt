package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.support.*

class BasemapDecoderContractTest {
    private fun parse(body: String, newline: String = "\n"): DreamBasemap.MapData =
        DreamBasemap.read(
            BasemapInputs.bytes(
                ("RDD-MAP 1" + newline + "test" + newline + body + newline).encodeToByteArray()
            ),
            "test",
        )

    private fun reject(body: String, message: String) {
        val error = assertFailsWith<BasemapReadException> { parse(body) }
        assertEquals(message, error.message)
    }

    @Test
    fun realMixedPathsKeepTheirExactFrameAndIndependentSegments() {
        val map = parse("road 1,2 3,4|5,6 7,8\nwater 0,0 0,1 1,0 0,0\ngreen 2,2 2,3 3,2 2,2")
        assertEquals("test", map.routeId())
        assertEquals(3, map.features().size)
        val road = map.features()[0]
        assertEquals(DreamBasemap.Kind.ROAD, road.kind())
        assertEquals(2, road.paths().size)
        assertEquals(DreamRoutes.Vertex(7.0, 8.0), road.paths()[1][1])
        assertEquals(
            map.features()[1].paths().single().first(),
            map.features()[1].paths().single().last(),
        )
    }

    @Test
    fun lineEndingVariantsPreserveGeometryWithoutCreatingEmptyFeatures() {
        val baseline = parse("road 1,2 3,4")
        assertEquals(baseline, parse("road 1,2 3,4", "\r"))
        assertEquals(baseline, parse("road 1,2 3,4", "\r\n"))
        val noTerminator = "RDD-MAP 1\ntest\nroad 1,2 3,4"
        assertEquals(
            baseline,
            DreamBasemap.read(BasemapInputs.bytes(noTerminator.encodeToByteArray()), "test"),
        )
    }

    @Test
    fun sourceFloatingPointSpellingsRetainActualValuesAndSignedZero() {
        val map =
            parse("ROAD +1.,.2 -3E-2,4e+1\nroad 0x1.0p0,2F 3d,4D\ngreen -0.0,0 0,1 1,0 -0.0,0")
        assertEquals(DreamRoutes.Vertex(1.0, 0.2), map.features()[0].paths()[0][0])
        assertEquals(DreamRoutes.Vertex(-0.03, 40.0), map.features()[0].paths()[0][1])
        assertEquals(DreamRoutes.Vertex(1.0, 2.0), map.features()[1].paths()[0][0])
        assertEquals((-0.0).toBits(), map.features()[2].paths()[0][0].x().toBits())
        reject("water 0,0 0,1 1,0 -0.0,0", "Unclosed basemap polygon")
    }

    @Test
    fun unsupportedFramesAndFormatsAreRejectedBeforeDrawing() {
        for ((text, expected) in
            listOf(
                "RDD-MAP 2\ntest\nroad 1,2 3,4" to "Unsupported basemap format",
                "RDD-MAP 1\nother\nroad 1,2 3,4" to "Basemap/course frame mismatch",
                "RDD-MAP 1" to "Basemap/course frame mismatch",
                "RDD-MAP 1\ntest\n" to "Empty basemap",
            )) {
            assertEquals(
                expected,
                assertFailsWith<BasemapReadException> {
                        DreamBasemap.read(BasemapInputs.bytes(text.encodeToByteArray()), "test")
                    }
                    .message,
            )
        }
    }

    @Test
    fun corruptCoordinatesAndEmptyTokensAreNeverSilentlyDropped() {
        for (body in
            listOf(
                "road NaN,2 3,4",
                "road Infinity,2 3,4",
                "road 100,2 3,4",
                "road 1,2 3,4 ",
                "road  1,2 3,4",
                "road 1,2 3,4|",
                "road \u00a01,2 3,4",
            )) reject(body, "Invalid basemap coordinate")
        reject("unknown 1,2 3,4", "Unknown basemap feature")
        reject("", "Empty basemap feature")
    }

    @Test
    fun incompletePathsAndUnclosedPolygonsRemainInvalid() {
        reject("road 1,2", "Incomplete basemap path")
        reject("water 0,0 0,1 1,0", "Incomplete basemap path")
        reject("green 0,0 0,1 1,0 1,1", "Unclosed basemap polygon")
    }

    @Test
    fun bytePointAndFeatureBudgetsKeepTheirOriginalRejectionBoundaries() {
        reject("road " + "1,2 ".repeat(750001), "Basemap exceeds size limit")
        reject("road " + "1,2 ".repeat(150001), "Too many basemap points")
        reject("road 1,2 3,4\n".repeat(20001), "Invalid basemap feature")
    }

    @Test
    fun rawOptionalMetadataAndDefensivePathSnapshotsArePreserved() {
        val path = mutableListOf(DreamRoutes.Vertex(1.0, 2.0))
        val paths = mutableListOf<List<DreamRoutes.Vertex>>(path)
        val feature = DreamBasemap.Feature(null, paths)
        path.clear()
        paths.clear()
        val features = mutableListOf(feature)
        val map = DreamBasemap.MapData(null, features)
        features.clear()
        assertNull(feature.kind())
        assertNull(map.routeId())
        assertEquals(1, map.features().single().paths().single().size)
    }
}
