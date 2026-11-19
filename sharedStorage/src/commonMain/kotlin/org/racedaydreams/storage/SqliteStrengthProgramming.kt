package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.StrengthProgrammingRepository.*
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.LocalDate
import org.racedaydreams.storage.GymStoredJson.array
import org.racedaydreams.storage.GymStoredJson.row
import org.racedaydreams.storage.GymStoredJson.text
import org.racedaydreams.storage.GymStoredJson.write

/** Original reviewed authoring commands. Prescriptions/decisions commit together, never actuals. */
class SqliteStrengthProgramming(
    private val database: SqlDatabase,
    private val now: () -> Instant,
    private val today: () -> LocalDate,
) : StrengthProgrammingRepository {
    private val records = SqliteStrengthProgrammingRecords(database)
    private val drafts = SqliteStrengthDrafts(database)

    override fun currentReference(): Reference? = records.currentReference()

    override fun programme(id: String, revision: Int): StrengthProgramme? =
        records.programme(id, revision)

    override fun reviewRevision(reference: Reference): RevisionReview =
        records.reviewRevision(reference)

    override fun versions(limit: Int, offset: Int): List<Summary> = records.versions(limit, offset)

    override fun draft(id: String): StrengthProgrammeDraft? = records.draft(id)

    override fun drafts(limit: Int, offset: Int): List<DraftSummary> = records.drafts(limit, offset)

    override fun saveDraft(draft: StrengthProgrammeDraft, expectedRevision: Int) =
        drafts.saveDraft(draft, expectedRevision)

    override fun deleteDraft(id: String, expectedRevision: Int) =
        drafts.deleteDraft(id, expectedRevision)

    override fun customExercise(id: String): CustomStrengthExercise? = records.customExercise(id)

    override fun customExercises(): List<CustomStrengthExercise> = records.customExercises()

    override fun saveCustom(exercise: CustomStrengthExercise, expected: CustomStrengthExercise?) =
        drafts.saveCustom(exercise, expected)

    override fun decisions(): List<Decision> = records.decisions()

    override fun activate(
        proposed: StrengthProgramme,
        expected: Reference?,
        decisionId: String,
        substitutions: List<StrengthProgrammeEditor.Change>,
    ): Decision = activateInternal(proposed, expected, decisionId, substitutions, null)

    override fun activateDraft(
        proposed: StrengthProgramme,
        expected: Reference?,
        decisionId: String,
        reviewed: StrengthProgrammeDraft,
    ): Decision = activateInternal(proposed, expected, decisionId, emptyList(), reviewed)

    private fun activateInternal(
        proposed: StrengthProgramme,
        expected: Reference?,
        decisionId: String,
        substitutions: List<StrengthProgrammeEditor.Change>,
        reviewed: StrengthProgrammeDraft?,
    ): Decision =
        database.transaction {
            sqlRequired(decisionId, "decision")
            val resultRef = Reference(Owner.AUTHORED, proposed.id(), proposed.revision())
            val retry = decision(decisionId)
            if (retry != null) {
                require(
                    retry.before() == expected &&
                        retry.after() == resultRef &&
                        records.currentReference() == resultRef &&
                        records.programme(proposed.id(), proposed.revision()) == proposed &&
                        retry.substitutions() == substitutions
                ) {
                    "The programme or decision changed. Reload and review."
                }
                return@transaction retry
            }
            if (reviewed != null) {
                require(records.draft(reviewed.id()) == reviewed) { "The reviewed draft changed." }
                val custom = records.customExercises().associateBy { it.exercise().id() }
                require(proposed == reviewed.preview(proposed.createdAt(), exclusions(), custom)) {
                    "Reviewed draft and programme preview differ."
                }
            }
            require(records.currentReference() == expected) {
                "The current programme changed. Reload and review."
            }
            require(
                !legacyId(proposed.id()) &&
                    !proposed.createdAt().isAfter(now()) &&
                    proposed.effectiveFrom().toEpochDay() >= today().toEpochDay()
            ) {
                "Review a current, unambiguous programme change."
            }
            val forward =
                expected != null &&
                    expected.owner() == Owner.AUTHORED &&
                    expected.id() == proposed.id()
            if (forward) {
                val executionRecords = SqliteGymExecutionRecords(database)
                for (summary in executionRecords.unfinished()) {
                    val source = executionRecords.load(summary.id()!!)!!.execution().source()
                    require(
                        source == null ||
                            source.kind() != GymExecution.SourceKind.PROGRAMME ||
                            source.id() != proposed.id()
                    ) {
                        "Finish or discard the current gym workout before activating changes to its programme. Its sets and prescription remain preserved."
                    }
                }
            }
            val review = if (forward) records.reviewRevision(expected!!) else null
            val before = review?.programme()
            val protected = review?.completedWorkoutIds() ?: emptySet()
            if (before != null)
                StrengthProgrammeEditor.Proposal(before, proposed, protected, substitutions)
            else
                require(proposed.revision() == 1 && substitutions.isEmpty()) {
                    "Review an initial programme or a forward revision."
                }
            require(records.programme(proposed.id(), proposed.revision()) == null) {
                "This programme revision is already retained."
            }
            eligible(proposed, before, protected, substitutions)
            val accepted =
                Decision(
                    decisionId,
                    expected,
                    resultRef,
                    now(),
                    proposed.reason(),
                    protected.sorted(),
                    substitutions,
                )
            insert(proposed)
            writeDecision(accepted)
            database.execute("DELETE FROM gym_current WHERE id=1")
            database.execute(
                "INSERT OR REPLACE INTO strength_programme_current(id,programme_id,revision) VALUES(1,?,?)",
                listOf(SqlValue.Text(proposed.id()), SqlValue.Integer(proposed.revision().toLong())),
            )
            accepted
        }

    override fun stop(expected: Reference, decisionId: String): Decision =
        database.transaction {
            sqlRequired(decisionId, "decision")
            val retry = decision(decisionId)
            if (retry != null) {
                require(
                    retry.before() == expected &&
                        retry.after() == null &&
                        records.currentReference() == null
                ) {
                    "The programme or stop decision changed."
                }
                return@transaction retry
            }
            require(records.currentReference() == expected) {
                "The current programme changed. Reload and review."
            }
            val choice =
                Decision(
                    decisionId,
                    expected,
                    null,
                    now(),
                    "Athlete stopped programming; all history is retained.",
                    emptyList(),
                    emptyList(),
                )
            writeDecision(choice)
            database.execute("DELETE FROM strength_programme_current WHERE id=1")
            database.execute("DELETE FROM gym_current WHERE id=1")
            choice
        }

    /**
     * Internal history population, never activation: no clock/current-day/decision generation.
     * Requires the fully validated parent and empty normalized target tables. Existing coaching /
     * legacy archives must already be populated in the same enclosing replacement transaction.
     */
    internal fun populateArchive(parent: UserBackupDataJson.Snapshot) =
        database.transaction {
            for (table in
                listOf(
                    "strength_programme_revisions",
                    "strength_programme_blocks",
                    "strength_programme_sessions",
                    "strength_programme_current",
                    "strength_programme_decisions",
                    "strength_programme_drafts",
                    "strength_programme_draft_blocks",
                    "strength_custom_exercises",
                    "strength_concrete_drafts",
                    "strength_concrete_draft_blocks",
                    "strength_concrete_draft_workouts",
                )) require(database.query("SELECT 1 FROM $table LIMIT 1").isEmpty()) {
                "Programming restore requires empty prepared tables; existing data was retained."
            }
            val graph = parent.programming ?: return@transaction
            require(
                graph.current == null ||
                    database.query("SELECT 1 FROM gym_current LIMIT 1").isEmpty()
            ) {
                "Two current strength programmes."
            }
            for (programme in graph.programmes.values) insert(programme)
            drafts.populateArchive(graph)
            for (decision in graph.decisions) writeDecision(decision)
            graph.current?.let { reference ->
                database.execute(
                    "INSERT INTO strength_programme_current VALUES(1,?,?)",
                    listOf(
                        SqlValue.Text(reference.id()),
                        SqlValue.Integer(reference.revision().toLong()),
                    ),
                )
            }
        }

    internal fun decision(id: String): Decision? =
        database
            .query(
                "SELECT payload FROM strength_programme_decisions WHERE id=?",
                listOf(SqlValue.Text(id)),
            )
            .firstOrNull()
            ?.let { StrengthProgrammingDecisionJson.decode(it.single().sqlText()) }

    private fun exclusions(): Set<String> =
        SqliteExerciseExclusions(database).exclusions().map { it.exerciseId() }.toSet() +
            database.query("SELECT exercise_id FROM strength_user_state WHERE archived=1").map {
                it.single().sqlText()
            }

    private fun legacyId(id: String): Boolean =
        database
            .query(
                "SELECT 1 FROM gym_plan_versions WHERE plan_id=? LIMIT 1",
                listOf(SqlValue.Text(id)),
            )
            .isNotEmpty()

    private fun eligible(
        p: StrengthProgramme,
        previous: StrengthProgramme?,
        protected: Set<String>,
        substitutions: List<StrengthProgrammeEditor.Change>,
    ) {
        val old = previous?.workouts()?.associateBy { it.id() } ?: emptyMap()
        val excluded = exclusions()
        for (change in substitutions) {
            val original =
                old.getValue(change.workoutId()).definition().slots().first {
                    it.id() == change.slotId()
                }
            val context =
                ExerciseSubstitutionEngine.Context(
                    original.exercise(),
                    p.settings().goal(),
                    p.settings().experience(),
                    p.settings().equipment(),
                    excluded,
                    emptySet(),
                    emptySet(),
                    ExerciseSubstitutionEngine.Reason.ALTERNATIVES,
                )
            require(
                ExerciseSubstitutionEngine.rank(context, 50).any {
                    it.exercise().id() == change.toExerciseId()
                }
            ) {
                "Replacement must retain programme intent and remain available."
            }
        }
        for (w in p.workouts()) {
            if (
                w.date().toEpochDay() < p.effectiveFrom().toEpochDay() ||
                    w.id() in protected ||
                    (previous != null &&
                        w == old[w.id()] &&
                        previous.settings().equipment() == p.settings().equipment())
            )
                continue
            require(w.date().getDayOfWeek() in p.settings().days()) {
                "Future workouts must respect available days."
            }
            for (slot in w.definition().slots()) {
                require(slot.target() != null && slot.exercise().id() !in excluded) {
                    "Review a target and the excluded exercise choices."
                }
                val metadata = ExerciseMetadataCatalog.find(slot.exercise().id())
                if (metadata != null)
                    require(
                        ExerciseCatalog.definition(slot.exercise().id())!!.exercise() ==
                            slot.exercise() &&
                            metadata.equipment().availableWith(p.settings().equipment())
                    ) {
                        "Review the canonical exercise setup."
                    }
                else {
                    val custom = records.customExercise(slot.exercise().id())
                    require(
                        custom != null &&
                            custom.exercise() == slot.exercise() &&
                            custom.equipment().availableWith(p.settings().equipment())
                    ) {
                        "Review the custom exercise setup."
                    }
                }
            }
            require(
                w.expectedMinutes() == StrengthWorkoutBudget.workoutMinutes(w.definition().slots())
            ) {
                "Review the changed workout time estimate."
            }
        }
    }

    private fun insert(p: StrengthProgramme) {
        StrengthProgrammingArchiveJson.validateRows(p)
        val root = StrengthProgrammingPayloadJson.element(p)
        database.execute(
            "INSERT INTO strength_programme_revisions(programme_id,revision,parent_revision,name,start_date,end_date,header) VALUES(?,?,?,?,?,?,?)",
            listOf(
                SqlValue.Text(p.id()),
                SqlValue.Integer(p.revision().toLong()),
                SqlValue.Integer(p.parentRevision().toLong()),
            ) +
                texts(
                    p.name(),
                    p.start().toString(),
                    p.end().toString(),
                    bounded(JsonObject(root - "blocks"), 262144),
                ),
        )
        for ((i, value) in array(root, "blocks", 12).withIndex()) {
            val block = row(value)
            val id = text(block, "id", 256)
            database.execute(
                "INSERT INTO strength_programme_blocks(programme_id,revision,position,block_id,header) VALUES(?,?,?,?,?)",
                listOf(
                    SqlValue.Text(p.id()),
                    SqlValue.Integer(p.revision().toLong()),
                    SqlValue.Integer(i.toLong()),
                ) + texts(id, bounded(JsonObject(block - "workouts"), 262144)),
            )
            for ((n, value) in array(block, "workouts", 156).withIndex()) {
                val workout = row(value)
                database.execute(
                    "INSERT INTO strength_programme_sessions(programme_id,revision,block_id,position,session_id,session_date,payload) VALUES(?,?,?,?,?,?,?)",
                    listOf(
                        SqlValue.Text(p.id()),
                        SqlValue.Integer(p.revision().toLong()),
                        SqlValue.Text(id),
                        SqlValue.Integer(n.toLong()),
                    ) +
                        texts(
                            text(workout, "id", 256),
                            text(workout, "date", 64),
                            bounded(workout, 262144),
                        ),
                )
            }
        }
    }

    private fun writeDecision(value: Decision) {
        val position =
            database
                .query("SELECT COALESCE(MAX(position),0)+1 FROM strength_programme_decisions")
                .single()
                .single()
                .sqlLong()
        database.execute(
            "INSERT INTO strength_programme_decisions(id,position,payload) VALUES(?,?,?)",
            listOf(
                SqlValue.Text(value.id()),
                SqlValue.Integer(position),
                SqlValue.Text(bounded(StrengthProgrammingDecisionJson.element(value), 524288)),
            ),
        )
    }

    private fun texts(vararg values: String): List<SqlValue> = values.map { SqlValue.Text(it) }

    private fun bounded(value: JsonObject, maximum: Int): String =
        write(value).also {
            require(it.encodeToByteArray().size <= maximum) { "Authoring row is too large." }
        }
}
