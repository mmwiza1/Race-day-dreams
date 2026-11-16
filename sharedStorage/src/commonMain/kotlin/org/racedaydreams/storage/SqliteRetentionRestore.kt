package org.racedaydreams.storage

import org.racedaydreams.domain.storage.RemovedRecordingDetail

/**
 * Internal part of the future complete replacement transaction, NOT an app restore or cleanup API.
 * The coordinator must validate and replace every parent section together. No raw rows are changed.
 */
internal class SqliteRetentionRestore(
    private val database: SqlDatabase,
    private val freshEpoch: () -> String,
) {
    fun replace(snapshot: DetailRetentionArchiveJson.Snapshot) =
        database.transaction {
            require(
                database
                    .query(
                        "SELECT 1 FROM recording_sessions WHERE state IN ('ACTIVE','PAUSED','RECOVERABLE') LIMIT 1"
                    )
                    .isEmpty()
            ) {
                "Finish or recover the active recording before replacing app data."
            }
            require(
                database
                    .query(
                        "SELECT 1 FROM strength_executions WHERE state IN ('ACTIVE','PAUSED','RECOVERY_REQUIRED') LIMIT 1"
                    )
                    .isEmpty()
            ) {
                "Finish or discard the unfinished gym workout before replacing app data."
            }
            val oldEpoch =
                database
                    .query("SELECT epoch FROM detail_retention_state WHERE id=1")
                    .single()
                    .single()
                    .sqlText()
            val epoch = sqlRequired(freshEpoch(), "storage identity")
            require(epoch != oldEpoch) { "Restored data requires a new storage identity." }
            // Check all provenance and available detail BEFORE changing preferences or receipts.
            val missing =
                snapshot.removals.filter { receipt ->
                    val activity =
                        database.query(
                            "SELECT provider,record_id FROM activities WHERE id=?",
                            listOf(SqlValue.Text(receipt.activityId())),
                        )
                    require(
                        activity.singleOrNull() ==
                            listOf(
                                SqlValue.Text(receipt.provider()),
                                SqlValue.Text(receipt.recordId()),
                            )
                    ) {
                        "Removed detail disagrees with retained activity provenance."
                    }
                    val hasRows =
                        listOf("recording_points", "recording_events").any { table ->
                            database
                                .query(
                                    "SELECT 1 FROM $table WHERE session_id=? LIMIT 1",
                                    listOf(SqlValue.Text(receipt.sessionId())),
                                )
                                .isNotEmpty()
                        }
                    if (hasRows) SqliteRecordingEvidence(database).requireMatches(receipt)
                    !hasRows
                }
            val p = snapshot.preferences
            database.execute(
                "UPDATE detail_retention_preferences SET policy=?,choice=?,chosen_at=?,choice_id=? WHERE id=1",
                listOf(
                    SqlValue.Text(p.policy().name),
                    SqlValue.Text(p.choice().name),
                    sqlText(p.chosenAt()?.toString()),
                    sqlText(p.choiceId()),
                ),
            )
            require(database.query("SELECT changes()").single().single().sqlLong() == 1L) {
                "Retention preferences missing; original data retained."
            }
            database.execute("DELETE FROM recording_detail_removals")
            missing.forEach(::insert)
            // A restored choice is not a fresh approval: every outstanding preview/approval
            // expires.
            database.execute(
                "UPDATE detail_retention_state SET epoch=? WHERE id=1",
                listOf(SqlValue.Text(epoch)),
            )
            require(database.query("SELECT changes()").single().single().sqlLong() == 1L) {
                "Storage identity missing."
            }
        }

    internal fun insert(row: RemovedRecordingDetail) {
        database.execute(
            "INSERT INTO recording_detail_removals(session_id,activity_id,provider,record_id,ended_at,removed_at,policy,started_at,started_mono,last_mono,point_count,event_count,points_hash,events_hash) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            listOf(
                SqlValue.Text(row.sessionId()),
                SqlValue.Text(row.activityId()),
                SqlValue.Text(row.provider()),
                SqlValue.Text(row.recordId()),
                SqlValue.Text(row.endedAt().toString()),
                SqlValue.Text(row.removedAt().toString()),
                SqlValue.Text(row.policy().name),
                SqlValue.Text(row.startedAt().toString()),
                SqlValue.Integer(row.startedMono()),
                SqlValue.Integer(row.lastMono()),
                SqlValue.Integer(row.points()),
                SqlValue.Integer(row.events()),
                SqlValue.Text(row.pointsHash()),
                SqlValue.Text(row.eventsHash()),
            ),
        )
    }
}
