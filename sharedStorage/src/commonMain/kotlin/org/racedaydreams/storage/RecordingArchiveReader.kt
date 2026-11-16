package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.storage.StoredJson.exactLong
import org.racedaydreams.storage.StoredJson.string

/**
 * Original manifest, independent of ZIP entry extraction. Hashes detect corruption, not authorship.
 */
data class RecordingArchiveManifest(val version: Int, val bytes: Long, val sha256: String) {
    init {
        require(
            version in 1..3 &&
                bytes in 0..RecordingArchiveRows.MAX_BYTES &&
                Regex("[0-9a-f]{64}").matches(sha256)
        ) {
            "Unsupported recording archive manifest."
        }
    }

    companion object {
        const val MAX_BYTES = 8192

        fun decode(bytes: ByteArray): RecordingArchiveManifest {
            require(bytes.size <= MAX_BYTES) { "Recording manifest exceeds 8 KiB." }
            val text = bytes.decodeToString(throwOnInvalidSequence = true).removePrefix("\uFEFF")
            StoredJsonSyntax(text, 32, 8192, MAX_BYTES) {
                    throw IllegalArgumentException("Invalid recording manifest: $it")
                }
                .validate()
            val row = Json.parseToJsonElement(text).jsonObject
            require(row.string("format") == "rdd-owned-recordings") {
                "Unsupported recording archive."
            }
            val version = row.exactLong("version")
            require(version in 1..3) { "Unsupported recording archive version." }
            return RecordingArchiveManifest(
                version.toInt(),
                row.exactLong("bytes"),
                row.string("sha256"),
            )
        }
    }
}

/**
 * Bounded UTF-8/newline framing and real platform SHA-256 for extracted recordings.jsonl bytes. ZIP
 * entry/name/expansion checks still belong to the platform container reader. No database writes.
 * Feed bounded chunks and close on cancellation; finish closes resources on success or failure.
 */
class RecordingArchiveReader(private val manifest: RecordingArchiveManifest) {
    private val digest = archiveSha256()
    private var rows: RecordingArchiveRows? = RecordingArchiveRows(manifest.version)
    private var buffer = ByteArray(4096)
    private var used = 0
    private var count = 0L
    private var closed = false

    fun accept(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size - offset): Unit =
        guarded {
            require(offset >= 0 && length >= 0 && offset <= bytes.size - length) {
                "Invalid archive chunk."
            }
            require(length.toLong() <= manifest.bytes - count) {
                "Recording archive byte count mismatch."
            }
            count += length
            digest.update(bytes, offset, length)
            for (index in offset until offset + length) {
                val value = bytes[index]
                if (value == 10.toByte()) {
                    val line = buffer.copyOf(used).decodeToString(throwOnInvalidSequence = true)
                    requireNotNull(rows).acceptLine(line)
                    used = 0
                } else {
                    // Four bytes per UTF-16 unit is a conservative allocation bound. The original
                    // 8MiB character limit is independently enforced after strict UTF-8 decoding.
                    val maximum = RecordingArchiveRows.MAX_LINE * 4
                    require(used < maximum) { "Recording archive line exceeds its supported size." }
                    if (used == buffer.size) buffer = buffer.copyOf(minOf(maximum, buffer.size * 2))
                    buffer[used++] = value
                }
            }
        }

    fun finish(): RecordingArchiveRows.Snapshot = guarded {
        require(count == manifest.bytes && used == 0) {
            "Truncated recording archive or missing final newline."
        }
        val actual = digest.finish().joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
        require(actual == manifest.sha256) { "Recording archive checksum mismatch." }
        val snapshot = requireNotNull(rows).finish()
        close()
        snapshot
    }

    /** Idempotent cancellation; no partial graph can subsequently be accepted or returned. */
    fun close() {
        if (closed) return
        closed = true
        digest.close()
        rows = null
        buffer = ByteArray(0)
        used = 0
    }

    private inline fun <T> guarded(block: () -> T): T {
        check(!closed) { "Recording archive reader is closed." }
        return try {
            block()
        } catch (failure: Throwable) {
            close()
            throw failure
        }
    }
}

internal interface ArchiveSha256 {
    fun update(bytes: ByteArray, offset: Int, length: Int)

    fun finish(): ByteArray

    fun close()
}

internal expect fun archiveSha256(): ArchiveSha256
