package org.racedaydreams.coach.documents

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppleGpxDecoderTest {
    private val payload =
        """
        <gpx xmlns="http://www.topografix.com/GPX/1/1"><trk>
          <trkseg><trkpt lat="51.5" lon="-0.1"><time>2026-11-17T12:00:00.123456789Z</time></trkpt></trkseg>
          <trkseg><trkpt lat="51.6" lon="-0.2"><ele>10.5</ele><time>2026-11-17T12:00:10Z</time></trkpt></trkseg>
        </trk></gpx>
        """
            .trimIndent()

    @Test
    fun readsNamespaceSegmentsAndExactTimestamps() {
        val segments = AppleGpxDecoder.decode(payload.encodeToByteArray())
        assertEquals(listOf(1, 1), segments.map { it.size })
        assertEquals(123456789, segments[0][0].time().getNano())
        assertEquals(10.5, segments[1][0].altitudeMetres())
    }

    @Test
    fun rejectsDoctypeMissingTimeAndInvalidCoordinates() {
        for (invalid in
            listOf(
                "<!DOCTYPE gpx [<!ENTITY test 'data'>]>$payload",
                payload.replace("<time>2026-11-17T12:00:10Z</time>", ""),
                payload.replace("51.6", "91.0"),
            )) {
            assertFailsWith<IllegalArgumentException> {
                AppleGpxDecoder.decode(invalid.encodeToByteArray())
            }
        }
    }
}
