package org.racedaydreams.storage

/**
 * Streams original-format ZIP containers without platform compression dependencies. Stored entries
 * fit every existing archive budget. Sources must be sealed private evidence; output MUST remain
 * private until all document/hash validation completes. This writes no database or public file.
 */
object BackupZipWriter {
    data class Source(val name: String, val bytes: BackupArchiveSource)

    private data class Prepared(
        val source: Source,
        val size: Long,
        val crc: Long,
        val offset: Long,
    )

    fun write(kind: BackupZipArchive.Kind, sources: List<Source>, consume: (ByteArray) -> Unit) {
        val inputs = sources.toList()
        val limits = backupZipLimits(kind)
        require(inputs.size == limits.size && inputs.map { it.name }.toSet() == limits.keys) {
            "Missing, duplicate or unsupported backup ZIP entries."
        }
        var next = 0L
        val prepared =
            inputs.map { entry ->
                val size = entry.bytes.size
                require(size in 0..limits.getValue(entry.name)) {
                    "Backup ZIP entry exceeds its limit."
                }
                val crc = scan(entry.bytes, size) {}
                Prepared(entry, size, crc, next).also { next += 30 + entry.name.length + size }
            }
        val directory = next
        for (entry in prepared) {
            val name = entry.source.name.encodeToByteArray()
            val header = ByteArray(30 + name.size)
            put32(header, 0, 0x04034b50)
            put16(header, 4, 20)
            put16(header, 6, 0x0800)
            put16(header, 12, 0x21) // DOS date 1 January 1980; no fabricated recording timestamp.
            put32(header, 14, entry.crc)
            put32(header, 18, entry.size)
            put32(header, 22, entry.size)
            put16(header, 26, name.size)
            name.copyInto(header, 30)
            consume(header)
            val actual = scan(entry.source.bytes, entry.size) { consume(it.copyOf()) }
            require(actual == entry.crc) { "Staged backup changed while writing ZIP." }
        }
        var directorySize = 0L
        for (entry in prepared) {
            val name = entry.source.name.encodeToByteArray()
            val header = ByteArray(46 + name.size)
            put32(header, 0, 0x02014b50)
            put16(header, 4, 20)
            put16(header, 6, 20)
            put16(header, 8, 0x0800)
            put16(header, 14, 0x21)
            put32(header, 16, entry.crc)
            put32(header, 20, entry.size)
            put32(header, 24, entry.size)
            put16(header, 28, name.size)
            put32(header, 42, entry.offset)
            name.copyInto(header, 46)
            consume(header)
            directorySize += header.size
        }
        val end = ByteArray(22)
        put32(end, 0, 0x06054b50)
        put16(end, 8, prepared.size)
        put16(end, 10, prepared.size)
        put32(end, 12, directorySize)
        put32(end, 16, directory)
        consume(end)
    }

    private fun scan(source: BackupArchiveSource, size: Long, consume: (ByteArray) -> Unit): Long {
        require(source.size == size) { "Staged ZIP source size changed." }
        val crc = ArchiveCrc32()
        var offset = 0L
        while (offset < size) {
            val maximum = minOf(32768L, size - offset).toInt()
            val bytes = source.read(offset, maximum)
            require(bytes.isNotEmpty() && bytes.size <= maximum) { "Incomplete staged ZIP source." }
            crc.update(bytes)
            consume(bytes)
            offset += bytes.size
        }
        require(source.size == size) { "Staged ZIP source size changed." }
        return crc.value()
    }

    private fun put16(bytes: ByteArray, offset: Int, value: Int) {
        require(value in 0..65535)
        bytes[offset] = value.toByte()
        bytes[offset + 1] = (value ushr 8).toByte()
    }

    private fun put32(bytes: ByteArray, offset: Int, value: Long) {
        require(value in 0..0xffffffffL)
        for (index in 0..3) bytes[offset + index] = (value ushr (index * 8)).toByte()
    }
}
