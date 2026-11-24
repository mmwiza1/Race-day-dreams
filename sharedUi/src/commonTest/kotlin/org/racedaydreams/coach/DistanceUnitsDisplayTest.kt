package org.racedaydreams.coach

import kotlin.math.abs
import kotlin.math.round
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.racedaydreams.domain.race.RaceStrategyEngine

/** B-04 and B-R3: km storage is shown in the user's distance unit on Home, review and race day. */
class DistanceUnitsDisplayTest {
    private val oneDecimal: (Double) -> String = { (round(it * 10) / 10).toString() }
    private val minutes: (Double) -> String = {
        val s = round(it).toLong()
        "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
    }

    @Test
    fun conversionKeepsStorageInKilometres() {
        assertEquals(10.0, DisplayUnits.distance(10.0, false))
        assertEquals(6.2137, DisplayUnits.distance(10.0, true), 1e-4)
        assertEquals(300.0, DisplayUnits.pace(300.0, false))
        assertEquals(482.8032, DisplayUnits.pace(300.0, true), 1e-9)
        assertEquals("km", DisplayUnits.distanceUnit(false))
        assertEquals("mi", DisplayUnits.distanceUnit(true))
    }

    @Test
    fun weeklyReviewHeadlineUsesTheChosenUnit() {
        assertEquals(
            "32.0 km · longest 16.1 km",
            weeklyDistanceHeadline(32.0, 16.1, false, oneDecimal),
        )
        assertEquals(
            "19.9 mi · longest 10.0 mi",
            weeklyDistanceHeadline(32.0, 16.1, true, oneDecimal),
        )
        assertEquals("19.9 mi", weeklyDistanceHeadline(32.0, null, true, oneDecimal))
    }

    @Test
    fun raceStagesUseTheChosenUnitForDistanceAndPace() {
        assertEquals("0.0–1.5 km", stageDistanceLabel(0.0, 1500.0, false, oneDecimal))
        assertEquals("1.5–8.5 km", stageDistanceLabel(1500.0, 8500.0, false, oneDecimal))
        assertEquals("0.9–5.3 mi", stageDistanceLabel(1500.0, 8500.0, true, oneDecimal))
        assertEquals("6:00–6:12/km", stagePaceLabel(360.0, 372.0, false, minutes))
        assertEquals("9:39–9:59/mi", stagePaceLabel(360.0, 372.0, true, minutes))
    }

    @Test
    fun closingPaceMakesTheStrategyAddUpToTheTarget() {
        val stages = RaceStrategyEngine.create(10000.0, 3600, false)
        val closing = requireNotNull(closingPaceForTarget(stages, 3600))
        // Running the middle of each earlier range and then the closing pace finishes on target.
        val used =
            stages.dropLast(1).sumOf {
                (it.fastPace()!! + it.slowPace()!!) / 2 * (it.to() - it.from()) / 1000
            }
        val last = stages.last()
        assertEquals(3600.0, used + closing * (last.to() - last.from()) / 1000, 1e-6)
        // The conservative opening means the finish is a little quicker than the 6:00/km average,
        // which the screen now states instead of implying the middle ranges are enough.
        assertTrue(closing < 360.0)
        assertTrue(abs(closing - 360.0) / 360.0 < 0.05)
    }

    @Test
    fun effortOnlyStrategiesShowNoClosingPace() {
        assertNull(closingPaceForTarget(RaceStrategyEngine.create(5000.0, null, false), null))
        assertNull(closingPaceForTarget(RaceStrategyEngine.create(5000.0, 1800, true), 1800))
    }
}
