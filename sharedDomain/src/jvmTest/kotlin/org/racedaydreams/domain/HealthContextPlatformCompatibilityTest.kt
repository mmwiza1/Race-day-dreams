package org.racedaydreams.domain

import java.util.Locale
import kotlin.test.*
import org.racedaydreams.domain.presentation.*

class HealthContextPlatformCompatibilityTest {
    @Test
    fun nativeWholeNumberAndGroupingTextMatchTheExistingUkFormatter() {
        for (i in -4000..4000) {
            val value = i / 10.0
            assertEquals(
                String.format(Locale.UK, "%.0f", value),
                healthWholeDecimal(value),
                value.toString(),
            )
        }
        for (value in
            listOf(
                -0.0,
                Double.NaN,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.MAX_VALUE,
                Double.MIN_VALUE,
                0.49999999999999994,
                -0.49999999999999994,
                1e100,
                1e-100,
            )) assertEquals(
            String.format(Locale.UK, "%.0f", value),
            healthWholeDecimal(value),
            value.toString(),
        )
        for (value in
            listOf(Long.MIN_VALUE, -1234567L, -1L, 0L, 1L, 1234567L, Long.MAX_VALUE)) assertEquals(
            String.format(Locale.UK, "%,d", value),
            healthGrouped(value),
        )
    }

    @Test
    fun factoryTrendListsAreReadOnlyAndRawContextStillRetainsItsAlias() {
        val trends =
            org.racedaydreams.domain.health.HealthIntelligence.evaluate(
                emptyList(),
                java.time.LocalDate.of(2026, 11, 15),
            )
        @Suppress("UNCHECKED_CAST")
        val exposed =
            trends as java.util.List<org.racedaydreams.domain.health.HealthIntelligence.Trend>
        assertFailsWith<UnsupportedOperationException> { exposed.clear() }
        val evidence = mutableListOf("before")
        val raw = org.racedaydreams.domain.health.RecoveryCoaching.Context(evidence, null, false)
        evidence.add("after")
        assertSame(evidence, raw.evidence())
        assertEquals(2, raw.evidence()!!.size)
    }
}
