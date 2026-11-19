package org.racedaydreams.storage

import kotlinx.serialization.json.*

/**
 * Original raw-v3 export from ONE existing-database snapshot. Dense payloads use byte-bounded
 * keyset pages; no route, event, summary or removal receipt is recalculated or deleted. Output is a
 * sealed private staging file which the caller must close, never an automatically published file.
 */
class SqliteRecordingArchive(private val database: SqlDatabase) {
    fun export(privateDirectory: String): BackupStagingFile {
        val raw = BackupStagingFile.create(privateDirectory, RecordingArchiveRows.MAX_BYTES)
        var output: BackupStagingFile? = null
        try {
            val digest = archiveSha256()
            val hash =
                try {
                    val graph = RecordingArchiveRows(3)
                    database.transaction {
                        val totals =
                            database
                                .query(
                                    "SELECT COUNT(*),COALESCE(MAX(LENGTH(id)),0),COALESCE(MAX(LENGTH(started_at)),0),COALESCE(MAX(LENGTH(state)),0) FROM recording_sessions"
                                )
                                .single()
                        require(
                            totals[0].sqlLong() <= 100000 &&
                                totals[1].sqlLong() <= 256 &&
                                totals[2].sqlLong() <= 64 &&
                                totals[3].sqlLong() <= 32
                        ) {
                            "Unsupported recording headers; nothing was truncated."
                        }
                        fun writeRow(text: String) {
                            graph.acceptLine(text)
                            val bytes =
                                (text + "\n").encodeToByteArray(throwOnInvalidSequence = true)
                            digest.update(bytes, 0, bytes.size)
                            for (at in bytes.indices step BackupStagingFile.BLOCK_BYTES) raw.append(
                                bytes.copyOfRange(
                                    at,
                                    minOf(bytes.size, at + BackupStagingFile.BLOCK_BYTES),
                                )
                            )
                        }
                        fun row(value: JsonObject) = writeRow(value.toString())
                        fun rawSample(kind: String, id: String, sequence: Long, text: String) {
                            // The original proof hashes stored bytes. Do not rewrite escapes,
                            // numeric spellings or interior spacing by serializing parsed JSON.
                            // JSONL cannot represent literal line breaks or outside-value spaces
                            // without losing those bytes; fail closed rather than export a lie.
                            require(text == text.trim() && '\n' !in text && '\r' !in text) {
                                "Recording payload cannot be preserved in one archive line; existing data retained."
                            }
                            val header =
                                buildJsonObject {
                                        put("kind", kind)
                                        put("id", id)
                                        put("seq", sequence)
                                    }
                                    .toString()
                            writeRow(header.dropLast(1) + ",\"payload\":" + text + "}")
                        }
                        var after: String? = null
                        while (true) {
                            val page =
                                database.query(
                                    "SELECT id,state,started_at,started_mono,last_mono,activity_id FROM recording_sessions" +
                                        (if (after == null) "" else " WHERE id>?") +
                                        " ORDER BY id LIMIT 512",
                                    after?.let { listOf(SqlValue.Text(it)) }.orEmpty(),
                                )
                            if (page.isEmpty()) break
                            for (session in page) {
                                val id = session[0].sqlText()
                                row(
                                    buildJsonObject {
                                        put("kind", "session")
                                        put("id", id)
                                        put("state", session[1].sqlText())
                                        put("start", session[2].sqlText())
                                        put("mono", session[3].sqlLong())
                                        put("last", session[4].sqlLong())
                                        session[5].nullableSqlText()?.let { put("activity", it) }
                                    }
                                )
                                fun payload(
                                    kind: String,
                                    body: JsonObject,
                                    sequence: Long? = null,
                                ) {
                                    row(
                                        buildJsonObject {
                                            put("kind", kind)
                                            put("id", id)
                                            if (sequence != null) put("seq", sequence)
                                            put("payload", body)
                                        }
                                    )
                                }
                                singlePayload(
                                        "coach_support",
                                        "kind='execution' AND id=?",
                                        listOf(SqlValue.Text(id)),
                                    )
                                    ?.let { payload("context", it) }
                                denseRows("recording_points", id) { sequence, value ->
                                    rawSample("point", id, sequence, value)
                                }
                                denseRows("recording_events", id) { sequence, value ->
                                    rawSample("event", id, sequence, value)
                                }
                                session[5].nullableSqlText()?.let { activity ->
                                    singlePayload(
                                            "activities",
                                            "id=?",
                                            listOf(SqlValue.Text(activity)),
                                        )
                                        ?.let { payload("activity", it) }
                                }
                                SqliteRecordingDetailReceipts(database).removal(id)?.let {
                                    payload("removed", DetailRetentionArchiveJson.element(it))
                                }
                            }
                            after = page.last()[0].sqlText()
                        }
                        graph.finish()
                    }
                    digest.finish().joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
                } finally {
                    digest.close()
                }
            raw.seal()
            val manifest =
                buildJsonObject {
                        put("format", "rdd-owned-recordings")
                        put("version", 3)
                        put("bytes", raw.size)
                        put("sha256", hash)
                    }
                    .toString()
                    .encodeToByteArray()
            val manifestSource =
                object : BackupArchiveSource {
                    override val size = manifest.size.toLong()

                    override fun read(offset: Long, maximum: Int) =
                        manifest.copyOfRange(
                            offset.toInt(),
                            minOf(manifest.size, offset.toInt() + maximum),
                        )
                }
            val result =
                BackupStagingFile.create(
                    privateDirectory,
                    CompleteBackupManifest.MAX_RECORDINGS_ZIP_BYTES,
                )
            output = result
            BackupZipWriter.write(
                BackupZipArchive.Kind.RECORDINGS,
                listOf(
                    BackupZipWriter.Source("manifest.json", manifestSource),
                    BackupZipWriter.Source("recordings.jsonl", raw),
                ),
                result::append,
            )
            result.seal()
            raw.close()
            return result
        } catch (error: Throwable) {
            for (file in listOfNotNull(output, raw)) {
                try {
                    file.close()
                } catch (cleanup: Throwable) {
                    error.addSuppressed(cleanup)
                }
            }
            throw error
        } finally {
            raw.close()
        }
    }

    private fun singlePayload(
        table: String,
        where: String,
        arguments: List<SqlValue>,
    ): JsonObject? {
        val metadata =
            database.query(
                "SELECT LENGTH(CAST(payload AS BLOB)) FROM $table WHERE $where",
                arguments,
            )
        if (metadata.isEmpty()) return null
        require(metadata.size == 1)
        sourceSize(metadata.single().single().sqlLong())
        val text =
            database
                .query("SELECT payload FROM $table WHERE $where", arguments)
                .single()
                .single()
                .sqlText()
        return payload(text)
    }

    private fun denseRows(table: String, session: String, consume: (Long, String) -> Unit) {
        var after: Long? = null
        while (true) {
            val arguments = mutableListOf<SqlValue>(SqlValue.Text(session))
            after?.let { arguments += SqlValue.Integer(it) }
            val page =
                database.query(
                    "SELECT sequence,LENGTH(CAST(payload AS BLOB)) FROM $table WHERE session_id=?" +
                        (if (after == null) "" else " AND sequence>?") +
                        " ORDER BY sequence LIMIT 512",
                    arguments,
                )
            if (page.isEmpty()) break
            var first = 0
            while (first < page.size) {
                var end = first
                var bytes = 0L
                while (end < page.size) {
                    val next = page[end][1].sqlLong()
                    sourceSize(next)
                    if (end > first && next > 1024 * 1024 - bytes) break
                    bytes += next
                    end++
                }
                val batch = page.subList(first, end)
                val values =
                    database
                        .query(
                            "SELECT sequence,payload FROM $table WHERE session_id=? AND sequence IN (${batch.joinToString(",") { "?" }})",
                            listOf(SqlValue.Text(session)) +
                                batch.map { SqlValue.Integer(it[0].sqlLong()) },
                        )
                        .associate { it[0].sqlLong() to it[1].sqlText() }
                require(values.size == batch.size) { "Incomplete recording snapshot." }
                for (row in batch) {
                    val sequence = row[0].sqlLong()
                    val text = values.getValue(sequence)
                    payload(text) // Retain the existing independent syntax/size/object guards.
                    consume(sequence, text)
                }
                first = end
            }
            after = page.last()[0].sqlLong()
        }
    }

    private fun sourceSize(bytes: Long) {
        require(bytes in 0..RecordingArchiveRows.MAX_LINE.toLong() * 4) {
            "Unsupported recording source row size; existing data was retained."
        }
    }

    private fun payload(text: String): JsonObject {
        StoredJsonSyntax(text, 32, 2000000, RecordingArchiveRows.MAX_LINE) {
                throw IllegalArgumentException("Invalid recording source payload: $it")
            }
            .validate()
        return Json.parseToJsonElement(text) as? JsonObject
            ?: error("Invalid recording source object.")
    }
}
