package org.racedaydreams.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.racedaydreams.domain.body.HeightUnit
import org.racedaydreams.domain.body.MassUnit

class PacePolicyTest {
    @Test
    fun parsingAndRoundingKeepTheExistingRules() {
        assertEquals(7199L, PacePolicy.parseTargetSeconds("1:59:59"))
        assertEquals(172800L, PacePolicy.parseTargetSeconds("48:00:00"))
        assertEquals(1620L, PacePolicy.parseTargetSeconds(" 27:00 "))
        assertEquals(
            "5:41/km",
            PacePolicy.targetPace(7199, RaceDistance.HALF_MARATHON, UnitSystem.METRIC),
        )
        assertEquals(
            "9:09/mi",
            PacePolicy.targetPace(7199, RaceDistance.HALF_MARATHON, UnitSystem.IMPERIAL),
        )
        assertEquals("6:00/km", PacePolicy.targetPace(1799, RaceDistance.FIVE_K, UnitSystem.METRIC))
        assertEquals("0:00:00", PacePolicy.formatSeconds(0))
        assertEquals("1:59:59", PacePolicy.formatSeconds(7199))
    }

    @Test
    fun missingAndInvalidEvidenceNeverInventsAPace() {
        for (invalid in listOf(null, "", "0:00", "48:00:01", "1:60", "١:٠٠", "\u00a027:00\u00a0")) {
            assertFailsWith<IllegalArgumentException> { PacePolicy.parseTargetSeconds(invalid) }
        }
        assertFailsWith<IllegalArgumentException> {
            PacePolicy.targetPace(100, RaceDistance.GENERAL_FITNESS, UnitSystem.METRIC)
        }
    }

    @Test
    fun bodyUnitsKeepCanonicalEvidenceAndValidation() {
        assertEquals(0.45359237, MassUnit.LB.toKilograms(1.0))
        assertEquals(0.0, MassUnit.KG.toKilograms(0.0))
        assertEquals(1.8, HeightUnit.CENTIMETRES.toMetres(180.0))
        assertEquals(72.0, HeightUnit.INCHES.fromMetres(72 * 0.0254), 1e-10)
        assertFailsWith<IllegalArgumentException> { MassUnit.KG.toKilograms(Double.NaN) }
        assertFailsWith<IllegalArgumentException> { HeightUnit.CENTIMETRES.toMetres(0.0) }
    }
}
