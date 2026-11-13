package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.time.*

class ActivityEvidenceModelsTest {
    private val start = Instants.ofEpochSecond(100, 900000000)
    private val end = Instants.ofEpochSecond(101, 100000000)

    private fun activity(
        moving: Duration?,
        samples: List<DistanceSample> = emptyList(),
        laps: List<ActivityLap> = emptyList(),
    ) =
        ActivityRecord(
            " actual ",
            SourceIdentity("rdd", "source", null),
            start,
            end,
            null,
            null,
            5000.0,
            moving,
            null,
            null,
            null,
            null,
            null,
            null,
            samples,
            laps,
            null,
            null,
        )

    @Test
    fun exactElapsedAndMovingTimeRetainFractionalBoundaries() {
        val elapsed = Durations.between(start, end)
        assertEquals(0L, elapsed.getSeconds())
        assertEquals(200000000, elapsed.getNano())
        assertEquals(elapsed, activity(elapsed).elapsedTime())
        assertTrue(Durations.between(end, start).isNegative())
        assertTrue(Durations.ofNanos(199999999) < elapsed)
        assertFailsWith<IllegalArgumentException> { activity(Durations.ofNanos(200000001)) }
        assertNull(activity(null).movingTime())
    }

    @Test
    fun snapshotsKeepRecordedTotalsAndDiscontinuitiesWithoutRecalculatingHistory() {
        val sample = DistanceSample(Durations.ofNanos(1), 2500.0, null, null, -25.0, true)
        val lap = ActivityLap(" lap ", start, end, null, null, null)
        val samples = mutableListOf(sample)
        val laps = mutableListOf(lap)
        val actual = activity(null, samples, laps)
        samples.clear()
        laps.clear()
        assertEquals(5000.0, actual.distanceMetres())
        assertEquals(2500.0, actual.samples().single().distanceMetres())
        assertTrue(actual.samples().single().discontinuity())
        assertEquals(-25.0, actual.samples().single().elevationMetres())
        assertNull(actual.samples().single().heartRate())
        assertEquals(" actual ", actual.id())
        assertEquals(" lap ", actual.laps().single().id())
        assertFails { (actual.samples() as MutableList<DistanceSample>).clear() }
        assertNull(actual.startOffset())
        assertNull(actual.notes())
    }

    @Test
    fun recordsAndFirstPersonalBestsNeverInventPreviousActuals() {
        val result =
            RecordedResult(
                " result ",
                PerformanceDistance.FIVE_K,
                Durations.ofMinutes(25),
                CalendarDates.of(2026, 11, 13),
                null,
                ResultSource.MANUAL,
                null,
                null,
            )
        val first = PersonalBestEntry(result, null, null)
        assertNull(first.previousBest())
        assertNull(first.improvement())
        assertEquals("5:00/km", first.pace(UnitSystem.METRIC))
        assertFailsWith<IllegalArgumentException> {
            PersonalBestEntry(result, null, Durations.ofNanos(1))
        }
        assertFailsWith<IllegalArgumentException> { result.copy(source = ResultSource.ROLLING) }
        assertFailsWith<IllegalArgumentException> { result.copy(duration = Durations.ofSeconds(0)) }
    }

    @Test
    fun invalidActivityAndSplitEvidenceIsRejectedWithoutTighteningUnknownValues() {
        assertFailsWith<IllegalArgumentException> {
            ActivityLap("x", start, start, null, null, null)
        }
        assertFailsWith<IllegalArgumentException> {
            DistanceSample(Durations.ofSeconds(0), 0.0, 0.0, null, null, false)
        }
        val split = RunSplit(1, 1000.0, Durations.ofSeconds(300, 1), null, null, null, false)
        assertEquals(300.000000001, split.secondsPerKilometre())
        assertNull(split.averageHeartRate())
        assertFalse(split.partial())
        assertEquals(
            "Samsung Health",
            ActivitySources.friendly(
                SourceIdentity("health-connect", "s", "com.sec.android.app.shealth")
            ),
        )
        assertEquals("HEALTH_CONNECT", ActivitySources.normalizeLegacyPreference(null))
    }

    @Test
    fun fullRetainedInstantSpanStillHasAnExactRepresentableDuration() {
        val low = Instants.ofEpochSecond(InstantParts.MIN_SECOND)
        val high = Instants.ofEpochSecond(InstantParts.MAX_SECOND, 999999999)
        val elapsed = Durations.between(low, high)
        assertEquals(InstantParts.MAX_SECOND - InstantParts.MIN_SECOND, elapsed.getSeconds())
        assertEquals(999999999, elapsed.getNano())
    }
}
