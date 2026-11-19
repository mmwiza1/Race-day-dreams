package org.racedaydreams.storage

import kotlin.test.*

class BackupZipArchiveTest {
    private class Source(val bytes: ByteArray, val chunk: Int = 32768) : BackupArchiveSource {
        var sizeDelta = 0
        override val size: Long
            get() = bytes.size.toLong() + sizeDelta

        override fun read(offset: Long, maximum: Int): ByteArray =
            bytes.copyOfRange(
                offset.toInt(),
                minOf(bytes.size, offset.toInt() + minOf(chunk, maximum)),
            )
    }

    private fun archive(bytes: ByteArray, chunk: Int = 32768) =
        BackupZipArchive.inspect(Source(bytes, chunk), BackupZipArchive.Kind.RECORDINGS)

    private fun bytes(zip: BackupZipArchive, name: String): ByteArray {
        val out = mutableListOf<Byte>()
        zip.extract(name) { out.addAll(it.toList()) }
        return out.toByteArray()
    }

    private fun signature(bytes: ByteArray, value: Long): Int =
        (0..bytes.size - 4).first { u32(bytes, it) == value }

    private fun u32(b: ByteArray, at: Int): Long =
        (0..3).fold(0L) { v, n -> v or ((b[at + n].toLong() and 255) shl (8 * n)) }

    private fun put32(b: ByteArray, at: Int, value: Long) {
        for (n in 0..3) b[at + n] = (value ushr (8 * n)).toByte()
    }

    private fun csvCentral(b: ByteArray): Int {
        var at = signature(b, 0x02014b50)
        while (true) {
            val length = (b[at + 28].toInt() and 255) or ((b[at + 29].toInt() and 255) shl 8)
            if (b.copyOfRange(at + 46, at + 46 + length).decodeToString() == "recordings.jsonl")
                return at
            val extra = (b[at + 30].toInt() and 255) or ((b[at + 31].toInt() and 255) shl 8)
            val comment = (b[at + 32].toInt() and 255) or ((b[at + 33].toInt() and 255) shl 8)
            at += 46 + length + extra + comment
        }
    }

    @Test
    fun originalStoredDeflatedDescriptorAndZip64ArchivesValidateTheSameRawEvidence() {
        for ((data, count) in
            listOf(
                BackupZipFixtures.stored to 0,
                BackupZipFixtures.deflated to 1,
                BackupZipFixtures.zip64 to 1,
            )) {
            val zip = archive(data)
            assertEquals(
                setOf("manifest.json", "recordings.jsonl"),
                zip.entries.map { it.name }.toSet(),
            )
            val manifest = RecordingArchiveManifest.decode(bytes(zip, "manifest.json"))
            val reader = RecordingArchiveReader(manifest)
            zip.extract("recordings.jsonl", reader::accept)
            val snapshot = reader.finish()
            assertEquals(count, snapshot.sessions.size)
            if (count > 0) {
                assertEquals("RECOVERABLE", snapshot.sessions.getValue("run").state)
                assertFalse(snapshot.sessions.getValue("run").hasActivity)
            }
            assertFails { (zip.entries as MutableList).clear() }
        }
    }

    @Test
    fun shortSourceReadsAndSmallCompressedInputsStillDrainLargeOutputCorrectly() {
        val zip = archive(BackupZipFixtures.deflated, 3)
        val entry = zip.entries.single { it.name == "recordings.jsonl" }
        assertTrue(entry.bytes > 100000 && entry.compressedBytes < 1000)
        var blocks = 0
        var count = 0L
        zip.extract(entry.name) {
            assertTrue(it.size <= 32768)
            blocks++
            count += it.size
        }
        assertTrue(blocks > 1)
        assertEquals(entry.bytes, count)
    }

    @Test
    fun crcUsesPublishedVectorAndCorruptStoredPayloadNeverFinishesSuccessfully() {
        val crc = ArchiveCrc32()
        crc.update("123456789".encodeToByteArray())
        assertEquals(0xcbf43926L, crc.value())
        assertEquals(0L, ArchiveCrc32().value())
        val data = BackupZipFixtures.stored
        val entry = archive(data).entries.single { it.name == "manifest.json" }
        data[entry.dataOffset.toInt()] = (data[entry.dataOffset.toInt()].toInt() xor 1).toByte()
        val corrupt = archive(data)
        assertFails { corrupt.extract("manifest.json") {} }
    }

    @Test
    fun duplicateUnknownAndIncorrectContainerEntriesAreRejected() {
        assertFails { archive(BackupZipFixtures.duplicate) }
        assertFails {
            BackupZipArchive.inspect(
                Source(BackupZipFixtures.stored),
                BackupZipArchive.Kind.COMPLETE,
            )
        }
        val data = BackupZipFixtures.stored
        val needle = "recordings.jsonl".encodeToByteArray()
        val replacement = "../bad-files.bin".encodeToByteArray()
        assertEquals(needle.size, replacement.size)
        for (i in 0..data.size - needle.size) if (
            data.copyOfRange(i, i + needle.size).contentEquals(needle)
        )
            replacement.copyInto(data, i)
        assertFails { archive(data) }
    }

    @Test
    fun invalidDirectoryTruncationSplitDisksAndUnsupportedMethodsReject() {
        val original = BackupZipFixtures.stored
        val end = signature(original, 0x06054b50)
        val central = signature(original, 0x02014b50)
        val cases = mutableListOf(original.copyOf(original.size - 1), original.copyOf(20))
        cases += original.copyOf().also { it[end + 4] = 1 }
        cases += original.copyOf().also { it[central + 10] = 99 }
        cases +=
            original.copyOf().also { it[central + 8] = (it[central + 8].toInt() or 1).toByte() }
        cases += original.copyOf().also { put32(it, end + 16, it.size.toLong()) }
        for (bad in cases) assertFails { archive(bad) }
    }

    @Test
    fun localNamesCrcAndDescriptorsCannotDisagreeWithCentralEvidence() {
        val stored = BackupZipFixtures.stored
        assertFails { archive(stored.copyOf().also { it[30] = 'z'.code.toByte() }) }
        assertFails { archive(stored.copyOf().also { it[14] = (it[14].toInt() xor 1).toByte() }) }
        val deflated = BackupZipFixtures.deflated
        val descriptor = signature(deflated, 0x08074b50)
        assertFails {
            archive(
                deflated.copyOf().also {
                    it[descriptor + 4] = (it[descriptor + 4].toInt() xor 1).toByte()
                }
            )
        }
    }

    @Test
    fun declaredOversizedOrOverlappingEntriesRejectBeforeExtraction() {
        val data = BackupZipFixtures.deflated
        val central = csvCentral(data)
        assertFails {
            archive(
                data.copyOf().also { put32(it, central + 24, RecordingArchiveRows.MAX_BYTES + 1) }
            )
        }
        assertFails { archive(data.copyOf().also { put32(it, central + 20, it.size.toLong()) }) }
        assertFails { archive(data.copyOf().also { put32(it, central + 42, 0) }) }
    }

    @Test
    fun dishonestSmallExpandedSizeStopsOutputBeforeItReachesStagingSink() {
        val data = BackupZipFixtures.deflated
        val originalEntry = archive(data).entries.single { it.name == "recordings.jsonl" }
        val descriptor = originalEntry.dataOffset.toInt() + originalEntry.compressedBytes.toInt()
        put32(data, csvCentral(data) + 24, 1)
        put32(data, descriptor + 12, 1)
        val zip = archive(data)
        var emitted = 0
        assertFails { zip.extract("recordings.jsonl") { emitted += it.size } }
        assertEquals(0, emitted)
    }

    @Test
    fun deflateRejectsTruncationTrailingBytesAndRecoversAfterConsumerFailure() {
        val data = BackupZipFixtures.deflated
        val zip = archive(data)
        val entry = zip.entries.single { it.name == "recordings.jsonl" }
        val compressed =
            data.copyOfRange(
                entry.dataOffset.toInt(),
                (entry.dataOffset + entry.compressedBytes).toInt(),
            )
        for (bad in listOf(compressed.copyOf(compressed.size - 1), compressed + byteArrayOf(0))) {
            assertFails {
                inflateBackupEntry(
                    bad.size.toLong(),
                    { at, max -> bad.copyOfRange(at.toInt(), minOf(bad.size, at.toInt() + max)) },
                ) {}
            }
        }
        assertFails { zip.extract("recordings.jsonl") { throw IllegalStateException("cancelled") } }
        assertEquals(entry.bytes.toInt(), bytes(zip, entry.name).size)
    }

    @Test
    fun changingOrIncompleteStagedSourceCannotReturnValidExtraction() {
        val source = Source(BackupZipFixtures.stored)
        val zip = BackupZipArchive.inspect(source, BackupZipArchive.Kind.RECORDINGS)
        source.sizeDelta = 1
        assertFails { zip.extract("manifest.json") {} }
        val incomplete =
            object : BackupArchiveSource {
                override val size = source.bytes.size.toLong()

                override fun read(offset: Long, maximum: Int) = ByteArray(0)
            }
        assertFails { BackupZipArchive.inspect(incomplete, BackupZipArchive.Kind.RECORDINGS) }
    }
}
