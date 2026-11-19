package org.racedaydreams.storage

import kotlin.test.*

abstract class BackupStagingFileContract {
    abstract fun directory(): String

    private fun packageSource(bytes: ByteArray) =
        object : BackupArchiveSource {
            override val size = bytes.size.toLong()

            override fun read(offset: Long, maximum: Int): ByteArray =
                bytes.copyOfRange(offset.toInt(), minOf(bytes.size, offset.toInt() + maximum))
        }

    @Test
    fun completePackageValidatesBothDocumentsAndRetainsOnlyPrivateRawBytesUntilClosed() {
        val checked =
            InspectedBackupPackage.inspect(
                packageSource(CompletePackageFixtures.valid),
                directory(),
            )
        val raw = checked.recordingArchive()
        try {
            assertEquals(1, checked.application.version)
            assertTrue(checked.application.snapshot.core.activities.isEmpty())
            assertEquals(1, checked.recordings.sessions.size)
            assertEquals("RECOVERABLE", checked.recordings.sessions.getValue("run").state)
            val zip = BackupZipArchive.inspect(raw, BackupZipArchive.Kind.RECORDINGS)
            assertEquals(2, zip.entries.size)
        } finally {
            checked.close()
            checked.close()
        }
        assertFails { raw.size }
        assertFails { checked.recordingArchive() }
    }

    @Test
    fun validZipCrcCannotHideIncorrectCompletePayloadHashesOrLengths() {
        for (bytes in
            listOf(
                CompletePackageFixtures.wrongHash,
                CompletePackageFixtures.wrongSize,
            )) assertFails { InspectedBackupPackage.inspect(packageSource(bytes), directory()) }
    }

    @Test
    fun correctPayloadHashesCannotBypassApplicationGraphOrFutureVersionValidation() {
        for (bytes in
            listOf(
                CompletePackageFixtures.invalidApp,
                CompletePackageFixtures.futureApp,
            )) assertFails { InspectedBackupPackage.inspect(packageSource(bytes), directory()) }
    }

    @Test
    fun correctOuterHashesCannotBypassRawManifestOrSessionGraphValidation() {
        for (bytes in
            listOf(
                CompletePackageFixtures.invalidRawHash,
                CompletePackageFixtures.invalidRawGraph,
            )) assertFails { InspectedBackupPackage.inspect(packageSource(bytes), directory()) }
    }

    @Test
    fun failedPackageInspectionsCannotReturnPartialEvidenceOrBreakLaterValidInspection() {
        repeat(8) {
            assertFails {
                InspectedBackupPackage.inspect(
                    packageSource(CompletePackageFixtures.invalidApp),
                    directory(),
                )
            }
        }
        assertFails {
            InspectedBackupPackage.inspect(
                packageSource(CompletePackageFixtures.valid),
                directory() + "/missing/parent",
            )
        }
        val checked =
            InspectedBackupPackage.inspect(
                packageSource(CompletePackageFixtures.valid),
                directory(),
            )
        try {
            assertEquals(1, checked.recordings.sessions.size)
        } finally {
            checked.close()
        }
    }

    @Test
    fun sealedFilesPreserveBoundedReadsWithoutExposingWritableSources() {
        val file = BackupStagingFile.create(directory(), 70000)
        try {
            val bytes = ByteArray(70000) { (it % 251).toByte() }
            for (offset in bytes.indices step 32768) file.append(
                bytes.copyOfRange(offset, minOf(bytes.size, offset + 32768))
            )
            val source = file.seal()
            assertSame(source, file.seal())
            assertEquals(70000L, source.size)
            assertContentEquals(bytes.copyOfRange(32760, 65528), source.read(32760, 32768))
            assertContentEquals(bytes.copyOfRange(69995, 70000), source.read(69995, 32768))
            assertTrue(source.read(70000, 32768).isEmpty())
            assertTrue(source.read(0, 0).isEmpty())
        } finally {
            file.close()
        }
    }

    @Test
    fun budgetAndBlockOverflowClosePartialStagingBeforeItCanBeUsed() {
        for ((limit, overflow) in listOf(3L to byteArrayOf(4), 100000L to ByteArray(32769))) {
            val file = BackupStagingFile.create(directory(), limit)
            file.append(byteArrayOf(1, 2, 3))
            assertFails { file.append(overflow) }
            assertFails { file.seal() }
            assertFails { file.read(0, 1) }
            file.close()
        }
        val empty = BackupStagingFile.create(directory(), 0)
        try {
            empty.append(ByteArray(0))
            assertEquals(0L, empty.seal().size)
        } finally {
            empty.close()
        }
    }

    @Test
    fun readingBeforeSealOrWritingAfterSealCannotLeaveUsablePartialEvidence() {
        val unsealed = BackupStagingFile.create(directory(), 10)
        assertFails { unsealed.read(0, 1) }
        assertFails { unsealed.append(byteArrayOf(1)) }
        unsealed.close()
        val sealed = BackupStagingFile.create(directory(), 10)
        sealed.append(byteArrayOf(1))
        sealed.seal()
        assertFails { sealed.append(byteArrayOf(2)) }
        assertFails { sealed.size }
        sealed.close()
    }

    @Test
    fun privateStagingFeedsRealZipAndRawGraphValidationWithoutWholeArchiveAllocation() {
        val zipFile = BackupStagingFile.create(directory(), 10000)
        val rawFile = BackupStagingFile.create(directory(), RecordingArchiveRows.MAX_BYTES)
        try {
            zipFile.append(BackupZipFixtures.deflated)
            val zip = BackupZipArchive.inspect(zipFile.seal(), BackupZipArchive.Kind.RECORDINGS)
            val manifestBytes = mutableListOf<Byte>()
            zip.extract("manifest.json") { manifestBytes.addAll(it.toList()) }
            val manifest = RecordingArchiveManifest.decode(manifestBytes.toByteArray())
            zip.extract("recordings.jsonl", rawFile::append)
            val source = rawFile.seal()
            assertEquals(manifest.bytes, source.size)
            val reader = RecordingArchiveReader(manifest)
            try {
                var offset = 0L
                while (offset < source.size) {
                    val chunk = source.read(offset, 32768)
                    reader.accept(chunk)
                    offset += chunk.size
                }
                val snapshot = reader.finish()
                assertEquals("RECOVERABLE", snapshot.sessions.getValue("run").state)
                assertFalse(snapshot.sessions.getValue("run").hasActivity)
            } finally {
                reader.close()
            }
        } finally {
            zipFile.close()
            rawFile.close()
        }
    }

    @Test
    fun returnedBytesAndParallelScratchFilesAreIndependentAndCloseIsIdempotent() {
        val first = BackupStagingFile.create(directory(), 10)
        val second = BackupStagingFile.create(directory(), 10)
        try {
            val input = byteArrayOf(1, 2, 3)
            first.append(input)
            input[0] = 99
            second.append(byteArrayOf(4, 5, 6))
            first.seal()
            second.seal()
            first.read(0, 3)[0] = 88
            assertContentEquals(byteArrayOf(1, 2, 3), first.read(0, 3))
            first.close()
            first.close()
            assertFails { first.size }
            assertContentEquals(byteArrayOf(4, 5, 6), second.read(0, 3))
        } finally {
            first.close()
            second.close()
        }
    }

    @Test
    fun invalidPathsLimitsAndReadBoundsRejectWithoutProducingEvidence() {
        for (path in listOf("", "\u0000", directory() + "/missing-parent/child")) assertFails {
            BackupStagingFile.create(path, 1)
        }
        for (limit in listOf(-1L, Long.MAX_VALUE)) assertFails {
            BackupStagingFile.create(directory(), limit)
        }
        for ((offset, maximum) in listOf(-1L to 1, 2L to 1, 0L to -1, 0L to 32769)) {
            val file = BackupStagingFile.create(directory(), 1)
            file.append(byteArrayOf(1))
            file.seal()
            assertFails { file.read(offset, maximum) }
            assertFails { file.read(0, 1) }
            file.close()
        }
    }
}
