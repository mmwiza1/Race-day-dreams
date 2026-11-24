package org.racedaydreams.coach

import kotlin.test.Test
import kotlin.test.assertEquals
import org.racedaydreams.domain.*
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.SourceIdentity
import org.racedaydreams.domain.coaching.PlanVersion
import org.racedaydreams.domain.presentation.StrengthTodaySummary
import org.racedaydreams.domain.strength.GymProgramme
import org.racedaydreams.domain.strength.StrengthExercise
import org.racedaydreams.domain.strength.StrengthSession
import org.racedaydreams.domain.time.*

/** B-07 / B-R9: the "Your next 7 days" strip counts each physical session once. */
class WeekAtAGlanceTest {
    private val utc = Offsets.ofTotalSeconds(0)
    // Wednesday 18 November 2026.
    private val today = CalendarDates.of(2026, 11, 18)
    private val now = at(today, 6)

    private fun at(date: LocalDate, hour: Int) =
        Instants.ofEpochSecond(date.toEpochDay() * 86_400 + hour * 3600L)

    private fun dateOf(instant: Instant): LocalDate = CalendarDates.atOffset(instant, utc)

    private fun run(id: String, date: LocalDate, provider: String, planned: String? = null) =
        ActivityRecord(
            id,
            SourceIdentity(provider, id, null),
            at(date, 7),
            at(date, 8),
            utc,
            utc,
            8_000.0,
            Durations.ofSeconds(3_000),
            null,
            null,
            null,
            null,
            null,
            "",
            emptyList(),
            emptyList(),
            planned,
            null,
        )

    private fun plan(vararg workouts: PlannedWorkout) =
        PlanVersion(
            "v",
            "p",
            1,
            now,
            "",
            UserProfile(
                RaceGoal(RaceDistance.HALF_MARATHON, "", null, GoalMode.FINISH, null),
                TrainingPreferences(
                    TrainingStrategy.BALANCED,
                    DayOfWeek.entries.toSet(),
                    4,
                    DayOfWeek.SUNDAY,
                    emptySet(),
                    ScheduleType.FIXED,
                    UnitSystem.METRIC,
                ),
                false,
            ),
            "",
            TrainingPlan(
                "p",
                today.minusDays(28),
                today.plusDays(28),
                emptyList(),
                workouts.toList(),
            ),
        )

    private fun easy(id: String, date: LocalDate) =
        PlannedWorkout(id, date, WorkoutCategory.EASY_RUN, 8.0, null, emptyList(), "")

    private fun gym(sessionDate: LocalDate) =
        GymProgramme(
            "g",
            1,
            now,
            "Athlete accepted",
            GymProgramme.Settings(
                GymProgramme.Goal.GENERAL,
                GymProgramme.Priority.STRENGTH_FIRST,
                GymProgramme.Experience.INTERMEDIATE,
                setOf(sessionDate.getDayOfWeek()),
                1,
                setOf("full gym"),
                45,
                GymProgramme.Effort.RPE,
            ),
            today.minusDays(7),
            today.plusDays(34),
            listOf(
                GymProgramme.Session(
                    "scheduled",
                    sessionDate,
                    "Upper",
                    listOf(
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
                    ),
                    "Original",
                )
            ),
            emptyList(),
        )

    private fun strength(id: String, date: LocalDate) =
        StrengthSession(
            id,
            at(date, 18),
            utc,
            Durations.ofMinutes(45),
            null,
            null,
            listOf(
                StrengthSession.ExerciseSets(
                    StrengthExercise.starters().first(),
                    listOf(StrengthSession.SetEntry("$id-set", 5, 60.0, null, false)),
                )
            ),
        )

    private fun days(state: CoachUiState) =
        WeekAtAGlance.nextSevenDays(state, today, ::dateOf) { it.name }

    private fun marks(state: CoachUiState, date: LocalDate) =
        days(state).first { it.date == date }.sessions.map { it.discipline to it.mark }

    @Test
    fun aRunImportedFromTwoSourcesIsOneDoneRun() {
        val state =
            CoachUiState(
                activities =
                    listOf(run("native", today, "rdd"), run("watch-copy", today, "health-connect"))
            )
        assertEquals(listOf(Discipline.RUNNING to SessionMark.DONE), marks(state, today))
    }

    @Test
    fun aCopyOfAPlannedRunAddsNoExtraChip() {
        val state =
            CoachUiState(
                currentPlan = plan(easy("easy", today)),
                activities =
                    listOf(
                        run("native", today, "rdd", planned = "easy"),
                        run("watch-copy", today, "health-connect"),
                    ),
            )
        assertEquals(listOf(Discipline.RUNNING to SessionMark.DONE), marks(state, today))
        assertEquals("Nothing else planned", WeekAtAGlance.summary(days(state)))
    }

    @Test
    fun separateRunsOnOneDayStaySeparate() {
        val morning = run("morning", today, "rdd")
        val evening =
            ActivityRecord(
                "evening",
                SourceIdentity("rdd", "evening", null),
                at(today, 18),
                at(today, 19),
                utc,
                utc,
                5_000.0,
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
        assertEquals(2, marks(CoachUiState(activities = listOf(morning, evening)), today).size)
    }

    @Test
    fun aGymWorkoutLinkedToASessionOnAnotherDayIsCountedOnce() {
        val scheduledDay = today.plusDays(2)
        val state =
            CoachUiState(
                gymProgramme = gym(scheduledDay),
                strengthSessions = listOf(strength("actual", today)),
                gymLinks =
                    mapOf(
                        "actual" to StrengthTodaySummary.Association("actual", "g:v1", "scheduled")
                    ),
            )
        // Shown once: as the scheduled session, done, on its scheduled day.
        assertEquals(emptyList(), marks(state, today))
        assertEquals(listOf(Discipline.STRENGTH to SessionMark.DONE), marks(state, scheduledDay))
        assertEquals(1, days(state).sumOf { it.sessions.size })
    }

    @Test
    fun anUnlinkedGymWorkoutStillShowsOnItsDay() {
        val scheduledDay = today.plusDays(2)
        val state =
            CoachUiState(
                gymProgramme = gym(scheduledDay),
                strengthSessions = listOf(strength("free", today)),
            )
        assertEquals(listOf(Discipline.STRENGTH to SessionMark.DONE), marks(state, today))
        assertEquals(listOf(Discipline.STRENGTH to SessionMark.PLANNED), marks(state, scheduledDay))
    }

    @Test
    fun theStripCrossesTheYearEnd() {
        val newYearsEve = CalendarDates.of(2026, 12, 31)
        val strip = WeekAtAGlance.nextSevenDays(CoachUiState(), newYearsEve, ::dateOf) { it.name }
        assertEquals(newYearsEve, strip.first().date)
        assertEquals(CalendarDates.of(2027, 1, 6), strip.last().date)
        assertEquals("Thu 31 Dec 2026 – Wed 6 Jan 2027", WeekAtAGlance.range(strip))
    }
}
