package org.racedaydreams.domain

import kotlin.test.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.nanoseconds
import kotlinx.datetime.Instant
import org.racedaydreams.domain.body.*
import org.racedaydreams.domain.settings.PrimaryFocus

class ProteinPolicyTest {
    private val now = Instant.parse("2026-11-05T12:00:00Z")

    private fun weight(
        id: String = "manual",
        at: Instant = now,
        mass: Double = 87.0,
        provider: String = "manual",
    ) = ProteinWeightEvidence(id, at, mass, 0, provider, id, null)

    private fun selection(row: ProteinWeightEvidence) =
        ProteinSelection(row.id, row.at, row.kilograms)

    @Test
    fun originalRangesStayDistinctForRunningStrengthAndUnknownFocus() {
        val row = weight()
        for ((focus, expected) in
            listOf(
                PrimaryFocus.RUNNING to (105 to 140),
                PrimaryFocus.STRENGTH to (120 to 175),
                null to (105 to 175),
            )) {
            val advice = ProteinPolicy.evaluate(true, selection(row), listOf(row), focus, now)
            assertEquals(expected.first, advice.minimumGrams)
            assertEquals(expected.second, advice.maximumGrams)
            assertEquals(row, advice.basis)
        }
    }

    @Test
    fun offOrUnselectedNeverChoosesANewerProviderWeight() {
        val manual = weight(at = now - 1.days)
        val recent = weight("provider", provider = "health-connect", mass = 90.0)
        assertFalse(
            ProteinPolicy.evaluate(
                    false,
                    selection(manual),
                    listOf(manual, recent),
                    PrimaryFocus.HYBRID,
                    now,
                )
                .available()
        )
        assertFalse(
            ProteinPolicy.evaluate(true, null, listOf(manual, recent), PrimaryFocus.HYBRID, now)
                .available()
        )
        assertEquals(
            manual,
            ProteinPolicy.evaluate(
                    true,
                    selection(manual),
                    listOf(manual, recent),
                    PrimaryFocus.HYBRID,
                    now,
                )
                .basis,
        )
    }

    @Test
    fun exactNinetyDayAndFutureBoundariesKeepTheSameMeaning() {
        for ((at, allowed) in
            listOf(
                now - 90.days to true,
                now - 90.days - 1.nanoseconds to false,
                now + 1.nanoseconds to false,
            )) {
            val row = weight(at = at)
            assertEquals(
                allowed,
                ProteinPolicy.evaluate(true, selection(row), listOf(row), PrimaryFocus.RUNNING, now)
                    .available(),
            )
        }
    }

    @Test
    fun changingSelectedEvidenceOrConflictingItsProvenanceNeedsReview() {
        val row = weight()
        assertFalse(
            ProteinPolicy.evaluate(
                    true,
                    selection(row),
                    listOf(row.copy(kilograms = 88.0)),
                    PrimaryFocus.RUNNING,
                    now,
                )
                .available()
        )
        assertFailsWith<IllegalArgumentException> {
            ProteinPolicy.evaluate(
                true,
                selection(row),
                listOf(row, row.copy(origin = "other.source")),
                PrimaryFocus.RUNNING,
                now,
            )
        }
    }

    @Test
    fun storedFocusAndLegacyEntryParsingRemainCompatible() {
        for (focus in PrimaryFocus.entries) assertEquals(
            focus,
            PrimaryFocus.fromStorageValue(focus.storageValue()),
        )
        assertEquals(PrimaryFocus.RUNNING, PrimaryFocus.fromLegacyStartingPath("record"))
        assertEquals(PrimaryFocus.RUNNING, PrimaryFocus.fromLegacyStartingPath("race"))
        assertEquals(PrimaryFocus.HYBRID, PrimaryFocus.fromLegacyStartingPath("hybrid"))
        assertNull(PrimaryFocus.fromLegacyStartingPath("strength"))
        assertNull(PrimaryFocus.fromStorageValue("RUNNING"))
        assertNull(PrimaryFocus.fromStorageValue(null))
    }

    @Test
    fun supportedMassesStayApproximateBoundedAndUnsupportedEvidenceGetsNoNumbers() {
        for (focus in PrimaryFocus.entries) for (mass in 30..250) {
            val row = weight(mass = mass.toDouble())
            val advice = ProteinPolicy.evaluate(true, selection(row), listOf(row), focus, now)
            assertTrue(advice.available())
            assertTrue(advice.minimumGrams!! >= mass * 1.2 - 2.5)
            assertTrue(advice.maximumGrams!! <= mass * 2.0 + 2.5)
            assertEquals(0, advice.minimumGrams % 5)
            assertEquals(0, advice.maximumGrams % 5)
        }
        for (mass in listOf(10.0, 29.9, 250.1, 400.0)) {
            val row = weight(mass = mass)
            assertFalse(
                ProteinPolicy.evaluate(
                        true,
                        selection(row),
                        listOf(row),
                        PrimaryFocus.STRENGTH,
                        now,
                    )
                    .available()
            )
        }
    }
}
