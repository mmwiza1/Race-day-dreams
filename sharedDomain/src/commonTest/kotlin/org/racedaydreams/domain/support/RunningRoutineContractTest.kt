package org.racedaydreams.domain.support

import kotlin.test.*
import org.racedaydreams.domain.WorkoutCategory
import org.racedaydreams.domain.coaching.WorkoutCategories
import org.racedaydreams.domain.strength.TechniqueIllustrations

class RunningRoutineContractTest {
    @Test
    fun originalRoutinesKeepAllMovementIdentitiesInstructionsAndReviewedArtwork() {
        assertEquals(14, RunningRoutines.MOVEMENTS.size)
        assertEquals(13, RunningRoutines.all().size)
        assertEquals(14, RunningRoutines.MOVEMENTS.map { it.id() }.toSet().size)
        assertEquals(13, RunningRoutines.all().map { it.id() }.toSet().size)
        for (movement in RunningRoutines.MOVEMENTS) {
            assertFalse(movement.title().isNullOrBlank())
            assertFalse(movement.instructions().isNullOrBlank())
            assertFalse(movement.dose().isNullOrBlank())
            assertFalse(movement.cue().isNullOrBlank())
            assertFalse(movement.mistake().isNullOrBlank())
            assertNotNull(TechniqueIllustrations.forRoutine(movement.id()))
        }
        for (routine in RunningRoutines.all()) {
            assertEquals(routine, RunningRoutines.get(routine.id()))
            assertTrue(routine.minutes() > 0)
            assertTrue(assertNotNull(routine.movements()).isNotEmpty())
            assertTrue(routine.movements()!!.all { it in RunningRoutines.MOVEMENTS })
        }
    }

    @Test
    fun qualityAndDemandingClassificationsRemainExactlyTheAdaptiveCoachRules() {
        val quality =
            setOf(
                WorkoutCategory.INTERVALS,
                WorkoutCategory.TEMPO_THRESHOLD_RUN,
                WorkoutCategory.RACE_PACE_RUN,
                WorkoutCategory.TIME_TRIAL,
                WorkoutCategory.PROGRESSION_RUN,
            )
        for (category in WorkoutCategory.values()) {
            assertEquals(category in quality, WorkoutCategories.isQuality(category))
            assertEquals(
                category in quality ||
                    category == WorkoutCategory.LONG_RUN ||
                    category == WorkoutCategory.RACE,
                WorkoutCategories.isDemanding(category),
            )
        }
        assertFailsWith<NullPointerException> { WorkoutCategories.isQuality(null) }
        assertFailsWith<NullPointerException> { WorkoutCategories.isDemanding(null) }
    }

    @Test
    fun originalRaceDistanceBoundariesAndOptionalWarmupSelectionArePreserved() {
        assertEquals("warm-race-short", RunningRoutines.warmup(WorkoutCategory.RACE, 19.999))
        assertEquals("warm-race-half", RunningRoutines.warmup(WorkoutCategory.RACE, 20.0))
        assertEquals("warm-race-half", RunningRoutines.warmup(WorkoutCategory.RACE, 39.999))
        assertEquals("warm-race-marathon", RunningRoutines.warmup(WorkoutCategory.RACE, 40.0))
        assertEquals("warm-race-marathon", RunningRoutines.warmup(WorkoutCategory.RACE, 42.195))
        assertEquals("warm-long", RunningRoutines.warmup(WorkoutCategory.LONG_RUN, 0.0))
        assertEquals("warm-quality", RunningRoutines.warmup(WorkoutCategory.INTERVALS, 0.0))
        assertEquals("warm-easy", RunningRoutines.warmup(WorkoutCategory.RECOVERY_RUN, 0.0))
        assertFailsWith<NullPointerException> { RunningRoutines.warmup(null, 0.0) }
    }

    @Test
    fun sharedDefinitionsAreFrozenButEachOriginalFixedSizeLibraryCanReplaceItsOwnElement() {
        assertFails { (RunningRoutines.MOVEMENTS as MutableList).clear() }
        val library = RunningRoutines.all() as MutableList
        val original = library[0]
        assertEquals(original, library.set(0, library[1]))
        assertNotEquals(original, library[0])
        assertEquals(original, RunningRoutines.get(original.id()))
        assertFails { library.add(original) }
        assertFails { library.removeAt(0) }
        assertFails { (original.movements() as MutableList).clear() }
    }

    @Test
    fun mobilityAndUnknownLookupsDoNotInventTrainingOrGuidance() {
        val expected = setOf("mobility-five", "mobility-ten", "hips", "ankles", "post-long")
        assertEquals(
            expected,
            RunningRoutines.all()
                .filter { RunningRoutines.isMobility(it.id()) }
                .map { it.id() }
                .toSet(),
        )
        assertFalse(RunningRoutines.isMobility(null))
        for (id in listOf(null, "", "custom", " warm-easy ")) {
            assertFailsWith<IllegalArgumentException> { RunningRoutines.get(id) }
        }
        assertTrue(
            RunningRoutines.get("warm-quality").purpose()!!.contains("rather than adding mileage")
        )
        assertTrue(
            RunningRoutines.get("cool-demanding")
                .purpose()!!
                .contains("not a guarantee of recovery")
        )
    }

    @Test
    fun rawRecordConstructorsKeepUnknownsAndOriginalShallowListBehaviour() {
        val raw = RunningRoutines.Movement(null, null, null, null, null, null, null)
        assertNull(raw.id())
        assertNull(raw.instructions())
        assertEquals(
            "Movement[id=null, title=null, dose=null, instructions=null, cue=null, mistake=null, pose=null]",
            raw.toString(),
        )
        val list = mutableListOf(raw)
        val routine = RunningRoutines.Routine(null, null, null, -1, list)
        list.clear()
        assertTrue(routine.movements()!!.isEmpty())
        assertEquals(-1, routine.minutes())
        assertNull(RunningRoutines.Routine(null, null, null, 0, null).movements())
    }
}
