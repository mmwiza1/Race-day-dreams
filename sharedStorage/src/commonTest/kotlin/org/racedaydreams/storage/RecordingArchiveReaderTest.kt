package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*

class RecordingArchiveReaderTest {
    private fun hex(bytes: ByteArray) =
        bytes.joinToString("") { it.toUByte().toString(16).padStart(2, '0') }

    private fun manifest(bytes: ByteArray) =
        RecordingArchiveManifest(3, bytes.size.toLong(), hex(legacyEvidenceSha256(bytes)))

    private fun session() = buildJsonObject {
        put("kind", "session")
        put("id", "run-🏃")
        put("state", "RECOVERABLE")
        put("start", "2026-11-16T09:00:00.123456789Z")
        put("mono", 9007199254740993L)
        put("last", 9007199254741993L)
        put("retainedExtra", "é 中文 🏃🏽 / honest missing GPS")
    }

    private fun raw() = (session().toString() + "\r\n\n").encodeToByteArray()

    @Test
    fun replayDeliversOnlyValidatedRowsAndConsumerFailureClosesReader() {
        val bytes = raw()
        val lines = mutableListOf<String>()
        val reader = RecordingArchiveReader.replay(manifest(bytes)) { lines += it }
        for (b in bytes) reader.accept(byteArrayOf(b))
        reader.finish()
        assertEquals(listOf(session().toString() + "\r"), lines)
        val failing = RecordingArchiveReader.replay(manifest(bytes)) { error("Consumer rejects") }
        assertFails { failing.accept(bytes) }
        assertFails { failing.finish() }
        assertFails { failing.accept(bytes) }
        val bad = "{\"kind\":\"point\",\"id\":\"missing\"}\n".encodeToByteArray()
        val invalid =
            RecordingArchiveReader.replay(manifest(bad)) {
                error("Invalid row must not be delivered")
            }
        val failure = assertFails { invalid.accept(bad) }
        assertNotEquals("Invalid row must not be delivered", failure.message)
    }

    @Test
    fun replayCallbackIsNotWholeArchiveApprovalAndBadFinalDigestStillRejects() {
        val bytes = raw()
        var rows = 0
        val reader =
            RecordingArchiveReader.replay(manifest(bytes).copy(sha256 = "00".repeat(32))) { rows++ }
        reader.accept(bytes)
        assertEquals(1, rows)
        assertFails { reader.finish() }
        assertFails { reader.accept(bytes) }
    }

    @Test
    fun realPlatformStreamingDigestMatchesPublishedEmptyAndAbcSha256Vectors() {
        for ((input, expected) in
            listOf(
                "" to "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "abc" to "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            )) {
            val digest = archiveSha256()
            val bytes = input.encodeToByteArray()
            digest.update(bytes, 0, 0)
            for (i in bytes.indices) digest.update(bytes, i, 1)
            assertEquals(expected, hex(digest.finish()))
            digest.close()
            assertFails { digest.update(bytes, 0, 0) }
            assertFails { digest.finish() }
        }
    }

    @Test
    fun splitUtf8CharactersAndCrLfBoundariesDoNotChangeExactSessionMetadata() {
        val bytes = raw()
        for (size in listOf(1, 2, 7, 31, 4096)) {
            val reader = RecordingArchiveReader(manifest(bytes))
            var at = 0
            while (at < bytes.size) {
                val length = minOf(size, bytes.size - at)
                reader.accept(bytes, at, length)
                at += length
            }
            val kept = reader.finish().sessions.getValue("run-🏃")
            assertEquals(9007199254740993L, kept.startedMono)
            assertEquals(9007199254741993L, kept.lastMono)
            assertEquals("RECOVERABLE", kept.state)
            assertEquals(0, kept.points)
            assertFalse(kept.hasActivity)
            assertNull(kept.removal)
        }
    }

    @Test
    fun emptyOriginalArchiveAndUtf8BomUseActualBytesAndOriginalHash() {
        val empty =
            RecordingArchiveReader(
                RecordingArchiveManifest(
                    1,
                    0,
                    "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                )
            )
        assertTrue(empty.finish().sessions.isEmpty())
        val bytes = byteArrayOf(0xef.toByte(), 0xbb.toByte(), 0xbf.toByte()) + raw()
        val reader = RecordingArchiveReader(manifest(bytes))
        reader.accept(bytes)
        assertEquals("RECOVERABLE", reader.finish().sessions.getValue("run-🏃").state)
    }

    @Test
    fun corruptDigestCannotReturnAnOtherwiseValidSessionGraph() {
        val bytes = raw()
        val reader = RecordingArchiveReader(manifest(bytes).copy(sha256 = "00".repeat(32)))
        reader.accept(bytes)
        assertFails { reader.finish() }
        assertFails { reader.accept(ByteArray(0)) }
        assertFails { reader.finish() }
        reader.close()
    }

    @Test
    fun declaredLengthMismatchFailsOnExtraBytesOrOnPrematureEof() {
        val bytes = raw()
        val short = RecordingArchiveReader(manifest(bytes).copy(bytes = bytes.size.toLong() - 1))
        assertFails { short.accept(bytes) }
        assertFails { short.finish() }
        val long = RecordingArchiveReader(manifest(bytes).copy(bytes = bytes.size.toLong() + 1))
        long.accept(bytes)
        assertFails { long.finish() }
        assertFails { long.accept(byteArrayOf(10)) }
    }

    @Test
    fun checksumCorrectButTruncatedRowOrInvalidUtf8StillRejects() {
        val noNewline = session().toString().encodeToByteArray()
        val unfinished = "{\"kind\":\"session\"\n".encodeToByteArray()
        val invalidUtf8 =
            "{\"bad\":\"".encodeToByteArray() +
                byteArrayOf(0xc3.toByte()) +
                "\"}\n".encodeToByteArray()
        for (bytes in listOf(noNewline, unfinished, invalidUtf8)) {
            val reader = RecordingArchiveReader(manifest(bytes))
            assertFails {
                reader.accept(bytes)
                reader.finish()
            }
            assertFails { reader.finish() }
        }
    }

    @Test
    fun canceledOrFinishedReaderCannotAcceptMoreDataOrReturnAnotherGraph() {
        val bytes = raw()
        val canceled = RecordingArchiveReader(manifest(bytes))
        canceled.accept(bytes, 0, 3)
        canceled.close()
        canceled.close()
        assertFails { canceled.finish() }
        assertFails { canceled.accept(bytes) }
        val finished = RecordingArchiveReader(manifest(bytes))
        finished.accept(bytes)
        assertEquals(1, finished.finish().sessions.size)
        assertFails { finished.finish() }
        assertFails { finished.accept(bytes) }
    }

    @Test
    fun invalidChunkRangesCloseRatherThanSkippingEvidence() {
        val bytes = raw()
        for ((offset, length) in listOf(-1 to 1, 0 to -1, bytes.size to 1, Int.MAX_VALUE to 1)) {
            val reader = RecordingArchiveReader(manifest(bytes))
            assertFails { reader.accept(bytes, offset, length) }
            assertFails { reader.accept(bytes) }
            assertFails { reader.finish() }
        }
    }

    private fun manifestJson() = buildJsonObject {
        put("format", "rdd-owned-recordings")
        put("version", 3)
        put("bytes", 0)
        put("sha256", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
    }

    @Test
    fun originalManifestVersionsAndHarmlessUnknownMetadataRemainReadable() {
        for (version in 1..3) {
            val row =
                JsonObject(
                    manifestJson() +
                        mapOf(
                            "version" to JsonPrimitive(version),
                            "legacyExtra" to JsonPrimitive("original metadata"),
                        )
                )
            val m = RecordingArchiveManifest.decode(("\uFEFF" + row).encodeToByteArray())
            assertEquals(version, m.version)
            assertEquals(0L, m.bytes)
            assertTrue(RecordingArchiveReader(m).finish().sessions.isEmpty())
        }
    }

    @Test
    fun boundedManifestRejectsFutureVersionsCorruptFieldsAndIncorrectScalarTypes() {
        val original = manifestJson()
        val cases =
            listOf(
                "version" to JsonPrimitive(4),
                "version" to JsonPrimitive(1.5),
                "version" to JsonPrimitive("3"),
                "bytes" to JsonPrimitive(-1),
                "bytes" to JsonPrimitive(1.5),
                "bytes" to JsonPrimitive(RecordingArchiveRows.MAX_BYTES + 1),
                "format" to JsonPrimitive("another-app"),
                "sha256" to JsonPrimitive("missing"),
                "sha256" to
                    JsonPrimitive(original.getValue("sha256").jsonPrimitive.content.uppercase()),
            )
        for ((name, value) in cases) assertFails {
            RecordingArchiveManifest.decode(
                JsonObject(original + (name to value)).toString().encodeToByteArray()
            )
        }
        assertFails { RecordingArchiveManifest.decode(ByteArray(8193) { 32 }) }
        assertFails {
            RecordingArchiveManifest.decode("{\"version\":3,\"version\":2}".encodeToByteArray())
        }
        assertFails {
            RecordingArchiveManifest.decode(original.toString().dropLast(1).encodeToByteArray())
        }
    }
}
