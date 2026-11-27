package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.baseline.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.presentation.WeeklyReviewData
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

class RunningCoachingReviewContractTest {
    private val today = CalendarDates.of(2026, 11, 15)
    private val now = instant(today, 43200)

    private fun instant(day: LocalDate, second: Long = 0) =
        Instants.ofEpochSecond(day.toEpochDay() * 86400 + second)

    private fun profile() =
        UserProfile(
            RaceGoal(RaceDistance.HALF_MARATHON, "", null, GoalMode.FINISH, null),
            TrainingPreferences(
                TrainingStrategy.BALANCED,
                setOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY,
                    DayOfWeek.SATURDAY,
                    DayOfWeek.SUNDAY,
                ),
                4,
                DayOfWeek.SUNDAY,
                emptySet(),
                ScheduleType.FIXED,
                UnitSystem.METRIC,
            ),
            false,
        )

    private fun workout(
        id: String,
        day: LocalDate,
        kind: WorkoutCategory = WorkoutCategory.EASY_RUN,
        km: Double = 10.0,
    ) =
        PlannedWorkout(
            id,
            day,
            kind,
            km,
            null,
            listOf(
                WorkoutSegment(
                    WorkoutSegment.Kind.RUN,
                    km,
                    null,
                    300.0,
                    360.0,
                    null,
                    6.0,
                    "original",
                )
            ),
            "original",
        )

    private fun version(workouts: List<PlannedWorkout> = emptyList()) =
        PlanVersion(
            "v",
            "p",
            1,
            now,
            "",
            profile(),
            "",
            TrainingPlan("p", today.minusDays(28), today.plusDays(28), emptyList(), workouts),
        )

    private fun run(
        id: String,
        w: PlannedWorkout,
        rpe: Double? = null,
        km: Double? = 10.0,
        offset: ZoneOffset? = null,
    ) =
        ActivityRecord(
            id,
            SourceIdentity("native", id, null),
            instant(w.date(), 3600),
            instant(w.date(), 7200),
            offset,
            offset,
            km?.times(1000),
            null,
            null,
            null,
            null,
            null,
            rpe,
            "",
            emptyList(),
            emptyList(),
            w.id(),
            null,
        )

    private fun gym(day: LocalDate) =
        StrengthSession(
            "gym",
            instant(day, 64800),
            Offsets.ofTotalSeconds(0),
            Durations.ofMinutes(45),
            8.5,
            "",
            listOf(
                StrengthSession.ExerciseSets(
                    StrengthExercise(
                        "test",
                        "Test",
                        StrengthExercise.Region.LOWER,
                        StrengthExercise.Mode.WEIGHTED,
                        true,
                    ),
                    (1..3).map { StrengthSession.SetEntry("set$it", 5, 50.0, 8.5, false) },
                )
            ),
        )

    private fun result(
        id: String,
        date: LocalDate,
        seconds: Long,
        distance: PerformanceDistance = PerformanceDistance.FIVE_K,
        source: ResultSource = ResultSource.MANUAL,
        activity: String? = null,
    ) =
        RecordedResult(
            id,
            distance,
            Durations.ofSeconds(seconds),
            date,
            null,
            source,
            activity,
            null,
        )

    private fun performance(
        rows: List<RecordedResult>,
        baseline: BaselineConfidence = BaselineConfidence.STRONG,
        completion: Double = .9,
        controlled: Int = 4,
        concern: Boolean = false,
    ) =
        PerformanceReassessment.review(
            rows,
            emptyList(),
            today,
            baseline,
            completion,
            controlled,
            concern,
        )

    private fun improvement() =
        listOf(result("old", today.minusDays(90), 2000), result("new", today.minusDays(5), 1800))

    @Test
    fun emptyReviewKeepsOriginalIdentityAndHonestInsufficientEvidence() {
        val v = version()
        val r =
            WeeklyCoaching.review(
                v,
                listOf(v),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                today,
                null,
            )
        assertEquals("7be53d95-c19f-3b03-9060-7dd63cbf362d", r.id())
        assertEquals(CalendarDates.of(2026, 11, 2), r.week())
        assertEquals(0, r.planned())
        assertEquals(0, r.completed())
        assertNull(r.longestKm())
        assertTrue(r.channels().all { it.state() == WeeklyCoaching.State.INSUFFICIENT_EVIDENCE })
        assertEquals(listOf("keep"), r.choices().map { it.id() })
        assertTrue(WeeklyReviewData.of(r).noChange())
        assertEquals(v.plan(), WeeklyCoaching.apply(v, r, r.choices().single(), emptySet()))
    }

    @Test
    fun weeklyWindowIncludesOnlyTheLatestCompletelyFinishedWeek() {
        val before = workout("before", CalendarDates.of(2026, 11, 1))
        val inside = workout("inside", CalendarDates.of(2026, 11, 2))
        val after = workout("after", CalendarDates.of(2026, 11, 9))
        val v = version(listOf(before, inside, after))
        val r =
            WeeklyCoaching.review(
                v,
                listOf(v),
                listOf(run("b", before), run("i", inside), run("a", after)),
                emptyList(),
                emptyList(),
                emptyList(),
                today,
                null,
            )
        assertEquals(1, r.planned())
        assertEquals(1, r.completed())
        assertEquals(10.0, r.kilometres())
        assertEquals(
            WeeklyCoaching.State.INSUFFICIENT_EVIDENCE,
            r.channels().first { it.name() == "Subjective response" }.state(),
        )
    }

    @Test
    fun completedMovedWorkoutUsesPreservedPrescriptionAndOriginalWeekday() {
        val original = workout("w", CalendarDates.of(2026, 11, 3))
        val moved = original.copy(date = CalendarDates.of(2026, 11, 4))
        val old =
            version(listOf(original))
                .copy(id = "old", createdAt = Instants.ofEpochSecond(now.getEpochSecond() - 1))
        val current = version(listOf(moved))
        val r =
            WeeklyCoaching.review(
                current,
                listOf(current, old),
                listOf(run("a", moved)),
                listOf(RunExecution("a", "w", "v", null, null, "")),
                emptyList(),
                emptyList(),
                today,
                null,
            )
        assertEquals(1, r.completed())
        assertEquals(1, r.planned())
        assertEquals(original.date(), old.plan().workouts().single().date())
    }

    @Test
    fun explicitSkipsRemainDistinctFromUnrecordedWorkouts() {
        val original = workout("skip", CalendarDates.of(2026, 11, 3))
        val old = version(listOf(original)).copy(id = "old")
        val current = version()
        val decision =
            WorkoutDecision(
                "choice",
                "skip",
                "old",
                original.date(),
                null,
                WorkoutDecision.Kind.SKIPPED,
                now,
            )
        val r =
            WeeklyCoaching.review(
                current,
                listOf(old),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                today,
                null,
                listOf(decision),
            )
        assertEquals(1, r.planned())
        assertEquals(0, r.completed())
        assertTrue(r.observations().any { it.contains("explicitly skipped") })
        assertTrue(current.plan().workouts().isEmpty())
    }

    @Test
    fun repeatedDemandingEvidenceCreatesOptionalChoicesWithoutChangingPlan() {
        val recent = (0..3).map { workout("past$it", today.minusDays((it + 8).toLong())) }
        val future = workout("future", today.plusDays(2), WorkoutCategory.TEMPO_THRESHOLD_RUN)
        val v = version(recent + future)
        val r =
            WeeklyCoaching.review(
                v,
                listOf(v),
                recent.mapIndexed { i, w -> run("a$i", w, 9.0, 4.0) },
                emptyList(),
                emptyList(),
                emptyList(),
                today,
                null,
            )
        assertTrue(r.choices().any { it.id() == "hold" })
        assertTrue(r.choices().any { it.id() == "reduce" })
        assertEquals(10.0, v.plan().workouts().last().distanceKm())
        val reduce = r.choices().first { it.id() == "reduce" }
        assertFailsWith<IllegalArgumentException> {
            WeeklyCoaching.apply(v, r, reduce, reduce.replacements().map { it.id() }.toSet())
        }
        assertFailsWith<IllegalArgumentException> {
            WeeklyCoaching.apply(v.copy(id = "different"), r, reduce, emptySet())
        }
        assertFails { (r.observations() as MutableList).clear() }
    }

    @Test
    fun interruptionOffersReturnBridgeButCannotRewriteCompletedOrRaceWork() {
        val future = workout("future", today.plusDays(1), WorkoutCategory.TEMPO_THRESHOLD_RUN)
        val race = workout("race", today.plusDays(5), WorkoutCategory.RACE)
        val v = version(listOf(future, race))
        val interruption =
            WeeklyCoaching.Interruption(today.minusDays(35), today.minusDays(1), "travel")
        val r =
            WeeklyCoaching.review(
                v,
                listOf(v),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                today,
                interruption,
            )
        val bridge = r.choices().first { it.id() == "return" }
        assertEquals(listOf("future"), bridge.replacements().map { it.id() })
        assertEquals(WorkoutCategory.EASY_RUN, bridge.replacements().single().category())
        assertEquals(WorkoutCategory.TEMPO_THRESHOLD_RUN, future.category())
        val applied = WeeklyCoaching.apply(v, r, bridge, emptySet())
        assertSame(race, applied.workouts().last())
    }

    @Test
    fun quietOrSingleNoisyRunDoesNotCreateAnAdjustment() {
        val past = workout("past", today.minusDays(1))
        val future = workout("future", today.plusDays(2), WorkoutCategory.TEMPO_THRESHOLD_RUN)
        val v = version(listOf(past, future))
        assertNull(AdaptiveCoaching.review(v, emptyList(), emptyList(), emptyList(), today, now))
        assertNull(
            AdaptiveCoaching.review(
                v,
                listOf(run("a", past, 7.0)),
                emptyList(),
                emptyList(),
                today,
                now,
            )
        )
    }

    @Test
    fun mirroredEffortDoesNotTriggerARepeatedHardSessionAdjustment() {
        val past = workout("past", today.minusDays(1))
        val future = workout("future", today.plusDays(2), WorkoutCategory.TEMPO_THRESHOLD_RUN)
        val plan = version(listOf(past, future))
        val native = run("native", past, rpe = 7.0)
        val watch =
            native.copy(
                id = "watch",
                source = SourceIdentity("health-connect", "watch", "com.fitbit.FitbitMobile"),
            )
        fun review(choice: DuplicateRunChoice?) =
            AdaptiveCoaching.review(
                plan,
                listOf(
                    if (choice == null) native
                    else native.withDuplicateChoices(mapOf(watch.id() to choice)),
                    watch,
                ),
                emptyList(),
                emptyList(),
                today,
                now,
            )
        assertNull(review(null))
        assertNull(review(DuplicateRunChoice.COUNT_OTHER))
        assertNotNull(review(DuplicateRunChoice.SEPARATE))
    }

    @Test
    fun lowCompletionAndHighEffortOffersKeepWithoutAutomaticActivation() {
        val past = workout("past", today.minusDays(1))
        val future = workout("future", today.plusDays(2), WorkoutCategory.TEMPO_THRESHOLD_RUN)
        val v = version(listOf(past, future))
        val p =
            assertNotNull(
                AdaptiveCoaching.review(
                    v,
                    listOf(run("a", past, 9.0, 4.0)),
                    emptyList(),
                    emptyList(),
                    today,
                    now,
                )
            )
        assertEquals("future", p.workoutId())
        assertTrue(p.options().any { it.action() == AdjustmentProposal.Action.KEEP })
        assertEquals(
            v.plan(),
            AdaptiveCoaching.apply(
                v.plan(),
                p,
                p.options().first { it.action() == AdjustmentProposal.Action.KEEP },
                emptySet(),
            ),
        )
        assertFailsWith<IllegalArgumentException> {
            AdaptiveCoaching.apply(v.plan(), p, p.options().first(), setOf("future"))
        }
        assertEquals(WorkoutCategory.TEMPO_THRESHOLD_RUN, future.category())
    }

    @Test
    fun staleFutureAndMissingEffortDoNotSupplyCurrentFatigueEvidence() {
        val old = workout("old", today.minusDays(11))
        val future = workout("future", today.plusDays(2), WorkoutCategory.TEMPO_THRESHOLD_RUN)
        val v = version(listOf(old, future))
        assertNull(
            AdaptiveCoaching.review(
                v,
                listOf(run("a", old, 10.0, 4.0)),
                emptyList(),
                emptyList(),
                today,
                now,
            )
        )
        val current = old.copy(date = today.minusDays(1))
        assertNull(
            AdaptiveCoaching.review(
                v,
                listOf(run("a", current, null, 4.0)),
                emptyList(),
                emptyList(),
                today,
                now,
            )
        )
    }

    @Test
    fun strengthConflictExplainsEvidenceAndMovePreservesFrequencyAndIdentity() {
        val w = workout("quality", today, WorkoutCategory.TEMPO_THRESHOLD_RUN)
        val v = version(listOf(w))
        val p =
            assertNotNull(
                AdaptiveCoaching.review(
                    v,
                    emptyList(),
                    emptyList(),
                    listOf(gym(today.minusDays(1))),
                    today,
                    now,
                )
            )
        assertTrue(p.evidence().any { it.contains("Recorded hard lower-body") })
        assertTrue(
            p.options().all { it.replacement() == null || it.replacement()!!.id() == w.id() }
        )
        val move = p.options().first { it.id() == "move" }
        assertEquals("Move demanding session to 17 Nov 2026 (recommended)", move.label())
        assertEquals(WorkoutCategory.TEMPO_THRESHOLD_RUN, move.replacement()!!.category())
    }

    @Test
    fun sourceAuthorityAndCorroborationBoundQualityPaceRecommendations() {
        val base = performance(improvement())
        assertTrue(base.propose())
        assertEquals(PerformanceReassessment.Confidence.MODERATE, base.confidence())
        assertEquals(.03, base.fasterFraction())
        for (r in
            listOf(
                performance(improvement(), baseline = BaselineConfidence.LOW),
                performance(improvement(), completion = .79),
                performance(improvement(), controlled = 2),
                performance(improvement(), concern = true),
            )) {
            assertFalse(r.propose())
            assertEquals(0.0, r.fasterFraction())
        }
        assertFails { (base.reasons() as MutableList).clear() }
    }

    @Test
    fun sameActivityNeverBecomesTwoIndependentImprovements() {
        val old =
            listOf(
                result("old5", today.minusDays(90), 2000),
                result("old3", today.minusDays(90), 1200, PerformanceDistance.THREE_K),
            )
        val newer =
            listOf(
                result("new5", today.minusDays(5), 1940, activity = "same"),
                result(
                    "new3",
                    today.minusDays(5),
                    1164,
                    PerformanceDistance.THREE_K,
                    activity = "same",
                ),
            )
        val r = performance(old + newer)
        assertFalse(r.propose())
        assertTrue(r.reasons()!!.any { it.startsWith("1 independent") })
        assertTrue(
            performance(old + newer.mapIndexed { i, x -> x.copy(activityId = "separate$i") })
                .propose()
        )
    }

    @Test
    fun controlledAssessmentsAreNotAutomaticallyMaximalPerformanceEvidence() {
        val old = listOf(result("old", today.minusDays(90), 2000))
        val controlled =
            AssessmentResult(
                "a",
                BaselineAssessment(5, AssessmentMode.CONTROLLED),
                Durations.ofSeconds(1800),
                6.0,
                instant(today.minusDays(5)),
                null,
            )
        assertFalse(
            PerformanceReassessment.review(
                    old,
                    listOf(controlled),
                    today,
                    BaselineConfidence.STRONG,
                    .9,
                    4,
                    false,
                )
                .propose()
        )
        assertTrue(
            PerformanceReassessment.review(
                    old,
                    listOf(
                        controlled.copy(
                            assessment = BaselineAssessment(5, AssessmentMode.BEST_EFFORT)
                        )
                    ),
                    today,
                    BaselineConfidence.STRONG,
                    .9,
                    4,
                    false,
                )
                .propose()
        )
    }

    @Test
    fun sixCalendarMonthsAndRecentWindowRespectBoundaryDates() {
        assertEquals(
            CalendarDates.of(2024, 2, 29),
            coachingSixMonthsBefore(CalendarDates.of(2024, 8, 31)),
        )
        assertEquals(
            CalendarDates.of(2025, 2, 28),
            coachingSixMonthsBefore(CalendarDates.of(2025, 8, 31)),
        )
        val recent = result("new", today.minusDays(5), 1800)
        assertFalse(
            performance(listOf(result("old", CalendarDates.of(2026, 5, 14), 2000), recent))
                .propose()
        )
        assertTrue(
            performance(listOf(result("old", CalendarDates.of(2026, 5, 15), 2000), recent))
                .propose()
        )
        assertFalse(
            performance(listOf(result("old", today.minusDays(90), 2000), recent.copy(date = today)))
                .propose()
        )
    }

    @Test
    fun briefingSeparatesSimilarRecordedEffortFromUnknownReadiness() {
        val w = workout("future", today)
        val p1 = workout("a", today.minusDays(3))
        val p2 = workout("b", today.minusDays(2))
        val quiet = WorkoutContext.briefing(w, listOf(w), emptyList(), emptyList(), null)
        assertEquals("Start with the planned effort", quiet.title())
        val high =
            WorkoutContext.briefing(
                w,
                listOf(w, p1, p2),
                listOf(run("x", p1, 8.0), run("y", p2, 9.0)),
                emptyList(),
                null,
            )
        assertEquals("Take care with today's intensity", high.title())
        assertFails { (high.observations() as MutableList).clear() }
        assertTrue(
            WorkoutContext.moveWarnings(w, null, listOf(w), emptyList())
                .single()
                .contains("valid date")
        )
    }

    @Test
    fun movingChangesOnlyTheSelectedWorkoutAndWarningsAreContextNotMedicalAdvice() {
        val w = workout("quality", today, WorkoutCategory.TEMPO_THRESHOLD_RUN)
        val warnings =
            WorkoutContext.moveWarnings(w, today, listOf(w), listOf(gym(today.minusDays(1))))
        assertTrue(warnings.any { it.contains("recorded hard lower-body") })
        assertTrue(warnings.last().contains("Only this workout will move"))
        val clear = WorkoutContext.moveWarnings(w, today.plusDays(5), listOf(w), emptyList())
        assertTrue(clear.first().contains("not medical readiness advice"))
        assertEquals(today, w.date())
    }

    @Test
    fun decimalAndCompensatedSumFollowRetainedPresentationAndArithmetic() {
        for ((value, expected) in
            listOf(
                1.25 to "1.3",
                2.35 to "2.4",
                .05 to "0.1",
                9.95 to "10.0",
                999.95 to "1000.0",
                -0.0 to "-0.0",
                -0.05 to "-0.1",
            )) assertEquals(expected, coachingDecimalOne(value))
        assertEquals(2.0, coachingSum(listOf(1e16, 1.0, 1.0, -1e16)))
        assertEquals(Double.POSITIVE_INFINITY, coachingSum(listOf(Double.POSITIVE_INFINITY, 1.0)))
    }

    @Test
    fun transactionActionPropagatesFailureAndRawRecordAliasesRemainSupported() {
        var calls = 0
        val action =
            object : CoachingTransaction {
                override fun run() {
                    calls++
                    throw IllegalStateException("stop")
                }
            }
        assertFailsWith<IllegalStateException> { action.run() }
        assertEquals(1, calls)
        val raw = mutableListOf("known")
        val record = PerformanceReassessment.Review(false, null, 0.0, 0.0, raw)
        raw.add("later")
        assertEquals(2, record.reasons()!!.size)
        assertNull(record.confidence())
        assertFailsWith<IllegalArgumentException> {
            WeeklyCoaching.Interruption(today, today.minusDays(1), "bad")
        }
    }
}
