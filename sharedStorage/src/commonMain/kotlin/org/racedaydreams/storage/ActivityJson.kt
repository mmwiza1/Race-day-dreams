package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.storage.StoredJson.array
import org.racedaydreams.storage.StoredJson.bool
import org.racedaydreams.storage.StoredJson.number
import org.racedaydreams.storage.StoredJson.required
import org.racedaydreams.storage.StoredJson.requiredNumber
import org.racedaydreams.storage.StoredJson.row
import org.racedaydreams.storage.StoredJson.string
import org.racedaydreams.storage.StoredJson.text

/** Existing activity payload. No route reconstruction, metric recalculation or invented samples. */
object ActivityJson {
    fun encode(activity: ActivityRecord): String =
        buildJsonObject {
                put("id", activity.id())
                put(
                    "source",
                    buildJsonObject {
                        put("provider", activity.source().provider())
                        put("recordId", activity.source().recordId())
                        put("originPackage", StoredJson.nullable(activity.source().originPackage()))
                    },
                )
                put("start", activity.start().toString())
                put("end", activity.end().toString())
                put("startOffset", StoredJson.nullable(activity.startOffset()?.toString()))
                put("endOffset", StoredJson.nullable(activity.endOffset()?.toString()))
                put("distanceMetres", StoredJson.nullable(activity.distanceMetres()))
                put("movingTime", StoredJson.nullable(activity.movingTime()?.toString()))
                put("averageHeartRate", StoredJson.nullable(activity.averageHeartRate()))
                put("maxHeartRate", StoredJson.nullable(activity.maxHeartRate()))
                put("elevationGainMetres", StoredJson.nullable(activity.elevationGainMetres()))
                put("cadence", StoredJson.nullable(activity.cadence()))
                put("rpe", StoredJson.nullable(activity.rpe()))
                put("notes", StoredJson.nullable(activity.notes()))
                put(
                    "samples",
                    buildJsonArray {
                        for (sample in activity.samples()) add(
                            buildJsonObject {
                                put("elapsed", sample.elapsed().toString())
                                put("distanceMetres", sample.distanceMetres())
                                put("heartRate", StoredJson.nullable(sample.heartRate()))
                                put("cadence", StoredJson.nullable(sample.cadence()))
                                put(
                                    "elevationMetres",
                                    StoredJson.nullable(sample.elevationMetres()),
                                )
                                put("discontinuity", sample.discontinuity())
                            }
                        )
                    },
                )
                put(
                    "laps",
                    buildJsonArray {
                        for (lap in activity.laps()) add(
                            buildJsonObject {
                                put("id", lap.id())
                                put("start", lap.start().toString())
                                put("end", lap.end().toString())
                                put("distanceMetres", StoredJson.nullable(lap.distanceMetres()))
                                put("label", StoredJson.nullable(lap.label()))
                                put("plannedSegmentId", StoredJson.nullable(lap.plannedSegmentId()))
                            }
                        )
                    },
                )
                put("plannedWorkoutId", StoredJson.nullable(activity.plannedWorkoutId()))
                put("shoeId", StoredJson.nullable(activity.shoeId()))
            }
            .toString()

    fun decode(text: String): ActivityRecord {
        val row = StoredJson.objectValue(text)
        val source =
            row["source"]?.row() ?: throw IllegalArgumentException("Missing activity source.")
        val samples =
            row.array("samples", 200000).map { element ->
                val sample = element.row()
                DistanceSample(
                    StoredDuration.parse(sample.string("elapsed")),
                    sample.requiredNumber("distanceMetres"),
                    sample.number("heartRate"),
                    sample.number("cadence"),
                    sample.number("elevationMetres"),
                    sample.bool("discontinuity"),
                )
            }
        val laps =
            row.array("laps", 10000).map { element ->
                val lap = element.row()
                ActivityLap(
                    lap.required("id"),
                    StoredInstant.parse(lap.string("start")),
                    StoredInstant.parse(lap.string("end")),
                    lap.number("distanceMetres"),
                    lap.text("label"),
                    lap.text("plannedSegmentId"),
                )
            }
        return ActivityRecord(
            row.required("id"),
            SourceIdentity(
                source.required("provider"),
                source.required("recordId"),
                source.text("originPackage"),
            ),
            StoredInstant.parse(row.string("start")),
            StoredInstant.parse(row.string("end")),
            row.text("startOffset")?.let { StoredOffset.parse(it) },
            row.text("endOffset")?.let { StoredOffset.parse(it) },
            row.number("distanceMetres"),
            row.text("movingTime")?.let { StoredDuration.parse(it) },
            row.number("averageHeartRate"),
            row.number("maxHeartRate"),
            row.number("elevationGainMetres"),
            row.number("cadence"),
            row.number("rpe"),
            row.text("notes"),
            samples,
            laps,
            row.text("plannedWorkoutId"),
            row.text("shoeId"),
        )
    }
}
