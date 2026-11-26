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

    @Test
    fun machinesAndCablesGenerateAndSuggestCompatibleExercises() {
        val equipmentCases =
            listOf(
                Triple(
                    ExerciseMetadata.EquipmentGroup.MACHINES,
                    GymNowWorkouts.MuscleGroup.LEGS,
                    "leg-press",
                ),
                Triple(
                    ExerciseMetadata.EquipmentGroup.CABLES,
                    GymNowWorkouts.MuscleGroup.ARMS,
                    "triceps-pressdown",
                ),
            )
        for ((group, muscle, exercise) in equipmentCases) {
            val label = group.name.lowercase()
            val available = GymNowWorkouts.capabilities(setOf(label))
            assertEquals(
                ExerciseMetadata.Equipment.entries.filter { it.group() == group }.toSet(),
                available,
            )
            val request = request(setOf(label), setOf(muscle))
            val workout =
                GymNowWorkouts.generate(label, time, request, favourites = setOf(exercise))
                    .definition()
            val original = workout.slots().first { it.exercise().id() == exercise }
            val alternatives =
                GymNowWorkouts.alternatives(
                    request,
                    workout.copy(slots = listOf(original)),
                    original,
                    emptySet(),
                    emptySet(),
                )
            assertTrue(alternatives.isNotEmpty(), "$label offers compatible alternatives")
            if (group == ExerciseMetadata.EquipmentGroup.CABLES)
                assertTrue(alternatives.any { it.exercise().id() == "rope-pressdown" })
            val offered = workout.slots().map { it.exercise() } + alternatives.map { it.exercise() }
            assertTrue(
                offered.all {
                    ExerciseMetadataCatalog.require(it.id()).equipment().availableWith(available)
                }
            )
        }
    }

    @Test
    fun generalOneOffUsesSelectedMachineAndCableSetups() {
        for ((label, required) in
            listOf(
                "machines" to setOf("leg-press", "machine-chest-press", "machine-row"),
                "cables" to setOf("seated-cable-row"),
            )) {
            val request = request(setOf(label), emptySet())
            val workout = GymNowWorkouts.generate(label, time, request).definition()
            val actual = workout.slots().map { it.exercise().id() }
            assertTrue(actual.containsAll(required), "$label generated $actual")
            val available = GymNowWorkouts.capabilities(setOf(label))
            assertTrue(
                actual.all {
                    ExerciseMetadataCatalog.require(it).equipment().availableWith(available)
                }
            )
        }
    }

    @Test
    fun equipmentCapabilityBoundariesFollowThePicker() {
        val band = ExerciseMetadata.Equipment.RESISTANCE_BAND
        val anchor = ExerciseMetadata.Equipment.SECURE_BAND_ANCHOR
        assertEquals(emptySet(), GymNowWorkouts.capabilities(setOf("bodyweight")))
        assertEquals(setOf(band), GymNowWorkouts.capabilities(setOf("resistance bands")))
        assertEquals(setOf(anchor), GymNowWorkouts.capabilities(setOf("band anchor")))
        assertEquals(
            setOf(band, anchor),
            GymNowWorkouts.capabilities(setOf("resistance bands", "band anchor")),
        )
        assertEquals(
            ExerciseMetadata.Equipment.entries.toSet(),
            GymNowWorkouts.capabilities(setOf("full gym")),
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
