@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.racedaydreams.coach.documents

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.racedaydreams.domain.storage.DetailRetention
import org.racedaydreams.storage.BackupArchiveSource
import org.racedaydreams.storage.BackupEnvelopeJson
import org.racedaydreams.storage.BackupStagingFile
import org.racedaydreams.storage.LocalBackupOperations
import org.racedaydreams.storage.LocalBackupOperations.ReviewedBackup
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSThread
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerModeImport
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.posix.*

/** Stages only a private copy. The system picker lets the athlete choose the destination. */
class AppleBackupDocuments(
    private val exports: LocalBackupOperations,
    private val io: CoroutineDispatcher,
) {
    enum class Kind(val filename: String) {
        APP("Race-Day-Dreams-app.json"),
        COMPLETE("Race-Day-Dreams-complete.zip"),
    }

    enum class Outcome {
        SAVED,
        CANCELLED,
    }

    private var activePicker: Picker? = null
    private var staging = false
    private var importPicker: ImportPicker? = null

    suspend fun export(kind: Kind, presenter: UIViewController, complete: (Outcome) -> Unit) {
        check(NSThread.isMainThread)
        check(
            !staging &&
                activePicker == null &&
                importPicker == null &&
                presenter.presentedViewController == null
        ) {
            "Finish the open document export first."
        }
        check(presenter.viewIfLoaded?.window != null) { "Open the app before exporting a backup." }
        staging = true
        var document: StagedDocument? = null
        try {
            withContext(io) { document = stage(kind) }
            val staged = checkNotNull(document)
            check(
                presenter.viewIfLoaded?.window != null && presenter.presentedViewController == null
            ) {
                "The export screen is no longer available."
            }
            val picker = Picker(staged, complete)
            activePicker = picker
            try {
                presenter.presentViewController(
                    picker.controller,
                    animated = true,
                    completion = null,
                )
            } catch (failure: Throwable) {
                activePicker = null
                throw failure
            }
        } catch (failure: Throwable) {
            document?.remove()
            throw failure
        } finally {
            staging = false
        }
    }

    fun chooseBackup(presenter: UIViewController, selected: (NSURL?) -> Unit) {
        check(NSThread.isMainThread)
        check(
            !staging &&
                activePicker == null &&
                importPicker == null &&
                presenter.viewIfLoaded?.window != null &&
                presenter.presentedViewController == null
        ) {
            "Finish the open document action first."
        }
        val picker = ImportPicker(selected)
        importPicker = picker
        try {
            presenter.presentViewController(picker.controller, animated = true, completion = null)
        } catch (failure: Throwable) {
            importPicker = null
            throw failure
        }
    }

    suspend fun review(url: NSURL): ReviewedBackup {
        var reviewed: ReviewedBackup? = null
        try {
            withContext(io) {
                val path = requireNotNull(url.path) { "This backup has no readable local file." }
                val file = checkNotNull(fopen(path, "rb")) { "Could not read the backup." }
                try {
                    check(fseeko(file, 0, SEEK_END) == 0) { "Could not size the backup." }
                    val fileSize = ftello(file)
                    require(fileSize in 1..BackupStagingFile.MAX_BYTES) {
                        "This backup exceeds the supported size."
                    }
                    val source =
                        object : BackupArchiveSource {
                            override val size = fileSize

                            override fun read(offset: Long, maximum: Int): ByteArray {
                                require(offset in 0..size && maximum >= 0)
                                val count = minOf(maximum.toLong(), size - offset).toInt()
                                if (count == 0) return ByteArray(0)
                                check(fseeko(file, offset, SEEK_SET) == 0) {
                                    "Could not read the backup."
                                }
                                return ByteArray(count).also { bytes ->
                                    val copied =
                                        bytes.usePinned {
                                            fread(it.addressOf(0), 1u, count.toULong(), file)
                                        }
                                    check(copied == count.toULong()) {
                                        "The backup changed while it was being read."
                                    }
                                }
                            }
                        }
                    val signature = source.read(0, 4)
                    val zip = signature.contentEquals(byteArrayOf(0x50, 0x4b, 3, 4))
                    reviewed =
                        if (zip) {
                            exports.reviewComplete(source, NSTemporaryDirectory())
                        } else {
                            require(fileSize <= BackupEnvelopeJson.MAX_BYTES) {
                                "Choose an app backup no larger than 32 MiB."
                            }
                            val bytes = source.read(0, fileSize.toInt())
                            check(fgetc(file) == EOF && ferror(file) == 0) {
                                "The backup changed while it was being read."
                            }
                            exports.reviewApp(bytes)
                        }
                } finally {
                    fclose(file)
                }
            }
            return checkNotNull(reviewed)
        } catch (failure: Throwable) {
            try {
                reviewed?.close()
            } catch (close: Throwable) {
                failure.addSuppressed(close)
            }
            throw failure
        }
    }

    suspend fun retentionPreferences() = withContext(io) { exports.retentionPreferences() }

    suspend fun acknowledgeRetention(policy: DetailRetention.Policy) =
        withContext(io) { exports.acknowledgeRetention(policy) }

    suspend fun previewRetention(policy: DetailRetention.Policy) =
        withContext(io) { exports.previewRetention(policy) }

    suspend fun cleanRetention(preview: DetailRetention.Preview, onProgress: (Int) -> Unit) {
        val approval = withContext(io) { exports.approveRetentionCleanup(preview) }
        var removed = 0
        for (recording in approval.preview().recordings()) {
            yield()
            val changed =
                withContext(io) { exports.cleanRecordingDetail(approval, recording.sessionId()) }
            if (changed) onProgress(++removed)
        }
    }

    suspend fun recordingInventory() = withContext(io) { exports.recordingInventory() }

    fun restoreReviewed(reviewed: ReviewedBackup) = exports.restoreReviewed(reviewed)

    private inner class ImportPicker(private val selected: (NSURL?) -> Unit) :
        NSObject(), UIDocumentPickerDelegateProtocol {
        val controller =
            UIDocumentPickerViewController(
                    documentTypes = listOf("public.json", "public.zip-archive"),
                    inMode = UIDocumentPickerModeImport,
                )
                .apply { delegate = this@ImportPicker }

        override fun documentPicker(
            controller: UIDocumentPickerViewController,
            didPickDocumentsAtURLs: List<*>,
        ) {
            finish(didPickDocumentsAtURLs.singleOrNull() as? NSURL)
        }

        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
            finish(null)
        }

        private fun finish(url: NSURL?) {
            if (importPicker !== this) return
            importPicker = null
            selected(url)
        }
    }

    private inner class Picker(
        private val staged: StagedDocument,
        private val complete: (Outcome) -> Unit,
    ) : NSObject(), UIDocumentPickerDelegateProtocol {
        val controller =
            UIDocumentPickerViewController(
                    forExportingURLs = listOf(NSURL.fileURLWithPath(staged.path)),
                    asCopy = true,
                )
                .apply { delegate = this@Picker }

        override fun documentPicker(
            controller: UIDocumentPickerViewController,
            didPickDocumentsAtURLs: List<*>,
        ) {
            finish(if (didPickDocumentsAtURLs.isEmpty()) Outcome.CANCELLED else Outcome.SAVED)
        }

        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
            finish(Outcome.CANCELLED)
        }

        private fun finish(outcome: Outcome) {
            if (activePicker !== this) return
            activePicker = null
            staged.remove()
            complete(outcome)
        }
    }

    private fun stage(kind: Kind): StagedDocument {
        val directory = NSTemporaryDirectory().trimEnd('/') + "/rdd-export-" + NSUUID().UUIDString
        check(mkdir(directory, 448u.toUShort()) == 0) { "Cannot create private export directory." }
        val document = StagedDocument(directory, "$directory/${kind.filename}")
        try {
            val stream = checkNotNull(fopen(document.path, "wb")) { "Cannot open private export." }
            try {
                val write: (ByteArray) -> Unit = { bytes ->
                    if (bytes.isNotEmpty()) {
                        val count =
                            bytes.usePinned {
                                fwrite(it.addressOf(0), 1u, bytes.size.toULong(), stream)
                            }
                        check(count == bytes.size.toULong()) {
                            "Could not write the complete backup."
                        }
                    }
                }
                when (kind) {
                    Kind.APP -> exports.writeApp(write)
                    Kind.COMPLETE -> exports.writeComplete(directory, write)
                }
                check(fflush(stream) == 0 && fsync(fileno(stream)) == 0) {
                    "Could not flush the backup."
                }
            } catch (failure: Throwable) {
                fclose(stream)
                throw failure
            }
            check(fclose(stream) == 0) { "Could not close the backup." }
            return document
        } catch (failure: Throwable) {
            document.remove()
            throw failure
        }
    }

    private data class StagedDocument(val directory: String, val path: String) {
        fun remove() {
            unlink(path)
            rmdir(directory)
        }
    }
}
