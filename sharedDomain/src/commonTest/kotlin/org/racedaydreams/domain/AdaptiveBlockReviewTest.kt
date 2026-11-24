package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.baseline.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.time.*

/** Training Piece 5A: the weekly review reassesses the coming block from actual running. */
class AdaptiveBlockReviewTest {
    // Monday of the review week, and a Wednesday "today" inside it.
    private val monday = CalendarDates.of(2026, 11, 23)
    private val today = monday.plusDays(2)

    private fun noon(day: LocalDate) = Instants.ofEpochSecond(day.toEpochDay() * 86400 + 43200)

    private fun easy(day: LocalDate, km: Double, category: WorkoutCategory) =
        PlannedWorkout(
            "w$day",
            day,
            category,
            km,
            null,
            listOf(
                WorkoutSegment(WorkoutSegment.Kind.RUN, km, null, null, null, null, 3.0, "easy")
            ),
            "planned",
        )

    /** A Piece 4 threshold session: 2 km warm-up, 2 × 1.6 km with a 2 min jog, 1 km cool-down. */
    private fun threshold(day: LocalDate) =
        PlannedWorkout(
            "w$day",
            day,
            WorkoutCategory.TEMPO_THRESHOLD_RUN,
            8.0,
            null,
            listOf(
                WorkoutSegment(WorkoutSegment.Kind.WARM_UP, 3.6, null, null, null, null, 3.0, "w"),
                WorkoutSegment(WorkoutSegment.Kind.RUN, 1.6, null, 290.0, 300.0, null, 6.0, "r1"),
                WorkoutSegment(
                    WorkoutSegment.Kind.RECOVERY,
                    null,
                    Durations.ofSeconds(120),
                    null,
                    null,
                    null,
                    2.0,
                    "jog",
                ),
                WorkoutSegment(WorkoutSegment.Kind.RUN, 1.6, null, 290.0, 300.0, null, 6.0, "r2"),
                WorkoutSegment(WorkoutSegment.Kind.COOL_DOWN, 1.0, null, null, null, null, 2.0, "c"),
            ),
            "Threshold",
        )

    /**
     * Tuesday 8 km easy, Thursday 8 km easy (a threshold session in future weeks), Sunday 10 km
     * long: 26 km a week, four weeks back and five ahead.
     */
    private fun version(
        distance: RaceDistance = RaceDistance.HALF_MARATHON,
        race: LocalDate? = monday.plusWeeks(12).minusDays(1),
        mode: GoalMode = GoalMode.FINISH,
        strategy: TrainingStrategy = TrainingStrategy.BALANCED,
    ): PlanVersion {
        val workouts = mutableListOf<PlannedWorkout>()
        for (week in -4..4) {
            val m = monday.plusWeeks(week.toLong())
            workouts += easy(m.plusDays(1), 8.0, WorkoutCategory.EASY_RUN)
            workouts +=
                if (week >= 0) threshold(m.plusDays(3))
                else easy(m.plusDays(3), 8.0, WorkoutCategory.EASY_RUN)
            workouts += easy(m.plusDays(6), 10.0, WorkoutCategory.LONG_RUN)
        }
        val start = monday.minusDays(28)
        return PlanVersion(
            "v",
            "p",
            1,
            noon(start),
            "",
            UserProfile(
                RaceGoal(
                    distance,
                    "",
                    race,
                    mode,
                    if (mode == GoalMode.TARGET_TIME) Durations.ofMinutes(100) else null,
                ),
                TrainingPreferences(
                    strategy,
                    setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SUNDAY),
                    3,
                    DayOfWeek.SUNDAY,
                    emptySet(),
                    ScheduleType.FIXED,
                    UnitSystem.METRIC,
                ),
                false,
            ),
            "",
            TrainingPlan("p", start, race ?: monday.plusWeeks(5), emptyList(), workouts),
        )
    }

    /** Runs linked to the planned workouts before [monday], at [share] of the planned distance. */
    private fun history(
        v: PlanVersion,
        share: Double,
        skip: (PlannedWorkout) -> Boolean = { false },
    ): List<ActivityRecord> =
        v.plan()
            .workouts()
            .filter { it.date().toEpochDay() < monday.toEpochDay() && !skip(it) }
            .map { w ->
                ActivityRecord(
                    "a${w.id()}",
                    SourceIdentity("manual", "a${w.id()}", null),
                    noon(w.date()),
                    Instants.ofEpochSecond(noon(w.date()).getEpochSecond() + 3000),
                    null,
                    null,
                    w.distanceKm()!! * share * 1000,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "",
                    emptyList(),
                    emptyList(),
                    w.id(),
                    null,
                )
            }

    private fun review(v: PlanVersion, runs: List<ActivityRecord>) =
        WeeklyCoaching.review(
            v,
            listOf(v),
            runs,
            emptyList(),
            emptyList(),
            emptyList(),
            today,
            null,
        )

    private fun weekTotal(workouts: List<PlannedWorkout>, week: Int): Double {
        val from = monday.plusWeeks(week.toLong()).toEpochDay()
        return workouts
            .filter { it.date().toEpochDay() in from until from + 7 }
            .sumOf { it.distanceKm() ?: 0.0 }
    }

    @Test
    fun generatedProgrammesFeedActualRunsIntoFourConsecutiveReviews() {
        val start = monday.minusDays(56)
        val freshness = noon(start.minusDays(1))
        val scenarios =
            listOf(
                Triple(RaceDistance.FIVE_K, TrainingStrategy.BALANCED, null),
                Triple(RaceDistance.FIVE_K, TrainingStrategy.PERFORMANCE, 27.0),
                Triple(RaceDistance.TEN_K, TrainingStrategy.BALANCED, 26.0),
                Triple(RaceDistance.HALF_MARATHON, TrainingStrategy.PERFORMANCE, 32.0),
                Triple(RaceDistance.MARATHON, TrainingStrategy.BALANCED, 45.0),
                Triple(RaceDistance.GENERAL_FITNESS, TrainingStrategy.ZONE_2, 22.0),
            )
        for ((distance, strategy, weeklyKm) in scenarios) {
            val race =
                if (distance == RaceDistance.GENERAL_FITNESS) null
                else start.plusWeeks(20).minusDays(1)
            val profile = version(distance, race, strategy = strategy).goalSnapshot()
            val empty = RunnerBaseline.empty(noon(start.minusDays(84)), noon(start))
            val baseline =
                if (weeklyKm == null) empty
                else
                    BaselineEngine.applyManualOverlay(
                        empty,
                        ManualBaselineInput(
                            weeklyKm,
                            3.0,
                            weeklyKm * .4,
                            Durations.ofMinutes(27),
                            null,
                            null,
                            395.0,
                            8,
                        ),
                        freshness,
                    )
            val plan = OriginalTrainingPlanGenerator().generate(profile, baseline, start)
            val version =
                PlanVersion(
                    "simulation",
                    plan.id(),
                    1,
                    noon(start),
                    "Initial",
                    profile,
                    "Fixture",
                    plan,
                )
            for (offset in 3..6) {
                val end = start.plusWeeks(offset.toLong())
                val actual =
                    plan
                        .workouts()
                        .filter { it.date().toEpochDay() < end.toEpochDay() }
                        .map { workout ->
                            val id = "actual-${workout.id()}"
                            val from = noon(workout.date())
                            ActivityRecord(
                                id,
                                SourceIdentity("manual", id, null),
                                from,
                                Instants.ofEpochSecond(from.getEpochSecond() + 1800),
                                null,
                                null,
                                workout.distanceKm()?.times(1000.0),
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                "",
                                emptyList(),
                                emptyList(),
                                workout.id(),
                                null,
                            )
                        }
                val review =
                    WeeklyCoaching.review(
                        version,
                        listOf(version),
                        actual,
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        end.plusDays(1),
                        null,
                    )
                assertTrue(review.completed() > 0, "$distance $strategy week $offset")
                assertEquals(
                    listOf("keep"),
                    review.choices().map { it.id() },
                    "$distance $strategy week $offset",
                )
                assertEquals(
                    plan,
                    WeeklyCoaching.apply(version, review, review.choices().single(), emptySet()),
                )
            }
            if (weeklyKm == null) {
                assertTrue(
                    plan
                        .workouts()
                        .filter { it.category() != WorkoutCategory.RACE }
                        .all { it.distanceKm() == null }
                )
            }
            if (strategy == TrainingStrategy.ZONE_2) {
                assertTrue(plan.workouts().none { WorkoutCategories.isQuality(it.category()) })
            }
        }
    }

    @Test
    fun missingDataIsNotTreatedAsInactivityAndChangesNothing() {
        val v = version()
        val r = review(v, emptyList())
        assertEquals(listOf("keep"), r.choices().map { it.id() })
        assertTrue(
            r.observations().any { it.contains("check that your watch or health app is syncing") }
        )
        assertTrue(r.recommendation()!!.startsWith("No adjustment required"))
    }

    @Test
    fun runningWellBelowTheComingWeeksOffersAGradualRebuild() {
        val v = version()
        // Every run was done, but at half the planned distance: about 13 km a week against 26.
        val r = review(v, history(v, .5))
        val rebuild = r.choices().single { it.id() == "rebuild" }
        assertTrue(r.recommendation()!!.contains("about 13.0 km a week"), r.recommendation())
        // Four weeks, rising about 5% a week from what was actually run; nothing in the past.
        for (week in 0 until 4) {
            val total = weekTotal(rebuild.replacements(), week)
            assertTrue(total <= 13.0 * (1 + .05 * week) + .35, "week $week: $total")
        }
        assertTrue(rebuild.replacements().none { it.date().toEpochDay() < monday.toEpochDay() })
        assertTrue(
            rebuild.replacements().all { it.note()!!.contains("missed sessions are not made up") }
        )
        // Nothing is applied until accepted; keeping leaves the plan unchanged.
        assertEquals(v.plan(), WeeklyCoaching.apply(v, r, r.choices().first(), emptySet()))
        val applied = WeeklyCoaching.apply(v, r, rebuild, emptySet())
        // History before the review week is untouched; workout ids and dates are preserved.
        assertEquals(
            v.plan().workouts().filter { it.date().toEpochDay() < monday.toEpochDay() },
            applied.workouts().filter { it.date().toEpochDay() < monday.toEpochDay() },
        )
        assertEquals(v.plan().workouts().map { it.id() }, applied.workouts().map { it.id() })
        // A completed session can never be changed by an accepted proposal.
        assertFailsWith<IllegalArgumentException> {
            WeeklyCoaching.apply(v, r, rebuild, setOf(rebuild.replacements().first().id()))
        }
    }

    @Test
    fun acceptedRebuildDoesNotSnapBackIfTheRunnerDoesNotReopenTheReview() {
        val original = version()
        val review = review(original, history(original, .5))
        val rebuild = review.choices().single { it.id() == "rebuild" }
        val applied = WeeklyCoaching.apply(original, review, rebuild, emptySet())
        val fourth = weekTotal(applied.workouts(), 3)
        assertEquals(26.0, weekTotal(original.plan().workouts(), 4), 1e-9)
        assertTrue(fourth in 14.5..14.95, "Fourth week was $fourth km")
        val fifth = weekTotal(applied.workouts(), 4)
        assertTrue(fifth in (fourth - .3)..fourth, "Fourth week $fourth km, fifth week $fifth km")
        assertEquals(original.plan().workouts().map { it.id() }, applied.workouts().map { it.id() })
    }

    @Test
    fun scaledQualityDistanceIncludesAnAllowanceForTimedRecoveriesWithoutRequiringDistance() {
        val session = threshold(monday.plusDays(3))
        for (factor in listOf(.85, .88, .94, 1.0)) {
            val workout = BlockReassessment.scale(session, factor, "Rebuilt.")
            val segments = workout.segments()
            val estimate = QualityWorkouts.estimatedRecoveryKm(segments)
            assertEquals(.36, estimate, 1e-9)
            assertEquals(
                workout.distanceKm()!!,
                segments.sumOf { it.distanceKm() ?: 0.0 } + estimate,
                .05001,
            )
            val recovery = segments.single { it.kind() == WorkoutSegment.Kind.RECOVERY }
            assertNull(recovery.distanceKm())
            assertEquals(120, recovery.duration()!!.getSeconds().toInt())
        }
    }

    @Test
    fun aRebuiltWeekKeepsStructuredSessionsIntactOrMakesThemEasy() {
        val session = threshold(monday.plusDays(3))
        val smaller = BlockReassessment.scale(session, .85, "Rebuilt.")
        // Repetitions and recoveries are kept; only the warm-up and cool-down shrink.
        assertEquals(WorkoutCategory.TEMPO_THRESHOLD_RUN, smaller.category())
        val work = { w: PlannedWorkout -> w.segments().filter { (it.rpe() ?: 0.0) > 3.0 } }
        assertEquals(work(session), work(smaller))
        assertEquals(1, smaller.segments().count { it.kind() == WorkoutSegment.Kind.RECOVERY })
        assertEquals(6.8, smaller.distanceKm()!!, 1e-9)
        // Too short for the session: an easy run instead, never a squeezed session.
        val easy = BlockReassessment.scale(session, .5, "Rebuilt.")
        assertEquals(WorkoutCategory.EASY_RUN, easy.category())
        assertTrue(easy.segments().all { it.rpe() == 3.0 })
    }

    @Test
    fun missedRecurringDaysPointToTheRoutineNotARebuild() {
        val v = version()
        val r = review(v, history(v, 1.0) { it.date().getDayOfWeek() == DayOfWeek.THURSDAY })
        assertEquals(listOf("keep"), r.choices().map { it.id() })
        assertTrue(r.observations().any { it.contains("Editing your regular days") })
    }

    @Test
    fun consistentlyRunningMoreOffersOnlyCautiousEasyProgression() {
        val v = version()
        // Every planned run done at 130% of its distance for four weeks.
        val r = review(v, history(v, 1.3))
        val progress = r.choices().single { it.id() == "progress" }
        // Only easy and long runs grow, by at most 10% a week; quality sessions are untouched.
        assertTrue(progress.replacements().none { WorkoutCategories.isQuality(it.category()) })
        for (week in 0 until 2) {
            val before = weekTotal(v.plan().workouts(), week)
            val after = weekTotal(WeeklyCoaching.apply(v, r, progress, emptySet()).workouts(), week)
            assertTrue(after <= before * 1.10 + 1e-9, "week $week $before -> $after")
            assertTrue(after > before)
        }
        // Only the next two weeks.
        assertTrue(
            progress.replacements().all {
                it.date().toEpochDay() < monday.plusWeeks(2).toEpochDay()
            }
        )
        // A long run grows no more than 10% beyond the longest recent recorded run (13 km).
        assertTrue(
            progress
                .replacements()
                .filter { it.category() == WorkoutCategory.LONG_RUN }
                .all { it.distanceKm()!! <= 13.0 * 1.10 + 1e-9 }
        )
    }

    @Test
    fun missedSessionsOrUnknownWeeksNeverLeadToProgression() {
        val v = version()
        // One run missed: no progression, even though the others were longer (no catch-up).
        var first = true
        val oneMissed =
            history(v, 1.5) {
                val skip = first && it.category() == WorkoutCategory.EASY_RUN
                if (skip) first = false
                skip
            }
        assertTrue(review(v, oneMissed).choices().none { it.id() == "progress" })
        // Two unrecorded weeks are unknown, not rest: no progression, and it is said.
        val gaps = history(v, 1.3) { it.date().toEpochDay() < monday.minusDays(14).toEpochDay() }
        val r = review(v, gaps)
        assertTrue(r.choices().none { it.id() == "progress" })
        assertTrue(r.observations().any { it.contains("treated as unknown, not as rest") })
        // General fitness maintains by default: no progression offered.
        val fitness = version(RaceDistance.GENERAL_FITNESS, null)
        assertTrue(review(fitness, history(fitness, 1.3)).choices().none { it.id() == "progress" })
    }

    @Test
    fun strainOrABreakTakesPriorityOverRebuildOrProgression() {
        val v = version()
        // Last run more than two weeks ago: the existing return bridge, not a rebuild.
        val old = history(v, .5) { it.date().toEpochDay() >= monday.minusDays(14).toEpochDay() }
        val r = review(v, old)
        assertTrue(r.choices().any { it.id() == "return" })
        assertTrue(r.choices().none { it.id() == "rebuild" || it.id() == "progress" })
    }

    @Test
    fun missedTrainingCloseToTheRaceIsExplainedHonestly() {
        val race = monday.plusWeeks(5).minusDays(1)
        val v = version(race = race, mode = GoalMode.TARGET_TIME)
        // Only Sunday long runs recorded in the last four weeks: 4 of 12 planned runs.
        val r = review(v, history(v, 1.0) { it.category() != WorkoutCategory.LONG_RUN })
        val note = r.observations().single { it.contains("may no longer fully support") }
        assertTrue(note.contains("or your target time"))
        assertTrue(note.contains("Missed runs are not made up"))
        // The race goal itself is never changed by the review.
        assertTrue(
            r.choices().flatMap { it.replacements() }.none { it.category() == WorkoutCategory.RACE }
        )
    }
}
