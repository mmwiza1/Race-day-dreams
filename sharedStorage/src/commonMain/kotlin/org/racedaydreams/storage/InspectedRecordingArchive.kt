package org.racedaydreams.storage

/**
 * Fully validated raw1–3 archive held in immutable private staging, never a provider-owned file.
 * Inspection has no database side effects. Internal replay is bounded and rechecks the complete
 * graph/hash/ZIP evidence; a future restore must wrap ALL callbacks and finish in one transaction.
 */
class InspectedRecordingArchive
private constructor(
    val snapshot: RecordingArchiveRows.Snapshot,
    private val manifest: RecordingArchiveManifest,
    private val raw: BackupStagingFile,
) {
    fun recordingArchive(): BackupArchiveSource = raw.also { it.size }

    fun close() = raw.close()

    internal fun replay(consume: (String) -> Unit): RecordingArchiveRows.Snapshot {
        val archive = BackupZipArchive.inspect(raw, BackupZipArchive.Kind.RECORDINGS)
        val current = RecordingArchiveManifest.decode(manifestBytes(archive))
        require(current == manifest) { "Inspected recording manifest changed; nothing committed." }
        val reader = RecordingArchiveReader.replay(current, consume)
        return try {
            archive.extract("recordings.jsonl", reader::accept)
            reader.finish().also {
                require(it.version == snapshot.version && it.sessions == snapshot.sessions) {
                    "Inspected recording graph changed; nothing committed."
                }
            }
        } finally {
            reader.close()
        }
    }

    companion object {
        fun inspect(
            source: BackupArchiveSource,
            privateDirectory: String,
        ): InspectedRecordingArchive {
            val size = source.size
            require(size in 0..CompleteBackupManifest.MAX_RECORDINGS_ZIP_BYTES) {
                "Recording archive exceeds its supported size."
            }
            val staged = BackupStagingFile.create(privateDirectory, size)
            try {
                var offset = 0L
                while (offset < size) {
                    val count = minOf(BackupStagingFile.BLOCK_BYTES.toLong(), size - offset).toInt()
                    val bytes = source.read(offset, count)
                    require(bytes.size == count) { "Truncated recording archive." }
                    staged.append(bytes)
                    offset += count
                }
                require(source.size == size) { "Recording archive size changed during inspection." }
                staged.seal()
                return inspectSealed(staged)
            } catch (error: Throwable) {
                try {
                    staged.close()
                } catch (cleanup: Throwable) {
                    error.addSuppressed(cleanup)
                }
                throw error
            }
        }

        /** Takes ownership on success; the caller closes on failure. No second dense-file copy. */
        internal fun inspectSealed(raw: BackupStagingFile): InspectedRecordingArchive {
            val archive = BackupZipArchive.inspect(raw, BackupZipArchive.Kind.RECORDINGS)
            val manifest = RecordingArchiveManifest.decode(manifestBytes(archive))
            val reader = RecordingArchiveReader(manifest)
            val snapshot =
                try {
                    archive.extract("recordings.jsonl", reader::accept)
                    reader.finish()
                } finally {
                    reader.close()
                }
            return InspectedRecordingArchive(snapshot, manifest, raw)
        }

        private fun manifestBytes(archive: BackupZipArchive): ByteArray {
            val entry = archive.entries.single { it.name == "manifest.json" }
            require(entry.bytes <= RecordingArchiveManifest.MAX_BYTES) {
                "Oversized recording manifest."
            }
            val bytes = ByteArray(entry.bytes.toInt())
            var offset = 0
            archive.extract("manifest.json") {
                it.copyInto(bytes, offset)
                offset += it.size
            }
            return bytes
        }
    }
}
