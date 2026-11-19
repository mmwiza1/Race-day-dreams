package org.racedaydreams.storage

import kotlinx.serialization.json.*

/**
 * Consistent original coach_evidence export. Imported WEIGHT caches are omitted BEFORE owned-row or
 * aggregate budgets, matching DevelopmentStore.exportData; nothing is deleted or reimported.
 */
class SqliteDevelopmentArchive(private val database: SqlDatabase) {
    fun snapshot(): DevelopmentArchiveJson.Snapshot =
        database.transaction {
            val records = mutableListOf<DevelopmentArchiveJson.Record>()
            var ownedBytes = 0L
            var afterKind: String? = null
            var afterId: String? = null
            while (true) {
                val arguments =
                    if (afterKind == null) emptyList()
                    else listOf(SqlValue.Text(afterKind), SqlValue.Text(requireNotNull(afterId)))
                val where = if (afterKind == null) "" else " WHERE (kind,id)>(?,?)"
                val page =
                    database.query(
                        "SELECT kind,id,LENGTH(CAST(payload AS BLOB)),rowid FROM coach_evidence$where ORDER BY kind,id LIMIT 512",
                        arguments,
                    )
                if (page.isEmpty()) break
                var first = 0
                while (first < page.size) {
                    var last = first
                    var sourceBytes = 0L
                    // Bound small payload batches; read larger supported rows alone.
                    // Tuple keysets and row IDs use the original schema's existing indexes.
                    while (last < page.size) {
                        val size = page[last][2].sqlLong()
                        require(size in 0..BackupEnvelopeJson.MAX_BYTES.toLong()) {
                            "Unsupported evidence source row size; existing data was retained."
                        }
                        if (last > first && size > 1024 * 1024 - sourceBytes) break
                        sourceBytes += size
                        last++
                    }
                    val batch = page.subList(first, last)
                    val payloads =
                        database
                            .query(
                                "SELECT rowid,payload FROM coach_evidence WHERE rowid IN (${batch.joinToString(",") { "?" }})",
                                batch.map { SqlValue.Integer(it[3].sqlLong()) },
                            )
                            .associate { it[0].sqlLong() to it[1].sqlText() }
                    require(payloads.size == batch.size) { "The evidence snapshot is incomplete." }
                    for (row in batch) {
                        val kind = row[0].sqlText()
                        val id = row[1].sqlText()
                        val bytes = row[2].sqlLong()
                        if (kind != "WEIGHT") budget(records.size, ownedBytes, bytes)
                        val payload = payloads.getValue(row[3].sqlLong())
                        StoredJsonSyntax(payload, 32, 2000000, 1024 * 1024) {
                                throw IllegalArgumentException("Invalid stored evidence: $it")
                            }
                            .validate()
                        val value = StoredJson.objectValue(payload)
                        // Filter providers before validating measurement fields.
                        // Cached data remains untouched, matching original export.
                        if (kind == "WEIGHT" && value["provider"] != JsonPrimitive("manual"))
                            continue
                        val overhead =
                            kind.encodeToByteArray().size.toLong() + id.encodeToByteArray().size
                        budget(records.size, ownedBytes, bytes + overhead)
                        val archive = buildJsonObject {
                            put("version", 1)
                            put(
                                "records",
                                buildJsonArray {
                                    add(
                                        buildJsonObject {
                                            put("kind", kind)
                                            put("id", id)
                                            put("payload", value)
                                        }
                                    )
                                },
                            )
                        }
                        records += DevelopmentArchiveJson.decode(archive).records.single()
                        ownedBytes += bytes + overhead
                    }
                    first = last
                }
                afterKind = page.last()[0].sqlText()
                afterId = page.last()[1].sqlText()
            }
            DevelopmentArchiveJson.Snapshot(records)
        }

    private fun budget(count: Int, accumulated: Long, next: Long) {
        require(count < 100000) { "Too many owned evidence records; nothing was truncated." }
        require(next >= 0 && next <= BackupEnvelopeJson.MAX_BYTES - accumulated) {
            "Owned evidence exceeds the whole-backup budget; nothing was truncated."
        }
    }
}
