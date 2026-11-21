package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.time.Durations

class RaceNameDistanceCheckTest {
    @Test
    fun aHalfMarathonNameWithTenKSelectedIsFlaggedButNeverChanged() {
        assertEquals(
            RaceDistance.HALF_MARATHON,
            RaceNameDistanceCheck.conflict("Hackney Half", RaceDistance.TEN_K),
        )
        assertNull(RaceNameDistanceCheck.conflict("Hackney Half", RaceDistance.HALF_MARATHON))
    }

    @Test
    fun namesWithoutAClearDistanceProduceNoWarning() {
        for (name in
            listOf("", "Hackney", "Great North Run", "Spring Classic", "Royal Parks")) assertNull(
            RaceNameDistanceCheck.conflict(name, RaceDistance.TEN_K),
            name,
        )
        // A name mentioning two different distances is ambiguous, so no guess is made.
        assertNull(RaceNameDistanceCheck.named("City 5K and 10K"))
    }

    @Test
    fun commonDistanceWordsAreRecognised() {
        assertEquals(RaceDistance.MARATHON, RaceNameDistanceCheck.named("London Marathon"))
        assertEquals(RaceDistance.HALF_MARATHON, RaceNameDistanceCheck.named("Bath Half Marathon"))
        assertEquals(RaceDistance.HALF_MARATHON, RaceNameDistanceCheck.named("Riverside 21K"))
        assertEquals(RaceDistance.TEN_K, RaceNameDistanceCheck.named("Vitality 10K"))
        assertEquals(RaceDistance.FIVE_K, RaceNameDistanceCheck.named("Hackney Marshes parkrun"))
        // General fitness has no race distance to contradict.
        assertNull(RaceNameDistanceCheck.conflict("Hackney Half", RaceDistance.GENERAL_FITNESS))
    }

    @Test
    fun targetPaceAlwaysUsesTheSelectedDistance() {
        val target = Durations.ofSeconds(1 * 3600 + 55 * 60)
        assertEquals(
            "5:27/km",
            PaceCalculator.targetPace(target, RaceDistance.HALF_MARATHON, UnitSystem.METRIC),
        )
        assertEquals(
            "11:30/km",
            PaceCalculator.targetPace(target, RaceDistance.TEN_K, UnitSystem.METRIC),
        )
    }
}
