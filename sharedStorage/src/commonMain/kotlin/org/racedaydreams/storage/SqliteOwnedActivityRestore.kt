package org.racedaydreams.storage

import org.racedaydreams.domain.activity.ActivityRecord

/**
 * Preserve app-owned recording evidence in place while replacing annotations and other app data.
 */
internal class SqliteOwnedActivityRestore(private val database: SqlDatabase) {
    fun requirePreserved(core: CoreBackupDataJson.Snapshot) {
        val incoming = core.activities.associateBy { it.id() }
        val totals =
            database
                .query(
                    "SELECT COUNT(*),COALESCE(MAX(LENGTH(id)),0),COALESCE(MAX(LENGTH(record_id)),0) FROM activities WHERE provider IN ('rdd','gpx')"
                )
                .single()
        require(
            totals[0].sqlLong() <= 100000 &&
                totals[1].sqlLong() <= 256 &&
                totals[2].sqlLong() <= 256
        ) {
            "Unsupported owned recording identities; existing data retained."
        }
        var after: String? = null
        while (true) {
            val page =
                database.query(
                    "SELECT id,LENGTH(CAST(payload AS BLOB)),provider,record_id FROM activities WHERE provider IN ('rdd','gpx')" +
                        (if (after == null) "" else " AND id>?") +
                        " ORDER BY id LIMIT 512",
                    after?.let { listOf(SqlValue.Text(it)) }.orEmpty(),
                )
            if (page.isEmpty()) break
            for (row in page) {
                require(row[1].sqlLong() in 0..BackupEnvelopeJson.MAX_BYTES.toLong()) {
                    "Unsupported retained recording size; existing data retained."
                }
                val id = row[0].sqlText()
                val restored =
                    requireNotNull(incoming[id]) {
                        "This backup would remove user-owned recordings. Export a complete backup first and restore into an empty installation."
                    }
                val old = requireNotNull(load(id))
                require(
                    old.id() == id &&
                        old.source().provider() == row[2].sqlText() &&
                        old.source().recordId() == row[3].sqlText()
                ) {
                    "Retained recording identity differs from its index."
                }
                require(
                    recordedActivityMeasurements(old) == recordedActivityMeasurements(restored)
                ) {
                    "Archived summary conflicts with retained recording evidence; existing data retained."
                }
                if (restored.samples().isNotEmpty()) {
                    if (old.samples().isNotEmpty())
                        require(old.samples() == restored.samples()) {
                            "Archived inline detail conflicts with retained evidence."
                        }
                    else if (hasRaw(id))
                        require(
                            SqliteActivities(database) { error("No new time") }
                                .withRetainedSamples(old)
                                .samples() == restored.samples()
                        ) {
                            "Archived inline detail conflicts with retained raw evidence."
                        }
                }
            }
            after = page.last()[0].sqlText()
        }
    }

    fun put(incoming: ActivityRecord) {
        val old = load(incoming.id())
        val samples =
            if (old != null && old.samples().isNotEmpty()) old.samples()
            else if (hasRaw(incoming.id())) emptyList() else incoming.samples()
        val payload = SqlValue.Text(ActivityJson.encode(incoming.copy(samples = samples)))
        if (old == null)
            database.execute(
                "INSERT INTO activities(id,provider,record_id,origin_package,start_millis,end_millis,payload) VALUES(?,?,?,?,?,?,?)",
                listOf(
                    SqlValue.Text(incoming.id()),
                    SqlValue.Text(incoming.source().provider()),
                    SqlValue.Text(incoming.source().recordId()),
                    sqlText(incoming.source().originPackage()),
                    SqlValue.Integer(storedEpochMillis(incoming.start())),
                    SqlValue.Integer(storedEpochMillis(incoming.end())),
                    payload,
                ),
            )
        else
            database.execute(
                "UPDATE activities SET payload=? WHERE id=?",
                listOf(payload, SqlValue.Text(incoming.id())),
            )
    }

    private fun hasRaw(id: String) =
        database
            .query(
                "SELECT 1 FROM recording_points p JOIN recording_sessions s ON s.id=p.session_id WHERE s.activity_id=? AND s.state='FINISHED' LIMIT 1",
                listOf(SqlValue.Text(id)),
            )
            .isNotEmpty()

    private fun load(id: String): ActivityRecord? {
        val text =
            database
                .query("SELECT payload FROM activities WHERE id=?", listOf(SqlValue.Text(id)))
                .firstOrNull()
                ?.single()
                ?.sqlText() ?: return null
        StoredJsonSyntax(text, 32, 2000000, 1024 * 1024) {
                throw IllegalArgumentException("Invalid retained recording: $it")
            }
            .validate()
        return ActivityJson.decode(text)
    }
}

/**
 * Compare immutable actual measurements; annotations and separately retained samples may differ.
 */
internal fun recordedActivityMeasurements(a: ActivityRecord) =
    a.copy(
        notes = null,
        rpe = null,
        shoeId = null,
        plannedWorkoutId = null,
        samples = emptyList(),
        distanceMetres = recordedZero(a.distanceMetres()),
        elevationGainMetres = recordedZero(a.elevationGainMetres()),
        cadence = recordedZero(a.cadence()),
    )

// JSONObject can read -0.0 summaries as integral zero. Other values and raw hashes never normalize.
private fun recordedZero(value: Double?): Double? =
    if (value != null && value == 0.0) 0.0 else value
