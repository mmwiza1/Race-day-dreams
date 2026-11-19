package org.racedaydreams.storage

import java.util.zip.Inflater

internal actual fun inflateBackupEntry(
    size: Long,
    read: (Long, Int) -> ByteArray,
    consume: (ByteArray) -> Unit,
) {
    val inflater = Inflater(true)
    try {
        val output = ByteArray(32768)
        var readAt = 0L
        while (!inflater.finished()) {
            if (inflater.needsInput() && readAt < size) {
                val maximum = minOf(32768L, size - readAt).toInt()
                val input = read(readAt, maximum)
                require(input.isNotEmpty() && input.size <= maximum) {
                    "Truncated deflated backup."
                }
                readAt += input.size
                inflater.setInput(input)
            }
            val before = inflater.bytesRead
            val count = inflater.inflate(output)
            if (count > 0) consume(output.copyOf(count))
            require(!inflater.needsDictionary()) { "Unsupported ZIP dictionary." }
            require(count > 0 || inflater.bytesRead > before || inflater.finished()) {
                "Truncated or stalled deflated backup."
            }
        }
        require(inflater.bytesRead == size) { "Deflated backup has trailing data." }
    } finally {
        inflater.end()
    }
}
