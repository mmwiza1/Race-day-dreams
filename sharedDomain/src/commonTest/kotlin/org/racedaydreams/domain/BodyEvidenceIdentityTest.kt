package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.body.BodyHeight
import org.racedaydreams.domain.body.BodyWeight
import org.racedaydreams.domain.time.*

class BodyEvidenceIdentityTest {
    @Test
    fun exactInstantRangeAndFractionsAreRetainedWithoutNativeLibraryClamping() {
        val fixtures =
            listOf(
                Triple(InstantParts.MIN_SECOND, 0L, "-1000000000-01-01T00:00:00Z"),
                Triple(
                    InstantParts.MAX_SECOND,
                    999999999L,
                    "+1000000000-12-31T23:59:59.999999999Z",
                ),
                Triple(0L, 0L, "1970-01-01T00:00:00Z"),
                Triple(-1L, 1L, "1969-12-31T23:59:59.000000001Z"),
                Triple(1794583801L, 123456789L, "2026-11-13T15:30:01.123456789Z"),
            )
        for ((seconds, nanos, text) in fixtures) {
            val at = Instants.ofEpochSecond(seconds, nanos)
            assertEquals(seconds, at.getEpochSecond())
            assertEquals(nanos.toInt(), at.getNano())
            assertEquals(text, at.toString())
            assertEquals(text, InstantParts.ofEpochSecond(seconds, nanos).text())
        }
        assertTrue(Instants.ofEpochSecond(-1, 999999999).isBefore(Instants.ofEpochSecond(0)))
        assertTrue(Instants.ofEpochSecond(0, 1).isAfter(Instants.ofEpochSecond(0)))
        assertEquals(Instants.ofEpochSecond(0), Instants.ofEpochSecond(-1, 1000000000))
        assertFails { Instants.ofEpochSecond(InstantParts.MAX_SECOND, 1000000000) }
        assertFails { Instants.ofEpochSecond(InstantParts.MIN_SECOND, -1) }
    }

    @Test
    fun offsetsRetainSecondsAndMissingHeightOffsetRemainsUnknown() {
        val at = Instants.ofEpochSecond(1794583801, 123456789)
        val height = BodyHeight(" height ", 1.82, at, null, "provider", " source ", null, null)
        assertNull(height.offset())
        assertNull(height.importedAt())
        assertEquals(" height ", height.id())
        assertEquals(" source ", height.sourceId())
        assertEquals("+05:30:01", Offsets.ofTotalSeconds(19801).getId())
        assertEquals("-18:00", Offsets.ofTotalSeconds(-64800).toString())
        assertEquals("Z", Offsets.ofTotalSeconds(0).toString())
        assertFails { Offsets.ofTotalSeconds(64801) }
    }

    @Test
    fun observedMassDoesNotAcquireNewRequirementsOrLoseItsSourceIdentity() {
        val at = Instants.ofEpochSecond(1794583801, 123456789)
        val offset = Offsets.ofTotalSeconds(19801)
        val weight = BodyWeight(" weight-id ", 87.0, at, offset, "provider", "source", null)
        assertEquals(" weight-id ", weight.id())
        assertEquals(at, weight.at())
        assertEquals(offset, weight.offset())
        assertEquals("provider", weight.provider())
        assertNull(weight.origin())
        assertEquals(
            weight,
            BodyWeight(
                " weight-id ",
                87.0,
                Instants.ofEpochSecond(1794583801, 123456789),
                Offsets.ofTotalSeconds(19801),
                "provider",
                "source",
                null,
            ),
        )
        // Legacy weights allowed empty provider/source strings; do not retroactively reject them.
        assertEquals("", BodyWeight("legacy", 70.0, at, offset, "", "", null).provider())
        assertFailsWith<IllegalArgumentException> {
            BodyWeight("x", Double.NaN, at, offset, "p", "s", null)
        }
        assertFailsWith<IllegalArgumentException> {
            BodyHeight("x", 4.0, at, null, "p", "s", null, null)
        }
        assertFailsWith<NullPointerException> { BodyWeight("x", 70.0, at, null, "p", "s", null) }
    }
}
