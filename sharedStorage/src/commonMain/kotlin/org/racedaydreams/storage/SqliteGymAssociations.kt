package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.GymWorkoutAssociation.Choice
import org.racedaydreams.domain.strength.GymWorkoutAssociation.Decision
import org.racedaydreams.domain.strength.GymWorkoutAssociation.Proposal
import org.racedaydreams.domain.time.Instant

/** Explicit association choices and coaching projections share the original SQLite transaction. */
class SqliteGymAssociations(private val database: SqlDatabase, private val now: () -> Instant) :
    GymAssociationRepository {
    private val executions = SqliteGymExecutionRecords(database)
    private val programmes = SqliteStrengthProgrammingRecords(database)
    // Reads only. Association commands never generate legacy programme decisions/identities.
    private val legacy =
        SqliteLegacyGymProgrammes(database, now) {
            error("Association cannot create legacy programming.")
        }

    override fun proposals(actualId: String, reviewedAt: Instant): List<Proposal> =
        database.transaction {
            val stored = executions.load(sqlRequired(actualId, "actual workout"))
            if (
                stored == null ||
                    stored.execution().status() != GymExecution.Status.FINISHED ||
                    linked(actualId)
            )
                return@transaction emptyList()
            val reference = programmes.currentReference()
            if (reference?.owner() == StrengthProgrammingRepository.Owner.AUTHORED)
                GymWorkoutAssociation.authoredProposals(
                    actual(actualId),
                    stored.execution().origin(),
                    programmes.programme(reference.id(), reference.revision()),
                    occupied(reference.key(), actualId),
                    reviewedAt,
                )
            else {
                val plan = legacy.current()
                GymWorkoutAssociation.proposals(
                    actual(actualId),
                    stored.execution().origin(),
                    plan,
                    if (plan == null) emptySet()
                    else occupied("${plan.id()}:v${plan.version()}", actualId),
                    reviewedAt,
                )
            }
        }

    override fun current(actualId: String): Decision? =
        database
            .query(
                "SELECT d.payload FROM strength_association_targets t JOIN strength_association_decisions d ON t.decision_id=d.id WHERE t.actual_id=?",
                texts(sqlRequired(actualId, "actual workout")),
            )
            .firstOrNull()
            ?.let { GymAssociationDecisionJson.decode(it.single().sqlText()) }

    /** Complete explicit choice history; legacy projections do not invent athlete decisions. */
    fun archive(): GymAssociationArchiveJson.Archive =
        database.transaction {
            GymAssociationArchiveJson.Archive(
                database
                    .query("SELECT payload FROM strength_association_decisions ORDER BY id")
                    .map { GymAssociationDecisionJson.decode(it.single().sqlText()) },
                database
                    .query(
                        "SELECT actual_id,decision_id FROM strength_association_targets WHERE decision_id IS NOT NULL ORDER BY actual_id"
                    )
                    .associate { it[0].sqlText() to it[1].sqlText() },
            )
        }

    override fun reviewCurrent(actualId: String, reviewedAt: Instant): Proposal? =
        database.transaction {
            val choice = current(actualId) ?: return@transaction null
            val stored = executions.load(actualId)
            checkConflict(
                stored != null && stored.execution().status() == GymExecution.Status.FINISHED
            )
            val p = choice.proposal()
            val review = p.reviewed(actual(actualId), stored!!.execution().origin(), reviewedAt)
            if (review.authored())
                GymWorkoutAssociation.validateAuthoredRetainedEvidence(
                    review,
                    review.actual(),
                    stored.execution().origin(),
                    programmes.programme(p.programmeId(), p.programmeRevision()),
                )
            else
                GymWorkoutAssociation.validateRetainedEvidence(
                    review,
                    review.actual(),
                    stored.execution().origin(),
                    legacy.version(p.programmeId(), p.programmeRevision()),
                )
            review
        }

    override fun decide(decision: Decision): Decision =
        database.transaction {
            val payload = GymAssociationDecisionJson.encode(decision)
            require(payload.encodeToByteArray().size <= 524288) {
                "This association snapshot exceeds the supported size. Actual training was preserved."
            }
            val previous =
                database
                    .query(
                        "SELECT payload FROM strength_association_decisions WHERE id=?",
                        texts(decision.id()),
                    )
                    .firstOrNull()
                    ?.let { GymAssociationDecisionJson.decode(it.single().sqlText()) }
            if (previous != null) {
                checkConflict(
                    previous == decision && decision == current(decision.proposal().actual().id())
                )
                return@transaction previous
            }
            val p = decision.proposal()
            val id = p.actual().id()
            val stored = executions.load(id)
            checkConflict(
                stored != null && stored.execution().status() == GymExecution.Status.FINISHED
            )
            checkConflict(!decision.decidedAt().isAfter(now()))
            if (decision.choice() == Choice.KEEP_EXTRA && linked(id)) {
                val target =
                    database
                        .query(
                            "SELECT programme_key,session_id FROM strength_association_targets WHERE actual_id=?",
                            texts(id),
                        )
                        .firstOrNull()
                checkConflict(
                    target != null &&
                        target[0] == SqlValue.Text(key(p)) &&
                        target[1] == SqlValue.Text(p.sessionId())
                )
            }
            val origin = stored!!.execution().origin()
            if (p.authored()) {
                if (decision.choice() == Choice.COUNT_TOWARD_SESSION) {
                    val reference = programmes.currentReference()
                    checkConflict(
                        reference?.owner() == StrengthProgrammingRepository.Owner.AUTHORED &&
                            reference.key() == key(p)
                    )
                }
                val plan = programmes.programme(p.programmeId(), p.programmeRevision())
                if (decision.choice() == Choice.KEEP_EXTRA)
                    GymWorkoutAssociation.validateAuthoredRetainedEvidence(
                        p,
                        actual(id),
                        origin,
                        plan,
                    )
                else
                    GymWorkoutAssociation.validateAuthoredForAcceptance(
                        p,
                        actual(id),
                        origin,
                        plan,
                        occupied(key(p), id),
                        linked(id),
                    )
            } else {
                val plan =
                    if (decision.choice() == Choice.COUNT_TOWARD_SESSION) legacy.current()
                    else legacy.version(p.programmeId(), p.programmeRevision())
                if (decision.choice() == Choice.KEEP_EXTRA)
                    GymWorkoutAssociation.validateRetainedEvidence(p, actual(id), origin, plan)
                else
                    GymWorkoutAssociation.validateForAcceptance(
                        p,
                        actual(id),
                        origin,
                        plan,
                        occupied(key(p), id),
                        linked(id),
                    )
            }
            database.execute(
                "INSERT INTO strength_association_decisions(id,actual_id,decided_at,payload) VALUES(?,?,?,?)",
                texts(decision.id(), id, decision.decidedAt().toString(), payload),
            )
            if (decision.choice() == Choice.COUNT_TOWARD_SESSION) {
                val link = buildJsonObject {
                    put("id", id)
                    put("plannedDate", p.date().toString())
                    put("gymVersion", key(p))
                    put("gymSession", p.sessionId())
                }
                database.execute(
                    "INSERT OR REPLACE INTO coach_evidence(kind,id,payload) VALUES('STRENGTH_LINK',?,?)",
                    texts(id, link.toString()),
                )
            } else
                database.execute(
                    "DELETE FROM coach_evidence WHERE kind='STRENGTH_LINK' AND id=?",
                    texts(id),
                )
            val counted = decision.choice() == Choice.COUNT_TOWARD_SESSION
            database.execute(
                "INSERT OR REPLACE INTO strength_association_targets(actual_id,decision_id,programme_key,session_id,planned_date) VALUES(?,?,?,?,?)",
                texts(id, decision.id()) +
                    listOf(
                        sqlText(if (counted) key(p) else null),
                        sqlText(if (counted) p.sessionId() else null),
                        sqlText(if (counted) p.date().toString() else null),
                    ),
            )
            decision
        }

    /**
     * Restore retained projections and explicit decisions, never call decide or regenerate links.
     */
    internal fun populateArchive(parent: UserBackupDataJson.Snapshot) =
        database.transaction {
            for (table in
                listOf("strength_association_targets", "strength_association_decisions")) require(
                database.query("SELECT 1 FROM $table LIMIT 1").isEmpty()
            ) {
                "Association restore requires empty prepared tables; existing data was retained."
            }
            // Historical links remain historical, including links predating explicit association
            // UI.
            for ((id, link) in GymExecutionArchiveJson.links(parent.development)) database.execute(
                "INSERT INTO strength_association_targets(actual_id,programme_key,session_id,planned_date,decision_id) VALUES(?,?,?,?,NULL)",
                listOf(
                    SqlValue.Text(id),
                    sqlText(link.programmeKey),
                    sqlText(link.sessionId),
                    SqlValue.Text(link.plannedDate),
                ),
            )
            val archive = parent.engine?.associations ?: return@transaction
            val decisions = archive.decisions.associateBy { it.id() }
            for (d in archive.decisions) {
                val payload = GymAssociationDecisionJson.encode(d)
                require(payload.encodeToByteArray().size <= 524288) {
                    "Association snapshot is too large."
                }
                database.execute(
                    "INSERT INTO strength_association_decisions(id,actual_id,decided_at,payload) VALUES(?,?,?,?)",
                    texts(d.id(), d.proposal().actual().id(), d.decidedAt().toString(), payload),
                )
            }
            for ((id, decision) in archive.current) {
                val d = decisions.getValue(decision)
                val p = d.proposal()
                require(id == p.actual().id()) {
                    "Association target differs from its recorded decision."
                }
                val counted = d.choice() == Choice.COUNT_TOWARD_SESSION
                database.execute(
                    "INSERT OR REPLACE INTO strength_association_targets(actual_id,decision_id,programme_key,session_id,planned_date) VALUES(?,?,?,?,?)",
                    texts(id, d.id()) +
                        listOf(
                            sqlText(if (counted) key(p) else null),
                            sqlText(if (counted) p.sessionId() else null),
                            sqlText(if (counted) p.date().toString() else null),
                        ),
                )
            }
        }

    private fun actual(id: String): StrengthSession =
        database
            .query("SELECT payload FROM strength_sessions WHERE id=?", texts(id))
            .firstOrNull()
            ?.let { StrengthEvidenceJson.decodeSession(it.single().sqlText()) }
            ?: throw IllegalArgumentException(
                "The workout or association changed. Review the saved evidence again."
            )

    private fun linked(id: String): Boolean =
        database
            .query(
                "SELECT 1 FROM strength_association_targets WHERE actual_id=? AND planned_date IS NOT NULL",
                texts(id),
            )
            .isNotEmpty()

    private fun occupied(key: String, except: String): Set<String?> =
        database
            .query(
                "SELECT session_id FROM strength_association_targets WHERE programme_key=? AND actual_id<>?",
                texts(key, except),
            )
            .map { it.single().nullableSqlText() }
            .toSet()

    private fun key(p: Proposal): String = "${p.programmeId()}:v${p.programmeRevision()}"

    private fun texts(vararg values: String): List<SqlValue> = values.map { SqlValue.Text(it) }

    private fun checkConflict(condition: Boolean) =
        require(condition) {
            "The workout or association changed. Review the saved evidence again."
        }
}
