@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.racedaydreams.storage

import kotlinx.cinterop.*
import platform.posix.*

internal actual fun openBackupScratch(privateDirectory: String): BackupScratchHandle {
    val template = (privateDirectory.trimEnd('/') + "/rdd-backup-XXXXXX\u0000").encodeToByteArray()
    val descriptor = template.usePinned { mkstemp(it.addressOf(0)) }
    check(descriptor >= 0) { "Cannot create private backup staging file." }
    val path = template.decodeToString().removeSuffix("\u0000")
    val handle = fdopen(descriptor, "w+b")
    if (handle == null) {
        close(descriptor)
        unlink(path)
        throw StorageException("Cannot open private backup staging file.")
    }
    if (unlink(path) != 0) {
        fclose(handle)
        unlink(path)
        throw StorageException("Cannot unlink private backup staging file.")
    }
    return object : BackupScratchHandle {
        override val size: Long
            get() {
                check(fseeko(handle, 0, SEEK_END) == 0) { "Cannot seek staged backup." }
                return ftello(handle).also { check(it >= 0) { "Cannot size staged backup." } }
            }

        override fun append(bytes: ByteArray) {
            check(fseeko(handle, 0, SEEK_END) == 0) { "Cannot seek staged backup." }
            val count =
                bytes.usePinned { fwrite(it.addressOf(0), 1u, bytes.size.toULong(), handle) }
            check(count == bytes.size.toULong()) { "Incomplete staging write." }
        }

        override fun flush() {
            check(fflush(handle) == 0) { "Cannot flush staged backup." }
        }

        override fun read(offset: Long, count: Int): ByteArray {
            check(fseeko(handle, offset, SEEK_SET) == 0) { "Cannot seek staged backup." }
            return ByteArray(count).also { bytes ->
                val actual = bytes.usePinned { fread(it.addressOf(0), 1u, count.toULong(), handle) }
                check(actual == count.toULong()) { "Incomplete staging read." }
            }
        }

        override fun close() {
            check(fclose(handle) == 0) { "Cannot close staged backup." }
        }
    }
}
