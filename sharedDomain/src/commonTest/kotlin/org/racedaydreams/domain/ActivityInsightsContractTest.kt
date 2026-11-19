package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

class ActivityInsightsContractTest {
    private fun run(
        id: String,
        days: Long = 0,
        distance: Double? = 5000.0,
        seconds: Long = 1500,
        planned: String? = null,
        effort: Double? = null,
        elevation: Double? = null,
        offset: ZoneOffset? = null,
    ): ActivityRecord {
        val start = 85500 - days * 86400
        return ActivityRecord(
            id,
            SourceIdentity("native", id, null),
            Instants.ofEpochSecond(start),
            Instants.ofEpochSecond(start + seconds),
            offset,
            offset,
            distance,
            null,
            null,
            null,
            elevation,
            null,
            effort,
            "",
            emptyList(),
            emptyList(),
            planned,
            null,
        )
    }

    private fun plan(id: String, category: WorkoutCategory = WorkoutCategory.EASY_RUN) =
        PlannedWorkout(id, CalendarDates.of(1970, 1, 1), category, 5.0, null, emptyList(), "")

    private fun gym(id: String, exercise: String = "romanian-deadlift") =
        StrengthSession(
            id,
            Instants.ofEpochSecond(85500),
            Offsets.ofTotalSeconds(7200),
            Durations.ofMinutes(30),
            null,
            "",
            listOf(
                StrengthSession.ExerciseSets(
                    requireNotNull(ExerciseCatalog.definition(exercise)).exercise(),
                    listOf(StrengthSession.SetEntry("$id-set", 5, 20.0, 7.0, false)),
                )
            ),
        )

    private fun sampled(
        factor: Double = 1.0,
        sensorStride: Int = 0,
        gap: Boolean = false,
    ): ActivityRecord {
        var metres = 0.0
        val points =
            (0..360).map { i ->
                if (i > 0) metres += if (i <= 180) 30.0 else 30.0 * factor
                val missing = sensorStride > 0 && i % sensorStride == 0
                DistanceSample(
                    Durations.ofSeconds(i * 10L),
                    metres,
                    if (missing) null else if (i <= 180) 125.0 else 140.0,
                    if (missing) null else if (i <= 180) 175.0 else 170.0,
                    null,
                    gap && i == 180,
                )
            }
        return run("sampled", distance = metres, seconds = 3600).copy(samples = points)
    }

    @Test
    fun knownPurposeMatchesExcludeFutureDifferentPurposeAndDistantRunsAndKeepStableTies() {
        val current = run("current", planned = "easy", effort = 8.0)
        val olderA = run("a", 7, planned = "old", effort = 5.0)
        val olderZ = run("z", 7, planned = "old")
        val history =
            listOf(
                olderZ,
                run("future", -1, planned = "old"),
                current,
                run("hard", 2, planned = "hard"),
                run("far", 2, 8000.0, 2800, "old"),
                olderA,
            )
        val matches =
            ComparableRuns.find(
                current,
                history,
                listOf(plan("easy"), plan("old"), plan("hard", WorkoutCategory.INTERVALS)),
            )
        assertEquals(listOf("a", "z"), matches.map { it.activity()!!.id() })
        assertTrue(matches.all { it.confidence() == "Strong" })
        assertTrue(matches.first().reasons()!!.contains("Same planned workout type"))
        assertFails { (matches as MutableList).clear() }
        assertFails { (matches.first().reasons() as MutableList).clear() }
        assertEquals(6, history.size)
    }

    @Test
    fun unknownPurposeRequiresCloseDistanceAndDurationAndNeverClaimsRouteMatching() {
        val current = run("current")
        val close = run("close", 1, 5250.0, 1500)
        val distant = run("distant", 1, 5251.0, 1500)
        val slow = run("slow", 1, 5000.0, 1800)
        val matches = ComparableRuns.find(current, listOf(close, distant, slow), emptyList())
        assertEquals(listOf("close"), matches.map { it.activity()!!.id() })
        assertEquals("Moderate", matches.single().confidence())
        assertTrue(matches.single().reasons()!!.none { it.contains("route", ignoreCase = true) })
    }

    @Test
    fun missingImplausibleOrShortDistanceNeverGeneratesAComparison() {
        val prior = run("prior", 7)
        for (current in
            listOf(
                run("missing", distance = null),
                run("short", distance = 499.0),
                run("fast", seconds = 10),
            )) assertTrue(ComparableRuns.find(current, listOf(prior), emptyList()).isEmpty())
        assertEquals(
            "Distance evidence is unavailable.",
            ComparableRuns.interpretation(run("missing", distance = null), prior),
        )
        assertTrue(
            ComparableRuns.interpretation(run("current", effort = 7.0), prior)
                .contains("reported effort is incomplete")
        )
        assertTrue(
            ComparableRuns.interpretation(run("current", effort = 7.0), prior.copy(rpe = 5.0))
                .contains("higher reported effort")
        )
        assertTrue(
            ComparableRuns.interpretation(run("current"), prior)
                .endsWith("does not establish a fitness change.")
        )
    }

    @Test
    fun comparisonAgeUsesWholeDaysWithoutRejectingTheFinalFractionOfDay365() {
        val current = run("current")
        val old = run("old", 365)
        val almost =
            old.copy(
                start = Instants.ofEpochSecond(old.start().getEpochSecond() - 86399),
                end = Instants.ofEpochSecond(old.end().getEpochSecond() - 86399),
            )
        assertEquals(
            listOf("old"),
            ComparableRuns.find(current, listOf(almost, run("expired", 366)), emptyList()).map {
                it.activity()!!.id()
            },
        )
    }

    @Test
    fun durabilityUsesActualTimeHalvesAndSeparatelySupportedSensorCoverage() {
        val value = LongRunDurability.analyse(sampled())
        assertTrue(value.sufficient())
        assertEquals(0.0, value.paceFadePercent()!!, 1e-9)
        assertEquals(12.0, value.heartRateDriftPercent()!!, 1e-9)
        assertEquals((170.0 / 175 - 1) * 100, value.cadenceChangePercent()!!, 1e-9)
        assertTrue(value.explanation()!!.contains("does not prove improved fitness"))
        assertNotNull(LongRunDurability.analyse(sampled(sensorStride = 5)).heartRateDriftPercent())
        val incomplete = LongRunDurability.analyse(sampled(sensorStride = 4))
        assertTrue(incomplete.sufficient())
        assertNull(incomplete.heartRateDriftPercent())
        assertNull(incomplete.cadenceChangePercent())
    }

    @Test
    fun durabilityNeverInfersSensorsOrPhysiologyFromAnAggregateOrDiscontinuousRun() {
        val whole = sampled()
        for (activity in
            listOf(
                run("totals", seconds = 3600),
                sampled(gap = true),
                whole.copy(distanceMetres = whole.distanceMetres()!! + 2000),
            )) {
            val result = LongRunDurability.analyse(activity)
            assertFalse(result.sufficient())
            assertNull(result.paceFadePercent())
            assertNull(result.heartRateDriftPercent())
        }
        val noSensors =
            whole.copy(samples = whole.samples().map { it.copy(heartRate = null, cadence = null) })
        val result = LongRunDurability.analyse(noSensors)
        assertTrue(result.sufficient())
        assertNull(result.heartRateDriftPercent())
        assertNull(result.cadenceChangePercent())
    }

    @Test
    fun durabilityRequiresThirtyMinutesAndCoverageOfBothEnds() {
        val whole = sampled()
        val short =
            whole.copy(
                end = Instants.ofEpochSecond(whole.start().getEpochSecond() + 1700),
                samples = whole.samples().take(171),
                distanceMetres = 5100.0,
            )
        val lateStart = whole.copy(samples = whole.samples().drop(4))
        val earlyFinish =
            whole.copy(end = Instants.ofEpochSecond(whole.end().getEpochSecond() + 60))
        for (activity in listOf(short, lateStart, earlyFinish)) assertFalse(
            LongRunDurability.analyse(activity).sufficient()
        )
        assertTrue(
            LongRunDurability.analyse(sampled(.8))
                .explanation()!!
                .contains("Terrain, conditions and intention")
        )
    }

    @Test
    fun historyCombinesOnlyActualRunsAndStrengthWithStableTiesAndBoundedPages() {
        val runs = listOf(run("c-run"), run("b-run", planned = "easy"))
        val strength = listOf(gym("a-gym"))
        val plans = listOf(plan("easy"), plan("uncompleted"))
        val first =
            WorkoutHistoryQuery.page(
                runs,
                strength,
                plans,
                WorkoutHistoryQuery.Kind.ALL,
                null,
                null,
                null,
                null,
                0,
                2,
            )
        assertEquals(3, first.total())
        assertEquals(listOf("a-gym", "b-run"), first.entries()!!.map { it.id() })
        assertTrue(first.hasMore())
        val next = WorkoutHistoryQuery.page(runs, strength, plans, null, null, "", null, null, 2, 2)
        assertEquals(listOf("c-run"), next.entries()!!.map { it.id() })
        assertFalse(next.hasMore())
        assertFails { (first.entries() as MutableList).clear() }
        assertTrue(
            WorkoutHistoryQuery.page(runs, strength, plans, null, null, "", null, null, 99, 2)
                .entries()!!
                .isEmpty()
        )
    }

    @Test
    fun historySearchFindsActualExerciseAliasesAndKeepsRunningAndStrengthFiltersSeparate() {
        val runs = listOf(run("run", planned = "easy"))
        val sessions = listOf(gym("hinge"), gym("press", "bench-press"))
        val plans = listOf(plan("easy"))
        for (query in listOf("RDL", "rDl", "  RDL\t")) assertEquals(
            listOf("hinge"),
            WorkoutHistoryQuery.page(runs, sessions, plans, null, null, query, null, null, 0, 20)
                .entries()!!
                .map { it.id() },
        )
        assertEquals(
            0,
            WorkoutHistoryQuery.page(
                    runs,
                    sessions,
                    plans,
                    WorkoutHistoryQuery.Kind.RUNNING,
                    null,
                    "RDL",
                    null,
                    null,
                    0,
                    20,
                )
                .total(),
        )
        assertEquals(
            1,
            WorkoutHistoryQuery.page(
                    runs,
                    sessions,
                    plans,
                    null,
                    WorkoutCategory.EASY_RUN,
                    "",
                    null,
                    null,
                    0,
                    20,
                )
                .total(),
        )
        assertEquals(
            0,
            WorkoutHistoryQuery.page(runs, sessions, plans, null, null, "\u00a0", null, null, 0, 20)
                .total(),
        )
    }

    @Test
    fun historyDateBoundsUseEachRecordedOffsetWithHonestUtcFallback() {
        val runs = listOf(run("utc"), run("offset", offset = Offsets.ofTotalSeconds(7200)))
        val day = CalendarDates.of(1970, 1, 1)
        assertEquals(
            listOf("utc"),
            WorkoutHistoryQuery.page(
                    runs,
                    emptyList(),
                    emptyList(),
                    null,
                    null,
                    "",
                    day,
                    day,
                    0,
                    20,
                )
                .entries()!!
                .map { it.id() },
        )
        val next = day.plusDays(1)
        assertEquals(
            listOf("offset"),
            WorkoutHistoryQuery.page(
                    runs,
                    emptyList(),
                    emptyList(),
                    null,
                    null,
                    "",
                    next,
                    next,
                    0,
                    20,
                )
                .entries()!!
                .map { it.id() },
        )
        assertNull(runs.first().startOffset())
        assertFailsWith<IllegalArgumentException> {
            WorkoutHistoryQuery.page(
                runs,
                emptyList(),
                emptyList(),
                null,
                null,
                "",
                next,
                day,
                0,
                20,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            WorkoutHistoryQuery.page(
                runs,
                emptyList(),
                emptyList(),
                null,
                null,
                "",
                null,
                null,
                -1,
                20,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            WorkoutHistoryQuery.page(
                runs,
                emptyList(),
                emptyList(),
                null,
                null,
                "",
                null,
                null,
                0,
                101,
            )
        }
    }

    @Test
    fun rawRecordUnknownsAndMutableAliasesRemainSupportedWithoutInventedValidation() {
        val reasons = mutableListOf("source")
        val match = ComparableRuns.Match(null, null, reasons, Double.NaN)
        reasons.add("retained")
        assertEquals(listOf("source", "retained"), match.reasons())
        assertNull(match.activity())
        assertTrue(match.relevance().isNaN())
        val entries =
            mutableListOf(WorkoutHistoryQuery.Entry(null, null, null, null, null, null, null))
        val page = WorkoutHistoryQuery.Page(entries, -1, true)
        entries.clear()
        assertTrue(page.entries()!!.isEmpty())
        assertNull(WorkoutHistoryQuery.Page(null, 0, false).entries())
        assertNull(LongRunDurability(false, null, null, null, null).explanation())
    }
}
