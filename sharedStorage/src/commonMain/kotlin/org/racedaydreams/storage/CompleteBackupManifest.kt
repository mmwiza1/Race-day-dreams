package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.storage.StoredJson.exactLong
import org.racedaydreams.storage.StoredJson.row
import org.racedaydreams.storage.StoredJson.string

/**
 * Original complete-package v1 manifest. Container extraction and BOTH inner document validators
 * remain mandatory; matching declared hashes alone is never permission to replace the database.
 */
class CompleteBackupManifest private constructor(files: Map<String, Entry>) {
    data class Entry(val bytes: Long, val sha256: String) {
        init {
            require(bytes >= 0 && Regex("[0-9a-f]{64}").matches(sha256)) {
                "Invalid complete-backup file evidence."
            }
        }
    }

    /** Measured from the actual extracted payload, not copied from its declared manifest entry. */
    data class Receipt(val name: String, val evidence: Entry)

    val files = Immutable.copyMap(files)

    /** Payload entries only. A list preserves duplicates, so they cannot disappear in a map. */
    fun verify(actual: List<Receipt>) {
        val receipts = Immutable.copyList(actual)
        require(receipts.size == 2 && receipts.map { it.name }.toSet() == files.keys) {
            "Unsupported, duplicate or incomplete backup payload entries."
        }
        for (entry in receipts) require(files[entry.name] == entry.evidence) {
            "Complete backup size/checksum mismatch."
        }
    }

    companion object {
        const val MAX_MANIFEST_BYTES = 8192
        const val MAX_RECORDINGS_ZIP_BYTES = RecordingArchiveRows.MAX_BYTES + 1024 * 1024

        fun decode(bytes: ByteArray): CompleteBackupManifest {
            require(bytes.size <= MAX_MANIFEST_BYTES) { "Complete-backup manifest exceeds 8 KiB." }
            val text = bytes.decodeToString(throwOnInvalidSequence = true).removePrefix("\uFEFF")
            StoredJsonSyntax(text, 32, MAX_MANIFEST_BYTES, MAX_MANIFEST_BYTES) {
                    throw IllegalArgumentException("Invalid complete-backup manifest: $it")
                }
                .validate()
            val root = Json.parseToJsonElement(text).row()
            require(
                root.string("format") == "rdd-complete-backup" && root.exactLong("version") == 1L
            ) {
                "Unsupported complete-backup format."
            }
            val files =
                root["files"]?.row()
                    ?: throw IllegalArgumentException("Missing complete-backup file evidence.")
            // Original readers ignore unknown manifest metadata. Unknown ZIP entries still reject.
            val known =
                listOf(
                    "app.json" to BackupEnvelopeJson.MAX_BYTES.toLong(),
                    "recordings.zip" to MAX_RECORDINGS_ZIP_BYTES,
                )
            return CompleteBackupManifest(
                known.associate { (name, limit) ->
                    val item =
                        files[name]?.row()
                            ?: throw IllegalArgumentException(
                                "Missing complete-backup payload: $name"
                            )
                    val entry = Entry(item.exactLong("bytes"), item.string("sha256"))
                    require(entry.bytes <= limit) {
                        "Complete-backup payload exceeds its supported size."
                    }
                    name to entry
                }
            )
        }
    }
}
