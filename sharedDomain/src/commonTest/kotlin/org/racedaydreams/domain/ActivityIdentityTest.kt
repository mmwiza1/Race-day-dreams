package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.activity.PerformanceDistance
import org.racedaydreams.domain.activity.SourceIdentity

class ActivityIdentityTest {
    @Test
    fun existingProviderIdentitiesKeepExactWhitespaceEqualityAndRecordRepresentation() {
        val identity = SourceIdentity(" provider ", " record ", "origin")
        assertEquals(" provider ", identity.provider())
        assertEquals(" record ", identity.recordId())
        assertEquals(identity, SourceIdentity(" provider ", " record ", "origin"))
        assertNotEquals(identity, SourceIdentity("provider", "record", "origin"))
        assertEquals(
            "SourceIdentity[provider= provider , recordId= record , originPackage=origin]",
            identity.toString(),
        )
        // Captured from the original Java records before conversion, not a new hash convention.
        assertEquals(-1911506010, identity.hashCode())
        assertEquals(317660798, SourceIdentity("health-connect", "id", null).hashCode())
    }

    @Test
    fun coreDistanceOrderValuesAndHashesStayCompatible() {
        assertEquals(
            listOf("1k", "mile", "2k", "3k", "5k", "10k", "half_marathon", "marathon"),
            PerformanceDistance.CORE.map { it.code() },
        )
        assertEquals(
            listOf(
                -781148652,
                914370765,
                -748612974,
                -731949296,
                -707319796,
                -629663432,
                -353361371,
                944816492,
            ),
            PerformanceDistance.CORE.map { it.hashCode() },
        )
        assertEquals(PerformanceDistance.ONE_MILE, PerformanceDistance("mile", 1609.344, "1 mile"))
        assertEquals(
            "PerformanceDistance[code=1k, metres=1000.0, label=1K]",
            PerformanceDistance.ONE_K.toString(),
        )
    }

    @Test
    fun invalidIdentityOrDistanceNeverCreatesAUsableRecord() {
        assertFailsWith<IllegalArgumentException> { SourceIdentity(null, "id", null) }
        assertFailsWith<IllegalArgumentException> { SourceIdentity("provider", " \t", null) }
        assertFailsWith<IllegalArgumentException> { PerformanceDistance(" 5k", 5000.0, "5K") }
        assertFailsWith<IllegalArgumentException> { PerformanceDistance("5k", Double.NaN, "5K") }
        assertFailsWith<IllegalArgumentException> { PerformanceDistance("5k", 0.0, "5K") }
    }
}
