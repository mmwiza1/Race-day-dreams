package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.presentation.StrengthPrescriptionText as Text
import org.racedaydreams.domain.strength.*

class StrengthPrescriptionTextContractTest {
    private val bench = ExerciseCatalog.definition("bench-press")!!.exercise()
    private val target =
        GymWorkoutDefinition.Target(
            2,
            4,
            6,
            null,
            null,
            GymWorkoutDefinition.EffortRange(GymWorkoutDefinition.EffortScale.RPE, 5.0, 7.0),
            180,
            80.0,
        )
    private val slot = GymWorkoutDefinition.Slot("bench", bench, target, null)

    @Test
    fun instructionsRetainTargetsRestAndActualIntendedEffort() {
        assertEquals(
            listOf(
                "Complete 2 sets of 4–6 repetitions.",
                "Rest for 3 minutes between sets.",
                "Aim for moderate to challenging perceived effort. Use how hard the set feels to guide your effort.",
            ),
            Text.instructions(slot),
        )
        val rir =
            target.copy(
                effort =
                    GymWorkoutDefinition.EffortRange(GymWorkoutDefinition.EffortScale.RIR, 1.5, 3.5)
            )
        assertTrue(
            Text.instructions(slot.copy(target = rir))
                .last()
                .contains("1.5–3.5 repetitions left before failure")
        )
        assertTrue(Text.effortDetails(rir.effort()).startsWith("RIR (repetitions in reserve)"))
        assertTrue(
            Text.effortDetails(target.effort()).startsWith("RPE (rating of perceived effort)")
        )
        assertFalse(
            Text.instructions(slot.copy(target = target.copy(effort = null))).any {
                it.contains("effort")
            }
        )
    }

    @Test
    fun referenceChangesAreNotInstructionsOrRecordedLifts() {
        val lines = Text.weight(slot, target.copy(referenceLoadKg = 82.0))
        assertEquals("Your current weight reference is 80 kg.", lines[0])
        assertEquals(
            "The suggested new weight reference is 82 kg. That is 2 kg more than your current reference.",
            lines[1],
        )
        assertTrue(lines.last().contains("not a weight you must lift or a recorded result"))
        assertTrue(
            Text.weight(slot, target)
                .contains("Your weight reference is unchanged in this suggestion.")
        )
        assertEquals(
            "Your current target has no weight reference.",
            Text.weight(slot.copy(target = target.withoutLoadReference()), target)[0],
        )
        assertTrue(Text.weight(slot, target.copy(referenceLoadKg = 75.0))[1].contains("5 kg less"))
    }

    @Test
    fun decimalWeightDifferenceDoesNotInventBinaryFloatingPointDigits() {
        val before = slot.copy(target = target.copy(referenceLoadKg = 0.1))
        assertTrue(
            Text.weight(before, target.copy(referenceLoadKg = 0.3))[1].contains("0.2 kg more")
        )
        assertTrue(
            Text.weight(before, target.copy(referenceLoadKg = 0.05))[1].contains("0.05 kg less")
        )
        for ((input, expected) in
            listOf(
                0.0 to "0",
                -0.0 to "0",
                1.5 to "1.5",
                82.50 to "82.5",
                0.000001 to "0.000001",
                100000.0 to "100000",
            )) assertEquals(expected, Text.number(input))
        for (bad in
            listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) assertFailsWith<
            NumberFormatException
        > {
            Text.number(bad)
        }
    }

    @Test
    fun onlyGenuineDisplayDifferencesAreHighlighted() {
        val rows = Text.comparisons(slot, target.copy(referenceLoadKg = 82.0))
        assertEquals(listOf("weight-reference"), rows.filter { it.changed() }.map { it.key() })
        assertEquals(
            listOf("sets", "repetitions", "effort", "rest"),
            rows.filter { !it.changed() }.map { it.key() },
        )
        assertEquals("80 kg", rows.last().currentValue())
        assertEquals("82 kg", rows.last().proposedValue())
        assertTrue(Text.comparisons(slot, null).none { it.changed() })
        assertEquals(
            "Not set",
            Text.comparisons(slot.copy(target = null), target).first().currentValue(),
        )
        assertTrue(Text.comparisons(slot.copy(target = null), null).isEmpty())
    }

    @Test
    fun nonRepetitionAndBodyweightTasksKeepTheirActualModality() {
        val plank = ExerciseCatalog.definition("plank")!!.exercise()
        val timed = GymWorkoutDefinition.Target(1, null, null, 61, null, null, 0, null)
        val timedSlot = GymWorkoutDefinition.Slot("hold", plank, timed, null)
        assertEquals(
            "Complete 1 set of 1 minute and 1 second.",
            Text.instructions(timedSlot).first(),
        )
        assertTrue(Text.instructions(timedSlot).contains("No rest is prescribed between sets."))
        assertEquals(
            listOf("No external weight is prescribed for this exercise."),
            Text.weight(timedSlot, null),
        )
        val carry = ExerciseCatalog.definition("farmers-carry")!!.exercise()
        val distance =
            GymWorkoutDefinition.Slot(
                "carry",
                carry,
                GymWorkoutDefinition.Target(2, null, null, null, 20.5, null, null, 40.0),
                null,
            )
        assertEquals("Complete 2 sets of 20.5 metres.", Text.instructions(distance).single())
        assertTrue(
            Text.instructions(timedSlot.copy(target = null))
                .single()
                .contains("No set or effort target is prescribed")
        )
    }

    @Test
    fun displayedFiniteValuesRoundTripWithoutPrecisionCapsOrScientificNotation() {
        val values =
            mutableListOf(
                Double.MIN_VALUE,
                Double.MAX_VALUE,
                -Double.MIN_VALUE,
                -Double.MAX_VALUE,
                1e23,
                0.000000000001,
                0.1,
                1500.0,
            )
        var bits = 0x123456789abcdefL
        repeat(1000) {
            bits = bits xor (bits shl 13)
            bits = bits xor (bits ushr 7)
            bits = bits xor (bits shl 17)
            val value = Double.fromBits(bits)
            if (value.isFinite() && value != 0.0) values.add(value)
        }
        for (value in values) {
            val text = Text.number(value)
            assertFalse(text.contains('e', ignoreCase = true), text)
            assertEquals(value.toBits(), text.toDouble().toBits(), text)
        }
    }
}
