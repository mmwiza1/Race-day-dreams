package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.recording.RunMetricsEngine.Fix
import org.racedaydreams.domain.recording.RunMetricsEngine.Point
import org.racedaydreams.domain.time.*

class ActivityStorageCodecTest {
    @Test
    fun recordedResultsKeepTheirActualSourceAndOriginalDistanceIdentity() {
        for (source in ResultSource.entries) {
            val result =
                RecordedResult(
                    "evidence",
                    PerformanceDistance("track", 400.0, "Track"),
                    Durations.ofSeconds(70, 123456789),
                    CalendarDates.of(2026, 11, 15),
                    null,
                    source,
                    if (source == ResultSource.ROLLING) "run" else null,
                    "actual note",
                )
            assertEquals(result, ResultJson.decode(ResultJson.encode(result)))
        }
    }

    @Test
    fun impossibleResultsCannotBeDecodedAsEvidence() {
        val result =
            RecordedResult(
                "official",
                PerformanceDistance.FIVE_K,
                Durations.ofMinutes(25),
                CalendarDates.of(2026, 11, 15),
                null,
                ResultSource.OFFICIAL_CHIP,
                null,
                null,
            )
        val row = Json.parseToJsonElement(ResultJson.encode(result)).jsonObject
        for (value in listOf(JsonPrimitive("PT0S"), JsonPrimitive("PT-1S"))) {
            assertFailsWith<IllegalArgumentException> {
                ResultJson.decode(JsonObject(row + ("duration" to value)).toString())
            }
        }
        assertFailsWith<IllegalArgumentException> {
            ResultJson.decode(JsonObject(row + ("source" to JsonPrimitive("ROLLING"))).toString())
        }
    }

    @Test
    fun acceptedRecordingPointRetainsCoordinatesAndLargeIntegerClocksExactly() {
        val point =
            Point(
                Fix(
                    StoredInstant.parse("2026-11-15T09:00:00.123456789Z"),
                    9007199254741017L,
                    51.501,
                    -.141,
                    null,
                    0.0,
                    null,
                    null,
                ),
                123.45,
                9007199254741011L,
                Int.MAX_VALUE,
                true,
                null,
                0.0,
            )
        val restored = RecordingPointJson.decode(RecordingPointJson.encode(point))
        assertEquals(point, restored)
        assertEquals(9007199254741017L, restored.fix().monotonicMillis())
        assertNull(restored.fix().accuracy())
        assertEquals(0.0, restored.fix().altitude())
        assertNull(restored.heartRate())
        assertEquals(0.0, restored.cadence())
    }

    @Test
    fun rawPointDecodeRejectsCoercedClockAndUnsupportedSegmentRatherThanRounding() {
        val point =
            Point(
                Fix(
                    StoredInstant.parse("2026-11-15T09:00:00Z"),
                    1000,
                    51.0,
                    0.0,
                    5.0,
                    null,
                    null,
                    null,
                ),
                0.0,
                0,
                0,
                false,
                null,
                null,
            )
        val row = Json.parseToJsonElement(RecordingPointJson.encode(point)).jsonObject
        for ((field, value) in
            listOf(
                "mono" to JsonPrimitive("1000"),
                "mono" to JsonPrimitive(1.5),
                "segment" to JsonPrimitive(2147483648L),
                "gap" to JsonPrimitive("false"),
                "lat" to JsonPrimitive(91.0),
            )) {
            assertFailsWith<IllegalArgumentException> {
                RecordingPointJson.decode(JsonObject(row + (field to value)).toString())
            }
        }
    }

    @Test
    fun storedEpochMillisMatchesOriginalFlooringIncludingBothLongEndpoints() {
        for (millis in
            listOf(
                Long.MIN_VALUE,
                Long.MIN_VALUE + 1,
                -1001L,
                -1000L,
                -999L,
                -1L,
                0L,
                1L,
                999L,
                1000L,
                Long.MAX_VALUE - 1,
                Long.MAX_VALUE,
            )) {
            val instant =
                Instants.ofEpochSecond(millis.floorDiv(1000L), millis.mod(1000L) * 1000000)
            assertEquals(millis, storedEpochMillis(instant))
        }
        assertEquals(-1L, storedEpochMillis(Instants.ofEpochSecond(-1, 999999999)))
        assertEquals(0L, storedEpochMillis(Instants.ofEpochSecond(0, 999999)))
        assertFailsWith<ArithmeticException> {
            storedEpochMillis(
                Instants.ofEpochSecond(
                    Long.MIN_VALUE.floorDiv(1000L),
                    Long.MIN_VALUE.mod(1000L) * 1000000 - 1,
                )
            )
        }
        assertFailsWith<ArithmeticException> {
            storedEpochMillis(
                Instants.ofEpochSecond(
                    Long.MAX_VALUE.floorDiv(1000L),
                    Long.MAX_VALUE.mod(1000L) * 1000000 + 1000000,
                )
            )
        }
    }
}
