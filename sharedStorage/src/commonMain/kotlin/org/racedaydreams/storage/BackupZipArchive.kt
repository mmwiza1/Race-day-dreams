package org.racedaydreams.storage

import org.racedaydreams.domain.coaching.Immutable

/**
 * A private staged file, not an open user document that another process can change during restore.
 */
interface BackupArchiveSource {
    val size: Long

    fun read(offset: Long, maximum: Int): ByteArray
}

/**
 * Bounded original ZIP containers, including stored/deflated entries and small ZIP64 archives.
 * Extraction writes only to a caller's private staging sink. It NEVER authorises database writes;
 * the parent/recording validators and transactional restore protections remain mandatory.
 */
class BackupZipArchive
private constructor(
    private val source: BackupArchiveSource,
    private val originalSize: Long,
    entries: List<Entry>,
) {
    enum class Kind {
        RECORDINGS,
        COMPLETE,
    }

    class Entry
    internal constructor(
        val name: String,
        val bytes: Long,
        val compressedBytes: Long,
        val crc32: Long,
        internal val method: Int,
        internal val dataOffset: Long,
    )

    val entries = Immutable.copyList(entries)

    /** The sink receives fresh bounded blocks; partial output stays private on CRC/size failure. */
    fun extract(name: String, consume: (ByteArray) -> Unit) {
        require(source.size == originalSize) { "The staged backup changed." }
        val entry =
            entries.singleOrNull { it.name == name }
                ?: throw IllegalArgumentException("Missing backup entry.")
        var count = 0L
        val crc = ArchiveCrc32()
        fun output(bytes: ByteArray) {
            require(bytes.size.toLong() <= entry.bytes - count) {
                "Expanded backup entry is too large."
            }
            count += bytes.size
            crc.update(bytes)
            consume(bytes)
        }
        if (entry.method == 0) {
            var offset = 0L
            while (offset < entry.compressedBytes) {
                val chunk =
                    readExact(
                        source,
                        entry.dataOffset + offset,
                        minOf(32768L, entry.compressedBytes - offset).toInt(),
                    )
                output(chunk)
                offset += chunk.size
            }
        } else
            inflateBackupEntry(
                entry.compressedBytes,
                { offset, maximum -> readExact(source, entry.dataOffset + offset, maximum) },
                ::output,
            )
        require(count == entry.bytes && crc.value() == entry.crc32 && source.size == originalSize) {
            "Backup ZIP size/checksum mismatch."
        }
    }

    companion object {
        private data class Central(
            val name: String,
            val bytes: Long,
            val compressed: Long,
            val crc: Long,
            val method: Int,
            val flags: Int,
            val offset: Long,
        )

        fun inspect(source: BackupArchiveSource, kind: Kind): BackupZipArchive {
            val limits =
                when (kind) {
                    Kind.RECORDINGS ->
                        linkedMapOf(
                            "manifest.json" to 8192L,
                            "recordings.jsonl" to RecordingArchiveRows.MAX_BYTES,
                        )
                    Kind.COMPLETE ->
                        linkedMapOf(
                            "manifest.json" to 8192L,
                            "app.json" to BackupEnvelopeJson.MAX_BYTES.toLong(),
                            "recordings.zip" to CompleteBackupManifest.MAX_RECORDINGS_ZIP_BYTES,
                        )
                }
            val size = source.size
            require(size in 22..(limits.values.sum() + 1024 * 1024)) {
                "Unsupported backup ZIP size."
            }
            val tailStart = maxOf(0, size - 65557)
            val tail = readExact(source, tailStart, (size - tailStart).toInt())
            val end =
                (tail.size - 22 downTo 0).firstOrNull {
                    u32(tail, it) == 0x06054b50L && it + 22 + u16(tail, it + 20) == tail.size
                } ?: throw IllegalArgumentException("Missing or truncated backup ZIP directory.")
            val endOffset = tailStart + end
            require(u16(tail, end + 4) == 0 && u16(tail, end + 6) == 0) {
                "Split ZIP backups are not supported."
            }
            var count = u16(tail, end + 10).toLong()
            var directorySize = u32(tail, end + 12)
            var directory = u32(tail, end + 16)
            var directoryEnd = endOffset
            require(u16(tail, end + 8).toLong() == count) { "Inconsistent ZIP entry count." }
            val locator =
                if (endOffset >= 20) readExact(source, endOffset - 20, 20) else ByteArray(20)
            val needs64 =
                count == 65535L ||
                    directorySize == 0xffffffffL ||
                    directory == 0xffffffffL ||
                    directory + directorySize != endOffset
            if (needs64 && u32(locator, 0) == 0x07064b50L) {
                require(u32(locator, 4) == 0L && u32(locator, 16) == 1L) { "Split ZIP64 backup." }
                val location = u64(locator, 8)
                val header = readExact(source, location, 56)
                require(
                    u32(header, 0) == 0x06064b50L &&
                        u64(header, 4) >= 44 &&
                        location <= endOffset - 20 - 12 &&
                        u64(header, 4) == endOffset - 20 - location - 12 &&
                        u32(header, 16) == 0L &&
                        u32(header, 20) == 0L &&
                        u64(header, 24) == u64(header, 32)
                ) {
                    "Invalid ZIP64 directory."
                }
                val n = u64(header, 32)
                val length = u64(header, 40)
                val offset = u64(header, 48)
                require(
                    (count == 65535L || count == n) &&
                        (directorySize == 0xffffffffL || directorySize == length) &&
                        (directory == 0xffffffffL || directory == offset)
                ) {
                    "ZIP64 directory disagrees."
                }
                count = n
                directorySize = length
                directory = offset
                directoryEnd = location
            }
            require(
                count == limits.size.toLong() &&
                    directory >= 0 &&
                    directorySize >= 0 &&
                    directory <= directoryEnd &&
                    directorySize == directoryEnd - directory
            ) {
                "Unsupported or incomplete backup ZIP directory."
            }
            val central = mutableListOf<Central>()
            val names = mutableSetOf<String>()
            var at = directory
            repeat(count.toInt()) {
                val h = readExact(source, at, 46)
                require(u32(h, 0) == 0x02014b50L) { "Invalid ZIP directory entry." }
                val flags = u16(h, 8)
                val method = u16(h, 10)
                require(flags and 0x080e.inv() == 0 && method in listOf(0, 8)) {
                    "Unsupported or encrypted ZIP entry."
                }
                val nameLength = u16(h, 28)
                val extraLength = u16(h, 30)
                val commentLength = u16(h, 32)
                val next = at + 46 + nameLength + extraLength + commentLength
                require(next <= directoryEnd && nameLength in 1..64) {
                    "Invalid backup ZIP name/length."
                }
                val name =
                    readExact(source, at + 46, nameLength)
                        .decodeToString(throwOnInvalidSequence = true)
                val limit =
                    limits[name] ?: throw IllegalArgumentException("Unsupported backup ZIP entry.")
                require(names.add(name)) { "Duplicate backup ZIP entry." }
                val rawBytes = u32(h, 24)
                val rawCompressed = u32(h, 20)
                val rawOffset = u32(h, 42)
                val extras = extras(readExact(source, at + 46 + nameLength, extraLength))
                val values =
                    zip64(
                        extras,
                        listOf(
                            rawBytes == 0xffffffffL,
                            rawCompressed == 0xffffffffL,
                            rawOffset == 0xffffffffL,
                        ),
                        u16(h, 34) == 65535,
                    )
                val bytes = values[0] ?: rawBytes
                val compressed = values[1] ?: rawCompressed
                val offset = values[2] ?: rawOffset
                require(
                    (u16(h, 34) == 0 || u16(h, 34) == 65535 && values[3] == 0L) &&
                        bytes <= limit &&
                        compressed <= size &&
                        offset < directory &&
                        (method != 0 || compressed == bytes)
                ) {
                    "Backup ZIP entry exceeds its supported bounds."
                }
                central += Central(name, bytes, compressed, u32(h, 16), method, flags, offset)
                at = next
            }
            require(at == directoryEnd && names == limits.keys) {
                "Unexpected ZIP directory contents."
            }
            val sorted = central.sortedBy { it.offset }
            require(sorted.first().offset == 0L) { "Unexpected ZIP prefix." }
            val entries =
                sorted.mapIndexed { index, entry ->
                    val next = if (index + 1 < sorted.size) sorted[index + 1].offset else directory
                    val h = readExact(source, entry.offset, 30)
                    require(
                        u32(h, 0) == 0x04034b50L &&
                            u16(h, 6) == entry.flags &&
                            u16(h, 8) == entry.method
                    ) {
                        "ZIP header disagrees with its directory."
                    }
                    val nameLength = u16(h, 26)
                    val extraLength = u16(h, 28)
                    val data = entry.offset + 30 + nameLength + extraLength
                    require(
                        data <= next &&
                            entry.compressed <= next - data &&
                            nameLength in 1..64 &&
                            readExact(source, entry.offset + 30, nameLength)
                                .decodeToString(throwOnInvalidSequence = true) == entry.name
                    ) {
                        "Overlapping or mismatched ZIP entries."
                    }
                    val extra =
                        extras(readExact(source, entry.offset + 30 + nameLength, extraLength))
                    val rawBytes = u32(h, 22)
                    val rawCompressed = u32(h, 18)
                    val values =
                        zip64(
                            extra,
                            listOf(rawBytes == 0xffffffffL, rawCompressed == 0xffffffffL, false),
                            false,
                        )
                    val bytes = values[0] ?: rawBytes
                    val compressed = values[1] ?: rawCompressed
                    if (entry.flags and 8 == 0)
                        require(
                            bytes == entry.bytes &&
                                compressed == entry.compressed &&
                                u32(h, 14) == entry.crc &&
                                data + compressed == next
                        ) {
                            "Stored ZIP entry metadata disagrees."
                        }
                    else {
                        require(
                            (bytes == 0L || bytes == entry.bytes) &&
                                (compressed == 0L || compressed == entry.compressed) &&
                                (u32(h, 14) == 0L || u32(h, 14) == entry.crc)
                        ) {
                            "ZIP descriptor header disagrees."
                        }
                        val length = next - data - entry.compressed
                        require(length in listOf(12L, 16L, 20L, 24L)) {
                            "Invalid ZIP data descriptor."
                        }
                        val d = readExact(source, data + entry.compressed, length.toInt())
                        val signed = length == 16L || length == 24L
                        val base = if (signed) 4 else 0
                        require(!signed || u32(d, 0) == 0x08074b50L) {
                            "Missing ZIP descriptor signature."
                        }
                        val wide = length == 20L || length == 24L
                        require(
                            u32(d, base) == entry.crc &&
                                (if (wide) u64(d, base + 4) else u32(d, base + 4)) ==
                                    entry.compressed &&
                                (if (wide) u64(d, base + 12) else u32(d, base + 8)) == entry.bytes
                        ) {
                            "ZIP descriptor disagrees."
                        }
                    }
                    Entry(entry.name, entry.bytes, entry.compressed, entry.crc, entry.method, data)
                }
            require(source.size == size) { "The staged ZIP changed." }
            return BackupZipArchive(source, size, entries)
        }

        private fun extras(bytes: ByteArray): Map<Int, ByteArray> {
            val out = mutableMapOf<Int, ByteArray>()
            var at = 0
            while (at < bytes.size) {
                require(bytes.size - at >= 4) { "Truncated ZIP extra field." }
                val id = u16(bytes, at)
                val count = u16(bytes, at + 2)
                require(count <= bytes.size - at - 4 && id !in out) {
                    "Duplicate or truncated ZIP extra field."
                }
                out[id] = bytes.copyOfRange(at + 4, at + 4 + count)
                at += 4 + count
            }
            return out
        }

        private fun zip64(
            extra: Map<Int, ByteArray>,
            wide: List<Boolean>,
            disk: Boolean,
        ): List<Long?> {
            val result = mutableListOf<Long?>()
            var at = 0
            val bytes = extra[1]
            for (needed in wide) if (needed) {
                result += u64(requireNotNull(bytes) { "Missing ZIP64 sizes." }, at)
                at += 8
            } else result += null
            result += if (disk) u32(requireNotNull(bytes) { "Missing ZIP64 disk." }, at) else null
            return result
        }

        private fun readExact(source: BackupArchiveSource, offset: Long, count: Int): ByteArray {
            require(
                offset >= 0 &&
                    count >= 0 &&
                    offset <= source.size &&
                    count.toLong() <= source.size - offset
            ) {
                "Truncated ZIP entry."
            }
            val out = ByteArray(count)
            var at = 0
            while (at < count) {
                val maximum = minOf(32768, count - at)
                val chunk = source.read(offset + at, maximum)
                require(chunk.isNotEmpty() && chunk.size <= maximum) { "Truncated ZIP input." }
                chunk.copyInto(out, at)
                at += chunk.size
            }
            return out
        }

        private fun u16(b: ByteArray, at: Int): Int {
            require(at >= 0 && at <= b.size - 2) { "Truncated ZIP number." }
            return (b[at].toInt() and 255) or ((b[at + 1].toInt() and 255) shl 8)
        }

        private fun u32(b: ByteArray, at: Int): Long =
            u16(b, at).toLong() or (u16(b, at + 2).toLong() shl 16)

        private fun u64(b: ByteArray, at: Int): Long {
            val high = u32(b, at + 4)
            require(high <= 0x7fffffffL) { "ZIP64 value exceeds supported bounds." }
            return u32(b, at) or (high shl 32)
        }
    }
}

internal class ArchiveCrc32 {
    private var crc = 0xffffffffL

    fun update(bytes: ByteArray) {
        for (byte in bytes) {
            crc = table[((crc xor (byte.toLong() and 255)) and 255).toInt()] xor (crc ushr 8)
        }
    }

    fun value(): Long = crc xor 0xffffffffL

    companion object {
        private val table =
            LongArray(256) { index ->
                var value = index.toLong()
                repeat(8) {
                    value = (value ushr 1) xor (if (value and 1 != 0L) 0xedb88320L else 0L)
                }
                value
            }
    }
}

internal expect fun inflateBackupEntry(
    size: Long,
    read: (Long, Int) -> ByteArray,
    consume: (ByteArray) -> Unit,
)
