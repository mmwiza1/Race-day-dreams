package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.activity.PerformanceDistance
import org.racedaydreams.domain.coaching.Immutable

class ImmutableAndPerformanceTest {
    @Test
    fun modifyingInputsCannotRewritePublishedListOrSetEvidence() {
        val source = mutableListOf("first", "second", "first")
        val list = Immutable.copyList(source)
        val set = Immutable.copySet(source)
        source.clear()
        assertEquals(listOf("first", "second", "first"), list)
        assertEquals(setOf("first", "second"), set)
    }

    @Test
    fun mutationAndIteratorRemovalRemainUnavailable() {
        val list = Immutable.list(1, 2, 3)
        val set = Immutable.set(1, 2, 3)
        assertFails { (list as MutableList<Int>).add(4) }
        assertFails { (list.subList(0, 1) as MutableList<Int>).clear() }
        assertFails { (set as MutableSet<Int>).clear() }
        assertFails { (set.iterator() as MutableIterator<Int>).remove() }
        assertEquals(listOf(1, 2, 3), list)
        assertEquals(setOf(1, 2, 3), set)
    }

    @Test
    fun nullEntriesAreRejectedRatherThanBecomingMissingEvidence() {
        assertFailsWith<NullPointerException> { Immutable.list<String?>(null) }
        assertFailsWith<NullPointerException> { Immutable.set<String?>(null) }
    }

    @Test
    fun measuredPaceKeepsUnitsFractionalTimesAndMinuteCarries() {
        assertEquals(
            "8:00/mi",
            PacePolicy.performancePace(480.0, PerformanceDistance.ONE_MILE, UnitSystem.IMPERIAL),
        )
        // Original Java multiplication/division gives just below the half-second here.
        // Retain that measured behavior instead of algebraically cancelling the unit ratio.
        assertEquals(
            "7:59/mi",
            PacePolicy.performancePace(479.5, PerformanceDistance.ONE_MILE, UnitSystem.IMPERIAL),
        )
        assertEquals(
            "8:00/mi",
            PacePolicy.performancePace(479.6, PerformanceDistance.ONE_MILE, UnitSystem.IMPERIAL),
        )
        assertEquals(
            "5:41/km",
            PacePolicy.performancePace(7199.0, PerformanceDistance.HALF_MARATHON, UnitSystem.METRIC),
        )
        assertEquals(
            "153722867280912930:07/km",
            PacePolicy.performancePace(
                1.0,
                PerformanceDistance("tiny", Double.MIN_VALUE, "Custom"),
                UnitSystem.METRIC,
            ),
        )
    }

    @Test
    fun justBelowHalfASecondDoesNotRoundUpBecauseOfAnIntermediateAddition() {
        val slightlyLonger =
            PerformanceDistance("next", Double.fromBits(1000.0.toBits() + 1), "Custom")
        assertEquals("0:00/km", PacePolicy.performancePace(0.5, slightlyLonger, UnitSystem.METRIC))
    }

    @Test
    fun absentOrInvalidTimeNeverFabricatesAPace() {
        for (value in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> {
                PacePolicy.performancePace(value, PerformanceDistance.FIVE_K, UnitSystem.METRIC)
            }
        }
    }
}
