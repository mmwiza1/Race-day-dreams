package org.racedaydreams.domain.portability

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.racedaydreams.domain.recording.RunMetricsEngine.Fix
import org.racedaydreams.domain.recording.RunMetricsEngine.Point
import org.racedaydreams.domain.time.Instants

class RecordedGpxWriterTest {
    @Test
    fun retainsExactCoordinatesTimestampsAndSegmentBoundaries() {
        val first =
            Point(
                Fix(Instants.ofEpochSecond(0), 0, 51.5, -0.1, null, null, null, null),
                0.0,
                0,
                0,
                false,
                null,
                null,
            )
        val second =
            Point(
                Fix(Instants.ofEpochSecond(5), 5000, 51.6, -0.2, null, 10.5, null, null),
                0.0,
                5000,
                1,
                true,
                null,
                null,
            )
        val xml = buildString { RecordedGpxWriter.write(listOf(first, second)) { append(it) } }
        assertEquals(
            "<trkseg><trkpt lat=\"51.5\" lon=\"-0.1\"><time>1970-01-01T00:00:00Z</time></trkpt></trkseg>" +
                "<trkseg><trkpt lat=\"51.6\" lon=\"-0.2\"><ele>10.5</ele><time>1970-01-01T00:00:05Z</time></trkpt></trkseg></trk></gpx>",
            xml.substring(xml.indexOf("<trkseg>")),
        )
        assertFailsWith<IllegalArgumentException> {
            RecordedGpxWriter.write(listOf(first)) { error("No document should be started") }
        }
    }
}
