package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.strength.*

class TechniqueGuidanceContractTest {
    @Test
    fun everyCanonicalExerciseRetainsItsCompleteOriginalGuideAndArtwork() {
        assertEquals(246, ExerciseCatalog.exercises().size)
        for (exercise in ExerciseCatalog.exercises()) {
            val guide = assertNotNull(TechniqueGuide.forExercise(exercise.id()))
            val art = assertNotNull(TechniqueIllustrations.forExercise(exercise.id()))
            assertFalse(guide.pattern().isNullOrBlank())
            assertFalse(guide.setup().isNullOrBlank())
            assertFalse(guide.movement().isNullOrBlank())
            assertFalse(guide.muscles().isNullOrBlank())
            assertTrue(assertNotNull(guide.cues()).isNotEmpty())
            assertTrue(assertNotNull(guide.mistakes()).isNotEmpty())
            assertTrue(assertNotNull(guide.alternatives()).isNotEmpty())
            assertTrue(art.asset().startsWith("guide_"))
            assertTrue(art.description().length > 60)
            assertTrue(art.phases().isNotEmpty())
            assertTrue(art.phases().none { it.isBlank() })
        }
    }

    @Test
    fun expandedIdentitiesAreFrozenAndEachRetainsItsSpecificInstructionAndArt() {
        val ids = ExpandedExerciseGuides.identities()
        assertEquals(46, ids.size)
        assertFails { (ids as MutableSet).clear() }
        for (id in ids) {
            assertNotNull(ExerciseCatalog.definition(id))
            assertEquals(ExpandedExerciseGuides.guide(id), TechniqueGuide.forExercise(id))
            assertEquals(ExpandedExerciseGuides.artwork(id), TechniqueIllustrations.forExercise(id))
            assertEquals(
                "guide_" + id.replace('-', '_'),
                ExpandedExerciseGuides.artwork(id)!!.asset(),
            )
        }
    }

    @Test
    fun explicitVariantsAndSafetyCopyDoNotCollapseIntoGenericPatterns() {
        assertTrue(
            TechniqueGuide.forExercise("front-squat")!!.setup()!!.contains(
                "front of your shoulders"
            )
        )
        assertTrue(
            TechniqueGuide.forExercise("low-bar-squat")!!.setup()!!.contains(
                "below the upper traps"
            )
        )
        assertTrue(
            TechniqueGuide.forExercise("sumo-deadlift")!!.setup()!!.contains("inside your knees")
        )
        assertTrue(TechniqueGuide.forExercise("pause-squat")!!.movement()!!.contains("pause"))
        assertTrue(TechniqueGuide.forExercise("weighted-push-up")!!.setup()!!.contains("vest"))
        assertTrue(
            TechniqueIllustrations.forExercise("kettlebell-swing")!!.description()
                .contains("not an overhead")
        )
        assertEquals(
            TechniqueIllustrations.forExercise("back-squat")!!.asset(),
            TechniqueIllustrations.forExercise("high-bar-squat")!!.asset(),
        )
        assertNotEquals(
            TechniqueIllustrations.forExercise("chin-up")!!.asset(),
            TechniqueIllustrations.forExercise("weighted-chin-up")!!.asset(),
        )
        assertEquals(1, TechniqueIllustrations.forExercise("weighted-side-plank")!!.phases().size)
    }

    @Test
    fun allOriginalRunningPreparationIllustrationsRemainDistinctFromLoadedGymWork() {
        val ids =
            listOf(
                "lunge",
                "bridge",
                "swing",
                "walk",
                "jog",
                "march",
                "calf",
                "hip",
                "quad",
                "glute",
                "stride",
                "circles",
                "calf-stretch",
                "ankle",
            )
        for (id in ids) {
            val art = assertNotNull(TechniqueIllustrations.forRoutine(id))
            assertTrue(art.description().isNotBlank())
            assertTrue(art.phases().isNotEmpty())
        }
        assertNotEquals(
            TechniqueIllustrations.forRoutine("calf")!!.asset(),
            TechniqueIllustrations.forExercise("calf-raise")!!.asset(),
        )
        assertEquals(
            TechniqueIllustrations.forExercise("walking-lunge"),
            TechniqueIllustrations.forRoutine("lunge"),
        )
        assertEquals(
            TechniqueIllustrations.forExercise("glute-bridge"),
            TechniqueIllustrations.forRoutine("bridge"),
        )
    }

    @Test
    fun unknownOrCustomExercisesNeverReceiveInventedGuidanceOrArtwork() {
        for (id in listOf("unknown", "custom-my-press", "", " bench-press ")) {
            assertNull(TechniqueGuide.forExercise(id))
            assertNull(TechniqueIllustrations.forExercise(id))
            assertNull(TechniqueIllustrations.forRoutine(id))
            assertNull(ExpandedExerciseGuides.guide(id))
            assertNull(ExpandedExerciseGuides.artwork(id))
        }
        assertFailsWith<NullPointerException> { TechniqueGuide.forExercise(null) }
        assertFailsWith<NullPointerException> { TechniqueIllustrations.forExercise(null) }
        assertFailsWith<NullPointerException> { TechniqueIllustrations.forRoutine(null) }
        assertFailsWith<NullPointerException> { ExpandedExerciseGuides.guide(null) }
    }

    @Test
    fun artworkSnapshotsItsPhasesAndPreservesOriginalValidationBoundary() {
        val phases = mutableListOf("Start", "Finish")
        val art = TechniqueIllustrations.Artwork("guide_reviewed", "Original description", phases)
        phases.clear()
        assertEquals(listOf("Start", "Finish"), art.phases())
        assertFails { (art.phases() as MutableList).clear() }
        assertFailsWith<IllegalArgumentException> {
            TechniqueIllustrations.Artwork("generic", "Description", listOf("Start"))
        }
        assertFailsWith<IllegalArgumentException> {
            TechniqueIllustrations.Artwork("guide_digit1", "Description", listOf("Start"))
        }
        assertFailsWith<IllegalArgumentException> {
            TechniqueIllustrations.Artwork("guide_ok", " \t\n", listOf("Start"))
        }
        assertFailsWith<IllegalArgumentException> {
            TechniqueIllustrations.Artwork("guide_ok", "Description", emptyList())
        }
        assertFailsWith<IllegalArgumentException> {
            TechniqueIllustrations.Artwork(null, "Description", listOf("Start"))
        }
        assertFailsWith<IllegalArgumentException> {
            TechniqueIllustrations.Artwork("guide_ok", "Description", null)
        }
        // Java String.trim does not trim a non-breaking space; the original raw DTO allowed it.
        assertEquals(
            "\u00a0",
            TechniqueIllustrations.Artwork("guide_ok", "\u00a0", listOf("")).description(),
        )
    }

    @Test
    fun originalRawGuideDtoRemainsUnvalidatedWhileCatalogueGuidesExposeFrozenLists() {
        val raw = TechniqueGuide(null, null, null, null, null, null, null)
        assertNull(raw.pattern())
        assertNull(raw.cues())
        assertEquals(
            "TechniqueGuide[pattern=null, setup=null, movement=null, cues=null, mistakes=null, muscles=null, alternatives=null]",
            raw.toString(),
        )
        val cues = mutableListOf("One")
        val rawList = raw.copy(cues = cues)
        cues.add("Two")
        assertEquals(listOf("One", "Two"), rawList.cues())
        val guide = TechniqueGuide.forExercise("bench-press")!!
        assertFails { (guide.cues() as MutableList).clear() }
        assertFails { (guide.mistakes() as MutableList).clear() }
    }

    @Test
    fun privateCopyTableSplittingKeepsOriginalJavaTrailingEmptySemantics() {
        assertEquals(listOf("one", "two"), techniqueWords("one|two||"))
        assertEquals(listOf(""), techniqueWords(""))
        assertEquals(emptyList(), techniqueWords("||"))
        assertEquals(listOf("", "two"), techniqueWords("|two"))
    }
}
