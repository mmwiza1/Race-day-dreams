package org.racedaydreams.storage

import kotlin.test.*
import org.racedaydreams.domain.time.Offsets

class StoredOffsetTest {
    @Test
    fun originalCanonicalOffsetsIncludingSourceSecondsRoundTrip() {
        for (seconds in -64800..64800 step 61) {
            val original = Offsets.ofTotalSeconds(seconds)
            assertEquals(original, StoredOffset.parse(original.toString()))
        }
        assertEquals(Offsets.ofTotalSeconds(64800), StoredOffset.parse("+18:00"))
        assertEquals(Offsets.ofTotalSeconds(0), StoredOffset.parse("Z"))
    }

    @Test
    fun invalidOrNoncanonicalSourceOffsetsRemainErrors() {
        for (bad in
            listOf(
                "+18:00:01",
                "-18:01",
                "+19:00",
                "+01:60",
                "+01:00:60",
                "+00:00",
                "-00:00",
                "UTC",
                "+1:00",
            )) {
            assertFailsWith<IllegalArgumentException>(bad) { StoredOffset.parse(bad) }
        }
    }
}
