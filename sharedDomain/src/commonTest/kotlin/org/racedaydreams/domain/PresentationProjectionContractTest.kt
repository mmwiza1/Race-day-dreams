package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.body.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.presentation.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

class PresentationProjectionContractTest {
    private val today = CalendarDates.of(2026, 11, 15)
    private val now = Instants.ofEpochSecond(today.toEpochDay() * 86400 + 43200)

    private fun weight(
        id: String,
        day: LocalDate,
        seconds: Long = 0,
        offset: Int = 0,
        kg: Double = 75.0,
    ) =
        BodyWeight(
            id,
            kg,
            Instants.ofEpochSecond(day.toEpochDay() * 86400 + seconds),
            Offsets.ofTotalSeconds(offset),
            "manual",
            id,
            null,
        )

    private fun programme(): GymProgramme {
        val p =
            GymProgramme.Prescription(
                "bench-press",
                2,
                6,
                8,
                null,
                null,
                6,
                8,
                120,
                "Original target",
            )
        return GymProgramme(
            "p",
            3,
            now,
            "Athlete accepted",
            GymProgramme.Settings(
                GymProgramme.Goal.GENERAL,
                GymProgramme.Priority.STRENGTH_FIRST,
                GymProgramme.Experience.INTERMEDIATE,
                setOf(DayOfWeek.SUNDAY),
                1,
                setOf("full gym"),
                45,
                GymProgramme.Effort.RPE,
            ),
            today.minusDays(7),
            today.plusDays(34),
            listOf(GymProgramme.Session("today", today, "Upper", listOf(p), "Original")),
            emptyList(),
        )
    }

    private fun actual(
        id: String = "actual",
        at: Instant = now,
        offset: Int = 0,
        warmup: Boolean = false,
    ) =
        StrengthSession(
            id,
            at,
            Offsets.ofTotalSeconds(offset),
            Durations.ofMinutes(40),
            null,
            "Recorded",
            listOf(
                StrengthSession.ExerciseSets(
                    ExerciseCatalog.definition("bench-press")!!.exercise(),
                    listOf(StrengthSession.SetEntry("$id-set", 8, 80.0, null, warmup)),
                )
            ),
        )

    private fun summary(
        history: List<StrengthSession>,
        key: String = "p:v1",
        id: String = "actual",
    ) =
        StrengthTodaySummary.from(
            programme(),
            history,
            listOf(StrengthTodaySummary.Association(id, key, "today")),
            today,
            now,
        )

    @Test
    fun noHistoryDoesNotBecomeZeroReadinessOrACompletedProgramme() {
        val home = HomeCoachingSummary.from(null)
        assertEquals("Still learning your training", home.title())
        assertEquals("Continue gathering evidence", home.recommendation())
        val strength = summary(emptyList())
        assertFalse(strength.todayCompleted())
        assertEquals(0, strength.completedThisWeek())
        assertNull(strength.recommendation())
    }

    @Test
    fun actualTrainingNeedsAnExplicitMatchingAssociation() {
        val evidence = listOf(actual())
        assertEquals(1, summary(evidence).actualWorkoutsThisWeek())
        assertTrue(summary(evidence).todayCompleted())
        assertFalse(summary(evidence, id = "missing").todayCompleted())
        assertFalse(summary(evidence, key = "other:v1").todayCompleted())
        assertEquals(0, summary(evidence, key = "other:v1").completedThisWeek())
    }

    @Test
    fun historicalRevisionsRemainEligibleButFutureOrInvalidRevisionsDoNot() {
        for (key in listOf("p:v1", "p:v2", "p:v3", "p:v+1", "p:v01", "p:v١", "p:v１")) assertTrue(
            summary(listOf(actual()), key).todayCompleted(),
            key,
        )
        for (key in
            listOf("p:v0", "p:v-1", "p:v4", "p:v2147483648", "p:v1 ", "p:v", "p:v1.0")) assertFalse(
            summary(listOf(actual()), key).todayCompleted(),
            key,
        )
    }

    @Test
    fun futureActualsAndMissingEffortDoNotCreateProgression() {
        val future = actual(at = Instants.ofEpochSecond(now.getEpochSecond() + 1))
        assertFalse(summary(listOf(future)).todayCompleted())
        assertEquals(0, summary(listOf(future)).actualWorkoutsThisWeek())
        val past = actual("earlier", Instants.ofEpochSecond(now.getEpochSecond() - 86400))
        assertNull(summary(listOf(past, actual())).recommendation())
    }

    @Test
    fun recordedOffsetDeterminesWeekNotUtcOrTheDisplayZone() {
        val monday = CalendarDates.of(2026, 11, 9)
        val at = Instants.ofEpochSecond(monday.toEpochDay() * 86400 - 1800)
        assertEquals(1, summary(listOf(actual(at = at, offset = 3600))).actualWorkoutsThisWeek())
        assertEquals(0, summary(listOf(actual(at = at, offset = -3600))).actualWorkoutsThisWeek())
    }

    @Test
    fun warmupsNeverBecomeMeasuredPersonalBests() {
        assertEquals(0, summary(listOf(actual(warmup = true))).recentPbExercises())
        assertEquals(1, summary(listOf(actual())).recentPbExercises())
    }

    @Test
    fun homeUsesReviewStatesInOrderWithoutInventingConfidence() {
        val channels =
            listOf(
                WeeklyCoaching.Channel("Unknown", WeeklyCoaching.State.INSUFFICIENT_EVIDENCE, ""),
                WeeklyCoaching.Channel("Running", WeeklyCoaching.State.STABLE, ""),
                WeeklyCoaching.Channel("Strength", WeeklyCoaching.State.NEEDS_ATTENTION, ""),
                WeeklyCoaching.Channel("History", WeeklyCoaching.State.IMPROVING, ""),
                WeeklyCoaching.Channel("More", WeeklyCoaching.State.STABLE, ""),
            )
        val review =
            WeeklyCoaching.Review(
                "r",
                "v",
                today,
                3,
                2,
                10.0,
                1,
                null,
                emptyList(),
                channels,
                "Keep original",
                emptyList(),
            )
        val home = HomeCoachingSummary.from(review)
        assertEquals("Something may need adjusting", home.title())
        assertEquals(
            listOf(
                "2 / 3 planned runs completed",
                "Running: stable",
                "Strength: needs attention",
                "History: improving",
            ),
            home.observations(),
        )
        assertEquals("Keep original", home.recommendation())
    }

    @Test
    fun rawHomeAndGraphRecordsRetainTheirOriginalAliasing() {
        val notes = mutableListOf("before")
        val raw = HomeCoachingSummary(null, notes, null)
        notes.add("after")
        assertSame(notes, raw.observations())
        assertEquals(2, raw.observations()!!.size)
        val points = mutableListOf<BodyWeight>()
        val graph = WeightGraphData(points, 0.0, 1.0, null)
        points.add(weight("a", today))
        assertSame(points, graph.points())
        assertEquals(0.5, graph.x(-10))
    }

    @Test
    fun weightRangesUseCalendarClampingAndActualOffsets() {
        val end = CalendarDates.of(2024, 3, 31)
        val records =
            listOf(
                weight("before", CalendarDates.of(2024, 2, 28)),
                weight("edge", CalendarDates.of(2024, 2, 29)),
                weight("today", end),
                weight("futureLocal", end, seconds = 23 * 3600, offset = 7200),
            )
        assertEquals(
            listOf("edge", "today"),
            WeightGraphData.of(records, end, "Month", MassUnit.KG).points()!!.map { it.id() },
        )
        val leap = CalendarDates.of(2024, 2, 29)
        assertEquals(
            listOf("boundary"),
            WeightGraphData.of(
                    listOf(
                        weight("boundary", CalendarDates.of(2023, 2, 28)),
                        weight("outside", CalendarDates.of(2023, 2, 27)),
                    ),
                    leap,
                    "Year",
                    MassUnit.KG,
                )
                .points()!!
                .map { it.id() },
        )
    }

    @Test
    fun graphSelectionRetainsDuplicateTimesAndOriginalMeasurementIdentity() {
        val a = weight("a", today, kg = 75.0)
        val b = weight("b", today, kg = 80.0)
        val g = WeightGraphData.of(listOf(b, a), today, "All", MassUnit.KG)
        assertEquals(listOf(a, b), g.points())
        assertEquals(0.5, g.x(0))
        assertSame(b, g.nearest(0.5, g.y(1)))
        assertSame(a, g.nearest(Double.NaN))
        assertEquals(74.25, g.minimum())
        assertEquals(80.75, g.maximum())
        assertNull(WeightGraphData.of(emptyList(), today, "All", MassUnit.KG).nearest(0.5))
    }

    @Test
    fun preEpochMillisecondsAreFlooredAndFullRangeOverflowIsRejected() {
        assertEquals(-1L, graphMillis(Instants.ofEpochSecond(-1, 999999999)))
        assertEquals(
            Long.MIN_VALUE,
            graphMillis(Instants.ofEpochSecond(-9223372036854776, 192000000)),
        )
        assertEquals(
            Long.MAX_VALUE,
            graphMillis(Instants.ofEpochSecond(9223372036854775, 807999999)),
        )
        assertFailsWith<ArithmeticException> {
            graphMillis(Instants.ofEpochSecond(9223372036854775, 808000000))
        }
    }

    @Test
    fun calendarDoesNotInferCompletionFromAGymVisitAndKeepsOriginalOutsideTheBlock() {
        val original =
            listOf(
                CalendarData.Entry(
                    CalendarData.Category.REST,
                    CalendarData.State.PLANNED,
                    "Rest day",
                    null,
                    null,
                )
            )
        val g = programme()
        assertSame(
            original,
            CalendarData.withGym(today.minusDays(8), today, original, g, emptyMap()),
        )
        val planned = CalendarData.withGym(today, today, original, g, emptyMap())
        assertEquals(listOf(CalendarData.State.PLANNED), planned.map { it.state() })
        assertEquals("Upper · programmed strength", planned.single().label())
        assertTrue(
            CalendarData.withGym(today, today, original, g, mapOf("today" to null)).isEmpty()
        )
    }

    @Test
    fun blankCalendarDatesAreNotAutomaticallyRestDays() {
        assertTrue(
            CalendarData.day(
                    today,
                    today,
                    null,
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyMap(),
                    emptyList(),
                )
                .isEmpty()
        )
        for (c in WorkoutCategory.entries) assertFalse(CalendarData.label(c).contains('_'))
        assertEquals("Tempo run", CalendarData.label(WorkoutCategory.TEMPO_THRESHOLD_RUN))
    }

    @Test
    fun displayLanguageKeepsEstimatedAndActualRecordsAndMeasurementModesDistinct() {
        assertEquals(
            "Estimated maximum for one repetition",
            AthleteLanguage.recordLabel("Estimated 1RM"),
        )
        assertEquals("Best weight for 1 repetition", AthleteLanguage.recordLabel("Load × 1"))
        assertEquals("Best weight for 5 repetitions", AthleteLanguage.recordLabel("Load × 5"))
        for (exercise in ExerciseCatalog.exercises()) {
            val instruction = AthleteLanguage.measurement(exercise)
            if (
                exercise.mode() == StrengthExercise.Mode.WEIGHTED_BODYWEIGHT &&
                    exercise.measurement() == StrengthExercise.MeasurementProfile.LOAD_REPS
            )
                assertTrue(instruction.contains("extra weight you add"))
        }
        assertTrue(
            AthleteLanguage.runningPurpose(WorkoutCategory.ZONE_2_RUN)
                .contains("no heart-rate zone is assumed")
        )
        assertTrue(
            AthleteLanguage.BACKUP.contains(
                "does not include detailed GPS routes or sensor samples"
            )
        )
    }
}
