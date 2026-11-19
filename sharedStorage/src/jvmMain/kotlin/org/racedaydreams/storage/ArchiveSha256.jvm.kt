package org.racedaydreams.storage

import java.security.MessageDigest

internal actual fun archiveSha256(): ArchiveSha256 = JvmArchiveSha256()

private class JvmArchiveSha256 : ArchiveSha256 {
    private val digest = MessageDigest.getInstance("SHA-256")
    private var closed = false

    override fun update(bytes: ByteArray, offset: Int, length: Int) {
        check(!closed) { "Archive digest is closed." }
        digest.update(bytes, offset, length)
    }

    override fun finish(): ByteArray {
        check(!closed) { "Archive digest is closed." }
        return try {
            digest.digest()
        } finally {
            close()
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        digest.reset()
    }
}
