package org.racedaydreams.coach.documents

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.racedaydreams.domain.portability.RecordedGpxWriter
import org.racedaydreams.domain.recording.RunMetricsEngine.Fix
import org.racedaydreams.domain.recording.RunMetricsEngine.Point
import org.racedaydreams.domain.time.Instants

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
    fun keepsEmptySegmentPositionsForStableTrackIdentity() {
        val withEmpty = payload.replace("<trk>", "<trk><trkseg/>")
        assertEquals(
            listOf(0, 1, 1),
            AppleGpxDecoder.decode(withEmpty.encodeToByteArray()).map { it.size },
        )
    }

    @Test
    fun readsProductionExportWithoutFillingGpsGapsOrMissingElevation() {
        val first = Fix(Instants.ofEpochSecond(0), 0, 51.5, -0.1, null, null, null, null)
        val resumed = Fix(Instants.ofEpochSecond(35), 35000, 51.51, -0.11, 3.0, 12.0, 4.0, null)
        val points =
            listOf(
                Point(first, 0.0, 0, 1, true, null, null),
                Point(resumed, 0.0, 35000, 2, true, null, null),
            )
        val output = buildString { RecordedGpxWriter.write(points) { append(it) } }
        val segments = AppleGpxDecoder.decode(output.encodeToByteArray())
        assertEquals(listOf(1, 1), segments.map { it.size })
        val observed = segments.flatten()
        assertEquals(listOf(first.timestamp(), resumed.timestamp()), observed.map { it.time() })
        assertEquals(listOf(51.5, 51.51), observed.map { it.latitude() })
        assertEquals(listOf(-0.1, -0.11), observed.map { it.longitude() })
        assertEquals(listOf(null, 12.0), observed.map { it.altitudeMetres() })
    }

    @Test
    fun rejectsDoctypeMissingTimeAndInvalidCoordinates() {
        for (invalid in
            listOf(
                "<!DOCTYPE gpx [<!ENTITY test 'data'>]>$payload",
                payload.replace("<time>2026-11-17T12:00:10Z</time>", ""),
                payload.replace("51.6", "91.0"),
                payload.replace("http://www.topografix.com/GPX/1/1", "urn:unrelated"),
                payload.replace("<ele>10.5</ele>", "<ele>10<extension/>.5</ele>"),
            )) {
            assertFailsWith<IllegalArgumentException> {
                AppleGpxDecoder.decode(invalid.encodeToByteArray())
            }
        }
    }
}
