package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.presentation.DurabilityData
import org.racedaydreams.domain.time.*

class RunningSummaryContractTest {
    private fun run(
        id: String,
        metres: Double? = 5000.0,
        seconds: Long = 1500,
        offset: ZoneOffset? = null,
        samples: List<DistanceSample> = emptyList(),
    ) =
        ActivityRecord(
            id,
            SourceIdentity("native", id, null),
            Instants.ofEpochSecond(85500),
            Instants.ofEpochSecond(85500 + seconds),
            offset,
            offset,
            metres,
            null,
            null,
            null,
            null,
            null,
            null,
            "",
            samples,
            emptyList(),
            null,
            null,
        )

    private fun result(id: String, seconds: Long, day: LocalDate = CalendarDates.of(1970, 1, 1)) =
        RecordedResult(
            id,
            PerformanceDistance.FIVE_K,
            Durations.ofSeconds(seconds),
            day,
            null,
            ResultSource.MANUAL,
            null,
            "",
        )

    private fun sampled(factor: Double = 1.0, initialMetres: Double = 0.0): ActivityRecord {
        var metres = initialMetres
        val points =
            (0..360).map { i ->
                if (i > 0) metres += if (i <= 180) 30.0 else 30.0 * factor
                DistanceSample(Durations.ofSeconds(i * 10L), metres, null, null, null, false)
            }
        return run("sampled", metres, 3600, samples = points)
    }

    @Test
    fun disabledMilestonesDoNotReadMissingRecords() {
        assertTrue(MilestoneEngine.calculate(false, null, null).isEmpty())
    }

    @Test
    fun aggregateDistanceCanEstablishARecordedFirstButNeverAnUnrecordedTimeThreshold() {
        val activity = run("recorded")
        val milestones = MilestoneEngine.calculate(true, listOf(activity), emptyList())
        assertEquals(listOf("first_5k"), milestones.map { it.code() })
        assertEquals("recorded", milestones.single().activityId())
        assertNull(milestones.single().resultId())
        assertTrue(milestones.single().explanation()!!.contains("not a claim about your lifetime"))
        assertFails { (milestones as MutableList).clear() }
    }

    @Test
    fun implausibleDistanceAndRejectedGpsDoNotCreateRecordedFirsts() {
        val corrupted =
            listOf(
                DistanceSample(Durations.ofSeconds(0), 0.0, null, null, null, false),
                DistanceSample(Durations.ofSeconds(10), 1000.0, null, null, null, false),
            )
        for (activity in
            listOf(
                run("impossible", seconds = 10),
                run("missing", metres = null),
                run("rejected", samples = corrupted),
            )) assertTrue(MilestoneEngine.calculate(true, listOf(activity), emptyList()).isEmpty())
    }

    @Test
    fun equalDateFirstActivitiesRetainOriginalInputPriority() {
        val z = run("z")
        val a = run("a")
        val recorded = result("result", 1700)
        assertEquals(
            "z",
            MilestoneEngine.calculate(true, listOf(z, a), listOf(recorded))
                .first { it.code() == "first_5k" }
                .activityId(),
        )
        assertEquals(
            "a",
            MilestoneEngine.calculate(true, listOf(a, z), listOf(recorded))
                .first { it.code() == "first_5k" }
                .activityId(),
        )
    }

    @Test
    fun timeThresholdsAreStrictAndResultTiesUseStableIds() {
        assertTrue(
            MilestoneEngine.calculate(true, emptyList(), listOf(result("exact", 1800))).none {
                it.code() == "sub_30_5k"
            }
        )
        val rows = listOf(result("z", 1799), result("a", 1799))
        val threshold =
            MilestoneEngine.calculate(true, emptyList(), rows).single { it.code() == "sub_30_5k" }
        assertEquals("a", threshold.resultId())
        assertNull(threshold.activityId())
        assertTrue(threshold.explanation()!!.contains("source is retained"))
    }

    @Test
    fun firstActivityDatesUseRecordedOffsetsAndDoNotMutateUnknownOffsets() {
        val utc = run("utc")
        val local = run("offset", offset = Offsets.ofTotalSeconds(7200))
        val first = MilestoneEngine.calculate(true, listOf(local, utc), emptyList()).single()
        assertEquals("utc", first.activityId())
        assertEquals(CalendarDates.of(1970, 1, 1), first.date())
        assertNull(utc.startOffset())
    }

    @Test
    fun earlierRetainedResultsTakePrecedenceAndOutputRemainsChronological() {
        val earlier = result("earlier", 1400, CalendarDates.of(1969, 12, 31))
        val rows = MilestoneEngine.calculate(true, listOf(run("run")), listOf(earlier))
        assertEquals("earlier", rows.first { it.code() == "first_5k" }.resultId())
        assertEquals(listOf("first_5k", "sub_25_5k", "sub_30_5k"), rows.map { it.code() })
    }

    @Test
    fun totalsAloneNeverBecomeDurabilityHalfPaces() {
        val data = DurabilityData.of(run("aggregate", 10800.0, 3600))
        assertEquals("Insufficient detailed run data", data.status())
        assertNull(data.firstHalfSecondsPerKm())
        assertNull(data.secondHalfSecondsPerKm())
        assertFalse(data.evidence()!!.sufficient())
    }

    @Test
    fun observedHalvesRetainStableSlowerAndFasterDescriptions() {
        for ((factor, status) in
            listOf(1.0 to "Stable pacing", .8 to "Late fade", 1.1 to "Faster second half")) {
            val activity = sampled(factor)
            val before = activity.copy()
            val data = DurabilityData.of(activity)
            assertEquals(status, data.status())
            assertEquals(1000.0 / 3, data.firstHalfSecondsPerKm()!!, 1e-8)
            assertEquals(1000.0 / (3 * factor), data.secondHalfSecondsPerKm()!!, 1e-8)
            assertNull(data.evidence()!!.heartRateDriftPercent())
            assertEquals(before, activity)
        }
    }

    @Test
    fun gapsIncompleteCoverageAndConflictingTotalsKeepHalfPacesUnavailable() {
        val full = sampled()
        val points =
            full.samples().mapIndexed { i, p -> if (i == 180) p.copy(discontinuity = true) else p }
        for (activity in
            listOf(
                full.copy(samples = points),
                full.copy(end = Instants.ofEpochSecond(full.end().getEpochSecond() + 60)),
                full.copy(distanceMetres = full.distanceMetres()!! + 2000),
            )) {
            val data = DurabilityData.of(activity)
            assertNull(data.firstHalfSecondsPerKm())
            assertNull(data.secondHalfSecondsPerKm())
            assertFalse(data.evidence()!!.sufficient())
        }
    }

    @Test
    fun summaryUsesObservedDistanceSpanRatherThanAnInventedInitialBridge() {
        val data = DurabilityData.of(sampled(initialMetres = 100.0))
        assertEquals(1000.0 / 3, data.firstHalfSecondsPerKm()!!, 1e-8)
        assertEquals(1000.0 / 3, data.secondHalfSecondsPerKm()!!, 1e-8)
    }

    @Test
    fun rawSummaryUnknownsRemainReadableWithoutNewValidation() {
        val data = DurabilityData(null, null, Double.NaN, null)
        assertNull(data.status())
        assertNull(data.firstHalfSecondsPerKm())
        assertTrue(data.secondHalfSecondsPerKm()!!.isNaN())
        assertNull(data.evidence())
    }
}
