package org.racedaydreams.coach

import android.graphics.BitmapFactory
import android.test.AndroidTestCase
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.support.RunningRoutines

/** Catalogue-wide packaged-resource integrity; design fidelity is reviewed separately. */
class GuidanceArtworkTest:AndroidTestCase() {
    private fun verify(art:TechniqueIllustrations.Artwork) {
        assertTrue(art.description().isNotBlank());assertTrue(art.phases().isNotEmpty()&&art.phases().all{it.isNotBlank()})
        val id=context.resources.getIdentifier(art.asset(),"drawable",context.packageName)
        assertTrue("Missing packaged artwork ${art.asset()}",id!=0)
        val options=BitmapFactory.Options().apply{inJustDecodeBounds=true}
        context.resources.openRawResource(id).use{BitmapFactory.decodeStream(it,null,options)}
        assertEquals(1200,options.outWidth);assertEquals(800,options.outHeight)
    }
    fun testEveryBuiltInRoutineMovementHasReadableBundledArtwork() {
        for(movement in RunningRoutines.MOVEMENTS) {
            val art=TechniqueIllustrations.forRoutine(movement.id())
            assertNotNull("Missing routine movement ${movement.id()}",art);verify(art!!)
            assertTrue(movement.instructions().isNotBlank());assertTrue(movement.cue().isNotBlank());assertTrue(movement.mistake().isNotBlank())
        }
    }
    fun testRegisteredCanonicalAssetsResolveAndCustomUnknownDoesNotInheritAPose() {
        var registered=0
        for(definition in ExerciseCatalog.definitions()) {
            val id=definition.exercise().id();val art=TechniqueIllustrations.forExercise(id)
            assertNotNull("Every canonical exercise needs bundled artwork: $id",art)
            registered++;verify(art!!);val guide=TechniqueGuide.forExercise(id)
            assertNotNull("Artwork needs movement instructions: $id",guide)
            assertTrue(guide!!.setup().isNotBlank()&&guide.movement().isNotBlank())
        }
        assertEquals(ExerciseCatalog.definitions().size,registered);assertNull(TechniqueIllustrations.forExercise("unknown-custom-exercise"))
    }
}
