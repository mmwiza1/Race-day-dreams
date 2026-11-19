@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.racedaydreams.storage

import kotlinx.cinterop.*
import platform.CoreCrypto.*

internal actual fun archiveSha256(): ArchiveSha256 = AppleArchiveSha256()

private class AppleArchiveSha256 : ArchiveSha256 {
    private val context = nativeHeap.alloc<CC_SHA256_CTX>()
    private var closed = false

    init {
        if (CC_SHA256_Init(context.ptr) != 1) {
            nativeHeap.free(context.ptr)
            closed = true
            throw StorageException("Cannot initialise recording archive checksum.")
        }
    }

    override fun update(bytes: ByteArray, offset: Int, length: Int) {
        check(!closed) { "Archive digest is closed." }
        require(offset >= 0 && length >= 0 && offset <= bytes.size - length)
        if (length == 0) return
        bytes.usePinned {
            check(CC_SHA256_Update(context.ptr, it.addressOf(offset), length.toUInt()) == 1) {
                "Cannot update recording archive checksum."
            }
        }
    }

    override fun finish(): ByteArray {
        check(!closed) { "Archive digest is closed." }
        return try {
            ByteArray(32).also { bytes ->
                bytes.usePinned {
                    check(CC_SHA256_Final(it.addressOf(0).reinterpret(), context.ptr) == 1) {
                        "Cannot finish recording archive checksum."
                    }
                }
            }
        } finally {
            close()
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        nativeHeap.free(context.ptr)
    }
}
