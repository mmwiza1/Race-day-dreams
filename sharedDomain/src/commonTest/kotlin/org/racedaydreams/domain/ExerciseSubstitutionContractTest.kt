package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.ExerciseMetadata.Equipment
import org.racedaydreams.domain.strength.ExerciseSubstitutionEngine.Context
import org.racedaydreams.domain.strength.ExerciseSubstitutionEngine.Reason

class ExerciseSubstitutionContractTest {
    private val all = Equipment.entries.toSet()
    private val bench = ExerciseCatalog.definition("bench-press")!!.exercise()

    private fun context(
        original: StrengthExercise = bench,
        equipment: Set<Equipment> = all,
        reason: Reason = Reason.ALTERNATIVES,
        excluded: Set<String> = emptySet(),
        favourites: Set<String> = emptySet(),
        history: Set<String> = emptySet(),
        experience: GymProgramme.Experience = GymProgramme.Experience.INTERMEDIATE,
        goal: GymProgramme.Goal = GymProgramme.Goal.STRENGTH,
    ) = Context(original, goal, experience, equipment, excluded, favourites, history, reason)

    @Test
    fun busyEquipmentOffersIntentCompatibleOptionsWithHonestLoadAndAvailabilityWarnings() {
        val choices = ExerciseSubstitutionEngine.rank(context(reason = Reason.EQUIPMENT_BUSY), 4)
        assertTrue(choices.isNotEmpty())
        assertTrue(choices.size <= 4)
        for (choice in choices) {
            assertEquals(
                ExerciseMetadata.Role.HORIZONTAL_PUSH,
                ExerciseMetadataCatalog.require(choice.exercise().id()).primaryRole(),
            )
            assertTrue(choice.explanation().contains("Check that this setup is free"))
            assertTrue(choice.explanation().contains("loads do not transfer"))
        }
        val noBench = all - Equipment.BENCH - Equipment.ADJUSTABLE_BENCH - Equipment.DECLINE_BENCH
        val available =
            ExerciseSubstitutionEngine.rank(
                context(equipment = noBench, reason = Reason.EQUIPMENT_UNAVAILABLE),
                50,
            )
        assertTrue(available.isNotEmpty())
        assertTrue(
            available.all {
                ExerciseMetadataCatalog.require(it.exercise().id())
                    .equipment()
                    .availableWith(noBench)
            }
        )
    }

    @Test
    fun exclusionsAndUnknownFrozenDefinitionsCannotBeOverriddenByHistoryOrFavourites() {
        val excluded =
            ExerciseSubstitutionEngine.rank(context(), 50).map { it.exercise().id() }.toSet()
        assertTrue(
            ExerciseSubstitutionEngine.rank(
                    context(excluded = excluded, favourites = excluded, history = excluded),
                    50,
                )
                .isEmpty()
        )
        val custom =
            StrengthExercise(
                "custom",
                "My press",
                bench.region(),
                bench.mode(),
                bench.estimateOneRepMax(),
                bench.measurement(),
            )
        assertTrue(ExerciseSubstitutionEngine.rank(context(original = custom), 4).isEmpty())
        val changed =
            StrengthExercise(
                bench.id(),
                bench.name(),
                bench.region(),
                StrengthExercise.Mode.BODYWEIGHT,
                false,
                StrengthExercise.MeasurementProfile.REPS_ONLY,
            )
        assertTrue(ExerciseSubstitutionEngine.rank(context(original = changed), 4).isEmpty())
        val known =
            ExerciseSubstitutionEngine.rank(context(history = setOf("push-up")), 50).first {
                it.exercise().id() == "push-up"
            }
        assertTrue(known.measurementChanged())
        assertTrue(known.explanation().contains("review a fresh target"))
        assertTrue(known.explanation().contains("not a starting-load recommendation"))
    }

    @Test
    fun reviewedEasierHarderRelationshipsAndDiscomfortDoNotPromiseMedicalSuitability() {
        val pushup = ExerciseCatalog.definition("push-up")!!.exercise()
        val easier =
            ExerciseSubstitutionEngine.rank(
                context(
                    original = pushup,
                    reason = Reason.EASIER,
                    experience = GymProgramme.Experience.BEGINNER,
                ),
                50,
            )
        assertTrue(
            easier.any {
                it.exercise().id() == "incline-push-up" || it.exercise().id() == "knee-push-up"
            }
        )
        assertFalse(easier.any { it.exercise().id() == "weighted-push-up" })
        val harder =
            ExerciseSubstitutionEngine.rank(context(original = pushup, reason = Reason.HARDER), 50)
        assertTrue(harder.any { it.exercise().id() == "weighted-push-up" })
        assertFalse(harder.any { it.exercise().id() == "wall-push-up" })
        val uncomfortable =
            ExerciseSubstitutionEngine.rank(context(reason = Reason.UNCOMFORTABLE), 4)
        assertTrue(uncomfortable.all { it.explanation().contains("not medical clearance") })
        assertTrue(ExerciseSubstitutionEngine.discomfortGuidance().contains("cannot determine"))
    }

    @Test
    fun everyCanonicalExerciseRetainsRoleTaskEquipmentExperienceAndExclusionGates() {
        val excluded = setOf("dumbbell-bench-press", "barbell-row", "plank")
        for (original in ExerciseCatalog.exercises()) for (experience in
            GymProgramme.Experience.entries) for (equipment in
            listOf(all, setOf(Equipment.DUMBBELLS, Equipment.BENCH))) for (reason in
            listOf(Reason.ALTERNATIVES, Reason.EQUIPMENT_BUSY)) {
            val choices =
                ExerciseSubstitutionEngine.rank(
                    context(original, equipment, reason, excluded, experience = experience),
                    4,
                )
            val metadata = ExerciseMetadataCatalog.require(original.id())
            assertTrue(choices.size <= 4)
            assertEquals(choices.size, choices.map { it.exercise().id() }.toSet().size)
            for (choice in choices) {
                val replacement = ExerciseMetadataCatalog.require(choice.exercise().id())
                assertNotEquals(original.id(), choice.exercise().id())
                assertFalse(choice.exercise().id() in excluded)
                assertEquals(metadata.primaryRole(), replacement.primaryRole())
                assertTrue(replacement.primaryAreas().any { it in metadata.primaryAreas() })
                assertTrue(replacement.suitability().appropriateFor(experience))
                assertTrue(replacement.equipment().availableWith(equipment))
                assertTrue(
                    ExerciseSubstitutionEngine.compatibleTask(
                        original.measurement(),
                        choice.exercise().measurement(),
                    )
                )
                assertEquals(
                    original.measurement() != choice.exercise().measurement(),
                    choice.measurementChanged(),
                )
            }
        }
    }

    @Test
    fun allGoalsKeepComparableOptionsWithoutInventingLoadsOrChangingTheOriginal() {
        val before = bench.toString()
        for (goal in GymProgramme.Goal.entries) {
            val choices = ExerciseSubstitutionEngine.rank(context(goal = goal), 50)
            assertTrue(choices.isNotEmpty())
            assertTrue(
                choices.all {
                    it.explanation().contains("Previous loads do not transfer") ||
                        it.explanation().contains("previous targets and loads do not transfer")
                }
            )
        }
        assertEquals(before, bench.toString())
    }

    @Test
    fun contextSnapshotsChoicesAndPreservesOriginalWhitespaceValidation() {
        val equipment = all.toMutableSet()
        val excluded = mutableSetOf("push-up")
        val c = context(equipment = equipment, excluded = excluded)
        val original = ExerciseSubstitutionEngine.rank(c, 4)
        equipment.clear()
        excluded.clear()
        assertEquals(original, ExerciseSubstitutionEngine.rank(c, 4))
        assertTrue(c.availableEquipment().isNotEmpty())
        assertEquals(Equipment.entries, c.availableEquipment().toList())
        assertFails { (c.availableEquipment() as MutableSet<Equipment>).clear() }
        assertTrue("push-up" in c.excludedIds())
        assertFailsWith<IllegalArgumentException> { context(excluded = setOf(" \t\n")) }
        assertTrue("\u00a0" in context(excluded = setOf("\u00a0")).excludedIds())
        assertFailsWith<IllegalArgumentException> { ExerciseSubstitutionEngine.rank(c, 0) }
        assertFailsWith<IllegalArgumentException> { ExerciseSubstitutionEngine.rank(c, 51) }
        assertFailsWith<NullPointerException> { ExerciseSubstitutionEngine.rank(null, 4) }
        assertFailsWith<IllegalArgumentException> {
            Context(
                bench,
                GymProgramme.Goal.GENERAL,
                GymProgramme.Experience.BEGINNER,
                setOf(null),
                emptySet(),
                emptySet(),
                emptySet(),
                Reason.ALTERNATIVES,
            )
        }
    }
}
