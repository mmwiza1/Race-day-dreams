package org.racedaydreams.storage

import kotlin.jvm.JvmStatic
import kotlinx.serialization.json.*
import org.racedaydreams.domain.*
import org.racedaydreams.storage.StoredJson.array
import org.racedaydreams.storage.StoredJson.integer
import org.racedaydreams.storage.StoredJson.number
import org.racedaydreams.storage.StoredJson.required
import org.racedaydreams.storage.StoredJson.row
import org.racedaydreams.storage.StoredJson.string
import org.racedaydreams.storage.StoredJson.text

/** Existing prescription payload. No prescribed value becomes recorded activity. */
object PlannedWorkoutJson {
    fun encode(workout: PlannedWorkout): String = element(workout).toString()

    internal fun element(workout: PlannedWorkout): JsonObject = buildJsonObject {
        put("id", workout.id())
        put("date", workout.date().toString())
        put("category", workout.category().name)
        put("distanceKm", StoredJson.nullable(workout.distanceKm()))
        put("duration", StoredJson.nullable(workout.duration()?.toString()))
        put(
            "segments",
            buildJsonArray {
                for (segment in workout.segments()) add(
                    buildJsonObject {
                        put("kind", segment.kind().name)
                        put("distanceKm", StoredJson.nullable(segment.distanceKm()))
                        put("duration", StoredJson.nullable(segment.duration()?.toString()))
                        put(
                            "minimumSecondsPerKm",
                            StoredJson.nullable(segment.minimumSecondsPerKm()),
                        )
                        put(
                            "maximumSecondsPerKm",
                            StoredJson.nullable(segment.maximumSecondsPerKm()),
                        )
                        put(
                            "heartRateZone",
                            segment.heartRateZone()?.let { JsonPrimitive(it) } ?: JsonNull,
                        )
                        put("rpe", StoredJson.nullable(segment.rpe()))
                        put("guidance", StoredJson.nullable(segment.guidance()))
                    }
                )
            },
        )
        put("note", StoredJson.nullable(workout.note()))
    }

    fun decode(text: String): PlannedWorkout = read(StoredJson.objectValue(text))

    internal fun read(row: JsonObject): PlannedWorkout =
        PlannedWorkout(
            row.required("id"),
            StoredDate.parse(row.string("date")),
            WorkoutCategory.valueOf(row.string("category")),
            row.number("distanceKm"),
            row.text("duration")?.let { StoredDuration.parse(it) },
            row.array("segments", 1000).map { element ->
                val segment = element.row()
                WorkoutSegment(
                    WorkoutSegment.Kind.valueOf(segment.string("kind")),
                    segment.number("distanceKm"),
                    segment.text("duration")?.let { StoredDuration.parse(it) },
                    segment.number("minimumSecondsPerKm"),
                    segment.number("maximumSecondsPerKm"),
                    segment.integer("heartRateZone"),
                    segment.number("rpe"),
                    segment.text("guidance"),
                )
            },
            row.text("note"),
        )

    /** JSON number spelling/key ordering must not create a new reminder revision on migration. */
    @JvmStatic
    fun samePrescription(a: PlannedWorkout, b: PlannedWorkout): Boolean =
        normalized(a) == normalized(b)

    private fun zero(value: Double?): Double? = value?.let { if (it == 0.0) 0.0 else it }

    private fun normalized(workout: PlannedWorkout): PlannedWorkout =
        workout.copy(
            distanceKm = zero(workout.distanceKm()),
            segments =
                workout.segments().map {
                    it.copy(
                        distanceKm = zero(it.distanceKm()),
                        minimumSecondsPerKm = zero(it.minimumSecondsPerKm()),
                        maximumSecondsPerKm = zero(it.maximumSecondsPerKm()),
                        rpe = zero(it.rpe()),
                    )
                },
        )
}
