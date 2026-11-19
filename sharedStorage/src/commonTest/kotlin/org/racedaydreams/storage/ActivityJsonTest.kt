package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.time.*

class ActivityJsonTest {
    private fun activity(): ActivityRecord {
        val start = StoredInstant.parse("2026-11-15T09:00:00.123456789Z")
        val end = StoredInstant.parse("2026-11-15T09:01:00.123456789Z")
        return ActivityRecord(
            "run",
            SourceIdentity("rdd", "source", "local"),
            start,
            end,
            Offsets.ofTotalSeconds(5445),
            null,
            150.0,
            Durations.ofSeconds(50),
            null,
            null,
            null,
            0.0,
            5.0,
            "retained 🏃\u0000 note",
            listOf(
                DistanceSample(Durations.ofSeconds(0), 0.0, null, null, null, false),
                DistanceSample(Durations.ofSeconds(15, 500000000), 50.0, 128.0, 160.0, null, false),
                DistanceSample(Durations.ofSeconds(40), 50.0, null, null, null, true),
            ),
            listOf(ActivityLap("lap", start, end, 150.0, "recorded lap", "planned-step")),
            "planned-workout",
            "shoe",
        )
    }

    @Test
    fun storedActualsSourcesGapsAndOptionalMetricsSurvive() {
        val original = activity()
        val restored = ActivityJson.decode(ActivityJson.encode(original))
        assertEquals(original, restored)
        assertEquals(
            restored,
            ActivityJson.decode(Json.parseToJsonElement(ActivityJson.encode(original)).jsonObject),
        )
        assertTrue(restored.samples().last().discontinuity())
        assertEquals(150.0, restored.distanceMetres())
        assertEquals(50.0, restored.samples().last().distanceMetres())
        assertNull(restored.averageHeartRate())
        assertNull(restored.endOffset())
        assertEquals(0.0, restored.cadence())
        assertEquals(5445, restored.startOffset()!!.getTotalSeconds())
    }

    @Test
    fun summariesRemainSummariesAndMissingEvidenceIsNotZero() {
        val summary =
            activity()
                .copy(
                    distanceMetres = null,
                    movingTime = null,
                    cadence = null,
                    rpe = null,
                    samples = emptyList(),
                    laps = emptyList(),
                    shoeId = null,
                )
        val restored = ActivityJson.decode(ActivityJson.encode(summary))
        assertEquals(summary, restored)
        assertNull(restored.distanceMetres())
        assertNull(restored.movingTime())
        assertNull(restored.cadence())
        assertTrue(restored.samples().isEmpty())
    }

    @Test
    fun coercionInvalidTimeAndSampleFlagsAreRejected() {
        val base = Json.parseToJsonElement(ActivityJson.encode(activity())).jsonObject
        val wrongDistance = JsonObject(base + ("distanceMetres" to JsonPrimitive("150")))
        assertFailsWith<IllegalArgumentException> { ActivityJson.decode(wrongDistance.toString()) }
        val wrongTime = JsonObject(base + ("movingTime" to JsonPrimitive("PT-1S")))
        assertFailsWith<IllegalArgumentException> { ActivityJson.decode(wrongTime.toString()) }
        val sample =
            JsonObject(
                base.getValue("samples").jsonArray.first().jsonObject +
                    ("discontinuity" to JsonPrimitive("false"))
            )
        val wrongSample = JsonObject(base + ("samples" to JsonArray(listOf(sample))))
        assertFailsWith<IllegalArgumentException> { ActivityJson.decode(wrongSample.toString()) }
    }

    @Test
    fun oversizedEvidenceIsRejectedRatherThanTruncated() {
        val base = Json.parseToJsonElement(ActivityJson.encode(activity())).jsonObject
        for ((field, size) in listOf("samples" to 200001, "laps" to 10001)) {
            val tooMany = JsonObject(base + (field to JsonArray(List(size) { JsonNull })))
            val error =
                assertFailsWith<IllegalArgumentException> {
                    ActivityJson.decode(tooMany.toString())
                }
            assertTrue(error.message.orEmpty().contains("Too many $field"))
        }
    }
}
