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
                    else
                        listOf(
                            SqlValue.Text(afterKind),
                            SqlValue.Text(afterKind),
                            SqlValue.Text(requireNotNull(afterId)),
                        )
                val where = if (afterKind == null) "" else " WHERE kind>? OR (kind=? AND id>?)"
                val page =
                    database.query(
                        "SELECT kind,id,LENGTH(CAST(payload AS BLOB)) FROM coach_evidence$where ORDER BY kind,id LIMIT 512",
                        arguments,
                    )
                if (page.isEmpty()) break
                for (row in page) {
                    val kind = row[0].sqlText()
                    val id = row[1].sqlText()
                    val bytes = row[2].sqlLong()
                    // Every supported historical file already bounds an individual source row by
                    // its enclosing32MiB. Do not load an unbounded/corrupt database payload into
                    // RAM.
                    require(bytes in 0..BackupEnvelopeJson.MAX_BYTES.toLong()) {
                        "Unsupported evidence source row size; existing data was retained."
                    }
                    if (kind != "WEIGHT") budget(records.size, ownedBytes, bytes)
                    val payload =
                        database
                            .query(
                                "SELECT payload FROM coach_evidence WHERE kind=? AND id=?",
                                listOf(SqlValue.Text(kind), SqlValue.Text(id)),
                            )
                            .single()
                            .single()
                            .sqlText()
                    StoredJsonSyntax(payload, 32, 2000000, 1024 * 1024) {
                            throw IllegalArgumentException("Invalid stored evidence: $it")
                        }
                        .validate()
                    val value = StoredJson.objectValue(payload)
                    // Original export checks optString(provider), NOT whether cached measurements
                    // satisfy the user-owned archive validator. No source/ID heuristics or
                    // deletion.
                    if (kind == "WEIGHT" && value["provider"] != JsonPrimitive("manual")) continue
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
