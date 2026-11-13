package org.racedaydreams.domain

import java.time.Duration
import java.util.Random
import kotlin.test.*
import org.racedaydreams.domain.time.DurationParts

class DurationSubtractionCompatibilityTest {
    @Test
    fun checkedSubtractionRetainsJdkNanosecondsAndOverflowIncludingMinimumSeconds() {
        val random = Random(218404L)
        val edges =
            listOf(Long.MIN_VALUE, Long.MIN_VALUE + 1, -1, 0, 1, Long.MAX_VALUE - 1, Long.MAX_VALUE)
        val pairs =
            edges.flatMap { a -> edges.map { b -> a to b } } +
                List(20000) { random.nextLong() to random.nextLong() }
        for ((a, b) in pairs) for ((an, bn) in listOf(0 to 0, 1 to 999999999, 999999999 to 1)) {
            val expected = runCatching {
                Duration.ofSeconds(a, an.toLong()).minus(Duration.ofSeconds(b, bn.toLong()))
            }
            val actual = runCatching { DurationParts(a, an).minus(DurationParts(b, bn)) }
            assertEquals(expected.isSuccess, actual.isSuccess, "$a/$an - $b/$bn")
            if (expected.isSuccess) {
                val old = expected.getOrThrow()
                val now = actual.getOrThrow()
                assertEquals(old.seconds, now.seconds)
                assertEquals(old.nano, now.nanos)
                assertEquals(old.toString(), now.text())
            } else assertIs<ArithmeticException>(actual.exceptionOrNull())
        }
    }
}
