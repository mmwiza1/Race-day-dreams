package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.body.MassUnit
import org.racedaydreams.domain.presentation.MassText
import org.racedaydreams.domain.presentation.StrengthPrescriptionText
import org.racedaydreams.domain.strength.*

/** B-05 and B-R8: gym loads are typed and shown in the user's unit; storage stays in kg. */
class MassTextTest {
    @Test
    fun poundEntryRoundTripsWithoutDrift() {
        for (typed in listOf("225", "135", "45", "2.5", "102.5", "315", "0.5", "1000")) {
            val stored = MassText.parse(typed, MassUnit.LB)
            // Stored at full precision: no kilogram rounding before saving.
            assertEquals(typed.toDouble() * 0.45359237, stored, 1e-12)
            assertEquals(typed.removeSuffix(".0"), MassText.entry(stored, MassUnit.LB))
            // Saving what is shown again stores the same value: repeated edits never drift.
            val again = MassText.parse(MassText.entry(stored, MassUnit.LB), MassUnit.LB)
            assertEquals(stored, again, 1e-9)
        }
        assertEquals("225 lb", MassText.label(MassText.parse("225", MassUnit.LB), MassUnit.LB))
    }

    @Test
    fun kilogramEntryIsUnchanged() {
        assertEquals(102.5, MassText.parse(" 102.5 ", MassUnit.KG))
        assertEquals("102.5 kg", MassText.label(102.5, MassUnit.KG))
        assertEquals("100 kg", MassText.label(100.0, MassUnit.KG))
    }

    @Test
    fun kilogramValuesAreRoundedToHundredthsOnlyForDisplay() {
        assertEquals("225.97 lb", MassText.label(102.5, MassUnit.LB))
        assertEquals(225.97, MassText.value(102.5, MassUnit.LB))
    }

    @Test
    fun invalidEntryNamesTheField() {
        assertEquals(
            "Enter load as a number.",
            assertFailsWith<IllegalArgumentException> { MassText.parse("abc", MassUnit.LB) }.message,
        )
        assertFailsWith<IllegalArgumentException> { MassText.parse("-5", MassUnit.LB) }
        assertFailsWith<IllegalArgumentException> { MassText.parse("NaN", MassUnit.KG) }
    }

    @Test
    fun warmupsRoundDownToALoadablePlateStepInTheUsersUnit() {
        // 0.4 × 62.5 kg = 25 kg; 0.65 × 62.5 kg = 40.625 kg, previously suggested as 40.5 kg.
        assertEquals(25.0, MassText.warmup(25.0, MassUnit.KG))
        assertEquals(40.0, MassText.warmup(40.5, MassUnit.KG))
        assertEquals(2.5, MassText.warmup(4.0, MassUnit.KG))
        // 40.5 kg = 89.29 lb -> 85 lb; never below one 5 lb step.
        assertEquals(85.0, MassText.warmup(40.5, MassUnit.LB))
        assertEquals(5.0, MassText.warmup(1.0, MassUnit.LB))
        // Exact steps are not pushed down by floating-point error.
        assertEquals(45.0, MassText.warmup(MassText.parse("45", MassUnit.LB), MassUnit.LB))
        for (kg in listOf(4.0, 10.0, 33.3, 61.25, 140.0)) for (unit in MassUnit.entries) {
            val shown = MassText.warmup(kg, unit)
            assertTrue(shown > 0)
            assertEquals(0.0, shown % MassText.plateStep(unit), 1e-9)
        }
    }

    @Test
    fun recordCategoriesShowTheirLoadInTheUsersUnitWithoutChangingTheKey() {
        val key = "Duration seconds at 20.0 kg"
        assertEquals(key, MassText.category(key, MassUnit.KG))
        assertEquals("Duration seconds at 44.09 lb", MassText.category(key, MassUnit.LB))
        assertEquals("Load × 5", MassText.category("Load × 5", MassUnit.LB))
    }

    @Test
    fun progressionReferenceTextUsesTheUsersUnit() {
        val bench = ExerciseCatalog.definition("bench-press")!!.exercise()
        val current = GymWorkoutDefinition.Target(2, 4, 6, null, null, null, 180, 100.0)
        val next = current.copy(referenceLoadKg = 102.5)
        val slot = GymWorkoutDefinition.Slot("bench", bench, current, null)
        val kg = StrengthPrescriptionText.weight(slot, next).joinToString(" ")
        assertTrue(kg.contains("100 kg") && kg.contains("102.5 kg") && kg.contains("2.5 kg more"))
        val lb = StrengthPrescriptionText.weight(slot, next, MassUnit.LB).joinToString(" ")
        assertTrue(lb.contains("220.46 lb"), lb)
        assertTrue(lb.contains("225.97 lb"), lb)
        assertTrue(lb.contains("5.51 lb more"), lb)
        assertFalse(lb.contains(" kg"), lb)
        val rows = StrengthPrescriptionText.comparisons(slot, next, MassUnit.LB)
        val reference = rows.first { it.key() == "weight-reference" }
        assertEquals("220.46 lb", reference.currentValue())
        assertEquals("225.97 lb", reference.proposedValue())
    }
}
