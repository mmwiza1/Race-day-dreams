package org.racedaydreams.domain

import java.time.Duration
import kotlin.random.Random
import kotlin.test.*
import org.racedaydreams.domain.time.DurationParts

class DurationMillisCompatibilityTest {
    @Test
    fun nativePartsConversionMatchesOriginalJdkAcrossValuesAndOverflowBoundaries() {
        val random = Random(771)
        repeat(8000) { n ->
            val seconds =
                when (n % 4) {
                    0 -> random.nextLong()
                    1 -> random.nextLong() / 1000
                    2 -> random.nextLong(-9223372036854777L, 9223372036854777L)
                    else -> random.nextInt(-10000, 10000).toLong()
                }
            val nanos = random.nextInt(1000000000)
            val original = runCatching { Duration.ofSeconds(seconds, nanos.toLong()).toMillis() }
            val shared = runCatching { DurationParts(seconds, nanos).toMillis() }
            if (original.isSuccess)
                assertEquals(original.getOrThrow(), shared.getOrThrow(), "$seconds:$nanos")
            else assertTrue(shared.exceptionOrNull() is ArithmeticException, "$seconds:$nanos")
        }
    }
}
