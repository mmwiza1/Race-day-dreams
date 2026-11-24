package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.baseline.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.time.*

class RunningPlanGenerationContractTest {
    private val start = CalendarDates.of(2026, 11, 15)
    private val at = Instants.ofEpochSecond(start.toEpochDay() * 86400)
    private val empty =
        RunnerBaseline.empty(Instants.ofEpochSecond(at.getEpochSecond() - 84 * 86400), at)
    private val trainingDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
    private val allDays =
        listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY,
        )

    private fun prefs(
        days: Set<DayOfWeek> = trainingDays,
        frequency: Int = 3,
        long: DayOfWeek? = DayOfWeek.FRIDAY,
    ) =
        TrainingPreferences(
            TrainingStrategy.BALANCED,
            days,
            frequency,
            long,
            emptySet(),
            ScheduleType.FIXED,
            UnitSystem.METRIC,
        )

    private fun profile(
        race: LocalDate? = null,
        preferences: TrainingPreferences = prefs(),
        distance: RaceDistance = RaceDistance.FIVE_K,
    ) =
        UserProfile(
            RaceGoal(
                distance,
                if (race == null) "" else "Athlete's race",
                race,
                GoalMode.FINISH,
                null,
            ),
            preferences,
            false,
        )

    private fun established(at: Instant = this.at) =
        BaselineEngine.applyManualOverlay(
            empty,
            ManualBaselineInput(30.0, 3.0, 10.0, null, null, null, null, 8),
            at,
        )

    private fun generate(
        p: UserProfile = profile(),
        b: RunnerBaseline = empty,
        first: LocalDate = start,
        unavailable: Set<LocalDate> = emptySet(),
        maximum: Int? = null,
        distribution: String = "BALANCED",
    ) =
        OriginalTrainingPlanGenerator()
            .generate(PlanRequest(p, b, first, unavailable, emptyList(), maximum, distribution))

    private fun monday(day: LocalDate) = day.minusDays((day.getDayOfWeek().getValue() - 1).toLong())

    @Test
    fun exactLegacyIdentityAndHorizonRemainStableAcrossPlatforms() {
        val plan = generate()
        assertEquals("84236977-a57d-333f-9348-cafe3bbfbe51", plan.id())
        assertEquals(36, plan.workouts().size)
        assertEquals(CalendarDates.of(2027, 2, 6), plan.endDate())
        assertTrue(plan.workouts().all { it.id() == "${plan.id()}:${it.date()}" })
    }

    @Test
    fun missingOrStaleHistoryProducesShortRunWalkSessionsWithoutFabricatedDistance() {
        for (b in
            listOf(empty, established(Instants.ofEpochSecond(at.getEpochSecond() - 365 * 86400)))) {
            val plan = generate(b = b)
            assertTrue(plan.workouts().all { it.distanceKm() == null && it.duration() != null })
            // Everyday run/walk sessions stay within 30 minutes; since Piece 3 the weekly longer
            // session may grow gently beyond that, up to 35 minutes for a 5K goal.
            assertTrue(
                plan
                    .workouts()
                    .filter { it.category() != WorkoutCategory.LONG_RUN }
                    .all { it.duration()!! <= Durations.ofMinutes(30) }
            )
            assertTrue(plan.workouts().all { it.duration()!! <= Durations.ofMinutes(35) })
            assertEquals(
                listOf(
                    WorkoutSegment.Kind.WARM_UP,
                    WorkoutSegment.Kind.RUN,
                    WorkoutSegment.Kind.COOL_DOWN,
                ),
                plan.workouts().first().segments().map { it.kind() },
            )
            assertTrue(
                plan.workouts().first().segments()[1].guidance()!!.contains("walking as needed")
            )
            assertEquals(0, b.activityCount())
        }
    }

    @Test
    fun everyStartWeekdayRespectsAvailabilityAndNeverCreatesDuplicateDates() {
        for (offset in 0L..6L) {
            val first = start.plusDays(offset)
            val blocked = setOf(first.plusDays(3))
            val plan = generate(first = first, unavailable = blocked)
            val dates = plan.workouts().map { it.date() }
            assertEquals(dates.size, dates.distinct().size)
            assertTrue(
                dates.all {
                    it.toEpochDay() >= first.toEpochDay() &&
                        it.toEpochDay() <= plan.endDate().toEpochDay() &&
                        it !in blocked &&
                        it.getDayOfWeek() in trainingDays
                }
            )
            assertTrue(dates.groupBy(::monday).values.all { it.size <= 3 })
        }
    }

    @Test
    fun athleteOwnedRaceSurvivesFinalWeekFrequencyLimitAndPhasePartition() {
        val race = start.plusDays(35)
        val p = profile(race)
        val plan = generate(p, established())
        val event = plan.workouts().single { it.category() == WorkoutCategory.RACE }
        assertEquals(race, event.date())
        assertEquals(5.0, event.distanceKm())
        assertEquals(race, p.raceGoal().raceDate())
        assertEquals(WorkoutCategory.RACE, plan.workouts().last().category())
        assertTrue(
            plan
                .workouts()
                .groupBy { monday(it.date()) }
                .values
                .all { it.size <= p.preferences().runsPerWeek() }
        )
        assertEquals(start, plan.phases().first().startDate())
        assertEquals(race, plan.phases().last().endDate())
        assertEquals(TrainingPhase.Kind.RACE, plan.phases().last().kind())
        for (i in 1 until plan.phases().size) assertEquals(
            plan.phases()[i - 1].endDate().plusDays(1),
            plan.phases()[i].startDate(),
        )
    }

    @Test
    fun unavailableRacePastRaceAndUnsupportedHorizonFailBeforePlanReplacement() {
        val race = start.plusDays(35)
        assertFailsWith<IllegalArgumentException> {
            generate(profile(race), unavailable = setOf(race))
        }
        assertFailsWith<IllegalArgumentException> { generate(profile(start.minusDays(1))) }
        assertFailsWith<IllegalArgumentException> { generate(profile(start.plusDays(36526))) }
    }

    @Test
    fun longHorizonHasDistinctFoundationAndRaceSpecificPhases() {
        val plan =
            generate(profile(start.plusDays(365), distance = RaceDistance.MARATHON), established())
        assertEquals(TrainingPhase.Kind.FOUNDATION, plan.phases().first().kind())
        assertTrue(plan.phases().any { it.kind() == TrainingPhase.Kind.ENDURANCE_DEVELOPMENT })
        assertTrue(plan.phases().any { it.kind() == TrainingPhase.Kind.RACE_SPECIFIC })
        assertEquals(TrainingPhase.Kind.RACE, plan.phases().last().kind())
    }

    @Test
    fun timePreferenceCapsUnknownLoadSessionsWithoutInventingDurationForKnownDistance() {
        val beginner = generate(maximum = 15)
        assertTrue(beginner.workouts().all { it.duration() == Durations.ofMinutes(15) })
        val experienced = generate(b = established(), maximum = 30)
        assertTrue(experienced.workouts().all { it.duration() == null && it.distanceKm() != null })
        assertTrue(
            experienced.workouts().all {
                it.note()!!.contains("no reliable pace-based duration estimate")
            }
        )
    }

    @Test
    fun sessionDistributionChangesOnlyProposedProgrammingAndRetainsEvidence() {
        val b = established()
        val balanced = generate(b = b)
        val fewer = generate(b = b, distribution = "FEWER_LONGER")
        assertTrue(fewer.workouts().groupBy { monday(it.date()) }.values.all { it.size <= 2 })
        val shorter = generate(b = b, distribution = "MORE_SHORTER")
        val originalLong = balanced.workouts().first { it.category() == WorkoutCategory.LONG_RUN }
        val shorterLong = shorter.workouts().first { it.date() == originalLong.date() }
        assertTrue(shorterLong.distanceKm()!! < originalLong.distanceKm()!!)
        assertEquals(30.0, b.weeklyDistanceKm())
        assertEquals(0, b.activityCount())
        assertEquals(at, b.manualRecordedAt())
    }

    @Test
    fun demandingWorkoutsRetainSpacingAcrossCalendarWeeks() {
        val plan = generate(profile(start.plusDays(140)), established())
        val demanding = plan.workouts().filter { WorkoutCategories.isDemanding(it.category()) }
        for (i in 1 until demanding.size) if (demanding[i].category() != WorkoutCategory.RACE)
            assertTrue(demanding[i].date().toEpochDay() - demanding[i - 1].date().toEpochDay() >= 2)
    }

    @Test
    fun schedulerExercisesEveryNonemptyWeekdaySubsetAndBothQualityChoices() {
        val first = start.plusDays(1)
        val blocked = setOf(first.plusDays(2))
        for (mask in 1..127) {
            val chosen = allDays.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.toSet()
            val preferences = prefs(chosen, minOf(3, chosen.size), null)
            for (quality in listOf(false, true)) {
                val schedule =
                    ConstraintScheduler.select(
                        first,
                        first.plusDays(6),
                        preferences.runsPerWeek(),
                        preferences,
                        blocked,
                        emptySet(),
                        quality,
                        null,
                    )
                val days = assertNotNull(schedule.days())
                val capacity =
                    (0L..6L)
                        .map { first.plusDays(it) }
                        .count { it.getDayOfWeek() in chosen && it !in blocked }
                assertEquals(minOf(capacity, preferences.runsPerWeek()), days.size)
                assertEquals(days.size, days.distinct().size)
                assertTrue(days.all { it.getDayOfWeek() in chosen && it !in blocked })
                schedule.quality()?.let {
                    assertTrue(
                        kotlin.math.abs(it.toEpochDay() - schedule.longRun()!!.toEpochDay()) >= 2
                    )
                }
                if (!quality) assertNull(schedule.quality())
            }
        }
    }

    @Test
    fun softStrengthConflictsNeverSilentlyDeleteAvailableSessions() {
        val first = start.plusDays(1)
        val heavy = (0L..7L).map { first.minusDays(1).plusDays(it) }.toSet()
        val schedule =
            ConstraintScheduler.select(
                first,
                first.plusDays(6),
                3,
                prefs(allDays.toSet()),
                emptySet(),
                heavy,
                true,
                null,
            )
        assertEquals(3, schedule.days()!!.size)
        assertTrue(schedule.cost() > 0)
        assertTrue(schedule.explanation()!!.contains("Gym availability alone"))
    }

    @Test
    fun deterministicTieOrderAndRawScheduleRecordAliasRemainCompatible() {
        val first = start.plusDays(1)
        val schedule =
            ConstraintScheduler.select(
                first,
                first.plusDays(6),
                2,
                prefs(allDays.toSet(), 2),
                emptySet(),
                emptySet(),
                true,
                null,
            )
        assertEquals(listOf(first, first.plusDays(4)), schedule.days())
        assertEquals(first.plusDays(4), schedule.longRun())
        assertEquals(first, schedule.quality())
        assertFails { (schedule.days() as MutableList).clear() }
        val raw = mutableListOf(first)
        val record = ConstraintScheduler.Schedule(raw, null, null, -1, null)
        raw.clear()
        assertTrue(record.days()!!.isEmpty())
        assertNull(record.explanation())
    }
}
