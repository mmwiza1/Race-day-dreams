package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.body.BodyWeight
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.presentation.StrengthTodaySummary
import org.racedaydreams.domain.strength.HistoricalStrengthRecord
import org.racedaydreams.domain.time.*
import org.racedaydreams.storage.DevelopmentArchiveJson.Evidence
import org.racedaydreams.storage.DevelopmentArchiveJson.Kind

/**
 * Existing typed coaching context and optional weekly decisions over the original coach_evidence
 * rows. Not a backup exporter: imported body-weight observations remain visible locally. The
 * application supplies one database/operation owner and a coaching repository using that same DB.
 */
class SqliteCoachingContext(
    private val database: SqlDatabase,
    private val now: () -> Instant,
    private val nextId: () -> String,
) {
    fun weights(): List<BodyWeight> =
        Immutable.copyList(
            rows(Kind.WEIGHT).map { (it.evidence as Evidence.Weight).value }.sortedBy { it.at() }
        )

    fun saveWeight(value: BodyWeight) =
        put(
            Kind.WEIGHT,
            value.id(),
            buildJsonObject {
                put("id", value.id())
                put("kg", value.kilograms())
                put("at", value.at().toString())
                put("offset", value.offset().toString())
                put("provider", value.provider())
                put("sourceId", value.sourceId())
                put("origin", StoredJson.nullable(value.origin()))
            },
        )

    fun deleteWeight(id: String) {
        database.execute(
            "DELETE FROM coach_evidence WHERE kind='WEIGHT' AND id=?",
            listOf(SqlValue.Text(id)),
        )
    }

    /**
     * Only an authoritative completed provider window may replace its records. Visible-only Apple
     * reads must use their existing qualified merge instead, never this command.
     */
    fun replaceWeightWindow(
        provider: String,
        from: Instant,
        until: Instant,
        values: List<BodyWeight>,
    ) =
        database.transaction {
            for (value in weights()) {
                if (
                    value.provider() == provider &&
                        !value.at().isBefore(from) &&
                        value.at().isBefore(until)
                )
                    deleteWeight(value.id())
            }
            for (value in values) {
                require(
                    value.provider() == provider &&
                        !value.at().isBefore(from) &&
                        value.at().isBefore(until)
                ) {
                    "Weight is outside its import scope."
                }
                saveWeight(value)
            }
        }

    fun saveHistorical(value: HistoricalStrengthRecord) =
        put(
            Kind.HISTORICAL_PB,
            value.id(),
            buildJsonObject {
                put("id", value.id())
                put("exercise", StrengthEvidenceJson.exercise(value.exercise()))
                put("kg", value.kilograms())
                put("reps", value.reps())
                put("date", StoredJson.nullable(value.date()?.toString()))
                put("precision", value.precision().name)
                put("notes", StoredJson.nullable(value.notes()))
                put("seconds", value.durationSeconds()?.let { JsonPrimitive(it) } ?: JsonNull)
                put("metres", value.distanceMetres()?.let { JsonPrimitive(it) } ?: JsonNull)
            },
        )

    fun deleteHistorical(id: String) {
        database.execute(
            "DELETE FROM coach_evidence WHERE kind='HISTORICAL_PB' AND id=?",
            listOf(SqlValue.Text(id)),
        )
    }

    fun linkGym(actual: String, planned: LocalDate, version: String, session: String) =
        saveStrengthLink(actual, planned, version, session)

    fun linkStrength(actual: String, planned: LocalDate) =
        saveStrengthLink(actual, planned, null, null)

    private fun saveStrengthLink(
        actual: String,
        planned: LocalDate,
        version: String?,
        session: String?,
    ) =
        database.transaction {
            if (version != null && session != null)
                require(
                    session !in
                        completedGymSessions(database, version.substringBeforeLast(":v"), actual)
                ) {
                    "This programmed session is already completed."
                }
            val payload = buildJsonObject {
                put("id", actual)
                put("plannedDate", planned.toString())
                if (version != null) {
                    put("gymVersion", version)
                    put("gymSession", session)
                }
            }
            put(Kind.STRENGTH_LINK, actual, payload)
            val values =
                listOf(sqlText(version), sqlText(session), SqlValue.Text(planned.toString()))
            val retained =
                database
                    .query(
                        "SELECT programme_key,session_id,planned_date,decision_id FROM strength_association_targets WHERE actual_id=?",
                        listOf(SqlValue.Text(actual)),
                    )
                    .firstOrNull()
            if (retained != null && retained[3] != SqlValue.Null) {
                require(retained.take(3) == values) {
                    "The workout or association changed. Review the saved evidence again."
                }
            } else {
                database.execute(
                    "INSERT OR REPLACE INTO strength_association_targets(actual_id,programme_key,session_id,planned_date,decision_id) VALUES(?,?,?,?,NULL)",
                    listOf(SqlValue.Text(actual)) + values,
                )
            }
        }

    fun deleteStrengthLink(id: String) =
        database.transaction {
            val retained =
                database
                    .query(
                        "SELECT decision_id FROM strength_association_targets WHERE actual_id=?",
                        listOf(SqlValue.Text(id)),
                    )
                    .firstOrNull()
            require(retained == null || retained.single() == SqlValue.Null) {
                "Review the explicit association choice before removing it."
            }
            database.execute(
                "DELETE FROM strength_association_targets WHERE actual_id=?",
                listOf(SqlValue.Text(id)),
            )
            database.execute(
                "DELETE FROM coach_evidence WHERE kind='STRENGTH_LINK' AND id=?",
                listOf(SqlValue.Text(id)),
            )
        }

    fun historical(): List<HistoricalStrengthRecord> =
        Immutable.copyList(
            rows(Kind.HISTORICAL_PB).map { (it.evidence as Evidence.Historical).value }
        )

    fun sessionDecisions(): List<WorkoutDecision> =
        Immutable.copyList(
            rows(Kind.SESSION_DECISION)
                .map { (it.evidence as Evidence.SessionDecision).value }
                .sortedBy { it.at() }
        )

    fun strengthLinks(): Map<String, LocalDate> =
        Immutable.copyMap(
            rows(Kind.STRENGTH_LINK).associate {
                it.id to (it.evidence as Evidence.StrengthLink).plannedDate
            }
        )

    fun gymLinks(): Map<String, StrengthTodaySummary.Association> =
        Immutable.copyMap(
            rows(Kind.STRENGTH_LINK)
                .mapNotNull { row ->
                    val link = row.evidence as Evidence.StrengthLink
                    if (link.gymVersion == null || link.gymSession == null) null
                    else
                        row.id to
                            StrengthTodaySummary.Association(
                                row.id,
                                link.gymVersion,
                                link.gymSession,
                            )
                }
                .toMap()
        )

    fun interruption(): WeeklyCoaching.Interruption? =
        (rows(Kind.INTERRUPTION).firstOrNull()?.evidence as? Evidence.Interruption)?.value

    fun saveInterruption(value: WeeklyCoaching.Interruption) =
        put(
            Kind.INTERRUPTION,
            "current",
            buildJsonObject {
                put("id", "current")
                put("from", value.from().toString())
                put("returned", value.returned().toString())
                put("reason", value.reason())
            },
        )

    fun saveSessionDecision(value: WorkoutDecision) =
        put(
            Kind.SESSION_DECISION,
            value.id(),
            buildJsonObject {
                put("id", value.id())
                put("workoutId", value.workoutId())
                put("versionId", value.versionId())
                put("originalDate", value.originalDate().toString())
                put("movedTo", StoredJson.nullable(value.movedTo()?.toString()))
                put("kind", value.kind().name)
                put("at", value.at().toString())
            },
        )

    fun reviewKnown(id: String): Boolean =
        database
            .query(
                "SELECT 1 FROM coach_evidence WHERE kind='WEEKLY_REVIEW' AND id=?",
                listOf(SqlValue.Text(id)),
            )
            .isNotEmpty()

    fun reviewDecision(id: String): String? =
        record(Kind.REVIEW_DECISION, id)?.let { (it.evidence as Evidence.ReviewDecision).choice }

    /**
     * Same week, recommendation and ordered observations; a changed base ID alone is not evidence.
     */
    fun decisionForReview(review: WeeklyCoaching.Review): String? {
        val observations = JsonArray(review.observations().map { JsonPrimitive(it) })
        for (decision in rows(Kind.REVIEW_DECISION)) {
            val previous = record(Kind.WEEKLY_REVIEW, decision.id) ?: continue
            if (
                previous.payload["week"] == JsonPrimitive(review.week().toString()) &&
                    previous.payload["recommendation"] == JsonPrimitive(review.recommendation()) &&
                    previous.payload["observations"] == observations
            )
                return (decision.evidence as Evidence.ReviewDecision).choice
        }
        return null
    }

    fun saveReview(review: WeeklyCoaching.Review) =
        put(
            Kind.WEEKLY_REVIEW,
            requireNotNull(review.id()),
            buildJsonObject {
                put("id", review.id())
                put("base", review.baseVersionId())
                put("week", review.week().toString())
                put("planned", review.planned())
                put("completed", review.completed())
                put("km", review.kilometres())
                put("strength", review.strengthSessions())
                put("longest", StoredJson.nullable(review.longestKm()))
                put("observations", JsonArray(review.observations().map { JsonPrimitive(it) }))
                put(
                    "channels",
                    buildJsonArray {
                        for (channel in review.channels()) add(
                            buildJsonObject {
                                put("name", channel.name())
                                put("state", requireNotNull(channel.state()).name)
                                put("reason", channel.reason())
                            }
                        )
                    },
                )
                put("recommendation", review.recommendation())
                put(
                    "choices",
                    buildJsonArray {
                        for (choice in review.choices()) add(
                            buildJsonObject {
                                put("id", choice.id())
                                put("label", choice.label())
                                put(
                                    "replacements",
                                    JsonArray(
                                        choice.replacements().map { PlannedWorkoutJson.element(it) }
                                    ),
                                )
                            }
                        )
                    },
                )
            },
        )

    /**
     * Only an explicit choice may create a revision. Original WeeklyCoaching applies all guards.
     */
    fun decide(
        review: WeeklyCoaching.Review,
        choiceId: String,
        coaching: CoachingRepository,
        completed: Set<String>,
    ) =
        database.transaction {
            val id = requireNotNull(review.id())
            require(reviewDecision(id) == null) { "This review was already resolved." }
            val current =
                requireNotNull(coaching.current()) { "Review no longer matches the plan." }
            val selected =
                review.choices().firstOrNull { it.id() == choiceId }
                    ?: throw IllegalArgumentException("Choose a review option.")
            val plan = WeeklyCoaching.apply(current, review, selected, completed)
            var resulting = current.id()
            if (selected.replacements().isNotEmpty()) {
                resulting = nextId()
                coaching.saveCurrent(
                    PlanVersion(
                        resulting,
                        current.planId(),
                        current.version() + 1,
                        now(),
                        "Weekly review: " + selected.label(),
                        current.goalSnapshot(),
                        current.evidenceSnapshot(),
                        plan,
                    )
                )
            }
            put(
                Kind.REVIEW_DECISION,
                id,
                buildJsonObject {
                    put("id", id)
                    put("base", current.id())
                    put("choice", choiceId)
                    put("result", resulting)
                },
            )
        }

    private fun put(kind: Kind, id: String, payload: JsonObject) {
        validated(kind, id, payload)
        database.transaction {
            val values =
                listOf(
                    SqlValue.Text(payload.toString()),
                    SqlValue.Text(kind.name),
                    SqlValue.Text(id),
                )
            database.execute("UPDATE coach_evidence SET payload=? WHERE kind=? AND id=?", values)
            if (database.query("SELECT changes()").single().single().sqlLong() == 0L) {
                database.execute(
                    "INSERT INTO coach_evidence(payload,kind,id) VALUES(?,?,?)",
                    values,
                )
            }
        }
    }

    private fun rows(kind: Kind): List<DevelopmentArchiveJson.Record> =
        database
            .query(
                "SELECT id,payload FROM coach_evidence WHERE kind=? ORDER BY id",
                listOf(SqlValue.Text(kind.name)),
            )
            .map { validated(kind, it[0].sqlText(), StoredJson.objectValue(it[1].sqlText())) }

    private fun record(kind: Kind, id: String): DevelopmentArchiveJson.Record? =
        database
            .query(
                "SELECT payload FROM coach_evidence WHERE kind=? AND id=?",
                listOf(SqlValue.Text(kind.name), SqlValue.Text(id)),
            )
            .firstOrNull()
            ?.let { validated(kind, id, StoredJson.objectValue(it.single().sqlText())) }

    private fun validated(
        kind: Kind,
        id: String,
        payload: JsonObject,
    ): DevelopmentArchiveJson.Record =
        DevelopmentArchiveJson.decode(
                buildJsonObject {
                    put("version", 1)
                    put(
                        "records",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("kind", kind.name)
                                    put("id", id)
                                    put("payload", payload)
                                }
                            )
                        },
                    )
                }
            )
            .records
            .single()
}
