package org.racedaydreams.storage

import java.io.File
import java.io.RandomAccessFile

internal actual fun openBackupScratch(privateDirectory: String): BackupScratchHandle {
    val path = File.createTempFile("rdd-backup-", ".stage", File(privateDirectory))
    var opened: RandomAccessFile? = null
    try {
        val handle = RandomAccessFile(path, "rw")
        opened = handle
        check(path.delete()) { "Cannot unlink private backup staging file." }
        return object : BackupScratchHandle {
            override val size: Long
                get() = handle.length()

            override fun append(bytes: ByteArray) {
                handle.seek(handle.length())
                handle.write(bytes)
            }

            override fun flush() = Unit // RandomAccessFile writes are not buffered in user space.

            override fun read(offset: Long, count: Int): ByteArray {
                handle.seek(offset)
                return ByteArray(count).also(handle::readFully)
            }

            override fun close() = handle.close()
        }
    } catch (error: Throwable) {
        try {
            opened?.close()
        } catch (cleanup: Throwable) {
            error.addSuppressed(cleanup)
        }
        path.delete()
        throw error
    }
}
