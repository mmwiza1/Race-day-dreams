package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*

class LegacyGymRowJsonTest {
    @Test
    fun originalStringEscapingIntegerPrecisionAndJavaUtf8ReplacementRemainExact() {
        val row = buildJsonObject {
            put("text", "a/\u0000\uD800")
            put("whole", 9007199254740993L)
            put("kg", 82.25)
        }
        val expected = "{\"text\":\"a\\/\\u0000?\",\"whole\":9007199254740993,\"kg\":82.25}"
        assertContentEquals(expected.encodeToByteArray(), LegacyGymRowJson.bytes(row))
    }

    @Test
    fun nativeCompactJsonCannotBypassOriginalEscapedRowBudget() {
        val row = buildJsonObject { put("notes", "/".repeat(140000)) }
        assertTrue(row.toString().encodeToByteArray().size < LegacyGymRowJson.MAX_BYTES)
        assertTrue(LegacyGymRowJson.bytes(row).size > LegacyGymRowJson.MAX_BYTES)
        assertFails { LegacyGymRowJson.check(row) }
        LegacyGymRowJson.check(buildJsonObject { put("notes", "/".repeat(100000)) })
    }

    @Test
    fun boundedNumericAndBooleanSpellingDoesNotBecomeLocaleOrNativeSpecific() {
        val row = buildJsonObject {
            put("negativeZero", -0.0)
            put("small", 0.0001)
            put("yes", true)
            put("missing", JsonNull)
        }
        assertEquals(
            "{\"negativeZero\":-0,\"small\":1.0E-4,\"yes\":true,\"missing\":null}",
            LegacyGymRowJson.bytes(row).decodeToString(),
        )
    }
}
