package org.racedaydreams.storage

import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.ConcreteStrengthProgrammeDraftRepository.Summary
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.LocalDate

/**
 * Reviewed concrete previews use the same programme acceptance transaction, never another engine.
 */
class SqliteConcreteStrengthProgrammes(
    database: SqlDatabase,
    now: () -> Instant,
    today: () -> LocalDate,
) : ConcreteStrengthProgrammeDraftRepository {
    private val database = database
    private val records = SqliteStrengthProgrammingRecords(database)
    private val drafts = SqliteStrengthDrafts(database)
    private val programmes = SqliteStrengthProgramming(database, now, today)

    override fun get(id: String): ConcreteStrengthProgrammeDraft? = records.concreteDraft(id)

    override fun list(limit: Int, offset: Int): List<Summary> =
        records.concreteDrafts(limit, offset)

    override fun save(draft: ConcreteStrengthProgrammeDraft, expectedRevision: Int) =
        drafts.saveConcrete(draft, expectedRevision)

    override fun delete(id: String, expectedRevision: Int) =
        drafts.deleteConcrete(id, expectedRevision)

    override fun activate(
        reviewed: ConcreteStrengthProgrammeDraft,
        decisionId: String,
    ): StrengthProgrammingRepository.Decision =
        database.transaction {
            sqlRequired(decisionId, "decision")
            require(
                programmes.decision(decisionId) != null ||
                    records.concreteDraft(reviewed.id()) == reviewed
            ) {
                "This preview changed. Reload and review before activating."
            }
            programmes.activate(
                reviewed.programme(),
                reviewed.before(),
                decisionId,
                reviewed.substitutions(),
            )
        }
}
