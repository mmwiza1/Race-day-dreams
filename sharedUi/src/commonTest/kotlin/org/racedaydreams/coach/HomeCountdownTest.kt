package org.racedaydreams.coach

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** B-03: Home's countdown card agrees with the race-day screen and never shows negative days. */
class HomeCountdownTest {
    @Test
    fun beforeTheRaceTheDiscCountsDownGrammatically() {
        assertEquals(CountdownBadge("84", "days"), countdownBadge(84))
        assertEquals(CountdownBadge("1", "day"), countdownBadge(1))
        assertEquals("Race preparation", homeRaceCountdownLabel(84))
        assertEquals("Race preparation", homeRaceCountdownLabel(1))
    }

    @Test
    fun raceDayReadsTodayNotZeroDays() {
        assertEquals("Today", countdownBadge(0).headline)
        // The same wording as the race-day screen's hero.
        assertEquals(raceCountdown(0), countdownBadge(0).headline)
        assertFalse(countdownBadge(0).headline.contains("0"))
        assertEquals("Race day", homeRaceCountdownLabel(0))
    }

    @Test
    fun afterTheRaceTheCardShowsElapsedDaysAndNeverANegativeNumber() {
        assertEquals(CountdownBadge("1", "day ago"), countdownBadge(-1))
        assertEquals(CountdownBadge("3", "days ago"), countdownBadge(-3))
        for (days in -400L..-1L) {
            val badge = countdownBadge(days)
            assertFalse(badge.headline.startsWith("-"), "negative headline for $days")
            assertTrue(badge.unit.endsWith("ago"))
            assertFalse(homeRaceCountdownLabel(days).contains("preparation"))
        }
        assertEquals("Race complete · add your result", homeRaceCountdownLabel(-3))
    }
}
