package org.racedaydreams.domain

import java.util.Locale
import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import org.racedaydreams.domain.activity.PerformanceDistance

/** Differential reference against the original JVM primitive, not another production engine. */
class PerformancePaceCompatibilityTest {
    @Test
    fun recordedPaceMatchesTheRetainedJavaArithmeticAtRoundingAndOverflowEdges() {
        val random = Random(492180L)
        val seconds =
            listOf(0.5, 479.5, 479.6, 4503599627370497.0, Long.MAX_VALUE.toDouble()) +
                List(1000) {
                    random.nextInt(1000000) + random.nextInt(1000000000) / 1_000_000_000.0
                }
        val courses =
            PerformanceDistance.CORE +
                listOf(
                    PerformanceDistance("tiny", Double.MIN_VALUE, "Custom"),
                    PerformanceDistance("large", Double.MAX_VALUE, "Custom"),
                    PerformanceDistance("next", Math.nextUp(1000.0), "Custom"),
                )
        for (time in seconds) for (course in courses) for (units in UnitSystem.entries) {
            val unitMetres = if (units == UnitSystem.IMPERIAL) 1609.344 else 1000.0
            val rounded = Math.round(time * unitMetres / course.metres())
            val expected =
                String.format(
                    Locale.ROOT,
                    "%d:%02d/%s",
                    rounded / 60,
                    rounded % 60,
                    if (units == UnitSystem.IMPERIAL) "mi" else "km",
                )
            assertEquals(
                expected,
                PacePolicy.performancePace(time, course, units),
                "$time/${course.code()}/$units",
            )
        }
    }
}
