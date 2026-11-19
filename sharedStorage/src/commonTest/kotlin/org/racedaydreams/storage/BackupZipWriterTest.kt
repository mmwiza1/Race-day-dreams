package org.racedaydreams.storage

import kotlin.test.*

class BackupZipWriterTest {
    private class Source(val bytes: ByteArray, val chunk: Int = 32768) : BackupArchiveSource {
        override val size = bytes.size.toLong()

        override fun read(offset: Long, maximum: Int) =
            bytes.copyOfRange(
                offset.toInt(),
                minOf(bytes.size, offset.toInt() + minOf(chunk, maximum)),
            )
    }

    private fun entries(zip: BackupZipArchive): List<BackupZipWriter.Source> =
        zip.entries.map { entry ->
            val out = mutableListOf<Byte>()
            zip.extract(entry.name) { out.addAll(it.toList()) }
            BackupZipWriter.Source(entry.name, Source(out.toByteArray(), 7))
        }

    private fun write(
        kind: BackupZipArchive.Kind,
        entries: List<BackupZipWriter.Source>,
    ): ByteArray {
        val out = mutableListOf<Byte>()
        BackupZipWriter.write(kind, entries) {
            assertTrue(it.size <= 32768)
            out.addAll(it.toList())
        }
        return out.toByteArray()
    }

    @Test
    fun generatedStoredArchivesPreserveOriginalRawBytesManifestAndRecoverableHistory() {
        for (fixture in listOf(BackupZipFixtures.stored, BackupZipFixtures.deflated)) {
            val original =
                BackupZipArchive.inspect(Source(fixture), BackupZipArchive.Kind.RECORDINGS)
            val inputs = entries(original)
            val output = write(BackupZipArchive.Kind.RECORDINGS, inputs)
            val zip = BackupZipArchive.inspect(Source(output, 3), BackupZipArchive.Kind.RECORDINGS)
            for (input in inputs) {
                val actual = mutableListOf<Byte>()
                zip.extract(input.name) { actual.addAll(it.toList()) }
                assertContentEquals((input.bytes as Source).bytes, actual.toByteArray())
            }
            val manifestBytes = (inputs.single { it.name == "manifest.json" }.bytes as Source).bytes
            val reader = RecordingArchiveReader(RecordingArchiveManifest.decode(manifestBytes))
            try {
                zip.extract("recordings.jsonl", reader::accept)
                val snapshot = reader.finish()
                assertTrue(
                    snapshot.sessions.values.all { it.state == "RECOVERABLE" && !it.hasActivity }
                )
            } finally {
                reader.close()
            }
        }
    }

    @Test
    fun completeContainerRepackPreservesEveryMeasuredPayloadWithoutChangingInnerDocuments() {
        val original =
            BackupZipArchive.inspect(
                Source(CompletePackageFixtures.valid),
                BackupZipArchive.Kind.COMPLETE,
            )
        val inputs = entries(original)
        val output = write(BackupZipArchive.Kind.COMPLETE, inputs.reversed())
        val zip = BackupZipArchive.inspect(Source(output), BackupZipArchive.Kind.COMPLETE)
        for (input in inputs) {
            val actual = mutableListOf<Byte>()
            zip.extract(input.name) { actual.addAll(it.toList()) }
            assertContentEquals((input.bytes as Source).bytes, actual.toByteArray())
        }
    }

    @Test
    fun missingDuplicateUnknownAndOversizedEntriesFailBeforeAnyOutput() {
        val original =
            BackupZipArchive.inspect(
                Source(BackupZipFixtures.stored),
                BackupZipArchive.Kind.RECORDINGS,
            )
        val inputs = entries(original)
        val oversized =
            object : BackupArchiveSource {
                override val size = 8193L

                override fun read(offset: Long, maximum: Int): ByteArray =
                    error("Must not read oversized input")
            }
        for (invalid in
            listOf(
                emptyList(),
                inputs.take(1),
                listOf(inputs.first(), inputs.first()),
                listOf(inputs.first().copy(name = "../file"), inputs.last()),
                listOf(inputs.first().copy(bytes = oversized), inputs.last()),
            )) {
            var output = 0
            assertFails {
                BackupZipWriter.write(BackupZipArchive.Kind.RECORDINGS, invalid) {
                    output += it.size
                }
            }
            assertEquals(0, output)
        }
    }

    @Test
    fun truncatedOrOverproducingSourcesCannotProduceACompleteArchive() {
        val original =
            BackupZipArchive.inspect(
                Source(BackupZipFixtures.stored),
                BackupZipArchive.Kind.RECORDINGS,
            )
        val inputs = entries(original)
        for (bad in listOf(ByteArray(0), ByteArray(32769))) {
            val broken =
                object : BackupArchiveSource {
                    override val size = 1L

                    override fun read(offset: Long, maximum: Int) = bad
                }
            assertFails {
                write(
                    BackupZipArchive.Kind.RECORDINGS,
                    listOf(inputs.first(), inputs.last().copy(bytes = broken)),
                )
            }
        }
    }

    @Test
    fun sourceMutationAndConsumerCancellationCannotCompleteOrDamageLaterExports() {
        val original =
            BackupZipArchive.inspect(
                Source(BackupZipFixtures.stored),
                BackupZipArchive.Kind.RECORDINGS,
            )
        val inputs = entries(original)
        val changing =
            object : BackupArchiveSource {
                var reads = 0
                override val size = 1L

                override fun read(offset: Long, maximum: Int) = byteArrayOf((++reads).toByte())
            }
        assertFails {
            write(
                BackupZipArchive.Kind.RECORDINGS,
                listOf(inputs.first(), inputs.last().copy(bytes = changing)),
            )
        }
        var calls = 0
        assertFails {
            BackupZipWriter.write(BackupZipArchive.Kind.RECORDINGS, inputs) {
                calls++
                throw IllegalStateException("cancelled")
            }
        }
        assertEquals(1, calls)
        assertEquals(
            2,
            BackupZipArchive.inspect(
                    Source(write(BackupZipArchive.Kind.RECORDINGS, inputs)),
                    BackupZipArchive.Kind.RECORDINGS,
                )
                .entries
                .size,
        )
    }
}
