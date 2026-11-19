package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.isJavaBlank
import org.racedaydreams.domain.strength.GymProgramme
import org.racedaydreams.domain.time.Instant

/** Original append-only legacy gym history; disabling its pointer never deletes training. */
class SqliteLegacyGymProgrammes(
    private val database: SqlDatabase,
    private val now: () -> Instant,
    private val nextId: () -> String,
) {
    /** Bounded read of the original tables; no current pointer or decision is applied. */
    fun archiveSnapshot(): LegacyGymProgrammeArchiveJson.Snapshot =
        database.transaction {
            var minimumBytes = 0L
            for ((table, maximum, size) in
                listOf(
                    Triple(
                        "gym_plan_versions",
                        1000,
                        "LENGTH(CAST(version_id AS BLOB))+LENGTH(CAST(plan_id AS BLOB))+LENGTH(CAST(payload AS BLOB))",
                    ),
                    Triple(
                        "gym_progression_decisions",
                        100000,
                        "LENGTH(CAST(id AS BLOB))+LENGTH(CAST(payload AS BLOB))",
                    ),
                )) {
                val totals =
                    database.query("SELECT COUNT(*),COALESCE(SUM($size),0) FROM $table").single()
                require(totals[0].sqlLong() <= maximum) {
                    "Too many gym archive records; nothing was truncated."
                }
                val bytes = totals[1].sqlLong()
                require(bytes >= 0 && bytes <= BackupEnvelopeJson.MAX_BYTES - minimumBytes) {
                    "Gym history exceeds the whole-backup budget; nothing was truncated."
                }
                minimumBytes += bytes
            }
            val versions =
                database
                    .query(
                        "SELECT version_id,plan_id,version,payload FROM gym_plan_versions ORDER BY rowid DESC LIMIT 1001"
                    )
                    .map { row ->
                        val payload = StoredJson.objectValue(row[3].sqlText())
                        val p = LegacyGymProgrammePayloadJson.decode(payload)
                        require(
                            row[0].sqlText() == key(p) &&
                                row[1].sqlText() == p.id() &&
                                row[2].sqlLong() == p.version().toLong()
                        ) {
                            "Stored gym programme identity differs."
                        }
                        payload
                    }
            val decisions =
                database
                    .query(
                        "SELECT id,payload FROM gym_progression_decisions ORDER BY rowid LIMIT 100001"
                    )
                    .map { row ->
                        val payload = StoredJson.objectValue(row[1].sqlText())
                        require(
                            LegacyGymProgrammePayloadJson.decodeDecision(payload).id ==
                                row[0].sqlText()
                        ) {
                            "Stored progression identity differs."
                        }
                        payload
                    }
            val current =
                database.query("SELECT id,version_id FROM gym_current").also {
                    require(it.size <= 1)
                }
            require(current.isEmpty() || current.single()[0].sqlLong() == 1L)
            LegacyGymProgrammeArchiveJson.decode(
                buildJsonObject {
                    put("version", 1)
                    put("versions", JsonArray(versions))
                    put("decisions", JsonArray(decisions))
                    put(
                        "current",
                        current.singleOrNull()?.get(1)?.sqlText()?.let { JsonPrimitive(it) }
                            ?: JsonNull,
                    )
                }
            )
        }

    fun versions(): List<GymProgramme> =
        database
            .query("SELECT plan_id,version,payload FROM gym_plan_versions ORDER BY rowid DESC")
            .map { readVersion(it) }

    fun current(): GymProgramme? =
        database
            .query(
                "SELECT v.plan_id,v.version,v.payload FROM gym_current c JOIN gym_plan_versions v ON c.version_id=v.version_id WHERE c.id=1"
            )
            .firstOrNull()
            ?.let { readVersion(it) }

    fun version(id: String?, revision: Int): GymProgramme? {
        require(id != null && !isJavaBlank(id) && revision >= 1) {
            "Choose a valid gym programme revision."
        }
        val result =
            database
                .query(
                    "SELECT plan_id,version,payload FROM gym_plan_versions WHERE version_id=?",
                    listOf(SqlValue.Text("$id:v$revision")),
                )
                .firstOrNull()
                ?.let { readVersion(it) } ?: return null
        require(result.id() == id && result.version() == revision) {
            "The retained programme identity is inconsistent."
        }
        return result
    }

    fun save(programme: GymProgramme) =
        database.transaction {
            require(
                database.query("SELECT 1 FROM strength_programme_current WHERE id=1").isEmpty()
            ) {
                "Review the active authored programme before changing programming."
            }
            val current = current()
            require(
                current == null ||
                    current.id() == programme.id() && programme.version() == current.version() + 1
            ) {
                "Review the current programme before saving this version."
            }
            val payload = LegacyGymProgrammePayloadJson.encode(programme)
            LegacyGymProgrammePayloadJson.decode(
                payload
            ) // Never insert a version our retained reader cannot open.
            database.execute(
                "INSERT INTO gym_plan_versions(version_id,plan_id,version,payload) VALUES(?,?,?,?)",
                listOf(
                    SqlValue.Text(key(programme)),
                    SqlValue.Text(programme.id()),
                    SqlValue.Integer(programme.version().toLong()),
                    SqlValue.Text(payload),
                ),
            )
            if (database.query("SELECT id FROM gym_current WHERE id=1").isEmpty())
                database.execute(
                    "INSERT INTO gym_current(id,version_id) VALUES(1,?)",
                    listOf(SqlValue.Text(key(programme))),
                )
            else
                database.execute(
                    "UPDATE gym_current SET version_id=? WHERE id=1",
                    listOf(SqlValue.Text(key(programme))),
                )
        }

    fun disable() {
        database.execute("DELETE FROM gym_current WHERE id=1")
    }

    fun decisions(): List<LegacyGymProgrammePayloadJson.Decision> =
        database.query("SELECT payload FROM gym_progression_decisions ORDER BY rowid").map {
            LegacyGymProgrammePayloadJson.decodeDecision(it.single().sqlText())
        }

    fun decide(
        expected: GymProgramme,
        proposed: GymProgramme?,
        session: String,
        exercise: String,
        evidence: String,
    ) =
        database.transaction {
            val current = current()
            require(current != null && key(current) == key(expected)) {
                "The programme changed. Review the current suggestion."
            }
            if (proposed != null) save(proposed)
            val decision =
                LegacyGymProgrammePayloadJson.Decision(
                    nextId(),
                    key(expected),
                    key(proposed ?: expected),
                    session,
                    exercise,
                    if (proposed == null) "KEEP" else "INCREASE",
                    evidence,
                    now(),
                )
            val payload = LegacyGymProgrammePayloadJson.encodeDecision(decision)
            LegacyGymProgrammePayloadJson.decodeDecision(payload)
            database.execute(
                "INSERT INTO gym_progression_decisions(id,payload) VALUES(?,?)",
                listOf(SqlValue.Text(decision.id), SqlValue.Text(payload)),
            )
        }

    private fun readVersion(row: List<SqlValue>): GymProgramme {
        val version = LegacyGymProgrammePayloadJson.decode(row[2].sqlText())
        require(
            version.id() == row[0].sqlText() && version.version().toLong() == row[1].sqlLong()
        ) {
            "The retained programme identity is inconsistent."
        }
        return version
    }

    private fun key(programme: GymProgramme): String = "${programme.id()}:v${programme.version()}"
}
