package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

class LegacyGymPlanningContractTest {
    private val start = CalendarDates.of(2026, 11, 9)
    private val at = Instants.ofEpochSecond(1794218400)
    private val engine = GymProgrammeGenerator()

    private fun settings(
        goal: GymProgramme.Goal = GymProgramme.Goal.GENERAL,
        exp: GymProgramme.Experience = GymProgramme.Experience.INTERMEDIATE,
        priority: GymProgramme.Priority = GymProgramme.Priority.BALANCED,
        frequency: Int = 2,
        minutes: Int = 45,
        equipment: Set<String> = setOf("full gym"),
        allDays: Boolean = true,
    ) =
        GymProgramme.Settings(
            goal,
            priority,
            exp,
            if (allDays) DayOfWeek.values().toSet() else DayOfWeek.values().take(frequency).toSet(),
            frequency,
            equipment,
            minutes,
            GymProgramme.Effort.RPE,
        )

    private fun generate(
        s: GymProgramme.Settings = settings(),
        first: LocalDate = start,
        excluded: Set<String> = emptySet(),
    ) =
        engine.generate(
            "programme",
            1,
            at,
            "Athlete chose preview",
            s,
            first,
            emptyList(),
            emptyList(),
            emptySet(),
            excluded,
        )

    @Test
    fun sixWeekAllocationRemainsAnchoredForEveryWeekdayAndFrequency() {
        for (offset in 0..6) for (frequency in 1..6) {
            val first = start.plusDays(offset.toLong())
            val p = generate(settings(frequency = frequency), first)
            assertEquals(first.plusDays(41), p.end())
            assertEquals(6 * frequency, p.sessions().size)
            assertEquals(p.sessions().size, p.sessions().map { it.id() }.toSet().size)
            assertEquals(p.sessions().size, p.sessions().map { it.date() }.toSet().size)
            val weeks = p.sessions().groupBy { (it.date().toEpochDay() - first.toEpochDay()) / 7 }
            assertEquals((0L..5L).toSet(), weeks.keys)
            assertTrue(weeks.values.all { it.size == frequency })
        }
    }

    @Test
    fun legacySimulatorPreservesEquipmentExclusionsAndFrozenPrescriptions() {
        var count = 0
        for (goal in GymProgramme.Goal.values()) for (exp in
            GymProgramme.Experience.values()) for (priority in
            GymProgramme.Priority.values()) for (frequency in listOf(1, 2, 4, 6)) for (minutes in
            listOf(30, 45, 75)) for (equipment in listOf(setOf("bodyweight"), setOf("full gym"))) {
            val s = settings(goal, exp, priority, frequency, minutes, equipment, allDays = false)
            val excluded = setOf("plank", "back-squat")
            val p = generate(s, excluded = excluded)
            assertEquals(frequency * 6, p.sessions().size)
            assertEquals(s, p.settings())
            assertTrue(p.sessions().all { it.date().getDayOfWeek() in s.days() })
            for (session in p.sessions()) for (e in session.exercises()) {
                assertTrue(GymProgrammeGenerator.available(e.exerciseId(), equipment))
                assertTrue(e.exerciseId() !in excluded)
                assertNull(e.referenceLoadKg())
            }
            assertFails { (p.sessions() as MutableList).clear() }
            count++
        }
        assertEquals(1080, count)
    }

    @Test
    fun lowerVariationsAndFavouriteEquivalenceRetainTheirOriginalIntent() {
        val p = generate(settings(frequency = 4))
        val lower = p.sessions().filter { it.title().startsWith("Lower") }.take(2)
        assertEquals(
            setOf("back-squat", "romanian-deadlift"),
            lower.map { it.exercises()[0].exerciseId() }.toSet(),
        )
        val favourite =
            engine.generate(
                "favourite",
                1,
                at,
                "Preview",
                settings(frequency = 1),
                start,
                emptyList(),
                emptyList(),
                setOf("romanian-deadlift", "dumbbell-bench-press"),
            )
        // A hinge favourite cannot displace the squat intent of the first lower session.
        assertEquals("back-squat", favourite.sessions()[0].exercises()[0].exerciseId())
        assertTrue(
            favourite.sessions()[0].exercises().any { it.exerciseId() == "dumbbell-bench-press" }
        )
        assertTrue(favourite.sessions()[0].explanation().contains("squat"))
    }

    @Test
    fun demandingRunContextRemainsDirectionalAndCoreIsNotLowerBodyWork() {
        fun run(date: LocalDate) =
            PlannedWorkout("run", date, WorkoutCategory.INTERVALS, null, null, emptyList(), null)
        val same =
            GymProgrammeGenerator.conflicts(start, true, listOf(run(start)), emptySet()).single()
        val before =
            GymProgrammeGenerator.conflicts(start, true, listOf(run(start.plusDays(1))), emptySet())
                .single()
        val after =
            GymProgrammeGenerator.conflicts(
                    start,
                    true,
                    listOf(run(start.minusDays(1))),
                    emptySet(),
                )
                .single()
        assertTrue(same.contains("the same day as"))
        assertTrue(before.contains("the day before"))
        assertTrue(after.contains("the day after"))
        assertTrue(after.contains("Review recovery"))
        assertTrue(
            GymProgrammeGenerator.conflicts(
                    start,
                    false,
                    listOf(run(start)),
                    setOf(start.minusDays(1)),
                )
                .isEmpty()
        )
        assertFalse(GymProgrammeGenerator.demanding(null))
        val core = GymProgramme.Prescription("plank", 2, 0, 0, 30, null, 5, 7, 90, "Core")
        assertFalse(GymProgrammeGenerator.hasLowerBodyWork(listOf(core)))
    }

    @Test
    fun explicitEditsNeverChangeTheOriginalOrProtectedSessions() {
        val p = generate(settings(GymProgramme.Goal.STRENGTH))
        val before = p.toString()
        val session = p.sessions()[0]
        assertFailsWith<IllegalArgumentException> {
            GymPlanEditor.reduce(p, session.id(), start, at, setOf(session.date()))
        }
        assertFailsWith<IllegalArgumentException> {
            GymPlanEditor.skip(p, session.id(), session.date().plusDays(1), at, emptySet())
        }
        val reduced = GymPlanEditor.reduce(p, session.id(), start, at, emptySet())
        assertEquals(2, reduced.version())
        assertEquals(p.settings(), reduced.settings())
        assertEquals(p.sessions().drop(1), reduced.sessions().drop(1))
        assertTrue(
            reduced.sessions()[0].exercises().zip(session.exercises()).all { (new, old) ->
                new.sets() == maxOf(1, old.sets() - 1)
            }
        )
        assertEquals(
            p.sessions().size - 1,
            GymPlanEditor.skip(p, session.id(), start, at, emptySet()).sessions().size,
        )
        val moved = GymPlanEditor.move(p, session.id(), start.plusDays(6), start, at, emptySet())
        assertEquals(start.plusDays(6), moved.sessions().first { it.id() == session.id() }.date())
        assertEquals(before, p.toString())
    }

    @Test
    fun loadReferencesAndSubstitutionsStayOptionalAndTaskSpecific() {
        val p = generate(settings(GymProgramme.Goal.STRENGTH, minutes = 60))
        val session = p.sessions()[0]
        assertTrue(session.exercises().any { it.exerciseId() == "bench-press" })
        val withReference =
            GymPlanEditor.loadReference(p, session.id(), "bench-press", 80.0, start, at, emptySet())
        val swapped =
            GymPlanEditor.swap(
                withReference,
                session.id(),
                "bench-press",
                "dumbbell-bench-press",
                start,
                at,
                emptySet(),
            )
        val replacement =
            swapped.sessions()[0].exercises().first { it.exerciseId() == "dumbbell-bench-press" }
        assertNull(replacement.referenceLoadKg())
        assertEquals(
            80.0,
            withReference
                .sessions()[0]
                .exercises()
                .first { it.exerciseId() == "bench-press" }
                .referenceLoadKg(),
        )
        assertFailsWith<IllegalArgumentException> {
            GymPlanEditor.swap(p, session.id(), "bench-press", "push-up", start, at, emptySet())
        }
        assertFailsWith<IllegalArgumentException> {
            GymPlanEditor.loadReference(
                withReference,
                session.id(),
                "bench-press",
                79.0,
                start,
                at,
                emptySet(),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            GymPlanEditor.loadReference(
                p,
                session.id(),
                "bench-press",
                Double.NaN,
                start,
                at,
                emptySet(),
            )
        }
    }

    @Test
    fun oneOffWorkoutsKeepPrivateCalendarAndProgrammeReferencesOutOfActualIntentions() {
        for (focus in GymNowWorkouts.Focus.values()) for (effort in GymProgramme.Effort.values()) {
            val request =
                GymNowWorkouts.Request(
                    60,
                    setOf("full gym"),
                    focus,
                    GymProgramme.Goal.GENERAL,
                    GymProgramme.Experience.INTERMEDIATE,
                    GymProgramme.Priority.BALANCED,
                    effort,
                )
            val suggestion =
                GymNowWorkouts.generate("one-off", at, request, emptySet(), setOf("back-squat"))
            assertEquals("one-off", suggestion.definition().id())
            assertEquals(focus.title(), suggestion.definition().title())
            assertTrue(
                suggestion.definition().slots().all {
                    it.exercise().id() != "back-squat" && it.target()?.referenceLoadKg() == null
                }
            )
            assertTrue(suggestion.explanation().contains("This does not change a programme."))
            assertFalse(suggestion.definition().toString().contains("one-off-reference"))
        }
        assertFailsWith<IllegalArgumentException> {
            GymNowWorkouts.Request(
                45,
                emptySet(),
                GymNowWorkouts.Focus.FULL_BODY,
                GymProgramme.Goal.GENERAL,
                GymProgramme.Experience.BEGINNER,
                GymProgramme.Priority.BALANCED,
                GymProgramme.Effort.RPE,
            )
        }
    }

    @Test
    fun nativeUkDatePresentationSupportsAllMonthsAndOriginalProlepticYearRange() {
        val months =
            listOf(
                "Jan",
                "Feb",
                "Mar",
                "Apr",
                "May",
                "Jun",
                "Jul",
                "Aug",
                "Sept",
                "Oct",
                "Nov",
                "Dec",
            )
        for (year in listOf(-999999999, -10000, -1, 0, 1, 9999, 10000, 999999999)) {
            val yearText =
                if (year < 0) "-" + (-year).toString().padStart(4, '0')
                else if (year > 9999) "+$year" else year.toString().padStart(4, '0')
            for (month in 1..12) {
                assertEquals(
                    "2 ${months[month - 1]} $yearText",
                    formatGymProgrammeDate(CalendarDates.of(year, month, 2)),
                )
            }
        }
    }
}
