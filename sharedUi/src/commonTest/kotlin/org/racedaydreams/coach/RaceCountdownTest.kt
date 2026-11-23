package org.racedaydreams.coach

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RaceCountdownTest {
    @Test
    fun countdownIsGrammaticalForEveryDistanceFromTheRace() {
        assertEquals("84 days to race day", raceCountdown(84))
        assertEquals("1 day to race day", raceCountdown(1))
        assertEquals("Race date was 1 day ago", raceCountdown(-1))
        assertEquals("Race date was 3 days ago", raceCountdown(-3))
    }

    @Test
    fun raceDayItselfDoesNotRepeatTheHeroEyebrow() {
        // The hero's eyebrow is "Race day"; the countdown must not say it a second time.
        assertEquals("Today", raceCountdown(0))
        assertFalse(raceCountdown(0).contains("Race day", ignoreCase = true))
    }
}
