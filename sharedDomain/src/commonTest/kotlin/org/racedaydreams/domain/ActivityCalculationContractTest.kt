package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.time.*

class ActivityCalculationContractTest {
    private fun point(
        seconds: Long,
        metres: Double,
        gap: Boolean = false,
        hr: Double? = null,
        elevation: Double? = null,
    ) = DistanceSample(Durations.ofSeconds(seconds), metres, hr, null, elevation, gap)

    private fun run(
        samples: List<DistanceSample>,
        total: Double? = samples.lastOrNull()?.distanceMetres(),
        elapsed: Long = maxOf(1, samples.lastOrNull()?.elapsed()?.getSeconds() ?: 1),
        offset: ZoneOffset? = null,
    ): ActivityRecord =
        ActivityRecord(
            "retained",
            SourceIdentity("native", "source", null),
            Instants.ofEpochSecond(85500),
            Instants.ofEpochSecond(85500 + elapsed),
            offset,
            offset,
            total,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            samples,
            emptyList(),
            null,
            null,
        )

    private fun uniform(count: Int) = (0..count).map { point(it * 3L, it * 10.0) }

    @Test
    fun continuousEvidenceProducesQualifiedRollingResultsWithoutChangingActuals() {
        val activity = run(uniform(500))
        val before = activity.copy()
        val efforts = BestEffortCalculator.calculate(activity)
        assertEquals(SampleQuality.QUALIFIED, efforts.quality())
        val five = efforts.efforts().single { it.distance() == PerformanceDistance.FIVE_K }
        assertEquals(1500000L, five.duration().toMillis())
        val results = BestEffortCalculator.qualifiedResults(activity)
        assertEquals(
            "retained:5k",
            results.single { it.distance() == PerformanceDistance.FIVE_K }.id(),
        )
        assertTrue(
            results.all { it.source() == ResultSource.ROLLING && it.activityId() == "retained" }
        )
        assertFails { (results as MutableList).clear() }
        assertEquals(before, activity)
    }

    @Test
    fun stationaryStartAndFinishBelongToFixedSplitsButNotTheFastestRollingEffort() {
        val points = mutableListOf(point(0, 0.0), point(10, 0.0), point(20, 0.0))
        for (i in 1..100) points.add(point(20 + i * 2L, i * 10.0))
        points.add(point(240, 1000.0))
        val activity = run(points)
        val split = SplitCalculator.calculate(activity, UnitSystem.METRIC).splits().single()
        assertEquals(240000L, split.duration().toMillis())
        val effort =
            BestEffortCalculator.calculate(activity, listOf(PerformanceDistance.ONE_K))
                .efforts()
                .single()
        assertEquals(20000L, effort.startElapsed().toMillis())
        assertEquals(220000L, effort.endElapsed().toMillis())
        assertEquals(200000L, effort.duration().toMillis())
        assertNull(activity.movingTime())
    }

    @Test
    fun adjacentSplitsShareTheFirstCrossingAndPreserveStationaryBoundaryTime() {
        val points = uniform(100).toMutableList()
        points.add(point(330, 1000.0))
        points.add(point(360, 1000.0))
        for (i in 1..100) points.add(point(360 + i * 3L, 1000.0 + i * 10))
        points.add(point(690, 2000.0))
        points.add(point(700, 2000.0))
        val splits = SplitCalculator.calculate(run(points), UnitSystem.METRIC).splits()
        assertEquals(listOf(300000L, 400000L), splits.map { it.duration().toMillis() })
        assertTrue(splits.none { it.partial() })
    }

    @Test
    fun aGapCannotEstablishAKilometreAcrossTwoShorterContinuousSections() {
        val points = (0..60).map { point(it * 10L, it * 30.0, it == 30) }
        val activity = run(points)
        assertTrue(
            BestEffortCalculator.calculate(activity, listOf(PerformanceDistance.ONE_K))
                .efforts()
                .isEmpty()
        )
        val splits = SplitCalculator.calculate(activity, UnitSystem.METRIC)
        assertEquals(SampleQuality.UNCERTAIN, splits.quality())
        assertTrue(splits.splits().none { it.number() == 1 })
        assertTrue(splits.splits().all { it.partial() })
    }

    @Test
    fun impossibleOrUnorderedEvidenceIsRejectedWithoutAutomaticPersonalBests() {
        val invalid =
            listOf(
                run(listOf(point(0, 0.0), point(10, 30.0), point(10, 60.0))),
                run(listOf(point(0, 0.0), point(10, 30.0), point(20, 29.0))),
                run(listOf(point(0, 0.0), point(10, 1000.0))),
                run(listOf(point(0, 0.0), point(10, 30.0)), elapsed = 9),
            )
        for (activity in invalid) {
            assertEquals(SampleQuality.REJECTED, BestEffortCalculator.calculate(activity).quality())
            assertTrue(BestEffortCalculator.qualifiedResults(activity).isEmpty())
            assertTrue(SplitCalculator.calculate(activity, UnitSystem.METRIC).splits().isEmpty())
        }
    }

    @Test
    fun sparseEffortsStayUncertainAndTotalsAloneNeverBecomeRollingResults() {
        val sparse =
            run(listOf(point(0, 0.0), point(30, 333.0), point(60, 666.0), point(90, 1000.0)))
        val analysis = BestEffortCalculator.calculate(sparse)
        assertTrue(analysis.efforts().isNotEmpty())
        assertTrue(analysis.efforts().all { it.quality() == SampleQuality.UNCERTAIN })
        assertTrue(BestEffortCalculator.qualifiedResults(sparse).isEmpty())
        val totalsOnly = run(emptyList(), 5000.0, 1500)
        assertEquals(
            SampleQuality.UNAVAILABLE,
            BestEffortCalculator.calculate(totalsOnly).quality(),
        )
        assertTrue(SplitCalculator.calculate(totalsOnly, UnitSystem.METRIC).splits().isEmpty())
    }

    @Test
    fun availableSensorMeansNeverFillMissingCadenceOrInventAnActivityAverage() {
        val points =
            (0..60).map {
                point(
                    it.toLong(),
                    it * 5.0,
                    hr =
                        when (it) {
                            0 -> 120.0
                            60 -> 140.0
                            else -> null
                        },
                    elevation =
                        when (it) {
                            0 -> 10.0
                            60 -> 20.0
                            else -> null
                        },
                )
            }
        val activity = run(points)
        val split = SplitCalculator.calculate(activity, 300.0).splits().single()
        assertEquals(130.0, split.averageHeartRate())
        assertNull(split.cadence())
        assertEquals(10.0, split.elevationChangeMetres())
        assertNull(activity.averageHeartRate())
    }

    @Test
    fun sourceOffsetsDetermineRecordDateAndMissingTotalsStayMissing() {
        val positive = run(uniform(100), null, offset = Offsets.ofTotalSeconds(7200))
        val result = BestEffortCalculator.qualifiedResults(positive).first()
        assertEquals(CalendarDates.of(1970, 1, 2), result.date())
        val unknownOffset = run(uniform(100), null)
        assertEquals(
            CalendarDates.of(1970, 1, 1),
            BestEffortCalculator.qualifiedResults(unknownOffset).first().date(),
        )
        assertNull(positive.distanceMetres())
        assertNull(unknownOffset.startOffset())
        assertTrue(BestEffortCalculator.qualifiedResults(run(uniform(100), 2000.0)).isEmpty())
    }

    @Test
    fun endBoundarySweepFindsAFasterWindowWithAnInterpolatedStart() {
        val metres =
            listOf(
                0.0,
                90.0,
                190.0,
                300.0,
                420.0,
                550.0,
                690.0,
                840.0,
                1000.0,
                1170.0,
                1350.0,
                1540.0,
            )
        val activity = run(metres.mapIndexed { index, distance -> point(index * 20L, distance) })
        val best =
            BestEffortCalculator.calculate(activity, listOf(PerformanceDistance.ONE_K))
                .efforts()
                .single()
        assertEquals(121.538461538, ActivityChecks.seconds(best.duration()), .000001)
        assertEquals(220000L, best.endElapsed().toMillis())
    }

    @Test
    fun boundedCustomSplitsAndUnknownArgumentsRetainOriginalValidation() {
        val activity = run(uniform(100))
        assertEquals(SampleQuality.REJECTED, SplitCalculator.calculate(activity, .01).quality())
        assertEquals(
            SplitCalculator.calculate(activity, UnitSystem.METRIC),
            SplitCalculator.calculate(activity, null as UnitSystem?),
        )
        for (distance in listOf(0.0, -1.0, Double.NaN)) assertFailsWith<IllegalArgumentException> {
            SplitCalculator.calculate(activity, distance)
        }
        assertFailsWith<NullPointerException> { BestEffortCalculator.calculate(null) }
        assertFailsWith<NullPointerException> { BestEffortCalculator.calculate(activity, null) }
        assertTrue(BestEffortCalculator.calculate(activity, emptyList()).efforts().isEmpty())
        assertEquals(
            40,
            SplitCalculator.calculate(run(uniform(4000)), UnitSystem.METRIC).splits().size,
        )
    }
}
