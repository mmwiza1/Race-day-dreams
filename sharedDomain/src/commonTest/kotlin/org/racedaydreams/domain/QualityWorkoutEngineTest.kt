package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.baseline.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.time.*

/** Training Piece 4: race-specific workouts chosen and dosed from current evidence. */
class QualityWorkoutEngineTest {
    private val start = CalendarDates.of(2026, 11, 23)
    private val at = Instants.ofEpochSecond(start.toEpochDay() * 86400)
    private val empty =
        RunnerBaseline.empty(Instants.ofEpochSecond(at.getEpochSecond() - 84 * 86400), at)
    private val four =
        setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    private fun baseline(
        weekly: Double,
        longest: Double,
        tenKMinutes: Long? = 50,
        consistent: Int = 8,
        runs: Double = 4.0,
    ) =
        BaselineEngine.applyManualOverlay(
            empty,
            ManualBaselineInput(
                weekly,
                runs,
                longest,
                null,
                tenKMinutes?.let { Durations.ofMinutes(it) },
                null,
                null,
                consistent,
            ),
            at,
        )

    private fun plan(
        distance: RaceDistance,
        weeks: Long,
        b: RunnerBaseline,
        strategy: TrainingStrategy = TrainingStrategy.BALANCED,
        target: Duration? = null,
    ): TrainingPlan {
        val race = if (distance == RaceDistance.GENERAL_FITNESS) null else raceDay(weeks)
        val p =
            UserProfile(
                RaceGoal(
                    distance,
                    if (race == null) "" else "Race",
                    race,
                    if (target == null) GoalMode.FINISH else GoalMode.TARGET_TIME,
                    target,
                ),
                TrainingPreferences(
                    strategy,
                    four,
                    4,
                    DayOfWeek.SUNDAY,
                    emptySet(),
                    ScheduleType.FIXED,
                    UnitSystem.METRIC,
                ),
                false,
            )
        return OriginalTrainingPlanGenerator()
            .generate(PlanRequest(p, b, start, emptySet(), emptyList(), null, "BALANCED"))
    }

    private fun raceDay(weeks: Long) = start.plusWeeks(weeks).minusDays(1)

    private fun quality(plan: TrainingPlan) =
        plan.workouts().filter { WorkoutCategories.isQuality(it.category()) }

    private fun hasStrides(w: PlannedWorkout) =
        w.segments().any { it.guidance()?.contains("strides:") == true }

    private fun work(w: PlannedWorkout) =
        w.segments().filter { it.kind() == WorkoutSegment.Kind.RUN && (it.rpe() ?: 0.0) >= 6.0 }

    @Test
    fun eachDistanceGetsItsOwnSessionsWithRecoveries() {
        val expected =
            mapOf(
                RaceDistance.FIVE_K to WorkoutCategory.INTERVALS,
                RaceDistance.TEN_K to WorkoutCategory.INTERVALS,
                RaceDistance.HALF_MARATHON to WorkoutCategory.RACE_PACE_RUN,
                RaceDistance.MARATHON to WorkoutCategory.RACE_PACE_RUN,
            )
        for ((distance, specific) in expected) {
            val weeks = if (distance == RaceDistance.MARATHON) 18L else 16L
            val q = quality(plan(distance, weeks, baseline(40.0, 16.0)))
            val categories = q.map { it.category() }.toSet()
            // Threshold work builds first; the race-specific session appears closer to the race.
            assertTrue(WorkoutCategory.TEMPO_THRESHOLD_RUN in categories, distance.name)
            assertTrue(specific in categories, "${distance.name} $categories")
            val firstSpecific = q.first { it.category() == specific }
            assertEquals(WorkoutCategory.TEMPO_THRESHOLD_RUN, q.first().category())
            assertTrue(firstSpecific.date().toEpochDay() > q.first().date().toEpochDay())
            for (w in q) {
                val kinds = w.segments().map { it.kind() }
                assertEquals(WorkoutSegment.Kind.WARM_UP, kinds.first(), w.note())
                assertEquals(WorkoutSegment.Kind.COOL_DOWN, kinds.last(), w.note())
                // Repetitions are separated by recovery segments (marathon pace is one block).
                val reps = work(w).size
                val recoveries = kinds.count { it == WorkoutSegment.Kind.RECOVERY }
                if (reps > 1) assertEquals(reps - 1, recoveries, w.note())
                // Segments add up to the planned distance, with recoveries carrying time only.
                assertTrue(
                    w.segments()
                        .filter { it.kind() == WorkoutSegment.Kind.RECOVERY }
                        .all { it.distanceKm() == null && it.duration() != null }
                )
                assertTrue(w.note()!!.contains("every other run stays easy"))
            }
        }
    }

    @Test
    fun shortRacesNeverGetLongContinuousNearRaceEffortBlocks() {
        for (strategy in
            listOf(TrainingStrategy.BALANCED, TrainingStrategy.PERFORMANCE)) for (distance in
            listOf(RaceDistance.FIVE_K, RaceDistance.TEN_K)) {
            val q = quality(plan(distance, 12, baseline(40.0, 14.0, 45), strategy))
            assertTrue(q.isNotEmpty())
            for (w in q) {
                assertTrue(w.category() != WorkoutCategory.RACE_PACE_RUN, w.note())
                // Hard pieces are short repetitions: at most 1.6 km (threshold), 1 km at
                // race effort.
                for (segment in work(w)) {
                    val limit = if ((segment.rpe() ?: 0.0) > 6.0) 1.0 else 1.6
                    assertTrue(segment.distanceKm()!! <= limit + 1e-9, w.note())
                }
            }
        }
    }

    @Test
    fun pacesComeFromRecentResultsNeverFromAGoalTimeAlone() {
        // No race result, but an ambitious target: sessions are prescribed by effort only.
        val noResult =
            quality(
                plan(
                    RaceDistance.HALF_MARATHON,
                    16,
                    baseline(30.0, 12.0, null),
                    target = Durations.ofMinutes(95),
                )
            )
        assertTrue(noResult.isNotEmpty())
        for (w in noResult) {
            assertTrue(w.segments().all { it.minimumSecondsPerKm() == null }, w.note())
            assertTrue(work(w).all { it.guidance()!!.contains("a goal time alone is not used") })
        }
        // A recent 10K of 50:00: the same paces whatever the target, from the result.
        val finish = quality(plan(RaceDistance.HALF_MARATHON, 16, baseline(30.0, 12.0)))
        val target =
            quality(
                plan(
                    RaceDistance.HALF_MARATHON,
                    16,
                    baseline(30.0, 12.0),
                    target = Durations.ofMinutes(95),
                )
            )
        fun paces(q: List<PlannedWorkout>) =
            q.flatMap { w -> work(w).map { it.minimumSecondsPerKm() } }
        assertEquals(paces(finish), paces(target))
        val expected = QualityWorkouts.paces(baseline(30.0, 12.0))
        val threshold =
            finish
                .first { it.category() == WorkoutCategory.TEMPO_THRESHOLD_RUN }
                .let { work(it).first() }
        assertEquals(expected.threshold!! * .98, threshold.minimumSecondsPerKm()!!, 0.01)
        // Threshold sits between 10K and half-marathon pace.
        assertTrue(expected.threshold!! > expected.tenK!! && expected.threshold!! < expected.half!!)
        assertTrue(threshold.guidance()!!.contains("your recent 10K (50:00)"))
    }

    @Test
    fun intensityIsGradedFromCurrentEvidence() {
        // No running history: run/walk only, nothing structured.
        val beginner = plan(RaceDistance.HALF_MARATHON, 16, empty)
        assertTrue(quality(beginner).isEmpty())
        assertTrue(beginner.workouts().none(::hasStrides))
        // Aerobic choices (Zone 2, general fitness) stay easy.
        assertTrue(
            quality(plan(RaceDistance.TEN_K, 12, baseline(30.0, 12.0), TrainingStrategy.ZONE_2))
                .isEmpty()
        )
        assertTrue(quality(plan(RaceDistance.GENERAL_FITNESS, 12, baseline(30.0, 12.0))).isEmpty())
        // Light or short-lived running: strides only, with guidance on when sessions unlock.
        for (b in listOf(baseline(12.0, 6.0, null), baseline(30.0, 12.0, consistent = 4))) {
            val light = plan(RaceDistance.HALF_MARATHON, 16, b)
            assertTrue(quality(light).isEmpty())
            val strides = light.workouts().filter(::hasStrides)
            assertTrue(strides.isNotEmpty())
            assertTrue(strides.all { it.category() != WorkoutCategory.LONG_RUN })
            assertTrue(strides.first().note()!!.contains("rebuild your plan then"))
        }
        // Few, short running days: a quality day too short for a session keeps strides, so each
        // week before race week has either a session or strides.
        val short =
            plan(RaceDistance.HALF_MARATHON, 12, baseline(20.0, 10.0, runs = 3.0))
                .workouts()
                .filter { it.category() != WorkoutCategory.RACE }
        val weeks = short.groupBy { (it.date().toEpochDay() - start.toEpochDay()) / 7 }
        for ((week, runs) in weeks) assertTrue(
            runs.any { WorkoutCategories.isQuality(it.category()) || hasStrides(it) },
            "week ${week + 1}",
        )
        // Performance adds repetitions only where its own gate supports it.
        fun maxReps(strategy: TrainingStrategy) =
            quality(plan(RaceDistance.TEN_K, 12, baseline(40.0, 14.0, 45), strategy)).maxOf {
                work(it).size
            }
        assertTrue(maxReps(TrainingStrategy.PERFORMANCE) > maxReps(TrainingStrategy.BALANCED))
    }

    @Test
    fun mostRunningStaysEasyWithAtMostOneSessionAWeek() {
        for (distance in
            listOf(
                RaceDistance.FIVE_K,
                RaceDistance.TEN_K,
                RaceDistance.HALF_MARATHON,
                RaceDistance.MARATHON,
            )) {
            val p = plan(distance, 18, baseline(40.0, 16.0))
            val q = quality(p)
            val byWeek = q.groupBy { (it.date().toEpochDay() - start.toEpochDay()) / 7 }
            assertTrue(byWeek.values.all { it.size == 1 }, distance.name)
            // Structured sessions never sit next to the long run.
            val longs = p.workouts().filter { it.category() == WorkoutCategory.LONG_RUN }
            for (w in q) assertTrue(
                longs.none { kotlin.math.abs(it.date().toEpochDay() - w.date().toEpochDay()) < 2 },
                "${distance.name} ${w.date()}",
            )
            // Hard running is a small share of the plan's distance.
            val hardKm = q.sumOf { w -> work(w).sumOf { it.distanceKm()!! } }
            val totalKm = p.workouts().sumOf { it.distanceKm() ?: 0.0 }
            assertTrue(hardKm < totalKm * .12, "${distance.name} $hardKm of $totalKm")
            // No structured session in the plan's first weeks beyond one a week, and none in race
            // week; race week keeps at most a few strides, at least two days before the race.
            val race = raceDay(18)
            assertTrue(q.none { race.toEpochDay() - it.date().toEpochDay() < 7 })
            assertTrue(
                p.workouts().filter(::hasStrides).none {
                    race.toEpochDay() - it.date().toEpochDay() < 2
                }
            )
        }
    }

    @Test
    fun theTaperKeepsAShorterFamiliarSession() {
        val race = raceDay(16)
        val q = quality(plan(RaceDistance.HALF_MARATHON, 16, baseline(40.0, 16.0)))
        val taper = q.filter { race.toEpochDay() - it.date().toEpochDay() in 7..13 }
        assertTrue(taper.isNotEmpty())
        for (w in taper) {
            assertTrue(w.note()!!.contains("shorter, familiar version"), w.note())
            assertEquals(2, work(w).size)
        }
    }

    @Test
    fun sessionsAreDosedAndFitTheirDay() {
        val paces = QualityWorkouts.paces(baseline(30.0, 12.0))
        val controlled = QualityWorkouts.Readiness.CONTROLLED
        // Strides-level readiness never gets a structured session.
        assertNull(
            QualityWorkouts.build(
                QualityWorkouts.Template.THRESHOLD,
                10.0,
                QualityWorkouts.Readiness.STRIDES,
                paces,
                false,
            )
        )
        // Too short a day for even the smallest dose: the day stays easy.
        assertNull(
            QualityWorkouts.build(QualityWorkouts.Template.THRESHOLD, 4.0, controlled, paces, false)
        )
        // A reduced dose (taper or after hard legs) is the smallest version.
        val reduced =
            QualityWorkouts.build(
                QualityWorkouts.Template.INTERVALS_5K,
                12.0,
                controlled,
                paces,
                true,
            )
        assertEquals(
            4,
            reduced!!.segments.count {
                it.kind() == WorkoutSegment.Kind.RUN && (it.rpe() ?: 0.0) >= 6
            },
        )
        for (template in QualityWorkouts.Template.entries) for (km in listOf(7.0, 9.0, 12.0)) {
            val s = QualityWorkouts.build(template, km, controlled, paces, false) ?: continue
            val covered =
                s.segments.sumOf { it.distanceKm() ?: (it.duration()!!.getSeconds() * .18 / 60) }
            assertEquals(km, covered, 0.15, "$template $km")
            assertTrue(s.segments.first().distanceKm()!! >= 1.0)
        }
        // Short races and the half use repetitions; only the marathon practises a steady block.
        assertEquals(
            listOf(
                QualityWorkouts.Template.INTERVALS_5K,
                QualityWorkouts.Template.THRESHOLD,
                QualityWorkouts.Template.THRESHOLD,
            ),
            listOf(0, 2, 5).map {
                QualityWorkouts.template(
                    RaceDistance.FIVE_K,
                    if (it == 5) TrainingPhase.Kind.FOUNDATION else TrainingPhase.Kind.PEAK,
                    it,
                )
            },
        )
    }

    @Test
    fun aRunnerReturningFromABreakStartsWithEasyRunningOnly() {
        // Three 10 km runs a week for 12 weeks, with a three-week break ending two weeks ago.
        val runs =
            (1L..84L)
                .filter { it % 7 in setOf(1L, 3L, 5L) && it !in 50L..70L }
                .map { day ->
                    val begin = Instants.ofEpochSecond(at.getEpochSecond() - (85 - day) * 86400)
                    ActivityRecord(
                        "run-$day",
                        SourceIdentity("health-connect", "run-$day", "watch"),
                        begin,
                        Instants.ofEpochSecond(begin.getEpochSecond() + 3600),
                        null,
                        null,
                        10000.0,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "",
                        emptyList(),
                        emptyList(),
                        null,
                        null,
                    )
                }
        val derived =
            BaselineEngine.derive(
                runs,
                Instants.ofEpochSecond(at.getEpochSecond() - 84 * 86400),
                at,
            )
        assertTrue(derived.gaps().any { it.daysWithoutRecordedRun() >= 14 })
        val returning =
            BaselineEngine.applyManualOverlay(
                derived,
                ManualBaselineInput(null, null, null, null, Durations.ofMinutes(50), null, null, 8),
                at,
            )
        val p = plan(RaceDistance.TEN_K, 16, returning)
        val q = quality(p)
        // No structured session for the first three weeks, and no strides in the first week.
        assertTrue(q.isNotEmpty())
        assertTrue(
            q.all { it.date().toEpochDay() - start.toEpochDay() >= 21 },
            q.first().date().toString(),
        )
        assertTrue(
            p.workouts().filter(::hasStrides).all {
                it.date().toEpochDay() - start.toEpochDay() >= 7
            }
        )
        // The same runner without the break gets a session sooner.
        val steady = quality(plan(RaceDistance.TEN_K, 16, baseline(30.0, 10.0)))
        assertTrue(steady.first().date().toEpochDay() < q.first().date().toEpochDay())
    }

    /** The audit R-02 runner: 30 km a week, 12 km longest, eight consistent weeks, a 10K result. */
    private fun twoDayPlan(requested: Int, distribution: String, strategy: TrainingStrategy) =
        OriginalTrainingPlanGenerator()
            .generate(
                PlanRequest(
                    UserProfile(
                        RaceGoal(
                            RaceDistance.HALF_MARATHON,
                            "Race",
                            raceDay(25),
                            GoalMode.FINISH,
                            null,
                        ),
                        TrainingPreferences(
                            strategy,
                            setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                            requested,
                            DayOfWeek.MONDAY,
                            emptySet(),
                            ScheduleType.FIXED,
                            UnitSystem.METRIC,
                        ),
                        false,
                    ),
                    baseline(30.0, 12.0, 45),
                    start,
                    emptySet(),
                    emptyList(),
                    null,
                    distribution,
                )
            )

    @Test
    fun equivalentTwoDaySchedulesGetTheSameIntensity() {
        // Audit R-02: three requested days reduced to two by Fewer longer runs used to get 19
        // structured sessions, while choosing two days directly got none. Intensity now follows
        // the frequency the plan actually schedules.
        for (strategy in listOf(TrainingStrategy.BALANCED, TrainingStrategy.PERFORMANCE)) {
            val reduced = twoDayPlan(3, "FEWER_LONGER", strategy)
            val direct = twoDayPlan(2, "BALANCED", strategy)
            fun runsPerWeek(p: TrainingPlan) =
                p.workouts()
                    .filter { it.category() != WorkoutCategory.RACE }
                    .groupBy { (it.date().toEpochDay() - start.toEpochDay()) / 7 }
                    .values
                    .maxOf { it.size }
            // Both really are two-day schedules.
            assertEquals(2, runsPerWeek(reduced), strategy.name)
            assertEquals(2, runsPerWeek(direct), strategy.name)
            // The same intensity: no structured sessions, weekly strides on the easy run, and an
            // explanation that says why two days don't unlock sessions.
            assertEquals(quality(direct).size, quality(reduced).size, strategy.name)
            assertTrue(quality(reduced).isEmpty(), strategy.name)
            for (p in listOf(reduced, direct)) {
                val strides = p.workouts().filter(::hasStrides)
                assertTrue(strides.size >= 20, "${strategy.name} strides ${strides.size}")
                assertTrue(strides.none { it.category() == WorkoutCategory.LONG_RUN })
                assertTrue(strides.first().note()!!.contains("need at least three running days"))
            }
        }
        // The same runner on three effective days still gets one structured session a week.
        val three = twoDayPlan(3, "BALANCED", TrainingStrategy.PERFORMANCE)
        assertTrue(quality(three).isNotEmpty())
        assertTrue(
            quality(three)
                .groupBy { (it.date().toEpochDay() - start.toEpochDay()) / 7 }
                .values
                .all { it.size == 1 }
        )
    }
}
