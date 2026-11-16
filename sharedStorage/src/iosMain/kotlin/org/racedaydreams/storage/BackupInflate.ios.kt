@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.racedaydreams.storage

import kotlinx.cinterop.*
import platform.posix.memset
import platform.zlib.*

internal actual fun inflateBackupEntry(
    size: Long,
    read: (Long, Int) -> ByteArray,
    consume: (ByteArray) -> Unit,
) = memScoped {
    val stream = alloc<z_stream>()
    memset(stream.ptr, 0, sizeOf<z_stream>().toULong())
    require(inflateInit2_(stream.ptr, -15, zlibVersion(), sizeOf<z_stream>().toInt()) == Z_OK) {
        "Cannot initialise backup decompression."
    }
    try {
        var readAt = 0L
        var input = ByteArray(0)
        var offset = 0
        var done = false
        val output = ByteArray(32768)
        while (!done) {
            if (offset == input.size && readAt < size) {
                val maximum = minOf(32768L, size - readAt).toInt()
                input = read(readAt, maximum)
                require(input.isNotEmpty() && input.size <= maximum) {
                    "Truncated deflated backup."
                }
                offset = 0
                readAt += input.size
            }
            val available = input.size - offset
            input.usePinned { incoming ->
                output.usePinned { outgoing ->
                    stream.next_in =
                        if (available == 0) null else incoming.addressOf(offset).reinterpret()
                    stream.avail_in = available.toUInt()
                    stream.next_out = outgoing.addressOf(0).reinterpret()
                    stream.avail_out = output.size.toUInt()
                    val code = inflate(stream.ptr, Z_NO_FLUSH)
                    val consumed = available - stream.avail_in.toInt()
                    val produced = output.size - stream.avail_out.toInt()
                    offset += consumed
                    if (produced > 0) consume(output.copyOf(produced))
                    require(code == Z_OK || code == Z_STREAM_END || code == Z_BUF_ERROR) {
                        "Invalid deflated backup."
                    }
                    done = code == Z_STREAM_END
                    require(done || consumed > 0 || produced > 0) {
                        "Truncated or stalled deflated backup."
                    }
                }
            }
        }
        require(readAt - (input.size - offset) == size) { "Deflated backup has trailing data." }
    } finally {
        stream.next_in = null
        stream.next_out = null
        inflateEnd(stream.ptr)
    }
}
