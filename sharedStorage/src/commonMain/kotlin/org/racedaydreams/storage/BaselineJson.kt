package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.baseline.*
import org.racedaydreams.storage.StoredJson.number
import org.racedaydreams.storage.StoredJson.required
import org.racedaydreams.storage.StoredJson.string
import org.racedaydreams.storage.StoredJson.text

/**
 * Existing baseline/assessment payloads, retaining optional answers and actual observation time.
 */
object BaselineJson {
    fun encode(baseline: ManualBaselineInput): String =
        buildJsonObject {
                put("weeklyDistanceKm", StoredJson.nullable(baseline.weeklyDistanceKm()))
                put("runsPerWeek", StoredJson.nullable(baseline.runsPerWeek()))
                put("longestRunKm", StoredJson.nullable(baseline.longestRunKm()))
                put("recent5k", StoredJson.nullable(baseline.recent5k()?.toString()))
                put("recent10k", StoredJson.nullable(baseline.recent10k()?.toString()))
                put("recentHalf", StoredJson.nullable(baseline.recentHalf()?.toString()))
                put("easyPaceSecondsPerKm", StoredJson.nullable(baseline.easyPaceSecondsPerKm()))
                put(
                    "consistentWeeks",
                    baseline.consistentWeeks()?.let { JsonPrimitive(it) } ?: JsonNull,
                )
            }
            .toString()

    fun decode(text: String): ManualBaselineInput {
        val row = StoredJson.objectValue(text)
        return ManualBaselineInput(
            row.number("weeklyDistanceKm"),
            row.number("runsPerWeek"),
            row.number("longestRunKm"),
            row.text("recent5k")?.let { StoredDuration.parse(it) },
            row.text("recent10k")?.let { StoredDuration.parse(it) },
            row.text("recentHalf")?.let { StoredDuration.parse(it) },
            row.number("easyPaceSecondsPerKm"),
            row.integer("consistentWeeks"),
        )
    }

    fun encodeAssessment(assessment: AssessmentResult): String =
        buildJsonObject {
                put("id", assessment.id())
                put("activityId", StoredJson.nullable(assessment.activityId()))
                put("distanceKm", assessment.assessment().distanceKm())
                put("mode", assessment.assessment().mode().name)
                put("duration", assessment.duration().toString())
                put("rpe", StoredJson.nullable(assessment.rpe()))
                put("completedAt", assessment.completedAt().toString())
            }
            .toString()

    fun decodeAssessment(text: String): AssessmentResult {
        val row = StoredJson.objectValue(text)
        return AssessmentResult(
            row.required("id"),
            BaselineAssessment(
                row.integer("distanceKm")
                    ?: throw IllegalArgumentException("Missing assessment distance."),
                AssessmentMode.valueOf(row.string("mode")),
            ),
            StoredDuration.parse(row.string("duration")),
            row.number("rpe"),
            StoredInstant.parse(row.string("completedAt")),
            row.text("activityId"),
        )
    }

    private fun JsonObject.integer(key: String): Int? {
        val number = number(key) ?: return null
        require(
            number in Int.MIN_VALUE.toDouble()..Int.MAX_VALUE.toDouble() &&
                number == number.toInt().toDouble()
        ) {
            "Expected whole number for $key."
        }
        return number.toInt()
    }
}
