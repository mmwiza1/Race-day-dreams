package org.racedaydreams.coach

import kotlin.test.Test
import kotlin.test.assertEquals
import org.racedaydreams.coach.strength.GymSlotInput
import org.racedaydreams.coach.strength.gymLoadKilograms
import org.racedaydreams.coach.strength.gymSlotFromInput
import org.racedaydreams.coach.strength.referenceKilogramsText
import org.racedaydreams.domain.body.MassUnit
import org.racedaydreams.domain.presentation.MassText
import org.racedaydreams.domain.strength.ExerciseCatalog

/** B-05: gym set and reference loads are entered in the user's unit and stored in kg. */
class GymMassEntryTest {
    @Test
    fun aPoundSetIsStoredInKilogramsAndShownAgainAsTyped() {
        val stored = gymLoadKilograms("225", MassUnit.LB)
        assertEquals(102.05828325, stored, 1e-9)
        assertEquals("225", MassText.entry(stored, MassUnit.LB))
        assertEquals(100.0, gymLoadKilograms("100", MassUnit.KG))
    }

    @Test
    fun aPoundReferenceLoadReachesTheTargetInKilograms() {
        val bench = ExerciseCatalog.definition("bench-press")!!.exercise()
        val input =
            GymSlotInput(
                minimumReps = "5",
                maximumReps = "5",
                referenceKilograms = referenceKilogramsText("225", MassUnit.LB),
            )
        val slot = gymSlotFromInput(null, bench, input) { "slot" }
        val kg = slot.target()!!.referenceLoadKg()!!
        assertEquals(102.05828325, kg, 1e-9)
        assertEquals("225", MassText.entry(kg, MassUnit.LB))
        // Kilograms, blanks and invalid text pass through untouched for the existing validation.
        assertEquals("80", referenceKilogramsText("80", MassUnit.KG))
        assertEquals("", referenceKilogramsText("", MassUnit.LB))
        assertEquals("abc", referenceKilogramsText("abc", MassUnit.LB))
    }
}
