package org.racedaydreams.storage

import kotlinx.serialization.json.*

/** Immutable execution context belongs to retained recordings, not replaceable preferences. */
internal class SqliteRecordedContextRestore(private val database: SqlDatabase) {
    /** Capture bounded original bytes and reject conflicts BEFORE the replacement clears tables. */
    fun preserve(incoming: SupportArchiveJson.Snapshot?): List<Pair<String, String>> {
        val where =
            "FROM coach_support c JOIN recording_sessions r ON r.id=c.id WHERE c.kind='execution'"
        val totals =
            database
                .query(
                    "SELECT COUNT(*),COALESCE(MAX(LENGTH(c.id)),0),COALESCE(SUM(LENGTH(CAST(c.payload AS BLOB))),0) $where"
                )
                .single()
        require(
            totals[0].sqlLong() <= 100000 &&
                totals[1].sqlLong() <= 256 &&
                totals[2].sqlLong() in 0..BackupEnvelopeJson.MAX_BYTES.toLong()
        ) {
            "Recorded workout context exceeds the restore budget; existing data retained."
        }
        val proposed =
            incoming
                ?.records
                .orEmpty()
                .filter { it.kind == SupportArchiveJson.Kind.EXECUTION }
                .associateBy { it.id }
        return database.query("SELECT c.id,c.payload $where ORDER BY c.id LIMIT 100001").map { row
            ->
            val id = row[0].sqlText()
            val original = row[1].sqlText()
            StoredJsonSyntax(original, 32, 2000000, BackupEnvelopeJson.MAX_BYTES) {
                    throw IllegalArgumentException("Invalid recorded workout context: $it")
                }
                .validate()
            val payload =
                Json.parseToJsonElement(original) as? JsonObject
                    ?: error("Invalid recorded workout context.")
            // Validate every original field through the existing codec. Retain unknown
            // extension fields too: typed Step equality alone could hide changed evidence.
            SupportArchiveJson.decode(
                buildJsonObject {
                    put("version", 1)
                    put(
                        "rows",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("kind", "execution")
                                    put("id", id)
                                    put("payload", payload)
                                }
                            )
                        },
                    )
                }
            )
            require(proposed[id]?.payload?.let { it == payload } != false) {
                "Conflicting recorded workout context; existing data retained."
            }
            id to original
        }
    }

    /** Restore exactly the original source bytes after other support records are populated. */
    fun populate(retained: List<Pair<String, String>>) {
        for ((id, payload) in retained) database.execute(
            "INSERT OR REPLACE INTO coach_support(kind,id,payload) VALUES('execution',?,?)",
            listOf(SqlValue.Text(id), SqlValue.Text(payload)),
        )
    }
}
