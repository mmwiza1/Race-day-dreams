package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.time.CalendarDates
import org.racedaydreams.domain.time.DurationParts
import org.racedaydreams.domain.time.Durations
import org.racedaydreams.domain.time.IsoCalendar

class RunningModelsAndTimeTest {
    private val today = CalendarDates.of(2026, 11, 13)

    @Test
    fun nativeCalendarRetainsOriginalFullRangeLeapRulesAndIsoIdentity() {
        val fixtures =
            listOf(
                listOf(-999999999, 1, 1) to -365243219162L,
                listOf(0, 1, 1) to -719528L,
                listOf(1900, 2, 28) to -25509L,
                listOf(2000, 2, 29) to 11016L,
                listOf(2026, 11, 13) to 20770L,
                listOf(999999999, 12, 31) to 365241780471L,
            )
        for ((parts, epoch) in fixtures) {
            val (year, month, day) = parts
            val date = CalendarDates.of(year, month, day)
            assertEquals(epoch, date.toEpochDay())
            assertEquals(epoch, IsoCalendar.epochDay(year, month, day))
            assertEquals(IsoCalendar.text(year, month, day), date.toString())
            assertEquals(date, CalendarDates.of(year, month, day))
        }
        assertFails { CalendarDates.of(1900, 2, 29) }
        assertFails { CalendarDates.of(2026, 4, 31) }
        assertFails { CalendarDates.of(1000000000, 1, 1) }
    }

    @Test
    fun exactDurationNormalizationKeepsSignsFractionsAndExtremeEvidence() {
        val cases =
            listOf(
                Triple(-1L, 1L, "PT-0.999999999S"),
                Triple(0L, -1L, "PT-0.000000001S"),
                Triple(-3600L, 1L, "PT-59M-59.999999999S"),
                Triple(-60L, 1L, "PT-59.999999999S"),
                Triple(3600L, 1L, "PT1H0.000000001S"),
                Triple(Long.MIN_VALUE, 0L, "PT-2562047788015215H-30M-8S"),
                Triple(Long.MAX_VALUE, 999999999L, "PT2562047788015215H30M7.999999999S"),
            )
        for ((seconds, nanos, text) in cases) {
            val actual = Durations.ofSeconds(seconds, nanos)
            val parts = DurationParts.ofSeconds(seconds, nanos)
            assertEquals(parts.seconds, actual.getSeconds())
            assertEquals(parts.nanos, actual.getNano())
            assertEquals(text, actual.toString())
            assertEquals(text, parts.text())
            assertEquals(actual, Durations.ofSeconds(parts.seconds, parts.nanos.toLong()))
        }
        assertEquals(Durations.ofSeconds(-1, 999000000), Durations.ofMillis(-1))
        assertEquals(Durations.ofSeconds(-1, 999999999), Durations.ofNanos(-1))
        assertFailsWith<ArithmeticException> { Durations.ofSeconds(Long.MAX_VALUE, 1000000000) }
        assertFailsWith<ArithmeticException> { Durations.ofSeconds(Long.MIN_VALUE, -1) }
        assertFailsWith<ArithmeticException> { Durations.ofHours(Long.MAX_VALUE) }
    }

    @Test
    fun targetTimesRejectFractionalAndStaleValuesWithoutChangingMessagesOrLimits() {
        assertEquals(Durations.ofMinutes(25), PaceCalculator.parseTime("25:00"))
        assertEquals(
            "5:00/km",
            PaceCalculator.targetPace(
                Durations.ofMinutes(25),
                RaceDistance.FIVE_K,
                UnitSystem.METRIC,
            ),
        )
        assertEquals(
            "Enter target time in whole seconds.",
            assertFailsWith<IllegalArgumentException> {
                    RaceGoal(
                        RaceDistance.FIVE_K,
                        "",
                        null,
                        GoalMode.TARGET_TIME,
                        Durations.ofSeconds(20, 1),
                    )
                }
                .message,
        )
        assertEquals(
            "Target time must be greater than zero and no more than 48 hours.",
            assertFailsWith<IllegalArgumentException> {
                    RaceGoal(
                        RaceDistance.FIVE_K,
                        "",
                        null,
                        GoalMode.TARGET_TIME,
                        Durations.ofSeconds(172800, 1),
                    )
                }
                .message,
        )
        assertFailsWith<IllegalArgumentException> {
            RaceGoal(RaceDistance.FIVE_K, "", null, GoalMode.FINISH, Durations.ofMinutes(25))
        }
        assertEquals(
            "Park Run",
            RaceGoal(RaceDistance.FIVE_K, " Park Run ", today, GoalMode.FINISH, null).raceName(),
        )
        assertEquals(
            "\u00a0Race\u00a0",
            RaceGoal(RaceDistance.FIVE_K, "\u00a0Race\u00a0", today, GoalMode.FINISH, null)
                .raceName(),
        )
        assertNull(
            RaceGoal(RaceDistance.GENERAL_FITNESS, null, null, GoalMode.FINISH, null).raceDate()
        )
    }

    @Test
    fun onboardingUsesExplicitLocalDateWithoutRejectingRetainedPastGoals() {
        val old =
            RaceGoal(
                RaceDistance.FIVE_K,
                "Past race",
                CalendarDates.of(2026, 11, 12),
                GoalMode.FINISH,
                null,
            )
        assertEquals("Past race", old.raceName())
        assertFailsWith<IllegalArgumentException> { GoalValidation.validateRaceGoal(old, today) }
        GoalValidation.validateRaceGoal(old, CalendarDates.of(2026, 11, 12))
        GoalValidation.validateRaceGoal(
            RaceGoal(RaceDistance.GENERAL_FITNESS, null, null, GoalMode.FINISH, null),
            today,
        )
    }

    @Test
    fun frozenPlansPreserveUnknownTargetsAndCopiesCannotRewriteHistory() {
        val segment =
            WorkoutSegment(WorkoutSegment.Kind.RUN, null, null, null, null, null, null, null)
        val segments = mutableListOf(segment)
        val workout =
            PlannedWorkout(
                " original-id ",
                today,
                WorkoutCategory.EASY_RUN,
                null,
                null,
                segments,
                null,
            )
        val phase = TrainingPhase(TrainingPhase.Kind.FOUNDATION, today, today, null)
        val phases = mutableListOf(phase)
        val workouts = mutableListOf(workout)
        val original = TrainingPlan(" plan-id ", today, today, phases, workouts)
        segments.clear()
        phases.clear()
        workouts.clear()
        assertEquals(" original-id ", workout.id())
        assertEquals(listOf(segment), workout.segments())
        assertEquals(listOf(phase), original.phases())
        assertEquals(listOf(workout), original.workouts())
        assertNull(original.workouts().single().distanceKm())
        assertNull(original.workouts().single().duration())
        assertNull(original.workouts().single().note())
        assertFails { (original.workouts() as MutableList<PlannedWorkout>).clear() }
        val future = original.copy(workouts = emptyList())
        assertTrue(future.workouts().isEmpty())
        assertEquals(listOf(workout), original.workouts())
    }

    @Test
    fun planBoundariesAndAthleteChoicesKeepTheirOriginalValidation() {
        val yesterday = CalendarDates.of(2026, 11, 12)
        assertFailsWith<IllegalArgumentException> {
            TrainingPhase(TrainingPhase.Kind.TAPER, today, yesterday, null)
        }
        val phase = TrainingPhase(TrainingPhase.Kind.FOUNDATION, yesterday, today, null)
        assertFailsWith<IllegalArgumentException> {
            TrainingPlan("p", today, today, listOf(phase), emptyList())
        }
        val workout =
            PlannedWorkout("w", yesterday, WorkoutCategory.EASY_RUN, null, null, emptyList(), null)
        assertFailsWith<IllegalArgumentException> {
            TrainingPlan("p", today, today, emptyList(), listOf(workout))
        }
        val choice =
            AdjustmentOption(
                "keep",
                "Keep",
                AdjustmentOption.Action.KEEP_ORIGINAL,
                emptyList(),
                "unchanged",
            )
        assertEquals(AdjustmentOption.Action.KEEP_ORIGINAL, choice.action())
        assertFailsWith<IllegalArgumentException> {
            AdjustmentOption("x", "x", null, emptyList(), "x")
        }
        assertEquals(0, RaceCalendar.daysUntil(yesterday, today))
        assertEquals(1, RaceCalendar.daysUntil(today, yesterday))
        assertEquals(0, RaceCalendar.weeksUntil(today, yesterday))
    }
}
