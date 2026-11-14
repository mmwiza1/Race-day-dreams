package org.racedaydreams.domain

import java.time.Instant
import java.time.ZoneOffset
import java.util.Random
import kotlin.math.round
import kotlin.test.*
import org.racedaydreams.domain.time.IsoCalendar

class StrengthRecordTimeCompatibilityTest {
    @Test
    fun storedOffsetDaysAndComparableLoadRoundingMatchOriginalJdk() {
        val random = Random(218406L)
        val seconds =
            listOf(
                Instant.MIN.epochSecond,
                Instant.MAX.epochSecond,
                -86401L,
                -86400,
                -1,
                0,
                1,
                86399,
                86400,
            ) +
                List(10000) {
                    random.nextLong().rem((Instant.MAX.epochSecond - Instant.MIN.epochSecond) / 2)
                }
        val offsets = listOf(-64800, -19800, -1, 0, 1, 19800, 64800)
        for (second in seconds) for (offset in offsets) {
            val old = runCatching {
                Instant.ofEpochSecond(second)
                    .atOffset(ZoneOffset.ofTotalSeconds(offset))
                    .toLocalDate()
            }
            val shared = runCatching {
                val parts = IsoCalendar.parts((second + offset).floorDiv(86400))
                IsoCalendar.validate(parts.year, parts.month, parts.day)
                IsoCalendar.text(parts.year, parts.month, parts.day)
            }
            assertEquals(old.isSuccess, shared.isSuccess)
            if (old.isSuccess) assertEquals(old.getOrThrow().toString(), shared.getOrThrow())
        }
        val loads =
            listOf(-0.0, 0.0, 0.0000005, 0.0000015, 80.0000005, 80.0000015, 1500.0) +
                List(10000) { random.nextDouble() * 1500 }
        for (load in loads) assertEquals(
            (Math.rint(load * 1000000) / 1000000).toBits(),
            (round(load * 1000000) / 1000000).toBits(),
        )
    }
}
