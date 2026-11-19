package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.settings.PrimaryFocus
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

class StrengthProgrammeGenerationContractTest {
    private val first = CalendarDates.of(2026, 11, 9)
    private val at = Instants.ofEpochSecond(1794218400)
    private val engine = StrengthProgrammeGenerator()
    private val equipment = ExerciseMetadata.Equipment.entries.toSet()

    private fun settings(
        goal: GymProgramme.Goal = GymProgramme.Goal.GENERAL,
        experience: GymProgramme.Experience = GymProgramme.Experience.INTERMEDIATE,
        priority: GymProgramme.Priority = GymProgramme.Priority.BALANCED,
        frequency: Int = 3,
        minutes: Int = 60,
        equipment: Set<ExerciseMetadata.Equipment> = this.equipment,
    ) =
        StrengthProgramme.Settings(
            goal,
            priority,
            experience,
            DayOfWeek.entries.toSet(),
            frequency,
            minutes,
            equipment,
        )

    private fun request(
        s: StrengthProgramme.Settings = settings(),
        weeks: Int = 6,
        start: LocalDate = first,
        template: String? = null,
        focus: PrimaryFocus = PrimaryFocus.STRENGTH,
        exclusions: Set<String> = emptySet(),
        runs: Set<LocalDate> = emptySet(),
        race: LocalDate? = null,
        recovery: Boolean = false,
    ) =
        StrengthProgrammeGenerator.Request(
            "programme",
            at,
            "My programme",
            s,
            start,
            weeks,
            template,
            focus,
            runs,
            race,
            exclusions,
            setOf("dumbbell-row", "push-up"),
            recovery,
        )

    @Test
    fun sixWeekCountsAreAnchoredToEveryActualStartWeekday() {
        for (weekday in 0..6) for (frequency in 1..6) {
            val start = first.plusDays(weekday.toLong())
            val p = engine.generate(request(settings(frequency = frequency), start = start))
            assertEquals(6 * frequency, p.workouts().size)
            assertEquals(6, p.blocks().sumOf { it.weeks() })
            assertEquals(start, p.start())
            assertEquals(start.plusDays(41), p.end())
            val weeks = p.workouts().groupBy { (it.date().toEpochDay() - start.toEpochDay()) / 7 }
            assertEquals((0L..5L).toSet(), weeks.keys)
            assertTrue(weeks.values.all { it.size == frequency })
            assertEquals(p.workouts().size, p.workouts().map { it.id() }.toSet().size)
            assertEquals(p.workouts().size, p.workouts().map { it.date() }.toSet().size)
        }
    }

    @Test
    fun nativeAndJvmSimulatorRespectRealMetadataTargetsTimeAndUnknownEvidence() {
        var generated = 0
        var insufficient = 0
        for (goal in GymProgramme.Goal.entries) for (experience in
            GymProgramme.Experience.entries) for (priority in
            GymProgramme.Priority.entries) for (frequency in listOf(1, 2, 4, 6)) for (minutes in
            listOf(20, 45, 60)) for (available in listOf(equipment, emptySet())) {
            val s = settings(goal, experience, priority, frequency, minutes, available)
            val excluded = setOf("back-squat", "bench-press")
            val r =
                request(
                    s,
                    weeks = 1,
                    focus = PrimaryFocus.HYBRID,
                    exclusions = excluded,
                    runs = setOf(first, first.plusDays(4)),
                )
            val p =
                try {
                    engine.generate(r)
                } catch (e: IllegalArgumentException) {
                    assertEquals(
                        "Not enough eligible exercises for a balanced session. Review equipment, exclusions or template.",
                        e.message,
                    )
                    insufficient++
                    continue
                }
            generated++
            assertEquals(frequency, p.workouts().size)
            assertEquals(1, p.revision())
            assertEquals(0, p.parentRevision())
            for (w in p.workouts()) {
                assertTrue(w.expectedMinutes() <= minutes)
                val slots = w.definition().slots()
                assertEquals(slots.size, slots.map { it.exercise().id() }.toSet().size)
                for (slot in slots) {
                    val e = slot.exercise()
                    val m = ExerciseMetadataCatalog.require(e.id())
                    assertTrue(e.id() !in excluded)
                    assertTrue(m.equipment().availableWith(available))
                    assertTrue(m.suitability().appropriateFor(experience))
                    val t = assertNotNull(slot.target())
                    assertNull(t.referenceLoadKg())
                    assertEquals(e.measurement().usesReps(), t.minimumReps() != null)
                    assertEquals(e.measurement().usesDuration(), t.seconds() != null)
                    assertEquals(e.measurement().usesDistance(), t.metres() != null)
                }
            }
            assertTrue(p.tradeoffs().isNotEmpty())
        }
        assertEquals(1080, generated + insufficient)
        assertTrue(generated > 500)
        assertTrue(insufficient > 0)
    }

    @Test
    fun experienceAndGoalsRetainTheirOriginalDistinctBlockStructuresAndTargets() {
        val plans =
            GymProgramme.Experience.entries.map { exp ->
                engine.generate(request(settings(GymProgramme.Goal.STRENGTH, exp), weeks = 12))
            }
        assertEquals(listOf(3, 2, 1), plans.map { it.blocks()[0].weeks() })
        assertTrue(
            plans.all { p ->
                p.blocks().any { it.phase() == StrengthProgramme.Phase.INTENSIFICATION }
            }
        )
        val strongest =
            plans
                .last()
                .blocks()
                .first { it.phase() == StrengthProgramme.Phase.INTENSIFICATION }
                .workouts()[0]
                .definition()
                .slots()[0]
                .target()!!
        assertEquals(3, strongest.minimumReps())
        assertEquals(5, strongest.maximumReps())
        assertEquals(180, strongest.restSeconds())
        val muscle = engine.generate(request(settings(GymProgramme.Goal.MUSCLE), weeks = 12))
        val target =
            muscle
                .blocks()
                .first { it.phase() == StrengthProgramme.Phase.DEVELOPMENT }
                .workouts()[0]
                .definition()
                .slots()[0]
                .target()!!
        assertEquals(8, target.minimumReps())
        assertEquals(12, target.maximumReps())
        assertNull(target.referenceLoadKg())
    }

    @Test
    fun upperLowerUsesDistinctStableSquatAndHingeDays() {
        val p = engine.generate(request(settings(frequency = 4), template = "upper-lower-4"))
        for (week in p.workouts().chunked(4)) {
            assertTrue(week[1].definition().title().contains("squat emphasis"))
            assertTrue(week[3].definition().title().contains("hinge emphasis"))
            assertEquals(
                ExerciseMetadata.Role.SQUAT,
                ExerciseMetadataCatalog.require(week[1].definition().slots()[0].exercise().id())
                    .primaryRole(),
            )
            assertEquals(
                ExerciseMetadata.Role.HINGE,
                ExerciseMetadataCatalog.require(week[3].definition().slots()[0].exercise().id())
                    .primaryRole(),
            )
        }
    }

    @Test
    fun powerSelectionAndMissingBeginnerBallisticsRemainHonest() {
        val novice =
            engine.generate(
                request(settings(GymProgramme.Goal.POWER, GymProgramme.Experience.BEGINNER))
            )
        assertTrue(
            novice.tradeoffs().any { it.contains("no beginner-classified ballistic option") }
        )
        assertTrue(
            novice.workouts().all { w ->
                w.definition().slots().none {
                    ExerciseMetadataCatalog.require(it.exercise().id()).primaryRole() ==
                        ExerciseMetadata.Role.POWER
                }
            }
        )
        for (exp in
            listOf(GymProgramme.Experience.INTERMEDIATE, GymProgramme.Experience.EXPERIENCED)) {
            val p = engine.generate(request(settings(GymProgramme.Goal.POWER, exp), weeks = 12))
            val slot = p.workouts()[0].definition().slots()[0]
            assertEquals(
                ExerciseMetadata.Role.POWER,
                ExerciseMetadataCatalog.require(slot.exercise().id()).primaryRole(),
            )
            assertTrue(slot.target()!!.maximumReps()!! <= 5)
            assertTrue(slot.target()!!.restSeconds()!! >= 180)
        }
    }

    @Test
    fun hybridConflictsAndRaceWindowsAreExplanationsWithoutChangingRunningData() {
        val runs = (0..41).map { first.plusDays(it.toLong()) }.toSet()
        val race = first.plusDays(30)
        val hybrid = engine.generate(request(focus = PrimaryFocus.HYBRID, runs = runs, race = race))
        assertTrue(hybrid.tradeoffs().any { it.contains("on a demanding run day") })
        assertTrue(hybrid.tradeoffs().any { it.contains("Race-week previews") })
        for (w in
            hybrid.workouts().filter {
                it.date().toEpochDay() in race.minusDays(7).toEpochDay()..race.toEpochDay()
            }) {
            assertTrue(w.definition().slots().all { it.target()!!.sets() == 1 })
        }
        val strength =
            engine.generate(request(focus = PrimaryFocus.STRENGTH, runs = runs, race = race))
        assertTrue(
            strength.tradeoffs().none {
                it.contains("on a demanding run day") || it.contains("Race-week previews")
            }
        )
        assertEquals(42, runs.size)
    }

    @Test
    fun nineTemplatesRemainEditableOriginalStructuresAndUnknownIdsFail() {
        val templates = StrengthProgrammeTemplates.all()
        assertEquals(9, templates.size)
        assertEquals(9, templates.map { it.id() }.toSet().size)
        for (t in templates) {
            assertFalse(t.name().isNullOrBlank())
            assertFalse(t.intendedUser().isNullOrBlank())
            assertFalse(t.tradeoff().isNullOrBlank())
            val p =
                engine.generate(
                    request(
                        settings(
                            goal = t.defaultGoal()!!,
                            frequency = t.frequency(),
                            minutes = t.suggestedMinutes(),
                        ),
                        template = t.id(),
                    )
                )
            assertEquals(StrengthProgramme.Origin.TEMPLATE, p.origin())
            assertEquals(t.id(), p.templateId())
            assertEquals(t.frequency() * 6, p.workouts().size)
        }
        assertFailsWith<IllegalArgumentException> { StrengthProgrammeTemplates.require("unknown") }
        assertFailsWith<IllegalArgumentException> {
            request(settings(frequency = 2), template = "upper-lower-4")
        }
        assertFails { (templates as MutableList).clear() }
        assertNull(
            StrengthProgrammeTemplates.Template(null, null, 0, null, null, null, null, 0, null)
                .name()
        )
    }

    @Test
    fun requestFreezesInputsAndRejectsUnsupportedDurationsWithoutApplyingAnything() {
        val exclusions = mutableSetOf("back-squat")
        val r = request(exclusions = exclusions)
        exclusions.clear()
        assertEquals(setOf("back-squat"), r.exclusions())
        assertFails { (r.exclusions() as MutableSet).clear() }
        assertFailsWith<IllegalArgumentException> { r.copy(weeks = 0) }
        assertFailsWith<IllegalArgumentException> { r.copy(weeks = 27) }
        assertEquals(engine.generate(r), engine.generate(r))
    }
}
