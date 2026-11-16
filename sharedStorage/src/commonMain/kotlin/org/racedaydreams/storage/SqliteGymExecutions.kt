package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.GymExecutionRepository.*
import org.racedaydreams.domain.time.Instant

/**
 * Original append-only execution, atomic finalization and explicit actual edits on the same DB19.
 * No host may enable this until its REQUIRED legacy fingerprint provider is verified compatible.
 */
class SqliteGymExecutions(
    private val database: SqlDatabase,
    private val now: () -> Instant,
    fingerprints: LegacyStrengthFingerprint,
) : GymExecutionRepository {
    private val records = SqliteGymExecutionRecords(database)
    private val workouts = SqliteGymWorkouts(database)
    private val sources = SqliteGymSources(database, fingerprints)

    override fun load(id: String): Stored? = records.load(id)

    override fun unfinished(): List<Summary> = records.unfinished()

    override fun workout(id: String, revision: Int): GymWorkoutDefinition? =
        workouts.workout(id, revision)

    override fun preferences(): List<Preference> = workouts.preferences()

    override fun savePreference(preference: Preference) = workouts.savePreference(preference)

    fun favourites(limit: Int, offset: Int = 0): List<GymWorkoutDefinition> =
        workouts.favourites(limit, offset)

    fun finishedActualIds(candidates: List<String>): Set<String> =
        records.finishedActualIds(candidates)

    /** Original indexed, bounded actual selection; never hydrate execution/set/event histories. */
    fun recentActual(limit: Int): List<StrengthSession> {
        require(limit in 1..20) { "Choose 1–20 recent actual workouts." }
        val epoch = "CAST(ROUND((julianday(started_at)-2440587.5)*86400000) AS INTEGER)"
        return database
            .query(
                "SELECT payload FROM strength_sessions WHERE $epoch<=? ORDER BY $epoch DESC,id LIMIT ?",
                listOf(SqlValue.Integer(storedEpochMillis(now())), SqlValue.Integer(limit.toLong())),
            )
            .map { StrengthEvidenceJson.decodeSession(it.single().sqlText()) }
    }

    override fun saveWorkout(workout: GymWorkoutDefinition, source: GymExecution.SourceReference?) =
        database.transaction { sources.ensure(workout, source) }

    override fun start(initial: GymExecution): Stored {
        require(
            initial.status() == GymExecution.Status.ACTIVE &&
                initial.actualSets().isEmpty() &&
                initial.events().size == 1 &&
                initial.recordedElapsedMillis() == 0L &&
                initial.activeMillis() == 0L
        ) {
            "Start with an empty, active gym execution."
        }
        require(
            initial.source()?.kind() != GymExecution.SourceKind.ACTUAL_SESSION ||
                initial.id() != initial.source()?.id()
        ) {
            "A gym execution cannot repeat itself."
        }
        val parts = parts(initial)
        return database.transaction {
            require(sources.actual(initial.id()) == null) {
                "This gym identity already belongs to a completed strength workout."
            }
            val retained = records.load(initial.id())
            if (retained != null) {
                conflict(retained.execution() == initial)
                return@transaction retained
            }
            require(!records.hasUnfinished()) {
                "Finish or recover the unfinished gym workout before starting another."
            }
            sources.ensure(initial.prescription(), initial.source())
            database.execute(
                "INSERT INTO strength_executions(id,revision,origin,state,started_at,title,recorded_elapsed,active_millis,set_count,event_count,workout_id,workout_revision,header,accepted_actual,actual_edited_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                listOf(SqlValue.Text(initial.id())) + summary(initial, 1, parts.header),
            )
            append(initial, parts, 0, 0)
            Stored(initial, 1)
        }
    }

    override fun save(snapshot: GymExecution, expectedRevision: Long): Stored {
        require(snapshot.status() != GymExecution.Status.FINISHED) {
            "Final gym evidence must use atomic finalisation."
        }
        return persist(snapshot, expectedRevision, false)
    }

    override fun finish(finished: GymExecution, expectedRevision: Long): Stored {
        require(finished.status() == GymExecution.Status.FINISHED) {
            "Finish the gym execution before finalising its actual evidence."
        }
        return persist(finished, expectedRevision, true)
    }

    private fun persist(next: GymExecution, expected: Long, finish: Boolean): Stored {
        conflict(expected >= 1 && expected < Long.MAX_VALUE)
        val prepared = parts(next)
        val actualPayload =
            if (finish) bounded(StrengthEvidenceJson.encodeSession(next.completedSession()!!))
            else null
        return database.transaction {
            val previous =
                records.load(next.id())
                    ?: throw IllegalArgumentException("The staged gym workout is missing.")
            if (previous.execution() == next) {
                if (finish) verifyCompleted(next)
                return@transaction previous
            }
            conflict(previous.revision() == expected)
            val before = previous.execution()
            conflict(
                before.status() != GymExecution.Status.FINISHED &&
                    before.status() != GymExecution.Status.DISCARDED &&
                    before.origin() == next.origin() &&
                    before.prescription() == next.prescription() &&
                    before.source() == next.source() &&
                    before.startedAt() == next.startedAt() &&
                    before.offset() == next.offset() &&
                    next.recordedElapsedMillis() >= before.recordedElapsedMillis() &&
                    next.activeMillis() >= before.activeMillis() &&
                    prefix(before.actualSets(), next.actualSets()) &&
                    prefix(before.events(), next.events())
            )
            sources.ensure(next.prescription(), next.source())
            if (finish) {
                require(!next.startedAt().isAfter(now())) {
                    "A completed strength session cannot be in the future."
                }
                saveActualAndAssociation(next, actualPayload!!)
            }
            database.execute(
                "UPDATE strength_executions SET revision=?,origin=?,state=?,started_at=?,title=?,recorded_elapsed=?,active_millis=?,set_count=?,event_count=?,workout_id=?,workout_revision=?,header=?,accepted_actual=?,actual_edited_at=? WHERE id=? AND revision=?",
                summary(next, expected + 1, prepared.header) +
                    listOf(SqlValue.Text(next.id()), SqlValue.Integer(expected)),
            )
            conflict(database.query("SELECT changes()").single().single().sqlLong() == 1L)
            append(next, prepared, before.actualSets().size, before.events().size)
            Stored(next, expected + 1)
        }
    }

    /**
     * Same operation as original CoachingStore.saveStrength; edited actual is never original
     * execution.
     */
    fun saveActual(edited: StrengthSession) {
        require(!edited.startedAt().isAfter(now())) {
            "A completed strength session cannot be in the future."
        }
        database.transaction {
            val staged =
                database
                    .query(
                        "SELECT state,accepted_actual FROM strength_executions WHERE id=?",
                        texts(edited.id()),
                    )
                    .firstOrNull()
            if (staged != null) {
                require(staged[0].sqlText() == GymExecution.Status.FINISHED.name) {
                    "An unfinished gym execution cannot be replaced by a completed workout."
                }
                conflict(
                    StrengthEvidenceJson.decodeAcceptedSession(staged[1].sqlText()) ==
                        sources.actual(edited.id())
                )
            }
            val at = now()
            sources.acknowledgeChange(edited.id(), edited, at)
            if (staged != null) {
                database.execute(
                    "UPDATE strength_executions SET accepted_actual=?,actual_edited_at=? WHERE id=?",
                    texts(
                        bounded(StrengthEvidenceJson.encodeSession(edited)),
                        at.toString(),
                        edited.id(),
                    ),
                )
                conflict(database.query("SELECT changes()").single().single().sqlLong() == 1L)
            }
            val payload = StrengthEvidenceJson.encodeSession(edited)
            database.execute(
                "UPDATE strength_sessions SET id=?,started_at=?,payload=? WHERE id=?",
                texts(edited.id(), edited.startedAt().toString(), payload, edited.id()),
            )
            if (database.query("SELECT changes()").single().single().sqlLong() == 0L)
                database.execute(
                    "INSERT INTO strength_sessions(id,started_at,payload) VALUES(?,?,?)",
                    texts(edited.id(), edited.startedAt().toString(), payload),
                )
        }
    }

    /**
     * Explicit actual deletion also removes its choices, while retaining acknowledged repeat
     * templates.
     */
    fun deleteActual(id: String) =
        database.transaction {
            sqlRequired(id, "gym execution")
            val state =
                database
                    .query("SELECT state FROM strength_executions WHERE id=?", texts(id))
                    .firstOrNull()
                    ?.single()
                    ?.sqlText()
            require(state == null || state == "FINISHED" || state == "DISCARDED") {
                "Finish or discard the unfinished gym workout first."
            }
            sources.acknowledgeChange(id, null, now())
            database.execute("DELETE FROM strength_executions WHERE id=?", texts(id))
            database.execute(
                "DELETE FROM strength_association_targets WHERE actual_id=?",
                texts(id),
            )
            database.execute(
                "DELETE FROM strength_association_decisions WHERE actual_id=?",
                texts(id),
            )
            database.execute("DELETE FROM strength_sessions WHERE id=?", texts(id))
            database.execute(
                "DELETE FROM coach_evidence WHERE kind='STRENGTH_LINK' AND id=?",
                texts(id),
            )
        }

    private fun saveActualAndAssociation(finished: GymExecution, payload: String) {
        val actual = sources.actual(finished.id())
        conflict(actual == null || actual == finished.completedSession())
        if (actual == null)
            database.execute(
                "INSERT INTO strength_sessions(id,started_at,payload) VALUES(?,?,?)",
                texts(finished.id(), finished.startedAt().toString(), payload),
            )
        val source = finished.plannedAssociation() ?: return
        val link = plannedLink(finished, source)
        val existing =
            database
                .query(
                    "SELECT payload FROM coach_evidence WHERE kind='STRENGTH_LINK' AND id=?",
                    texts(finished.id()),
                )
                .firstOrNull()
        if (existing != null)
            conflict(sameLink(GymStoredJson.read(existing.single().sqlText()), link))
        else
            database.execute(
                "INSERT INTO coach_evidence(kind,id,payload) VALUES('STRENGTH_LINK',?,?)",
                texts(finished.id(), bounded(link.toString())),
            )
        project(finished.id(), link)
    }

    private fun verifyCompleted(finished: GymExecution) {
        conflict(finished.completedSession() == sources.actual(finished.id()))
        val source = finished.plannedAssociation() ?: return
        val link =
            database
                .query(
                    "SELECT payload FROM coach_evidence WHERE kind='STRENGTH_LINK' AND id=?",
                    texts(finished.id()),
                )
                .firstOrNull()
        conflict(
            link != null &&
                sameLink(GymStoredJson.read(link.single().sqlText()), plannedLink(finished, source))
        )
    }

    private fun plannedLink(
        finished: GymExecution,
        source: GymExecution.SourceReference,
    ): JsonObject = buildJsonObject {
        put("id", finished.id())
        put("plannedDate", sources.programmeDate(finished.prescription()!!, source).toString())
        put("gymVersion", "${source.id()}:v${source.revision()}")
        put("gymSession", source.sessionId())
    }

    private fun project(id: String, link: JsonObject) {
        val version = GymStoredJson.text(link, "gymVersion", 256)
        val session = GymStoredJson.text(link, "gymSession", 256)
        val date = GymStoredJson.text(link, "plannedDate", 64)
        val retained =
            database
                .query(
                    "SELECT programme_key,session_id,planned_date,decision_id FROM strength_association_targets WHERE actual_id=?",
                    texts(id),
                )
                .firstOrNull()
        if (retained != null && retained[3] != SqlValue.Null) {
            conflict(retained.take(3) == texts(version, session, date))
            return
        }
        database.execute(
            "INSERT OR REPLACE INTO strength_association_targets(actual_id,programme_key,session_id,planned_date,decision_id) VALUES(?,?,?,?,NULL)",
            texts(id, version, session, date),
        )
    }

    private data class Parts(val header: String, val sets: List<String>, val events: List<String>)

    private fun parts(execution: GymExecution): Parts {
        val root = GymExecutionSnapshotJson.element(execution)
        val header = JsonObject(root - setOf("actualSets", "events"))
        val reduced =
            if (execution.completedSession() == null) header
            else
                JsonObject(
                    header +
                        ("completedSession" to
                            JsonObject(
                                header.getValue("completedSession").jsonObject - "exercises"
                            ))
                )
        return Parts(
            bounded(reduced.toString()),
            root.getValue("actualSets").jsonArray.map { bounded(it.toString()) },
            root.getValue("events").jsonArray.map { bounded(it.toString()) },
        )
    }

    private fun summary(e: GymExecution, revision: Long, header: String): List<SqlValue> =
        listOf(
            SqlValue.Integer(revision),
            SqlValue.Text(e.origin().name),
            SqlValue.Text(e.status().name),
            SqlValue.Text(e.startedAt().toString()),
            SqlValue.Text(e.prescription()?.title() ?: "Free workout"),
            SqlValue.Integer(e.recordedElapsedMillis()),
            SqlValue.Integer(e.activeMillis()),
            SqlValue.Integer(e.actualSets().size.toLong()),
            SqlValue.Integer(e.events().size.toLong()),
            sqlText(e.prescription()?.id()),
            e.prescription()?.revision()?.let { SqlValue.Integer(it.toLong()) } ?: SqlValue.Null,
            SqlValue.Text(header),
            e.completedSession()?.let {
                SqlValue.Text(bounded(StrengthEvidenceJson.encodeSession(it)))
            } ?: SqlValue.Null,
            SqlValue.Null,
        )

    private fun append(e: GymExecution, p: Parts, existingSets: Int, existingEvents: Int) {
        for (i in existingSets until p.sets.size) database.execute(
            "INSERT INTO strength_execution_sets(execution_id,sequence,actual_set_id,payload) VALUES(?,?,?,?)",
            listOf(
                SqlValue.Text(e.id()),
                SqlValue.Integer(i.toLong()),
                SqlValue.Text(e.actualSets()[i].actual().id()),
                SqlValue.Text(p.sets[i]),
            ),
        )
        for (i in existingEvents until p.events.size) database.execute(
            "INSERT INTO strength_execution_events(execution_id,sequence,payload) VALUES(?,?,?)",
            listOf(SqlValue.Text(e.id()), SqlValue.Integer(i.toLong()), SqlValue.Text(p.events[i])),
        )
    }

    private fun <T> prefix(before: List<T>, after: List<T>): Boolean =
        after.size >= before.size && before == after.take(before.size)

    private fun sameLink(actual: JsonObject, expected: JsonObject): Boolean =
        listOf("id", "plannedDate", "gymVersion", "gymSession").all { actual[it] == expected[it] }

    private fun bounded(payload: String): String =
        payload.also {
            require(it.encodeToByteArray().size <= 262144) {
                "This gym record exceeds the supported 256 KiB row limit. Existing staged sets were retained."
            }
        }

    private fun texts(vararg values: String): List<SqlValue> = values.map { SqlValue.Text(it) }

    private fun conflict(ok: Boolean) =
        require(ok) { "The gym workout changed. Reload and review the saved evidence." }
}
