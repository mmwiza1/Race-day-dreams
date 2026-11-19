package org.racedaydreams.domain

import java.time.Instant
import java.time.ZoneOffset
import java.util.Random
import kotlin.test.*
import org.racedaydreams.domain.time.InstantParts
import org.racedaydreams.domain.time.offsetText

class InstantCompatibilityTest {
    @Test
    fun nativeInstantPartsRetainFullOriginalRangeAndIsoFractionFormatting() {
        val random = Random(51771L)
        val seconds =
            listOf(
                Instant.MIN.epochSecond,
                Instant.MAX.epochSecond,
                -62167219200,
                -62198755200,
                -1,
                0,
                1,
                1794583801,
            ) +
                List(10000) {
                    Instant.MIN.epochSecond +
                        random.nextLong().ushr(1) %
                            (Instant.MAX.epochSecond - Instant.MIN.epochSecond + 1)
                }
        for (second in seconds) for (nanos in
            listOf(0L, 1L, 1000L, 1000000L, 123456000L, 123456789L, 999999999L)) {
            val original = Instant.ofEpochSecond(second, nanos)
            val nativeParts = InstantParts.ofEpochSecond(second, nanos)
            assertEquals(original.epochSecond, nativeParts.seconds)
            assertEquals(original.nano, nativeParts.nanos)
            assertEquals(original.toString(), nativeParts.text())
        }
    }

    @Test
    fun allSupportedSecondOffsetsKeepOriginalCanonicalIds() {
        for (seconds in -64800..64800) assertEquals(
            ZoneOffset.ofTotalSeconds(seconds).id,
            offsetText(seconds),
        )
    }
}
