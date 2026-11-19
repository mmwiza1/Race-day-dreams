package org.racedaydreams.storage

import kotlin.test.*

class StoredInstantTest {
    @Test
    fun originalCanonicalRangeAndNanosecondsSurvive() {
        for (text in
            listOf(
                "1970-01-01T00:00:00Z",
                "2024-02-29T12:34:56.123456789Z",
                "1900-03-01T00:00:00Z",
                "0000-01-01T00:00:00Z",
                "-1000000000-01-01T00:00:00Z",
                "+1000000000-12-31T23:59:59.999999999Z",
                "+10000-01-01T00:00:00Z",
            )) {
            assertEquals(text, StoredInstant.parse(text).toString())
        }
    }

    @Test
    fun malformedNoncanonicalOrImpossibleStoredDatesFail() {
        for (text in
            listOf(
                "2023-02-29T00:00:00Z",
                "2024-02-30T00:00:00Z",
                "2026-11-15T25:00:00Z",
                "2026-11-15T00:00:00.0Z",
                "2026-11-15T00:00:00+01:00",
                "garbage",
                "1970-01-01T00:00:00Z trailing",
            )) {
            assertFailsWith<IllegalArgumentException>(text) { StoredInstant.parse(text) }
        }
    }

    @Test
    fun retainedFullRangeInstantsRoundTripWithoutNativeDateClamping() {
        val random = kotlin.random.Random(1729)
        repeat(2048) {
            val seconds = random.nextLong(-31557014167219200L, 31556889864403200L)
            val original =
                org.racedaydreams.domain.time.Instants.ofEpochSecond(
                    seconds,
                    random.nextLong(1000000000L),
                )
            assertEquals(original, StoredInstant.parse(original.toString()))
        }
    }
}
