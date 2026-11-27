package org.racedaydreams.coach

import android.graphics.BitmapFactory
import android.test.AndroidTestCase
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.support.RunningRoutines

/** Catalogue-wide packaged-resource integrity; design fidelity is reviewed separately. */
class GuidanceArtworkTest : AndroidTestCase() {
    // Reviewed contact-sheet crops retain their original framing; older guides are 1200 × 800.
    private val contactSheetBounds =
        mapOf(
            "guide_decline_push_up" to (506 to 318),
            "guide_diamond_push_up" to (506 to 318),
            "guide_oblique_crunch" to (506 to 318),
            "guide_floor_crunch" to (506 to 335),
            "guide_plank_hip_dip" to (506 to 335),
            "guide_spiderman_crunch" to (506 to 335),
            "guide_forward_step_down" to (506 to 350),
            "guide_ski_erg" to (506 to 350),
            "guide_wide_grip_push_up" to (506 to 350),
            "guide_air_bike" to (508 to 337),
            "guide_bicycle_crunch" to (508 to 337),
            "guide_box_jump" to (508 to 337),
            "guide_heel_touch_crunch" to (508 to 337),
            "guide_high_knees" to (508 to 337),
            "guide_medicine_ball_slam" to (508 to 337),
            "guide_pop_squat" to (508 to 337),
            "guide_reverse_crunch" to (508 to 337),
            "guide_squat_jump" to (508 to 337),
            "guide_band_pull_apart" to (508 to 508),
            "guide_banded_curl" to (508 to 508),
            "guide_banded_glute_bridge" to (508 to 508),
            "guide_banded_pallof_press" to (508 to 508),
            "guide_banded_row" to (508 to 508),
            "guide_lateral_band_walk" to (508 to 508),
        )

    private fun verify(art: TechniqueIllustrations.Artwork) {
        assertTrue(art.description().isNotBlank())
        assertTrue(art.phases().isNotEmpty() && art.phases().all { it.isNotBlank() })
        val id = context.resources.getIdentifier(art.asset(), "drawable", context.packageName)
        assertTrue("Missing packaged artwork ${art.asset()}", id != 0)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.resources.openRawResource(id).use { BitmapFactory.decodeStream(it, null, options) }
        val expected = contactSheetBounds[art.asset()] ?: (1200 to 800)
        assertEquals("Width of ${art.asset()}", expected.first, options.outWidth)
        assertEquals("Height of ${art.asset()}", expected.second, options.outHeight)
    }

    fun testEveryBuiltInRoutineMovementHasReadableBundledArtwork() {
        for (movement in RunningRoutines.MOVEMENTS) {
            val art = TechniqueIllustrations.forRoutine(movement.id())
            assertNotNull("Missing routine movement ${movement.id()}", art)
            verify(art!!)
            assertTrue(requireNotNull(movement.instructions()).isNotBlank())
            assertTrue(requireNotNull(movement.cue()).isNotBlank())
            assertTrue(requireNotNull(movement.mistake()).isNotBlank())
        }
    }

    fun testRegisteredCanonicalAssetsResolveAndCustomUnknownDoesNotInheritAPose() {
        var registered = 0
        for (definition in ExerciseCatalog.definitions()) {
            val id = definition.exercise().id()
            val art = TechniqueIllustrations.forExercise(id)
            assertNotNull("Every canonical exercise needs bundled artwork: $id", art)
            registered++
            verify(art!!)
            val guide = TechniqueGuide.forExercise(id)
            assertNotNull("Artwork needs movement instructions: $id", guide)
            assertTrue(
                requireNotNull(guide!!.setup()).isNotBlank() &&
                    requireNotNull(guide.movement()).isNotBlank()
            )
        }
        assertEquals(ExerciseCatalog.definitions().size, registered)
        assertNull(TechniqueIllustrations.forExercise("unknown-custom-exercise"))
    }
}
