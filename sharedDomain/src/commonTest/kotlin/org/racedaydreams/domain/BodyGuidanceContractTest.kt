package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.body.*
import org.racedaydreams.domain.settings.PrimaryFocus
import org.racedaydreams.domain.time.*

class BodyGuidanceContractTest {
    private val now = Instants.ofEpochSecond(1793880000, 500)

    private fun row(at: Instant) =
        BodyWeight("manual", 87.0, at, Offsets.ofTotalSeconds(0), "manual", "manual", null)

    @Test
    fun defaultsStayOptionalAndAdviceReturnsTheOriginalActualMeasurement() {
        val preferences = BodyMeasurementsRepository.Preferences.defaults()
        assertFalse(preferences.proteinEnabled())
        assertNull(preferences.basis())
        assertEquals(HeightUnit.CENTIMETRES, preferences.heightUnit())
        val actual = row(now)
        val selected = ProteinGuidance.Basis.from(actual)
        val advice =
            ProteinGuidance.evaluate(true, selected, listOf(actual), PrimaryFocus.RUNNING, now)
        assertSame(actual, advice.basis())
        assertEquals(105, advice.minimumGrams())
        assertEquals(140, advice.maximumGrams())
        assertFalse(
            ProteinGuidance.evaluate(false, selected, listOf(actual), null, now).available()
        )
        assertFalse(ProteinGuidance.evaluate(true, null, listOf(actual), null, now).available())
    }

    @Test
    fun nanosecondFreshnessAndFutureBoundariesDoNotChangeTheNinetyDayRule() {
        val cutoffSecond = now.getEpochSecond() - 90 * 86400
        for ((at, expected) in
            listOf(
                Instants.ofEpochSecond(cutoffSecond, 500) to true,
                Instants.ofEpochSecond(cutoffSecond, 499) to false,
                Instants.ofEpochSecond(cutoffSecond, 501) to true,
                Instants.ofEpochSecond(now.getEpochSecond(), 501) to false,
            )) {
            val actual = row(at)
            assertEquals(
                expected,
                ProteinGuidance.evaluate(
                        true,
                        ProteinGuidance.Basis.from(actual),
                        listOf(actual),
                        PrimaryFocus.HYBRID,
                        now,
                    )
                    .available(),
            )
        }
    }

    @Test
    fun extremeRetainedTimestampsDoNotBecomeRecentByNativeClamping() {
        for (at in
            listOf(
                Instants.ofEpochSecond(InstantParts.MIN_SECOND),
                Instants.ofEpochSecond(InstantParts.MAX_SECOND),
            )) {
            val actual = row(at)
            val advice =
                ProteinGuidance.evaluate(
                    true,
                    ProteinGuidance.Basis.from(actual),
                    listOf(actual),
                    null,
                    now,
                )
            assertFalse(advice.available())
            assertNull(advice.minimumGrams())
            assertNull(advice.maximumGrams())
            assertEquals(at, actual.at())
        }
    }
}
