package org.racedaydreams.storage

import kotlin.test.*
import org.racedaydreams.domain.time.Durations

class StoredDurationTest {
    @Test
    fun exactOriginalDurationRangeAndFractionalSecondsRoundTrip() {
        val seconds =
            listOf(
                Long.MIN_VALUE,
                Long.MIN_VALUE + 1,
                -86401L,
                -3600L,
                -60L,
                -1L,
                0L,
                1L,
                60L,
                3600L,
                86401L,
                Long.MAX_VALUE - 1,
                Long.MAX_VALUE,
            )
        for (second in seconds) for (nanos in listOf(0L, 1L, 123456789L, 999999999L)) {
            val original = Durations.ofSeconds(second, nanos)
            assertEquals(original, StoredDuration.parse(original.toString()))
        }
        val random = kotlin.random.Random(2911)
        repeat(2048) {
            val original = Durations.ofSeconds(random.nextLong(), random.nextLong(1000000000L))
            assertEquals(original, StoredDuration.parse(original.toString()))
        }
    }

    @Test
    fun incompleteNonCanonicalAndOverflowingPayloadTimesAreRejected() {
        for (bad in
            listOf(
                "PT",
                "P1D",
                "PT1.0S",
                "PT60S",
                "PT0H",
                "PT1H0M",
                "PT1.0000000001S",
                "PT-0S",
                "PT1S trailing",
                "PT9223372036854775808S",
                "PT2562047788015216H",
            )) {
            assertFails(bad) { StoredDuration.parse(bad) }
        }
    }
}
