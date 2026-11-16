package org.racedaydreams.storage

/**
 * Validates the actual complete-package container, both measured payload hashes and BOTH inner
 * documents. Raw ZIP bytes stay in bounded private scratch storage for a future protected restore.
 * This object neither replaces application data nor reconciles recordings. Close after use.
 */
class InspectedBackupPackage
private constructor(
    val application: BackupEnvelopeJson.Checked<UserBackupDataJson.Snapshot>,
    val recordings: RecordingArchiveRows.Snapshot,
    private val raw: BackupStagingFile,
) {
    fun recordingArchive(): BackupArchiveSource = raw.also { it.size }

    fun close() = raw.close()

    companion object {
        fun inspect(source: BackupArchiveSource, privateDirectory: String): InspectedBackupPackage {
            val container = BackupZipArchive.inspect(source, BackupZipArchive.Kind.COMPLETE)
            val manifest = CompleteBackupManifest.decode(entryBytes(container, "manifest.json"))
            val raw =
                BackupStagingFile.create(
                    privateDirectory,
                    manifest.files.getValue("recordings.zip").bytes,
                )
            try {
                val appBytes =
                    ByteArray(container.entries.single { it.name == "app.json" }.bytes.toInt())
                var offset = 0
                val appEvidence =
                    measured(container, "app.json") { bytes ->
                        bytes.copyInto(appBytes, offset)
                        offset += bytes.size
                    }
                val rawEvidence = measured(container, "recordings.zip", raw::append)
                manifest.verify(listOf(appEvidence, rawEvidence))
                val application = UserBackupDataJson.inspect(appBytes)
                val archive = BackupZipArchive.inspect(raw.seal(), BackupZipArchive.Kind.RECORDINGS)
                val recordingManifest =
                    RecordingArchiveManifest.decode(entryBytes(archive, "manifest.json"))
                val reader = RecordingArchiveReader(recordingManifest)
                val recordings =
                    try {
                        archive.extract("recordings.jsonl", reader::accept)
                        reader.finish()
                    } finally {
                        reader.close()
                    }
                return InspectedBackupPackage(application, recordings, raw)
            } catch (error: Throwable) {
                try {
                    raw.close()
                } catch (cleanup: Throwable) {
                    error.addSuppressed(cleanup)
                }
                throw error
            }
        }

        private fun entryBytes(zip: BackupZipArchive, name: String): ByteArray {
            val entry = zip.entries.single { it.name == name }
            require(entry.bytes <= 8192) { "Oversized backup manifest." }
            val out = ByteArray(entry.bytes.toInt())
            var offset = 0
            zip.extract(name) {
                it.copyInto(out, offset)
                offset += it.size
            }
            return out
        }

        private fun measured(
            zip: BackupZipArchive,
            name: String,
            consume: (ByteArray) -> Unit,
        ): CompleteBackupManifest.Receipt {
            val digest = archiveSha256()
            try {
                var size = 0L
                zip.extract(name) {
                    digest.update(it, 0, it.size)
                    size += it.size
                    consume(it)
                }
                val hash =
                    digest.finish().joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
                return CompleteBackupManifest.Receipt(
                    name,
                    CompleteBackupManifest.Entry(size, hash),
                )
            } finally {
                digest.close()
            }
        }
    }
}
