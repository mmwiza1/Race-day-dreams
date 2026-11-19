package org.racedaydreams.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.activity.*
import org.racedaydreams.storage.ActivityJson

@RunWith(AndroidJUnit4::class)
class SharedActivityCodecTest {
    private fun actual(): ActivityRecord {
        val start = Instant.parse("2026-11-15T09:00:00.123456789Z")
        val end = start.plusSeconds(60)
        return ActivityRecord(
            "run",
            SourceIdentity("rdd", "record", "local"),
            start,
            end,
            ZoneOffset.ofTotalSeconds(5445),
            null,
            150.0,
            Duration.ofSeconds(50),
            null,
            null,
            null,
            0.0,
            5.0,
            "actual 🏃\u0000 note",
            listOf(
                DistanceSample(Duration.ZERO, 0.0, null, null, null, false),
                DistanceSample(Duration.ofNanos(15500000000L), 50.0, 128.0, 160.0, null, false),
                DistanceSample(Duration.ofSeconds(40), 50.0, null, null, null, true),
            ),
            listOf(ActivityLap("lap", start, end, 150.0, "retained lap", "planned-step")),
            "workout",
            "shoe",
        )
    }

    @Test
    fun originalAndroidAndPortableCodecsPreserveActualEvidenceInBothDirections() {
        val recorded = actual()
        val summary =
            recorded.copy(
                samples = emptyList(),
                laps = emptyList(),
                movingTime = null,
                distanceMetres = null,
                cadence = null,
            )
        for (record in listOf(recorded, summary)) {
            assertEquals(record, ActivityJson.decode(FeatureJson.activity(record).toString()))
            assertEquals(record, FeatureJson.activity(JSONObject(ActivityJson.encode(record))))
        }
    }

    @Test
    fun bothCodecsRejectInventedTypesAndKeepMissingMeasurementsUnknown() {
        val row = FeatureJson.activity(actual())
        row.put("distanceMetres", "150")
        assertThrows(IllegalArgumentException::class.java) { FeatureJson.activity(row) }
        assertThrows(IllegalArgumentException::class.java) { ActivityJson.decode(row.toString()) }
        row.put("distanceMetres", JSONObject.NULL)
        row.put("movingTime", JSONObject.NULL)
        val original = FeatureJson.activity(row)
        val shared = ActivityJson.decode(row.toString())
        assertEquals(original, shared)
        assertNull(shared.distanceMetres())
        assertNull(shared.movingTime())
    }
}
