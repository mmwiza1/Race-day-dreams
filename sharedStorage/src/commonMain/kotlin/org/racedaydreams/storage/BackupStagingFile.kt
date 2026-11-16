package org.racedaydreams.storage

/**
 * Bounded private scratch evidence, owned by one serialised backup operation. The directory must be
 * app-private. Files are unlinked on opening and closed on cancellation, failure or completion;
 * they are never database files or durable backups. Seal before inspecting or reading.
 */
class BackupStagingFile
private constructor(private val file: BackupScratchHandle, private val maximum: Long) :
    BackupArchiveSource {
    private var written = 0L
    private var sealed = false
    private var closed = false

    override val size: Long
        get() = guarded {
            readable()
            file.size.also { require(it == written) { "Staged backup size changed." } }
        }

    fun append(bytes: ByteArray) = guarded {
        check(!sealed) { "Staged backup is sealed." }
        require(bytes.size <= BLOCK_BYTES && bytes.size.toLong() <= maximum - written) {
            "Staged backup exceeds its supported limit."
        }
        if (bytes.isNotEmpty()) file.append(bytes)
        written += bytes.size
    }

    fun seal(): BackupArchiveSource = guarded {
        if (!sealed) {
            file.flush()
            require(file.size == written) { "Incomplete staged backup." }
            sealed = true
        }
        this
    }

    override fun read(offset: Long, maximum: Int): ByteArray = guarded {
        readable()
        require(offset >= 0 && offset <= size && maximum in 0..BLOCK_BYTES) {
            "Invalid staged backup read."
        }
        val count = minOf(maximum.toLong(), size - offset).toInt()
        if (count == 0) ByteArray(0)
        else
            file.read(offset, count).also {
                require(it.size == count) { "Truncated staging file." }
            }
    }

    private fun readable() {
        check(!closed && sealed) { "Staged backup is closed or not yet sealed." }
    }

    private fun <T> guarded(block: () -> T): T {
        check(!closed) { "Staged backup is closed." }
        try {
            return block()
        } catch (error: Throwable) {
            try {
                close()
            } catch (cleanup: Throwable) {
                error.addSuppressed(cleanup)
            }
            throw error
        }
    }

    fun close() {
        if (closed) return
        closed = true
        file.close()
    }

    companion object {
        const val BLOCK_BYTES = 32768
        const val MAX_BYTES =
            CompleteBackupManifest.MAX_RECORDINGS_ZIP_BYTES +
                BackupEnvelopeJson.MAX_BYTES +
                8192L +
                1024 * 1024

        fun create(privateDirectory: String, maximumBytes: Long): BackupStagingFile {
            require(privateDirectory.isNotBlank() && '\u0000' !in privateDirectory)
            require(maximumBytes in 0..MAX_BYTES) { "Unsupported staging limit." }
            return BackupStagingFile(openBackupScratch(privateDirectory), maximumBytes)
        }
    }
}

internal interface BackupScratchHandle {
    val size: Long

    fun append(bytes: ByteArray)

    fun flush()

    fun read(offset: Long, count: Int): ByteArray

    fun close()
}

internal expect fun openBackupScratch(privateDirectory: String): BackupScratchHandle
