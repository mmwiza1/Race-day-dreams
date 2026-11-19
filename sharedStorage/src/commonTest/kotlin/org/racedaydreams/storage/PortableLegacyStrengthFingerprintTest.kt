package org.racedaydreams.storage

import kotlin.test.*

class PortableLegacyStrengthFingerprintTest {
    @Test
    fun nativeNumberCorpusMatchesIndependentJavaOracleGolden() {
        val input =
            LegacyFingerprintFixtures.numbers().joinToString("\n") {
                LegacyStrengthNumbers.text(it)
            }
        val hash =
            legacyEvidenceSha256(input.encodeToByteArray()).joinToString("") {
                (it.toInt() and 255).toString(16).padStart(2, '0')
            }
        // Produced by independent java.lang.Double/Android-number rules in JVM oracle test.
        assertEquals("17b120904110c831ca55d6b1dfed150e599faaa5c01eda91e3b87b052993d95b", hash)
    }

    @Test
    fun completeAllModalityFixtureMatchesIndependentWireGolden() {
        assertEquals(
            "1c7caee335838b43cfb111bffb0b90fcda0ec12bcc2a1d61cbd8998bcef69cf0",
            PortableLegacyStrengthFingerprint.fingerprint(LegacyFingerprintFixtures.session()),
        )
    }

    @Test
    fun isolatedUtf16SurrogatesUseJavaReplacementWithoutChangingValidPairs() {
        val actual = LegacyFingerprintFixtures.session("\ud800/\udc00/💪")
        assertEquals(
            "78478f4a028c630416e91867e294f1108b6f664ab52d819f8e69c05795fa8746",
            PortableLegacyStrengthFingerprint.fingerprint(actual),
        )
    }

    @Test
    fun emptyNotesNullMeasurementsAndReferenceFreeActualsRemainExact() {
        assertEquals(
            "557c4450731c2b6c4377de4a211e92203140d47d79c71fa4bd98aafb8442fa76",
            PortableLegacyStrengthFingerprint.fingerprint(LegacyFingerprintFixtures.session("")),
        )
    }

    @Test
    fun systemSha256MatchesStandardByteVectors() {
        fun hex(bytes: ByteArray) =
            bytes.joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            hex(legacyEvidenceSha256(byteArrayOf())),
        )
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            hex(legacyEvidenceSha256("abc".encodeToByteArray())),
        )
    }

    @Test
    fun unsupportedNumbersFailBeforeAnyFingerprintCanBeClaimed() {
        for (number in
            listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY, 100001.0)) assertFailsWith<
            IllegalArgumentException
        > {
            LegacyStrengthNumbers.text(number)
        }
    }
}
