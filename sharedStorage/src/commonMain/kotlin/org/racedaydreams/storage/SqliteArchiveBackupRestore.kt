package org.racedaydreams.storage

/**
 * Internal population of validated legacy feature archives. No clearing, new decisions, history
 * recalculation, or independent restore permission. The full replacement coordinator remains
 * responsible for every normalized programming/execution/body/retention section in ONE transaction.
 */
internal class SqliteArchiveBackupRestore(private val database: SqlDatabase) {
    fun populate(snapshot: UserBackupDataJson.Snapshot) =
        database.transaction {
            val coaching = CoachingArchiveJson.Table.entries
            for (table in
                coaching.map { it.sqlName } +
                    listOf(
                        "coach_evidence",
                        "strength_user_state",
                        "pb_acknowledgements",
                        "strength_exclusions",
                        "gym_plan_versions",
                        "gym_current",
                        "gym_progression_decisions",
                        "coach_support",
                        "release08",
                    )) require(database.query("SELECT 1 FROM $table LIMIT 1").isEmpty()) {
                "Archive restore requires empty prepared tables; existing data was retained."
            }
            // Reverse table dependency order exactly as original CoachingStore.restore. Row payload
            // strings are copied verbatim: optional columns and recorded numerical spelling
            // survive.
            snapshot.coaching?.let { archive ->
                for (table in coaching.reversed()) for (row in
                    archive.rows.getValue(table)) database.execute(
                    "INSERT INTO ${table.sqlName}(${table.columns.joinToString(",")}) VALUES(${table.columns.joinToString(",") { "?" }})",
                    table.columns.map { sqlText(row[it]) },
                )
            }
            snapshot.development?.records?.forEach { r ->
                record("coach_evidence", r.kind.name, r.id, r.payload.toString())
            }
            snapshot.choices?.let { choices ->
                choices.choices.forEach { c ->
                    database.execute(
                        "INSERT INTO strength_user_state VALUES(?,?,?)",
                        listOf(
                            SqlValue.Text(c.exerciseId),
                            SqlValue.Integer(if (c.favourite) 1 else 0),
                            SqlValue.Integer(if (c.archived) 1 else 0),
                        ),
                    )
                }
                choices.acknowledged.forEach {
                    database.execute(
                        "INSERT INTO pb_acknowledgements VALUES(?)",
                        listOf(SqlValue.Text(it)),
                    )
                }
                choices.exclusions.forEach { SqliteExerciseExclusions(database).exclude(it) }
            }
            snapshot.legacy?.let { legacy ->
                for ((key, programme) in legacy.programmes) database.execute(
                    "INSERT INTO gym_plan_versions VALUES(?,?,?,?)",
                    listOf(
                        SqlValue.Text(key),
                        SqlValue.Text(programme.id()),
                        SqlValue.Integer(programme.version().toLong()),
                        SqlValue.Text(LegacyGymProgrammePayloadJson.encode(programme)),
                    ),
                )
                legacy.decisions.forEach { d ->
                    database.execute(
                        "INSERT INTO gym_progression_decisions VALUES(?,?)",
                        listOf(
                            SqlValue.Text(d.id),
                            SqlValue.Text(LegacyGymProgrammePayloadJson.encodeDecision(d)),
                        ),
                    )
                }
                legacy.current?.let {
                    database.execute(
                        "INSERT INTO gym_current VALUES(1,?)",
                        listOf(SqlValue.Text(it)),
                    )
                }
            }
            snapshot.support?.records?.forEach { r ->
                record("coach_support", r.kind.wire, r.id, r.payload.toString())
            }
            snapshot.release08?.records?.forEach { r ->
                record("release08", r.kind.wire, r.id, r.payload.toString())
            }
        }

    private fun record(table: String, kind: String, id: String, payload: String) =
        database.execute(
            "INSERT INTO $table(kind,id,payload) VALUES(?,?,?)",
            listOf(SqlValue.Text(kind), SqlValue.Text(id), SqlValue.Text(payload)),
        )
}
