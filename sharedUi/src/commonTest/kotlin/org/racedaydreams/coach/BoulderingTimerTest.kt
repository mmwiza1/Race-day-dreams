package org.racedaydreams.coach

import kotlin.test.Test
import kotlin.test.assertEquals
import org.racedaydreams.coach.climbing.stoppedClimbingSeconds
import org.racedaydreams.domain.climbing.ClimbingSession
import org.racedaydreams.domain.time.Instants
import org.racedaydreams.domain.time.Offsets

/** B-R6: a live bouldering timer left running past 24 hours can still be stopped and saved. */
class BoulderingTimerTest {
    @Test
    fun aStoppedTimerAlwaysFitsTheSessionLimits() {
        assertEquals(60L, stoppedClimbingSeconds(5))
        assertEquals(5_400L, stoppedClimbingSeconds(5_400))
        assertEquals(
            ClimbingSession.MAX_DURATION,
            stoppedClimbingSeconds(ClimbingSession.MAX_DURATION),
        )
        assertEquals(ClimbingSession.MAX_DURATION, stoppedClimbingSeconds(30 * 3_600L))
        // The clamped time is a valid session duration, so the session can be saved.
        val clamped = stoppedClimbingSeconds(3 * 24 * 3_600L)
        val session =
            ClimbingSession(
                "s",
                Instants.ofEpochSecond(0),
                Offsets.ofTotalSeconds(0),
                clamped,
                "",
                "",
                emptyList(),
            )
        assertEquals(ClimbingSession.MAX_DURATION, session.durationSeconds)
    }
}
