package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*

class BackupEnvelopeJsonTest {
    private val at = StoredInstant.parse("2026-11-16T10:00:00.123456789Z")
    private val introduced =
        linkedMapOf(
            "coaching" to 2,
            "development" to 3,
            "strengthState" to 4,
            "gymProgramming" to 4,
            "support" to 6,
            "release08" to 7,
            "strengthEngine" to 8,
            "strengthProgramming" to 9,
            "gymTools" to 10,
            "achievements" to 11,
            "bodyMeasurements" to 12,
            "detailRetention" to 13,
        )

    // These are envelope fixtures, not substitutes for the complete application-row validator.
    private fun data(version: Int) = buildJsonObject {
        for ((section, minimum) in introduced) if (version >= minimum)
            put(section, buildJsonObject {})
    }

    private fun text(data: String, version: String = "1") =
        """{"format":"race-day-dreams-backup","version":$version,"createdAt":"$at","data":$data}"""

    private fun bytes(data: JsonObject = data(1), version: Int = 1) =
        text(data.toString(), version.toString()).encodeToByteArray()

    private fun inspect(bytes: ByteArray) = BackupEnvelopeJson.inspect(bytes) { it }

    @Test
    fun allThirteenSupportedVersionsRequireAndDelegateCallerValidationOnce() {
        for (version in 1..13) {
            var calls = 0
            val expected = data(version)
            val checked =
                BackupEnvelopeJson.inspect(bytes(expected, version)) {
                    calls++
                    assertEquals(expected, it)
                    "fully checked by caller"
                }
            assertEquals(1, calls)
            assertEquals(version, checked.version)
            assertEquals(at, checked.createdAt)
            assertEquals("fully checked by caller", checked.snapshot)
        }
    }

    @Test
    fun eachSectionIsRequiredOnlyFromItsOriginalEnvelopeVersion() {
        for ((section, minimum) in introduced) {
            val incomplete = JsonObject(data(minimum) - section)
            var called = false
            assertFails { BackupEnvelopeJson.inspect(bytes(incomplete, minimum)) { called = true } }
            assertFalse(called)
            assertEquals(minimum - 1, inspect(bytes(data(minimum - 1), minimum - 1)).version)
        }
    }

    @Test
    fun fullValidatorFailurePropagatesWithoutReturningInspectionOrExport() {
        val error = IllegalArgumentException("Invalid retained graph")
        assertSame(error, assertFails { BackupEnvelopeJson.inspect(bytes()) { throw error } })
        assertSame(error, assertFails { BackupEnvelopeJson.encode(data(13), at) { throw error } })
    }

    @Test
    fun exportUsesCurrentVersionAndRetainsExactLongsTextAndOptionalMetadata() {
        val data =
            JsonObject(
                data(13) +
                    mapOf(
                        "exact" to JsonPrimitive(9007199254740993L),
                        "text" to JsonPrimitive("Recorded / 🏋️ \"\n\u0000"),
                        "unknown" to JsonNull,
                    )
            )
        var calls = 0
        val encoded =
            BackupEnvelopeJson.encode(data, at) {
                calls++
                assertEquals(data, it)
            }
        assertEquals(1, calls)
        val checked = inspect(encoded)
        assertEquals(13, checked.version)
        assertEquals(data, checked.snapshot)
        assertEquals("9007199254740993", checked.snapshot["exact"].toString())
    }

    @Test
    fun invalidVersionFormatTimeRootFieldsAndSectionShapesDoNotReachValidator() {
        val base = Json.parseToJsonElement(text("{}")) as JsonObject
        val bad =
            listOf(
                    JsonObject(base - "createdAt"),
                    JsonObject(base + ("extra" to JsonNull)),
                    JsonObject(base + ("format" to JsonPrimitive("another-app"))),
                    JsonObject(base + ("createdAt" to JsonPrimitive("yesterday"))),
                    JsonObject(base + ("data" to JsonArray(emptyList()))),
                )
                .map { it.toString() } +
                listOf("0", "14", "1.0", "1e0", "true", "\"1\"").map { text("{}", it) }
        for (payload in bad) {
            var called = false
            assertFails {
                BackupEnvelopeJson.inspect(payload.encodeToByteArray()) { called = true }
            }
            assertFalse(called)
        }
    }

    @Test
    fun duplicateEscapedKeysInvalidEscapesAndTrailingDocumentsAreRejected() {
        for (payload in
            listOf(
                text("{}") + "{}",
                text("{\"a\":1,\"\\u0061\":2}"),
                text("{\"x\":{\"id\":1,\"id\":2}}"),
                text("{\"bad\":\"\\q\"}"),
                text("{\"bad\":01}"),
                text("{\"bad\":NaN}"),
                text("{};"),
            )) assertFails { inspect(payload.encodeToByteArray()) }
    }

    @Test
    fun malformedUtf8AndOversizedInputFailBeforeDecodingOrValidation() {
        for (payload in
            listOf(byteArrayOf(0xc3.toByte(), 0x28), ByteArray(BackupEnvelopeJson.MAX_BYTES + 1))) {
            var called = false
            assertFails { BackupEnvelopeJson.inspect(payload) { called = true } }
            assertFalse(called)
        }
    }

    @Test
    fun originalLeadingUtf8BomAndWhitespaceRemainSupported() {
        val payload = bytes()
        assertEquals(
            inspect(payload),
            inspect(byteArrayOf(0xef.toByte(), 0xbb.toByte(), 0xbf.toByte()) + payload),
        )
        assertEquals(
            inspect(payload),
            inspect((" \r\n\t" + payload.decodeToString() + "\n").encodeToByteArray()),
        )
    }

    @Test
    fun rootSerializedTextBudgetDoesNotInheritStandaloneGymStringLimit() {
        val payload = text("{\"retainedPayload\":\"${"x".repeat(150000)}\"}")
        assertEquals(
            150000,
            inspect(payload.encodeToByteArray())
                .snapshot["retainedPayload"]!!
                .jsonPrimitive
                .content
                .length,
        )
        assertFails { GymStoredJson.read(payload) }
    }

    @Test
    fun wholeEnvelopeSyntaxCanExceedStandaloneSixteenMiBBudgetWithoutDroppingData() {
        val content = "x".repeat(900000)
        val data = buildJsonObject { repeat(20) { put("retained-$it", content) } }
        val payload = bytes(data)
        assertTrue(payload.size > 16 * 1024 * 1024 && payload.size < BackupEnvelopeJson.MAX_BYTES)
        val decoded = inspect(payload).snapshot
        assertEquals(20, decoded.size)
        assertTrue(decoded.values.all { it.jsonPrimitive.content == content })
    }

    @Test
    fun originalDepthStringAndStructuralLimitsAreEnforcedBeforeDomParsing() {
        val valid = "{\"value\":" + "[".repeat(30) + "0" + "]".repeat(30) + "}"
        inspect(text(valid).encodeToByteArray())
        val tooDeep = "{\"value\":" + "[".repeat(31) + "0" + "]".repeat(31) + "}"
        for (payload in
            listOf(
                text(tooDeep),
                text("{\"value\":\"${"x".repeat(1048576)}\"}"),
                "{" + ",".repeat(2000001) + "}",
            )) {
            var called = false
            assertFails {
                BackupEnvelopeJson.inspect(payload.encodeToByteArray()) { called = true }
            }
            assertFalse(called)
        }
    }
}
