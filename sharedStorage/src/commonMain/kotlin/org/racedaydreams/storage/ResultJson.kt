package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.storage.StoredJson.required
import org.racedaydreams.storage.StoredJson.requiredNumber
import org.racedaydreams.storage.StoredJson.row
import org.racedaydreams.storage.StoredJson.string
import org.racedaydreams.storage.StoredJson.text

/** Existing FeatureJson result payload; recorded and rolling result identities stay distinct. */
object ResultJson {
    fun encode(result: RecordedResult): String =
        buildJsonObject {
                put("id", result.id())
                put(
                    "distance",
                    buildJsonObject {
                        put("code", result.distance().code())
                        put("metres", result.distance().metres())
                        put("label", result.distance().label())
                    },
                )
                put("duration", result.duration().toString())
                put("date", result.date().toString())
                put("eventName", StoredJson.nullable(result.eventName()))
                put("source", result.source().name)
                put("activityId", StoredJson.nullable(result.activityId()))
                put("note", StoredJson.nullable(result.note()))
            }
            .toString()

    fun decode(text: String): RecordedResult {
        val row = StoredJson.objectValue(text)
        val distance =
            row["distance"]?.row() ?: throw IllegalArgumentException("Missing result distance.")
        return RecordedResult(
            row.required("id"),
            PerformanceDistance(
                distance.required("code"),
                distance.requiredNumber("metres"),
                distance.required("label"),
            ),
            StoredDuration.parse(row.string("duration")),
            StoredDate.parse(row.string("date")),
            row.text("eventName"),
            ResultSource.valueOf(row.string("source")),
            row.text("activityId"),
            row.text("note"),
        )
    }
}
