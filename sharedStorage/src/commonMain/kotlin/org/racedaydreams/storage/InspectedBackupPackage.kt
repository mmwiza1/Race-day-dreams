package org.racedaydreams.storage

import kotlinx.serialization.json.*

/**
 * Validates the actual complete-package container, both measured payload hashes and BOTH inner
 * documents. Raw ZIP bytes stay in bounded private scratch storage for a future protected restore.
 * This object neither replaces application data nor reconciles recordings. Close after use.
 */
class InspectedBackupPackage
private constructor(
    val application: BackupEnvelopeJson.Checked<UserBackupDataJson.Snapshot>,
    val recordings: RecordingArchiveRows.Snapshot,
    private val raw: InspectedRecordingArchive,
) {
    fun recordingArchive(): BackupArchiveSource = raw.recordingArchive()

    internal fun replayRecordings(consume: (String) -> Unit) = raw.replay(consume)

    fun close() = raw.close()

    /**
     * Two valid documents may still contradict each other. Validate the cross-file evidence too.
     */
    private fun requireConsistentApplication() {
        val parent = application.snapshot
        val activities = parent.core.activities.associateBy { it.id() }
        val contexts =
            parent.support
                ?.records
                .orEmpty()
                .filter { it.kind == SupportArchiveJson.Kind.EXECUTION }
                .associateBy { it.id }
        val removals = parent.retention.removals.associateBy { it.sessionId() }
        for ((id, session) in recordings.sessions) {
            if (application.version >= 6)
                require(session.hasContext == (id in contexts)) {
                    "Complete backup application and raw workout contexts disagree."
                }
            if (application.version >= 13)
                require(removals[id] == session.removal) {
                    "Complete backup application and raw removal provenance disagree."
                }
        }
        raw.replay { line ->
            val row = Json.parseToJsonElement(line.removePrefix("\uFEFF")).jsonObject
            val id = row.getValue("id").jsonPrimitive.content
            when (row.getValue("kind").jsonPrimitive.content) {
                "activity" -> {
                    val activity = ActivityJson.decode(row.getValue("payload").jsonObject)
                    val summary =
                        requireNotNull(activities[activity.id()]) {
                            "Complete backup is missing an archived activity summary."
                        }
                    require(
                        recordedActivityMeasurements(summary) ==
                            recordedActivityMeasurements(activity)
                    ) {
                        "Complete backup application and raw measured summaries disagree."
                    }
                }
                "context" -> {
                    val original = contexts[id]
                    require(original == null || original.payload == row.getValue("payload")) {
                        "Complete backup application and raw workout instructions disagree."
                    }
                }
            }
        }
    }

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
                raw.seal()
                val inspected = InspectedRecordingArchive.inspectSealed(raw)
                return InspectedBackupPackage(application, inspected.snapshot, inspected).also {
                    it.requireConsistentApplication()
                }
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
