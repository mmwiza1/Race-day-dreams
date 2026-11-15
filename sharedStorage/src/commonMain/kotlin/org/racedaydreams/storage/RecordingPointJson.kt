package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.recording.RunMetricsEngine.Fix
import org.racedaydreams.domain.recording.RunMetricsEngine.Point
import org.racedaydreams.storage.StoredJson.bool
import org.racedaydreams.storage.StoredJson.number
import org.racedaydreams.storage.StoredJson.requiredNumber
import org.racedaydreams.storage.StoredJson.string

/** Existing normalized recording-point payload, including the original accepted coordinates. */
object RecordingPointJson {
    fun encode(point: Point): String =
        buildJsonObject {
                val fix = point.fix()
                put("wall", fix.timestamp().toString())
                put("mono", fix.monotonicMillis())
                put("lat", fix.latitude())
                put("lon", fix.longitude())
                put("accuracy", StoredJson.nullable(fix.accuracy()))
                put("alt", StoredJson.nullable(fix.altitude()))
                put("vertical", StoredJson.nullable(fix.verticalAccuracy()))
                put("speed", StoredJson.nullable(fix.speed()))
                put("distance", point.cumulativeMetres())
                put("active", point.activeMillis())
                put("segment", point.segment())
                put("gap", point.discontinuity())
                put("hr", StoredJson.nullable(point.heartRate()))
                put("cadence", StoredJson.nullable(point.cadence()))
            }
            .toString()

    fun decode(text: String): Point {
        val row = StoredJson.objectValue(text)
        val segment = row.long("segment")
        require(segment in 0..Int.MAX_VALUE.toLong()) { "Invalid recording segment." }
        return Point(
            Fix(
                StoredInstant.parse(row.string("wall")),
                row.long("mono"),
                row.requiredNumber("lat"),
                row.requiredNumber("lon"),
                row.number("accuracy"),
                row.number("alt"),
                row.number("vertical"),
                row.number("speed"),
            ),
            row.requiredNumber("distance"),
            row.long("active"),
            segment.toInt(),
            row.bool("gap"),
            row.number("hr"),
            row.number("cadence"),
        )
    }

    private fun JsonObject.long(key: String): Long {
        val value = this[key]
        require(value is JsonPrimitive && !value.isString) { "Expected whole number for $key." }
        return value.longOrNull
            ?: throw IllegalArgumentException("Expected exact whole number for $key.")
    }
}
