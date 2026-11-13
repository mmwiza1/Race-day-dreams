package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.GymWorkoutDefinition.*
import org.racedaydreams.domain.time.*

class AuthoredStrengthModelsTest {
    private val at = Instants.ofEpochSecond(100, 123456789)
    private val start = CalendarDates.of(2026, 11, 13)
    private val bench = requireNotNull(ExerciseCatalog.definition("bench-press")).exercise()
    private val row = requireNotNull(ExerciseCatalog.definition("barbell-row")).exercise()
    private val effort = EffortRange(EffortScale.RIR, 2.5, 4.0)
    private val target = Target(2, 4, 6, null, null, effort, 120, 80.0)

    private fun slot(
        id: String = "bench",
        exercise: StrengthExercise = bench,
        group: String? = null,
    ) = Slot(id, exercise, target, group)

    private val equipment =
        setOf(
            ExerciseMetadata.Equipment.BARBELL,
            ExerciseMetadata.Equipment.BENCH,
            ExerciseMetadata.Equipment.SQUAT_RACK,
        )

    private fun settings() =
        StrengthProgramme.Settings(
            GymProgramme.Goal.STRENGTH,
            GymProgramme.Priority.BALANCED,
            GymProgramme.Experience.INTERMEDIATE,
            setOf(DayOfWeek.TUESDAY),
            1,
            60,
            equipment,
        )

    private fun day(slots: List<Slot> = listOf(slot())) =
        StrengthProgrammeDraft.Day("day", DayOfWeek.TUESDAY, "Upper", slots, null)

    private fun draft(
        first: LocalDate = start,
        blocks: List<StrengthProgrammeDraft.WeekBlock> =
            listOf(
                StrengthProgrammeDraft.WeekBlock(
                    "foundation",
                    StrengthProgramme.Phase.FOUNDATION,
                    "Foundation",
                    2,
                    listOf(day()),
                )
            ),
    ) =
        StrengthProgrammeDraft(
            "draft",
            1,
            "programme",
            0,
            "Authored",
            settings(),
            first,
            blocks,
            StrengthProgramme.Origin.CUSTOM,
            null,
        )

    @Test
    fun targetReferencesRemainOptionalAndDoNotCreateActualWeights() {
        assertEquals(80.0, target.referenceLoadKg())
        assertNull(target.withoutLoadReference().referenceLoadKg())
        assertEquals(effort, target.withoutLoadReference().effort())
        assertEquals(2.5, effort.minimum())
        assertEquals(EffortScale.RIR, effort.scale())
        assertNull(Slot("open", bench, null, null).target())
        assertEquals(0, target.copy(restSeconds = 0).restSeconds())
        assertFailsWith<IllegalArgumentException> { target.copy(minimumReps = null) }
        assertFailsWith<IllegalArgumentException> { target.copy(seconds = 30) }
        assertFailsWith<IllegalArgumentException> { effort.copy(maximum = 2.0) }
        val timed = Target(1, null, null, 30, null, effort, null, null)
        val plank = requireNotNull(ExerciseCatalog.definition("plank")).exercise()
        assertFailsWith<IllegalArgumentException> { Slot("hold", plank, timed, null) }
        assertEquals(
            timed.copy(effort = null),
            Slot("hold", plank, timed.copy(effort = null), null).target(),
        )
        assertFailsWith<IllegalArgumentException> {
            Slot("hold", plank, timed.copy(effort = null, referenceLoadKg = 10.0), null)
        }
    }

    @Test
    fun workoutSnapshotsRetainGroupingAndRejectSplitOrSingletonSupersets() {
        val slots = mutableListOf(slot("a", bench, "group"), slot("b", row, "group"))
        val frozen = GymWorkoutDefinition("definition", 1, at, "Upper", slots)
        slots.clear()
        assertEquals(listOf("a", "b"), frozen.slots().map { it.id() })
        assertFails { (frozen.slots() as MutableList<Slot>).clear() }
        assertFailsWith<IllegalArgumentException> {
            frozen.copy(slots = listOf(slot("a", bench, "group")))
        }
        assertFailsWith<IllegalArgumentException> {
            frozen.copy(
                slots = listOf(slot("a", bench, "group"), slot("b", row), slot("c", bench, "group"))
            )
        }
        assertFailsWith<IllegalArgumentException> { frozen.copy(slots = listOf(slot(), slot())) }
        assertEquals(
            2,
            frozen.copy(slots = frozen.slots().map { it.copy(supersetGroup = null) }).slots().size,
        )
    }

    @Test
    fun incompleteDraftsAreSaveableButPreviewNeverInventsTargetsOrEquipment() {
        assertEquals("", draft().copy(name = "", blocks = emptyList()).name())
        val open =
            draft(
                blocks =
                    listOf(
                        StrengthProgrammeDraft.WeekBlock(
                            "foundation",
                            StrengthProgramme.Phase.FOUNDATION,
                            "Foundation",
                            1,
                            listOf(day(listOf(Slot("open", bench, null, null)))),
                        )
                    )
            )
        assertFailsWith<IllegalArgumentException> { open.preview(at, emptySet(), emptyMap()) }
        assertFailsWith<IllegalArgumentException> {
            draft().preview(at, setOf(bench.id()), emptyMap())
        }
        assertFailsWith<IllegalArgumentException> {
            draft()
                .copy(settings = settings().copy(equipment = emptySet()))
                .preview(at, emptySet(), emptyMap())
        }
        assertFailsWith<IllegalArgumentException> {
            draft().copy(blocks = emptyList()).preview(at, emptySet(), emptyMap())
        }
        val original = draft()
        val preview = original.preview(at, emptySet(), emptyMap())
        assertEquals(1, original.revision())
        assertEquals(1, preview.revision())
        assertEquals(0, preview.parentRevision())
        assertEquals(target, preview.workouts().first().definition().slots().first().target())
        assertTrue(preview.reason().contains("not yet activated"))
        assertEquals(123456789, preview.createdAt().getNano())
    }

    @Test
    fun everyStartWeekdayKeepsDatedPrescriptionsInsideActualProgrammeWeeks() {
        for (offset in 0L..6L) {
            val first = start.plusDays(offset)
            val source = draft(first)
            val preview = source.preview(at, emptySet(), emptyMap())
            assertEquals(2, preview.workouts().size)
            assertEquals(first, preview.start())
            assertEquals(first.plusDays(13), preview.end())
            assertEquals(
                7L,
                preview.workouts()[1].date().toEpochDay() -
                    preview.workouts()[0].date().toEpochDay(),
            )
            for (workout in preview.workouts()) {
                assertEquals(DayOfWeek.TUESDAY, workout.date().getDayOfWeek())
                assertTrue(
                    workout.date().toEpochDay() in
                        first.toEpochDay()..first.plusDays(13).toEpochDay()
                )
            }
        }
    }

    @Test
    fun draftEditsCreateNewRevisionsAndKeepPreviousSnapshots() {
        val source = draft()
        val duplicate = source.duplicateBlock("foundation", "development")
        assertEquals(1, source.blocks().size)
        assertEquals(2, duplicate.blocks().size)
        assertEquals(2, duplicate.revision())
        assertEquals("development-day-0", duplicate.blocks()[1].days().single().id())
        val reordered = duplicate.reorderBlock("development", 0)
        assertEquals("development", reordered.blocks().first().id())
        assertEquals("foundation", duplicate.blocks().first().id())
        assertTrue(source.deleteBlock("foundation").blocks().isEmpty())
        assertFailsWith<IllegalArgumentException> { source.deleteDay("foundation", "missing") }
        assertFailsWith<IllegalArgumentException> { source.reorderBlock("foundation", 2) }
        val withSecond =
            source
                .duplicateDay("foundation", "day", "other", DayOfWeek.THURSDAY)
                .replaceDay(
                    "foundation",
                    StrengthProgrammeDraft.Day(
                        "other",
                        DayOfWeek.THURSDAY,
                        "Other",
                        listOf(slot("row", row)),
                        null,
                    ),
                )
        val moved = withSecond.moveExercise("foundation", "day", "foundation", "other", "bench", 0)
        assertTrue(moved.blocks().first().days().first().slots().isEmpty())
        assertEquals(2, moved.blocks().first().days()[1].slots().size)
        assertEquals(1, source.blocks().first().days().single().slots().size)
    }

    @Test
    fun customMetadataMustMatchFrozenDefinitionsWithoutAutomaticProgression() {
        val exercise =
            StrengthExercise(
                "custom-press",
                "Custom Press",
                StrengthExercise.Region.UPPER,
                StrengthExercise.Mode.WEIGHTED,
                false,
            )
        val custom =
            CustomStrengthExercise(
                exercise,
                ExerciseMetadata.BodyArea.CHEST,
                ExerciseMetadata.Role.HORIZONTAL_PUSH,
                ExerciseMetadata.EquipmentRequirement(listOf(equipment)),
                null,
            )
        val source =
            draft(
                blocks =
                    listOf(
                        StrengthProgrammeDraft.WeekBlock(
                            "foundation",
                            StrengthProgramme.Phase.FOUNDATION,
                            "Foundation",
                            1,
                            listOf(day(listOf(slot("custom", exercise)))),
                        )
                    )
            )
        assertFailsWith<IllegalArgumentException> { source.preview(at, emptySet(), emptyMap()) }
        assertFailsWith<IllegalArgumentException> {
            source.preview(
                at,
                emptySet(),
                mapOf(exercise.id() to custom.copy(exercise = exercise.copy(name = "Renamed"))),
            )
        }
        assertEquals(
            exercise,
            source
                .preview(at, emptySet(), mapOf(exercise.id() to custom))
                .workouts()
                .single()
                .definition()
                .slots()
                .single()
                .exercise(),
        )
        assertFailsWith<IllegalArgumentException> {
            custom.copy(exercise = exercise.copy(estimateOneRepMax = true))
        }
        assertNull(custom.notes())
    }

    @Test
    fun exclusionPreservesIdentityReasonAndTimeWithoutDeletingTheEvidence() {
        val exclusion =
            ExerciseExclusionRepository.Exclusion(
                " bench-press ",
                ExerciseExclusionRepository.Reason.UNCOMFORTABLE,
                at,
            )
        assertEquals(" bench-press ", exclusion.exerciseId())
        assertEquals(123456789, exclusion.excludedAt().getNano())
        assertEquals(ExerciseExclusionRepository.Reason.UNCOMFORTABLE, exclusion.reason())
        assertFailsWith<IllegalArgumentException> { exclusion.copy(exerciseId = "\u2003") }
        assertEquals("\u00a0", exclusion.copy(exerciseId = "\u00a0").exerciseId())
        assertFailsWith<IllegalArgumentException> {
            StrengthProgramme.text("\u2003", 120, false, "name")
        }
        assertEquals("\u00a0", StrengthProgramme.text("\u00a0", 120, false, "name"))
        assertNull(StrengthProgramme.text(null, 120, true, "notes"))
        assertFailsWith<IllegalArgumentException> { StrengthProgramme.text("", 120, true, "notes") }
    }

    @Test
    fun budgetKeepsOriginalOptionalRestAndDistanceRounding() {
        assertEquals(4, StrengthWorkoutBudget.workoutMinutes(emptyList()))
        assertEquals(
            1,
            StrengthWorkoutBudget.exerciseMinutes(
                Target(1, null, null, null, 0.24999999999999997, null, 0, null)
            ),
        )
        assertEquals(
            2,
            StrengthWorkoutBudget.exerciseMinutes(Target(1, null, null, null, 0.25, null, 0, null)),
        )
        assertTrue(
            StrengthWorkoutBudget.exerciseMinutes(target.copy(sets = 3, restSeconds = null)) >
                StrengthWorkoutBudget.exerciseMinutes(target.copy(sets = 3, restSeconds = 0))
        )
        assertFailsWith<IllegalArgumentException> { StrengthWorkoutBudget.exerciseMinutes(null) }
    }
}
