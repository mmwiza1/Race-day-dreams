@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.racedaydreams.health.body

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toNSDate

class HealthKitDateInteropTest {
    @Test
    fun appleDatesUseTheSameEpochForCurrentAndPreEpochObservations() {
        for (value in
            listOf("1970-01-01T00:00:00Z", "1969-12-31T23:59:59Z", "2026-11-13T12:00:00Z")) {
            val original = Instant.parse(value)
            assertEquals(original, original.toNSDate().toKotlinInstant())
        }
    }

    @Test
    fun representableSubsecondsAreNotReplacedByWholeSeconds() {
        val original = Instant.parse("2026-11-13T12:00:00.500Z")
        assertEquals(original, original.toNSDate().toKotlinInstant())
    }
}
