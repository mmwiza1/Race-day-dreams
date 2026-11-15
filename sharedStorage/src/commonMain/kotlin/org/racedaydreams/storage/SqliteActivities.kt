package org.racedaydreams.storage

import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.time.*

/**
 * Existing activity/import rules over the shared schema. Platform owns IO and the real wall clock.
 */
class SqliteActivities(private val database: SqlDatabase, private val now: () -> Instant) :
    ActivityRepository {
    private val results = SqliteResults(database)

    override fun activities(): List<ActivityRecord> = database.transaction { snapshot(true) }

    override fun activitySummaries(): List<ActivityRecord> =
        database.transaction { snapshot(false) }

    override fun activityDetail(id: String): ActivityRecord? =
        database.transaction {
            snapshot(false, onlyId = id)
                .firstOrNull { it.id() == id }
                ?.let { withRetainedSamples(it) }
        }

    override fun activitySummaryPage(
        before: Instant?,
        afterId: String?,
        limit: Int,
    ): List<ActivityRecord> {
        require(limit in 1..100 && (before == null) == (afterId == null)) {
            "Invalid history cursor/page size"
        }
        return database.transaction {
            snapshot(false, before = before, afterId = afterId, limit = limit)
        }
    }

    override fun coachingActivities(today: LocalDate): List<ActivityRecord> =
        database.transaction {
            val summary = snapshot(false)
            val cutoff = Instants.ofEpochSecond(today.minusDays(28).toEpochDay() * 86400)
            val detailed =
                summary
                    .filter { !it.start().isBefore(cutoff) }
                    .sortedByDescending { it.start() }
                    .take(12)
                    .map { it.id() }
                    .toSet()
            var remaining = 60000L
            summary.map { activity ->
                val count =
                    if (activity.id() in detailed)
                        database
                            .query(
                                "SELECT COUNT(*) FROM recording_points p JOIN recording_sessions s ON s.id=p.session_id WHERE s.activity_id=?",
                                listOf(SqlValue.Text(activity.id())),
                            )
                            .single()
                            .single()
                            .sqlLong()
                    else 0L
                if (count <= remaining && activity.id() in detailed) {
                    remaining -= count
                    withRetainedSamples(activity)
                } else activity
            }
        }

    private fun snapshot(
        includeSamples: Boolean,
        onlyId: String? = null,
        before: Instant? = null,
        afterId: String? = null,
        limit: Int = 0,
    ): List<ActivityRecord> {
        val where =
            when {
                onlyId != null -> " WHERE a.id=?"
                before != null -> " WHERE (a.start_millis<? OR (a.start_millis=? AND a.id>?))"
                else -> ""
            }
        val arguments =
            when {
                onlyId != null -> listOf(SqlValue.Text(onlyId))
                before != null ->
                    listOf(
                        SqlValue.Integer(storedEpochMillis(before)),
                        SqlValue.Integer(storedEpochMillis(before)),
                        SqlValue.Text(afterId!!),
                    )
                else -> emptyList()
            }
        val rows =
            database.query(
                "SELECT a.payload,n.provider,n.notes,n.rpe,n.shoe_id,r.planned_workout_id FROM activities a LEFT JOIN activity_annotations n ON a.provider=n.provider AND a.record_id=n.record_id LEFT JOIN run_responses r ON r.activity_id=a.id$where ORDER BY a.start_millis DESC,a.id" +
                    if (limit == 0) "" else " LIMIT $limit",
                arguments,
            )
        return rows.map { row ->
            var activity = ActivityJson.decode(row[0].sqlText())
            if (row[1] != SqlValue.Null)
                activity =
                    activity.copy(
                        notes = row[2].nullableSqlText() ?: activity.notes(),
                        rpe = row[3].nullableSqlNumber(),
                        shoeId = row[4].nullableSqlText(),
                    )
            if (row[5] != SqlValue.Null)
                activity = activity.copy(plannedWorkoutId = row[5].sqlText())
            if (includeSamples) activity = withRetainedSamples(activity)
            activity
        }
    }

    /** Hydrate normalized evidence in bounded SQL pages; stored totals/laps remain unchanged. */
    internal fun withRetainedSamples(activity: ActivityRecord): ActivityRecord {
        if (
            activity.samples().isNotEmpty() || activity.source().provider() !in listOf("rdd", "gpx")
        )
            return activity
        val session =
            database
                .query(
                    "SELECT id,started_mono FROM recording_sessions WHERE activity_id=? AND state='FINISHED'",
                    listOf(SqlValue.Text(activity.id())),
                )
                .firstOrNull() ?: return activity
        val key = session[0].sqlText()
        val mono = session[1].sqlLong()
        val samples = mutableListOf<DistanceSample>()
        var last: Long? = null
        while (true) {
            val rows =
                if (last == null)
                    database.query(
                        "SELECT sequence,payload FROM recording_points WHERE session_id=? ORDER BY sequence LIMIT 500",
                        listOf(SqlValue.Text(key)),
                    )
                else
                    database.query(
                        "SELECT sequence,payload FROM recording_points WHERE session_id=? AND sequence>? ORDER BY sequence LIMIT 500",
                        listOf(SqlValue.Text(key), SqlValue.Integer(last)),
                    )
            for (row in rows) {
                val point = RecordingPointJson.decode(row[1].sqlText())
                samples.add(
                    DistanceSample(
                        Durations.ofMillis(point.fix().monotonicMillis() - mono),
                        point.cumulativeMetres(),
                        point.heartRate(),
                        point.cadence(),
                        point.fix().altitude(),
                        point.discontinuity(),
                    )
                )
            }
            if (rows.size < 500) break
            last = rows.last()[0].sqlLong()
        }
        return activity.copy(samples = samples)
    }

    override fun upsertManualActivity(activity: ActivityRecord) {
        require(isUserActivity(activity)) { "Use the import boundary for provider activities." }
        database.transaction { putActivity(activity, true) }
    }

    override fun applyImport(
        provider: String,
        upserts: List<ActivityRecord>,
        deletedRecordIds: List<String>,
        nextToken: String?,
    ) {
        sqlRequired(provider, "provider")
        require(provider !in listOf("manual", "assessment")) {
            "Manual activities do not use provider imports."
        }
        require(upserts.all { it.source().provider() == provider }) {
            "Import source does not match provider."
        }
        database.transaction {
            for (recordId in deletedRecordIds) {
                val source =
                    listOf(
                        SqlValue.Text(provider),
                        SqlValue.Text(sqlRequired(recordId, "source record ID")),
                    )
                database.execute(
                    "DELETE FROM preserved_best_efforts WHERE provider=? AND record_id=?",
                    source,
                )
                database.execute("DELETE FROM activities WHERE provider=? AND record_id=?", source)
            }
            for (activity in upserts) putActivity(activity, false)
            database.execute(
                "INSERT OR REPLACE INTO import_checkpoints(provider,token,updated_at) VALUES(?,?,?)",
                listOf(SqlValue.Text(provider), sqlText(nextToken), SqlValue.Text(now().toString())),
            )
        }
    }

    internal fun putActivity(
        activity: ActivityRecord,
        userOwned: Boolean,
        assignDefault: Boolean = true,
    ) {
        sqlRequired(activity.id(), "activity ID")
        sqlRequired(activity.source().provider(), "activity provider")
        sqlRequired(activity.source().recordId(), "source record ID")
        val identity =
            listOf(
                SqlValue.Text(activity.id()),
                SqlValue.Text(activity.source().provider()),
                SqlValue.Text(activity.source().recordId()),
            )
        val existing =
            database.query(
                "SELECT id,provider,record_id FROM activities WHERE id=? OR (provider=? AND record_id=?)",
                identity,
            )
        require(existing.all { it == identity }) {
            "Activity identity conflicts with existing data."
        }
        val stored =
            if (activity.source().provider() in listOf("rdd", "gpx"))
                activity.copy(samples = emptyList())
            else activity
        val values =
            listOf(
                SqlValue.Text(activity.source().provider()),
                SqlValue.Text(activity.source().recordId()),
                sqlText(activity.source().originPackage()),
                SqlValue.Integer(storedEpochMillis(activity.start())),
                SqlValue.Integer(storedEpochMillis(activity.end())),
                SqlValue.Text(ActivityJson.encode(stored)),
                SqlValue.Text(activity.id()),
            )
        if (existing.isEmpty())
            database.execute(
                "INSERT INTO activities(provider,record_id,origin_package,start_millis,end_millis,payload,id) VALUES(?,?,?,?,?,?,?)",
                values,
            )
        else
            database.execute(
                "UPDATE activities SET provider=?,record_id=?,origin_package=?,start_millis=?,end_millis=?,payload=? WHERE id=?",
                values,
            )
        if (userOwned) {
            val shoe =
                if (existing.isEmpty() && assignDefault && activity.shoeId() == null) defaultShoe()
                else activity.shoeId()
            putAnnotation(
                activity.source().provider(),
                activity.source().recordId(),
                activity.notes(),
                activity.rpe(),
                shoe,
            )
        } else {
            val annotation =
                database.query(
                    "SELECT 1 FROM activity_annotations WHERE provider=? AND record_id=?",
                    identity.drop(1),
                )
            if (annotation.isEmpty())
                putAnnotation(
                    activity.source().provider(),
                    activity.source().recordId(),
                    null,
                    null,
                    if (assignDefault) defaultShoe() else null,
                )
        }
        database.execute(
            "DELETE FROM best_efforts WHERE activity_id=?",
            listOf(SqlValue.Text(activity.id())),
        )
        database.execute(
            "DELETE FROM preserved_best_efforts WHERE activity_id=? OR (provider=? AND record_id=?)",
            identity,
        )
        for (result in BestEffortCalculator.qualifiedResults(activity)) results.putResult(
            result,
            true,
        )
    }

    private fun defaultShoe(): String? =
        database.query("SELECT id FROM shoes WHERE is_default=1").firstOrNull()?.first()?.sqlText()

    override fun deleteActivity(id: String) =
        database.transaction {
            val key = listOf(SqlValue.Text(sqlRequired(id, "activity ID")))
            val source =
                database
                    .query("SELECT provider,record_id FROM activities WHERE id=?", key)
                    .firstOrNull()
            if (source != null)
                database.execute(
                    "DELETE FROM activity_annotations WHERE provider=? AND record_id=?",
                    source,
                )
            database.execute("DELETE FROM preserved_best_efforts WHERE activity_id=?", key)
            database.execute("DELETE FROM recording_sessions WHERE activity_id=?", key)
            database.execute("DELETE FROM activities WHERE id=?", key)
        }

    override fun importToken(provider: String): String? =
        database
            .query(
                "SELECT token FROM import_checkpoints WHERE provider=?",
                listOf(SqlValue.Text(sqlRequired(provider, "provider"))),
            )
            .firstOrNull()
            ?.single()
            ?.nullableSqlText()

    override fun saveActivityNotes(id: String, notes: String?, rpe: Double?) =
        updateAnnotation(id, notes ?: "", rpe, null, false)

    override fun assignShoe(activityId: String, shoeId: String?) =
        updateAnnotation(activityId, null, null, shoeId, true)

    private fun updateAnnotation(
        id: String,
        notes: String?,
        rpe: Double?,
        shoe: String?,
        changeShoe: Boolean,
    ) =
        database.transaction {
            val source =
                database
                    .query(
                        "SELECT provider,record_id FROM activities WHERE id=?",
                        listOf(SqlValue.Text(sqlRequired(id, "activity ID"))),
                    )
                    .firstOrNull() ?: throw IllegalArgumentException("Activity no longer exists.")
            val old =
                database
                    .query(
                        "SELECT notes,rpe,shoe_id FROM activity_annotations WHERE provider=? AND record_id=?",
                        source,
                    )
                    .firstOrNull()
            putAnnotation(
                source[0].sqlText(),
                source[1].sqlText(),
                if (changeShoe) old?.get(0)?.nullableSqlText() else notes,
                if (changeShoe) old?.get(1)?.nullableSqlNumber() else rpe,
                if (changeShoe) shoe else old?.get(2)?.nullableSqlText(),
            )
        }

    internal fun putAnnotation(
        provider: String,
        record: String,
        notes: String?,
        rpe: Double?,
        shoe: String?,
    ) {
        require(rpe == null || rpe.isFinite() && rpe in 0.0..10.0) {
            "RPE must be between 0 and 10."
        }
        database.execute(
            "INSERT OR REPLACE INTO activity_annotations(provider,record_id,notes,rpe,shoe_id) VALUES(?,?,?,?,?)",
            listOf(
                SqlValue.Text(sqlRequired(provider, "annotation provider")),
                SqlValue.Text(sqlRequired(record, "annotation record ID")),
                sqlText(notes),
                sqlNumber(rpe),
                sqlText(shoe),
            ),
        )
    }
}

internal fun isUserActivity(activity: ActivityRecord): Boolean =
    activity.source().provider() in listOf("manual", "assessment", "rdd", "gpx")
