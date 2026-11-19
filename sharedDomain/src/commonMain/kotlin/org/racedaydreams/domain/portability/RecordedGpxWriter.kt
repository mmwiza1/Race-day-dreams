package org.racedaydreams.domain.portability

import org.racedaydreams.domain.recording.RunMetricsEngine.Point

/** Writes only recorded coordinates, timestamps and optional elevation. */
object RecordedGpxWriter {
    fun write(points: List<Point>, append: (String) -> Unit) {
        require(points.size >= 2) { "No route samples retained for this recording." }
        append(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><gpx version=\"1.1\" creator=\"Race Day Dreams\" xmlns=\"http://www.topografix.com/GPX/1/1\"><trk><name>Recorded run</name>"
        )
        var segment: Int? = null
        for (point in points) {
            if (segment != point.segment()) {
                if (segment != null) append("</trkseg>")
                append("<trkseg>")
                segment = point.segment()
            }
            val fix = point.fix()
            append("<trkpt lat=\"${fix.latitude()}\" lon=\"${fix.longitude()}\">")
            fix.altitude()?.let { append("<ele>$it</ele>") }
            append("<time>${fix.timestamp()}</time></trkpt>")
        }
        append("</trkseg></trk></gpx>")
    }
}
