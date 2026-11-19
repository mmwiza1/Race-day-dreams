package org.racedaydreams.storage

import kotlin.test.*

class LegacyStrengthNumbersJvmTest {
    private fun verify(value: Double) {
        val expected =
            when {
                value.toBits() == Long.MIN_VALUE -> "-0"
                value == value.toLong().toDouble() -> value.toLong().toString()
                else -> java.lang.Double.toString(value)
            }
        assertEquals(
            expected,
            LegacyStrengthNumbers.text(value),
            "bits=${value.toBits().toString(16)}",
        )
    }

    @Test
    fun exactLegacyJavaOracleAcrossSubnormalBoundariesAndAllMeasurementExponents() {
        verify(0.0)
        verify(-0.0)
        for (bits in 1L..2048) verify(Double.fromBits(bits))
        for (exponent in 1..1039) {
            val value = Double.fromBits(exponent.toLong() shl 52)
            for (candidate in listOf(Math.nextDown(value), value, Math.nextUp(value))) if (
                candidate <= 100000
            )
                verify(candidate)
        }
    }

    @Test
    fun deterministicFiniteDoubleCorpusMatchesOriginalNumberSpelling() {
        var bits = 0x1234567812345678L
        repeat(20000) {
            bits = bits xor (bits shl 13)
            bits = bits xor (bits ushr 7)
            bits = bits xor (bits shl 17)
            val candidate = Double.fromBits(bits and Long.MAX_VALUE)
            if (candidate.isFinite() && candidate <= 100000) verify(candidate)
        }
        for (value in listOf(0.1, 0.01, 0.001, 0.0001, 2.5, 82.25, 99999.99999999999)) verify(value)
    }

    @Test
    fun independentJvmOracleProducesTheNativeCorpusGolden() {
        val expected =
            LegacyFingerprintFixtures.numbers().joinToString("\n") { value ->
                when {
                    value.toBits() == Long.MIN_VALUE -> "-0"
                    value == value.toLong().toDouble() -> value.toLong().toString()
                    else -> java.lang.Double.toString(value)
                }
            }
        val digest =
            java.security.MessageDigest.getInstance("SHA-256")
                .digest(expected.toByteArray(Charsets.UTF_8))
                .joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
        println("LEGACY_NUMBER_ORACLE_SHA=$digest")
        assertEquals(
            expected,
            LegacyFingerprintFixtures.numbers().joinToString("\n") {
                LegacyStrengthNumbers.text(it)
            },
        )
    }

    @Test
    fun everydayMeasurementRangeMatchesLegacyJavaAcrossOneHundredThousandValues() {
        var bits = 0x1234567812345678L
        repeat(100000) {
            bits = bits xor (bits shl 13)
            bits = bits xor (bits ushr 7)
            bits = bits xor (bits shl 17)
            verify((bits ushr 11).toDouble() / 9007199254740992.0 * 100000)
        }
    }
}
