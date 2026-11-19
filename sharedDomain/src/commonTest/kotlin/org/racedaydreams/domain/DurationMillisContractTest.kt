package org.racedaydreams.domain

import kotlin.random.Random
import kotlin.test.*
import org.racedaydreams.domain.time.*

class DurationMillisContractTest {
    @Test
    fun fractionalMillisecondsTruncateTowardsZeroOnBothSides() {
        for ((nanos, expected) in
            listOf(
                -1000001L to -1L,
                -1000000L to -1L,
                -999999L to 0L,
                -1L to 0L,
                0L to 0L,
                1L to 0L,
                999999L to 0L,
                1000000L to 1L,
                1000001L to 1L,
            )) {
            assertEquals(expected, Durations.ofNanos(nanos).toMillis())
        }
    }

    @Test
    fun entireLongMillisRangeRoundTripsIncludingNegativeCancellationBoundary() {
        val random = Random(330)
        val values =
            listOf(
                Long.MIN_VALUE,
                Long.MIN_VALUE + 1,
                -1L,
                0L,
                1L,
                Long.MAX_VALUE - 1,
                Long.MAX_VALUE,
            ) + List(1000) { random.nextLong() }
        for (value in values) assertEquals(value, Durations.ofMillis(value).toMillis())
    }

    @Test
    fun overflowIsRejectedRatherThanClampedOrWrapped() {
        for (duration in
            listOf(
                Durations.ofSeconds(Long.MAX_VALUE),
                Durations.ofSeconds(Long.MIN_VALUE),
                Durations.ofSeconds(9223372036854775L, 808000000),
                Durations.ofSeconds(-9223372036854776L, 191000000),
            )) {
            assertFailsWith<ArithmeticException> { duration.toMillis() }
        }
        assertEquals(Long.MAX_VALUE, Durations.ofSeconds(9223372036854775L, 807999999).toMillis())
        assertEquals(Long.MIN_VALUE, Durations.ofSeconds(-9223372036854776L, 191999999).toMillis())
    }

    @Test
    fun normalizedSecondsAndNanosRetainOriginalSubMillisecondSemantics() {
        assertEquals(0, Durations.ofSeconds(0, -1).toMillis())
        assertEquals(-1, Durations.ofSeconds(0, -1000001).toMillis())
        assertEquals(0, Durations.ofSeconds(1, -1000000001).toMillis())
        assertEquals(0, Durations.ofSeconds(-1, 1000000001).toMillis())
        assertEquals(1000, Durations.ofSeconds(0, 1000000001).toMillis())
    }
}
