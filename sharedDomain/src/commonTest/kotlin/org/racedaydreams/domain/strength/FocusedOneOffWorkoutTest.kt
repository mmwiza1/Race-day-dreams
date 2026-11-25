package org.racedaydreams.domain.strength

import kotlin.test.*
import org.racedaydreams.domain.time.Instants

class FocusedOneOffWorkoutTest {
    private val groups =
        setOf(
            GymNowWorkouts.MuscleGroup.CHEST,
            GymNowWorkouts.MuscleGroup.SHOULDERS,
            GymNowWorkouts.MuscleGroup.ARMS,
        )
    private val equipment = setOf("bodyweight", "dumbbells", "bench")
    private val time = Instants.ofEpochSecond(100)

    @Test
    fun sharesTimeBudgetAcrossSelectedGroupsWithValidTargets() {
        val request = request(equipment, groups)
        val excluded = setOf("bench-press", "dumbbell-bench-press")
        val result = GymNowWorkouts.generate("focused", time, request, excluded = excluded)
        val slots = result.definition().slots()
        assertTrue(slots.size in 3..4)
        assertEquals(slots.size, slots.map { it.exercise().id() }.toSet().size)
        assertTrue(slots.none { it.exercise().id() in excluded })
        val setup = GymNowWorkouts.capabilities(equipment)
        assertTrue(
            slots.all {
                ExerciseMetadataCatalog.require(it.exercise().id()).equipment().availableWith(setup)
            }
        )
        groups.forEach { group ->
            assertTrue(
                slots.any { group.matches(ExerciseMetadataCatalog.require(it.exercise().id())) }
            )
        }
        assertTrue(slots.all { it.target()?.referenceLoadKg() == null })
        val trainingSeconds =
            slots.sumOf {
                it.target()!!.let { target -> target.sets() * (60 + target.restSeconds()!!) + 60 }
            }
        assertTrue(trainingSeconds + 360 <= 30 * 60)
    }

    @Test
    fun bodyweightOnlyAndAlternativesRespectEvidenceAndSetup() {
        val request = request(setOf("bodyweight"), setOf(GymNowWorkouts.MuscleGroup.CORE))
        val workout = GymNowWorkouts.generate("bodyweight", time, request).definition()
        workout.slots().forEach { original ->
            assertTrue(
                ExerciseMetadataCatalog.require(original.exercise().id())
                    .equipment()
                    .equipmentFree()
            )
            val options =
                GymNowWorkouts.alternatives(request, workout, original, emptySet(), emptySet())
            assertTrue(
                options.all { option ->
                    option.exercise().measurement() == original.exercise().measurement() &&
                        ExerciseMetadataCatalog.require(option.exercise().id())
                            .equipment()
                            .equipmentFree() &&
                        workout.slots().none { it.exercise().id() == option.exercise().id() }
                }
            )
        }
    }

    @Test
    fun bandAndAnchorAreSeparateCapabilitiesForSelectionAndAlternatives() {
        val arms = setOf(GymNowWorkouts.MuscleGroup.ARMS)
        val band = setOf("bodyweight", "resistance bands")
        val result = GymNowWorkouts.generate("bands", time, request(band, arms)).definition()
        assertTrue(result.slots().any { it.exercise().id() == "banded-curl" })
        assertFalse(GymProgrammeGenerator.available("banded-row", band))
        assertTrue(GymProgrammeGenerator.available("banded-row", band + "band anchor"))
        assertTrue(GymProgrammeGenerator.available("banded-glute-bridge", band))
        assertFalse(GymProgrammeGenerator.available("banded-glute-bridge", setOf("bodyweight")))
        assertEquals(
            StrengthExercise.MeasurementProfile.DURATION,
            ExerciseCatalog.definition("static-squat-hold")!!.exercise().measurement(),
        )
        assertEquals(
            StrengthExercise.MeasurementProfile.REPS_ONLY,
            ExerciseCatalog.definition("squat-pulse")!!.exercise().measurement(),
        )
    }

    private fun request(equipment: Set<String>, groups: Set<GymNowWorkouts.MuscleGroup>) =
        GymNowWorkouts.Request(
            30,
            equipment,
            GymNowWorkouts.Focus.FULL_BODY,
            GymProgramme.Goal.GENERAL,
            GymProgramme.Experience.BEGINNER,
            GymProgramme.Priority.STRENGTH_FIRST,
            GymProgramme.Effort.RPE,
            groups,
        )
}
