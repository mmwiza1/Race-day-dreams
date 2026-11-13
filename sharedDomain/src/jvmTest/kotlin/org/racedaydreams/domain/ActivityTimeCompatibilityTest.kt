package org.racedaydreams.domain

import java.time.Duration
import java.util.Random
import kotlin.test.*
import org.racedaydreams.domain.activity.ActivityChecks
import org.racedaydreams.domain.activity.roundedNanos

class ActivityTimeCompatibilityTest {
    @Test
    fun fractionalSecondConversionRetainsOriginalJavaRoundingAndSaturation() {
        val random = Random(147770L)
        val values =
            listOf(
                Double.NaN,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.MIN_VALUE,
                -Double.MIN_VALUE,
                -0.5,
                0.5,
                Math.nextDown(0.5),
                Math.nextUp(-0.5),
                Math.nextDown(-0.5),
                Long.MIN_VALUE.toDouble(),
                Long.MAX_VALUE.toDouble(),
            ) + List(20000) { Double.fromBits(random.nextLong()) }
        for (value in values) {
            assertEquals(Math.round(value), roundedNanos(value), value.toString())
            assertEquals(
                Duration.ofNanos(Math.round(value * 1000000000.0)),
                ActivityChecks.duration(value),
                value.toString(),
            )
        }
    }
}
