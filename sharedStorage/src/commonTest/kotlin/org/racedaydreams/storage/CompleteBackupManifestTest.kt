package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*

class CompleteBackupManifestTest {
    private val emptySha = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

    private fun entry(size: Long = 0, sha: String = emptySha) = buildJsonObject {
        put("bytes", size)
        put("sha256", sha)
    }

    private fun root(app: JsonElement = entry(), raw: JsonElement = entry()) = buildJsonObject {
        put("format", "rdd-complete-backup")
        put("version", 1)
        put(
            "files",
            buildJsonObject {
                put("app.json", app)
                put("recordings.zip", raw)
            },
        )
    }

    private fun read(row: JsonObject) =
        CompleteBackupManifest.decode(row.toString().encodeToByteArray())

    private fun receipt(name: String, size: Long = 0, sha: String = emptySha) =
        CompleteBackupManifest.Receipt(name, CompleteBackupManifest.Entry(size, sha))

    @Test
    fun measuredExactPayloadEvidenceMatchesOriginalNamesAndImmutableDeclarations() {
        val manifest = read(root(entry(123), entry(456)))
        manifest.verify(listOf(receipt("app.json", 123), receipt("recordings.zip", 456)))
        assertEquals(123L, manifest.files.getValue("app.json").bytes)
        assertEquals(456L, manifest.files.getValue("recordings.zip").bytes)
        assertFails { (manifest.files as MutableMap).clear() }
    }

    @Test
    fun missingDuplicateUnknownOrMismatchedPayloadsNeverVerify() {
        val manifest = read(root())
        val app = receipt("app.json")
        val raw = receipt("recordings.zip")
        for (actual in
            listOf(
                emptyList(),
                listOf(app),
                listOf(app, app),
                listOf(app, receipt("../../recordings.zip")),
                listOf(app, raw, raw),
                listOf(app, receipt("recordings.zip", 1)),
                listOf(receipt("app.json", sha = "00".repeat(32)), raw),
            )) {
            assertFails { manifest.verify(actual) }
        }
        manifest.verify(listOf(raw, app))
    }

    @Test
    fun originalDistinctAppAndRawContainerBoundsAreEnforcedWithoutTruncation() {
        read(
            root(
                entry(BackupEnvelopeJson.MAX_BYTES.toLong()),
                entry(CompleteBackupManifest.MAX_RECORDINGS_ZIP_BYTES),
            )
        )
        assertFails { read(root(entry(BackupEnvelopeJson.MAX_BYTES.toLong() + 1))) }
        assertFails { read(root(raw = entry(CompleteBackupManifest.MAX_RECORDINGS_ZIP_BYTES + 1))) }
        assertFails { read(root(entry(-1))) }
        assertFails { read(root(entry(sha = "invalid"))) }
    }

    @Test
    fun originalUnknownManifestMetadataAndUtf8BomDoNotEnableUnknownZipPayloads() {
        val original = root()
        val files = original.getValue("files").jsonObject
        val extra =
            JsonObject(
                original +
                    mapOf(
                        "legacy" to JsonPrimitive("original metadata"),
                        "files" to
                            JsonObject(files + ("unused-old-description" to JsonPrimitive(true))),
                    )
            )
        val manifest = CompleteBackupManifest.decode(("\uFEFF" + extra).encodeToByteArray())
        manifest.verify(listOf(receipt("app.json"), receipt("recordings.zip")))
        assertFails {
            manifest.verify(listOf(receipt("app.json"), receipt("unused-old-description")))
        }
    }

    @Test
    fun oversizedFutureDuplicateMalformedAndCoercedDeclarationsRejectBeforeUse() {
        for ((field, value) in
            listOf(
                "version" to JsonPrimitive(2),
                "version" to JsonPrimitive("1"),
                "version" to JsonPrimitive(1.0),
                "format" to JsonPrimitive("other-app"),
                "files" to JsonNull,
                "files" to buildJsonObject { put("app.json", entry()) },
            )) {
            assertFails { read(JsonObject(root() + (field to value))) }
        }
        val badSize = JsonObject(entry() + ("bytes" to JsonPrimitive("0")))
        assertFails { read(root(badSize)) }
        assertFails { CompleteBackupManifest.decode(ByteArray(8193) { 32 }) }
        assertFails {
            CompleteBackupManifest.decode("{\"version\":1,\"version\":1}".encodeToByteArray())
        }
        assertFails {
            CompleteBackupManifest.decode(root().toString().dropLast(1).encodeToByteArray())
        }
    }
}
