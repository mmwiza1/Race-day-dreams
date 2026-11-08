package org.racedaydreams.coach

import java.time.Duration
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test
import org.racedaydreams.domain.activity.*

/** Deterministic presentation invariants; no new training or recording calculation. */
class RunningPerformancePresentationTest {
    private val today = LocalDate.of(2026, 11, 8)
    private val monday = LocalDate.of(2026, 11, 2)

    private fun run(
        id: String,
        date: LocalDate,
        metres: Double?,
        seconds: Long = 1800,
    ): ActivityRecord {
        val start = date.atTime(12, 0).toInstant(ZoneOffset.UTC)
        return ActivityRecord(
            id,
            SourceIdentity("manual", id, null),
            start,
            start.plusSeconds(seconds),
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            metres,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            emptyList(),
            emptyList(),
            null,
            null,
        )
    }

    @Test
    fun recordedZeroUnknownDistanceAndUnrecordedWeeksRemainDifferent() {
        val runs =
            listOf(
                run("zero", monday, 0.0),
                run("unknown", monday.minusWeeks(1), null),
                run("known", monday.minusWeeks(3), 5000.0),
            )
        val weeks = ActivityAnalytics.weekly(runs, today, ZoneOffset.UTC, 4)
        val points =
            runningTrendPoints(weeks, runs, RunningTrendMetric.DISTANCE, today, ZoneOffset.UTC)
        val zero = points.single { it.week.weekStart() == monday }
        val unknown = points.single { it.week.weekStart() == monday.minusWeeks(1) }
        val absent = points.single { it.week.weekStart() == monday.minusWeeks(2) }
        assertEquals(0.0, zero.value!!, 0.0)
        assertEquals(1, zero.week.activityCount())
        assertEquals(1, zero.measuredRuns)
        assertNull(unknown.value)
        assertEquals(1, unknown.week.activityCount())
        assertEquals(0, unknown.measuredRuns)
        assertEquals(0, absent.week.activityCount())
        // Existing weekly aggregation retains a zero sum for an empty week. Presentation
        // must use the observation count to label it "No recorded runs", not measured zero.
        assertEquals(0.0, absent.value!!, 0.0)
    }

    @Test
    fun weeklyPaceExcludesUnknownAndZeroDistanceWhileTimeRetainsRecordedDuration() {
        val runs =
            listOf(
                run("measured", monday, 5000.0, 1800),
                run("unknown", monday.plusDays(1), null, 600),
                run("zero", monday.plusDays(2), 0.0, 300),
            )
        val weeks = ActivityAnalytics.weekly(runs, today, ZoneOffset.UTC, 4)
        val pace =
            runningTrendPoints(weeks, runs, RunningTrendMetric.PACE, today, ZoneOffset.UTC).last()
        val time =
            runningTrendPoints(weeks, runs, RunningTrendMetric.TIME, today, ZoneOffset.UTC).last()
        assertEquals(360.0, pace.value!!, .000001)
        assertEquals(1, pace.measuredRuns)
        assertEquals(3, pace.week.activityCount())
        assertEquals(2700.0, time.value!!, 0.0)
        assertEquals(3, time.measuredRuns)
        val noPace = listOf(run("missing", monday, null), run("stationary", monday, 0.0))
        assertNull(
            runningTrendPoints(
                    ActivityAnalytics.weekly(noPace, today, ZoneOffset.UTC, 4),
                    noPace,
                    RunningTrendMetric.PACE,
                    today,
                    ZoneOffset.UTC,
                )
                .last()
                .value
        )
    }

    @Test
    fun rankingsRetainSourcePrecedenceAndOnlyThreeSupportedIndependentResults() {
        fun result(id: String, seconds: Long, source: ResultSource, activity: String? = null) =
            RecordedResult(
                id,
                PerformanceDistance.TEN_K,
                Duration.ofSeconds(seconds),
                today,
                null,
                source,
                activity,
                null,
            )
        val official = result("official", 3000, ResultSource.OFFICIAL_CHIP, "linked-run")
        val slower = result("second", 3100, ResultSource.MANUAL)
        val third = result("third", 3200, ResultSource.MANUAL)
        val source =
            listOf(
                result("rolling", 2800, ResultSource.ROLLING, "linked-run"),
                result("linked-manual", 2900, ResultSource.MANUAL, "linked-run"),
                official,
                slower,
                third,
                result("fourth", 3300, ResultSource.MANUAL),
                official,
                RecordedResult(
                    "different-distance",
                    PerformanceDistance.FIVE_K,
                    Duration.ofSeconds(1200),
                    today,
                    null,
                    ResultSource.MANUAL,
                    null,
                    null,
                ),
            )
        assertEquals(listOf(official, slower, third), rankedRunningResults(source, "10k"))
        assertTrue(rankedRunningResults(source, "marathon").isEmpty())
        assertEquals(8, source.size)
    }

    @Test
    fun fourTiedIndependentResultsKeepTheAuthoritativeBestInsideOnlyThreeRows() {
        fun result(id: String, source: ResultSource) =
            RecordedResult(
                id,
                PerformanceDistance.TEN_K,
                Duration.ofSeconds(3000),
                today,
                null,
                source,
                null,
                null,
            )
        val official = result("z", ResultSource.OFFICIAL_CHIP)
        val source =
            listOf(
                result("a", ResultSource.MANUAL),
                result("b", ResultSource.MANUAL),
                result("c", ResultSource.MANUAL),
                official,
            )
        val ranked = rankedRunningResults(source, "10k")
        assertEquals(3, ranked.size)
        assertEquals(official, ranked.first())
        assertEquals(listOf("z", "a", "b"), ranked.map { it.id() })
        assertEquals(4, source.size)
    }
}
